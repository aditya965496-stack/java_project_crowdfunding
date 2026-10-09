package com.crowdfund.service;

import com.crowdfund.concurrency.ConcurrentPaymentVerificationQueue;
import com.crowdfund.dao.jdbc.ContributionJdbcDao;
import com.crowdfund.entity.*;
import com.crowdfund.exception.BusinessRuleException;
import com.crowdfund.exception.PaymentVerificationException;
import com.crowdfund.repository.ContributionRepository;
import com.crowdfund.service.payment.PaymentContext;
import com.crowdfund.service.payment.PaymentExecutionResult;
import com.crowdfund.service.payment.PaymentProcessor;
import com.crowdfund.service.payment.PaymentProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Handles the contribution flow: UPI submission with UTR & screenshot,
 * admin verification lifecycle, multithreaded queueing, and JDBC audit synchronization.
 */
@Service
@Transactional
public class ContributionService {

    private static final Logger log = LoggerFactory.getLogger(ContributionService.class);

    private final ContributionRepository contributionRepository;
    private final CampaignService campaignService;
    private final SystemSettingService systemSettingService;
    private final ActivityLogService activityLogService;
    private final PaymentProcessorFactory paymentProcessorFactory;
    private final ConcurrentPaymentVerificationQueue verificationQueue;
    private final ContributionJdbcDao contributionJdbcDao;

    public ContributionService(ContributionRepository contributionRepository,
                               CampaignService campaignService,
                               SystemSettingService systemSettingService,
                               ActivityLogService activityLogService,
                               PaymentProcessorFactory paymentProcessorFactory,
                               ConcurrentPaymentVerificationQueue verificationQueue,
                               ContributionJdbcDao contributionJdbcDao) {
        this.contributionRepository = contributionRepository;
        this.campaignService = campaignService;
        this.systemSettingService = systemSettingService;
        this.activityLogService = activityLogService;
        this.paymentProcessorFactory = paymentProcessorFactory;
        this.verificationQueue = verificationQueue;
        this.contributionJdbcDao = contributionJdbcDao;
    }

    /**
     * Submits a UPI payment contribution requiring admin verification with UTR and payment screenshot.
     */
    public Contribution submitUpiContribution(Long campaignId, User contributor,
                                              BigDecimal amount, String utrNumber,
                                              String screenshotPath) {
        Campaign campaign = campaignService.findById(campaignId);

        validateContributionEligibility(campaign, contributor, amount);

        if (utrNumber == null || utrNumber.trim().length() < 6) {
            throw new PaymentVerificationException("A valid 12-digit UPI UTR (Transaction Reference) is required.");
        }
        String cleanUtr = utrNumber.trim();

        if (contributionRepository.findByUtrNumber(cleanUtr).isPresent()) {
            throw new PaymentVerificationException("This UTR number (" + cleanUtr + ") has already been submitted.");
        }

        if (screenshotPath == null || screenshotPath.trim().isEmpty()) {
            throw new PaymentVerificationException("Please upload a payment proof screenshot before submitting.");
        }

        // Polymorphic processing via paymentProcessorFactory
        PaymentProcessor processor = paymentProcessorFactory.getProcessor("UPI");
        PaymentContext context = new PaymentContext(
                campaignId, contributor.getId(), amount, "UPI",
                "adityalive49@oksbi", cleanUtr, screenshotPath, Collections.emptyMap()
        );
        PaymentExecutionResult execResult = processor.processPayment(context);

        String txId = execResult.getTransactionId();
        Contribution contribution = new Contribution();
        contribution.setCampaign(campaign);
        contribution.setContributor(contributor);
        contribution.setAmount(amount);
        contribution.setPaymentMethod("UPI");
        contribution.setUpiId("adityalive49@oksbi");
        contribution.setUtrNumber(cleanUtr);
        contribution.setPaymentScreenshotPath(screenshotPath);
        contribution.setTransactionId(txId);
        contribution.setIdempotencyKey("IDEM-" + txId);
        contribution.setStatus(execResult.getResultingStatus()); // PENDING_VERIFICATION

        contribution = contributionRepository.save(contribution);

        // Submit to multithreaded background queue for asynchronous processing & audit
        verificationQueue.submitVerificationTask(contribution.getId(), cleanUtr, "SUBMITTED_FOR_VERIFICATION");

        activityLogService.log("PAYMENT_SUBMITTED",
                contributor.getName() + " submitted UPI payment of ₹" + amount +
                        " (UTR: " + cleanUtr + ") for campaign: " + campaign.getTitle(), contributor);

        log.info("UPI Contribution submitted: id={}, contributor={}, amount={}, UTR={}",
                contribution.getId(), contributor.getEmail(), amount, cleanUtr);

        return contribution;
    }

    /**
     * Admin verifies and approves or rejects a submitted payment.
     */
    public Contribution verifyPayment(Long contributionId, User adminUser, boolean approve, String adminNotes) {
        if (adminUser == null || adminUser.getRole() != Role.ADMIN) {
            throw new BusinessRuleException("Only administrators can verify payment transactions.");
        }

        Contribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new BusinessRuleException("Contribution not found with ID: " + contributionId));

        if (contribution.getStatus() != ContributionStatus.PENDING_VERIFICATION) {
            throw new BusinessRuleException("Contribution status is already " + contribution.getStatus() +
                    " and cannot be modified.");
        }

        LocalDateTime now = LocalDateTime.now();
        contribution.setVerifiedBy(adminUser);
        contribution.setVerifiedAt(now);
        contribution.setAdminNotes(adminNotes != null ? adminNotes.trim() : null);

