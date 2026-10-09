package com.crowdfund.service;

import com.crowdfund.dto.PaymentIntent;
import com.crowdfund.dto.PaymentRequest;
import com.crowdfund.entity.*;
import com.crowdfund.exception.BusinessRuleException;
import com.crowdfund.exception.ResourceNotFoundException;
import com.crowdfund.repository.ActivityLogRepository;
import com.crowdfund.repository.CampaignRepository;
import com.crowdfund.repository.ContributionRepository;
import com.crowdfund.repository.UserRepository;
import com.crowdfund.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class SimulatedPaymentService implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(SimulatedPaymentService.class);

    private final ContributionRepository contributionRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;
    private final CampaignService campaignService;

    @Value("${crowdfund.payment.webhook.secret:sec_live_9b4e2f1807d84a56c0e3952f1a67d0}")
    private String webhookSecret;

    public SimulatedPaymentService(ContributionRepository contributionRepository,
                                   CampaignRepository campaignRepository,
                                   UserRepository userRepository,
                                   ActivityLogRepository activityLogRepository,
                                   CampaignService campaignService) {
        this.contributionRepository = contributionRepository;
        this.campaignRepository = campaignRepository;
        this.userRepository = userRepository;
        this.activityLogRepository = activityLogRepository;
        this.campaignService = campaignService;
    }

    @Override
    public PaymentIntent createPaymentIntent(PaymentRequest request, String contributorEmail) {
        if (request == null || request.getCampaignId() == null) {
            throw new BusinessRuleException("Valid campaign ID is required.");
        }

        InputSanitizer.validateAmount("Contribution amount", request.getAmount(),
                BigDecimal.valueOf(1.00), BigDecimal.valueOf(1000000.00));

        Campaign campaign = campaignRepository.findById(request.getCampaignId())
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + request.getCampaignId()));

        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new BusinessRuleException("Cannot contribute to a campaign that is not ACTIVE.");
        }

        String idempotencyKey = request.getIdempotencyKey();
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = "idem_" + UUID.randomUUID();
        }

        // Check if an existing contribution with this idempotency key exists
        Optional<Contribution> existing = contributionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Contribution c = existing.get();
            return new PaymentIntent(c.getTransactionId(), "secret_" + c.getTransactionId(),
                    c.getAmount(), "INR", c.getStatus().name(), c.getIdempotencyKey());
        }

        String transactionId = "txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String clientSecret = "cs_" + UUID.randomUUID().toString().replace("-", "");

        return new PaymentIntent(transactionId, clientSecret, request.getAmount(),
                "INR", "PENDING", idempotencyKey);
    }

    @Override
    @Transactional
    public Contribution processSuccessfulPayment(Long campaignId, String contributorEmail, BigDecimal amount,
                                                 String paymentMethod, String transactionId, String idempotencyKey) {

        // Idempotency check: prevent duplicate charges
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Contribution> existing = contributionRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent payment hit for key {}: returning existing contribution.", idempotencyKey);
                return existing.get();
            }
        }

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));

        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new BusinessRuleException("Cannot process payment for inactive campaign.");
        }

        User contributor = userRepository.findByEmail(contributorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + contributorEmail));

        Contribution contribution = new Contribution();
        contribution.setCampaign(campaign);
        contribution.setContributor(contributor);
        contribution.setAmount(amount);
        contribution.setPaymentMethod(paymentMethod != null ? paymentMethod : "UPI");
        contribution.setTransactionId(transactionId != null ? transactionId : "txn_" + UUID.randomUUID());
        contribution.setIdempotencyKey(idempotencyKey != null ? idempotencyKey : "idem_" + UUID.randomUUID());
        contribution.setStatus(ContributionStatus.SUCCESS);

        contribution = contributionRepository.save(contribution);

        // Atomically recalculate campaign raised amount
        campaignService.recalculateRaisedAmount(campaign);

        // Activity log
        activityLogRepository.save(new ActivityLog("PAYMENT_SUCCESS",
                String.format("Payment succeeded: %s contributed ₹%s to '%s' (Tx: %s)",
                        contributor.getName(), amount, campaign.getTitle(), contribution.getTransactionId()),
                contributor));

        log.info("Successfully processed payment {} for campaign {}", contribution.getTransactionId(), campaignId);
        return contribution;
    }

    @Override
    @Transactional
    public Contribution processFailedPayment(Long campaignId, String contributorEmail, BigDecimal amount,
                                             String paymentMethod, String transactionId, String idempotencyKey) {

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));

        User contributor = userRepository.findByEmail(contributorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + contributorEmail));

        Contribution contribution = new Contribution();
        contribution.setCampaign(campaign);
        contribution.setContributor(contributor);
        contribution.setAmount(amount);
        contribution.setPaymentMethod(paymentMethod != null ? paymentMethod : "UPI");
        contribution.setTransactionId(transactionId != null ? transactionId : "txn_" + UUID.randomUUID());
        contribution.setIdempotencyKey(idempotencyKey != null ? idempotencyKey : "idem_" + UUID.randomUUID());
        contribution.setStatus(ContributionStatus.FAILED);

        contribution = contributionRepository.save(contribution);

        activityLogRepository.save(new ActivityLog("PAYMENT_FAILED",
                String.format("Payment failed: %s attempted ₹%s to '%s'",
                        contributor.getName(), amount, campaign.getTitle()),
                contributor));

        return contribution;
    }

    @Override
    public boolean verifyWebhookSignature(String rawPayload, String signatureHeader) {
        if (signatureHeader == null || rawPayload == null) {
            return false;
        }
        try {
            String expected = computeSignature(rawPayload);
            // Constant-time comparison to prevent timing side-channel attacks
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.trim().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Error computing webhook signature: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String computeSignature(String rawPayload) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256", e);
        }
    }
}