        if (approve) {
            contribution.setStatus(ContributionStatus.SUCCESS);
            contribution = contributionRepository.save(contribution);

            // Recalculate campaign totals so raisedAmount updates immediately
            campaignService.recalculateRaisedAmount(contribution.getCampaign());

            // Synchronize with raw JDBC DAO to demonstrate JDBC update operations
            try {
                contributionJdbcDao.updatePaymentVerification(contributionId, ContributionStatus.SUCCESS,
                        adminNotes, adminUser.getId(), now);
            } catch (Exception e) {
                log.warn("JDBC DAO sync log: {}", e.getMessage());
            }

            verificationQueue.submitVerificationTask(contributionId, contribution.getUtrNumber(), "APPROVED");

            activityLogService.log("PAYMENT_VERIFIED",
                    "Admin " + adminUser.getName() + " approved payment of ₹" + contribution.getAmount() +
                            " (UTR: " + contribution.getUtrNumber() + ") for " + contribution.getCampaign().getTitle(),
                    adminUser);
        } else {
            contribution.setStatus(ContributionStatus.REJECTED);
            contribution = contributionRepository.save(contribution);

            // Synchronize with raw JDBC DAO
            try {
                contributionJdbcDao.updatePaymentVerification(contributionId, ContributionStatus.REJECTED,
                        adminNotes, adminUser.getId(), now);
            } catch (Exception e) {
                log.warn("JDBC DAO sync log: {}", e.getMessage());
            }

            verificationQueue.submitVerificationTask(contributionId, contribution.getUtrNumber(), "REJECTED");

            activityLogService.log("PAYMENT_REJECTED",
                    "Admin " + adminUser.getName() + " rejected payment (UTR: " + contribution.getUtrNumber() +
                            "). Reason: " + (adminNotes != null ? adminNotes : "None"),
                    adminUser);
        }

        return contribution;
    }

    /**
     * Standard contribution method for backward compatibility.
     */
    public Contribution contribute(Long campaignId, User contributor,
                                   BigDecimal amount, String paymentMethod) {
        return contribute(campaignId, contributor, amount, paymentMethod, null);
    }

    /**
     * Processes a contribution with an optional idempotency key.
     */
    public Contribution contribute(Long campaignId, User contributor,
                                   BigDecimal amount, String paymentMethod, String idempotencyKey) {
        Campaign campaign = campaignService.findById(campaignId);
        validateContributionEligibility(campaign, contributor, amount);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            java.util.Optional<Contribution> existing = contributionRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent contribution return for key: {}", idempotencyKey);
                return existing.get();
            }
        }

        String txId = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String finalIdemKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : "IDEM-" + txId;

        Contribution contribution = new Contribution();
        contribution.setCampaign(campaign);
        contribution.setContributor(contributor);
        contribution.setAmount(amount);
        contribution.setPaymentMethod(paymentMethod != null ? paymentMethod : "UPI");
        contribution.setTransactionId(txId);
        contribution.setIdempotencyKey(finalIdemKey);
        contribution.setStatus(ContributionStatus.SUCCESS);
        contribution = contributionRepository.save(contribution);

        // Recalculate campaign totals
        campaignService.recalculateRaisedAmount(campaign);

        activityLogService.log("CONTRIBUTION",
                contributor.getName() + " contributed ₹" + amount +
                        " to " + campaign.getTitle() + " (Tx: " + txId + ")", contributor);

        return contribution;
    }

    private void validateContributionEligibility(Campaign campaign, User contributor, BigDecimal amount) {
        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "Contributions are only accepted for active campaigns. Current status: " + campaign.getStatus());
        }

        if (campaign.getDeadline().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("This campaign's deadline has passed");
        }

        if (contributor == null || !contributor.isActive()) {
            throw new BusinessRuleException("User account is inactive or invalid");
        }

        BigDecimal minAmount = systemSettingService.getMinContributionAmount();
        if (amount == null || amount.compareTo(minAmount) < 0) {
            throw new BusinessRuleException("Minimum contribution amount is ₹" + minAmount);
        }
    }

    @Transactional(readOnly = true)
    public List<Contribution> getPendingVerificationContributions() {
        return contributionRepository.findByStatusOrderByContributedAtDesc(ContributionStatus.PENDING_VERIFICATION);
    }

    @Transactional(readOnly = true)
    public List<Contribution> getContributionsByStatus(ContributionStatus status) {
        return contributionRepository.findByStatusOrderByContributedAtDesc(status);
    }

    @Transactional(readOnly = true)
    public List<Contribution> getAllContributions() {
        return contributionRepository.findAllByOrderByContributedAtDesc();
    }

    @Transactional(readOnly = true)
    public long countPendingVerifications() {
        return contributionRepository.countByStatus(ContributionStatus.PENDING_VERIFICATION);
    }

    @Transactional(readOnly = true)
    public Page<Contribution> findByContributor(User contributor, Pageable pageable) {
        return contributionRepository.findByContributor(contributor, pageable);
    }

    @Transactional(readOnly = true)
    public List<Contribution> findByContributor(User contributor) {
        return contributionRepository.findByContributor(contributor);
    }

    @Transactional(readOnly = true)
    public List<Contribution> findByCampaign(Campaign campaign) {
        return contributionRepository.findByCampaign(campaign);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalByContributor(User contributor) {
        return contributionRepository.sumByContributor(contributor);
    }

    @Transactional(readOnly = true)
    public List<Campaign> campaignsSupportedBy(User contributor) {
        return contributionRepository.findCampaignsByContributor(contributor);
    }

    @Transactional(readOnly = true)
    public long count() {
        return contributionRepository.count();
    }
}
