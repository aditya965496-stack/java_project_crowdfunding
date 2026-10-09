package com.crowdfund.service;

import com.crowdfund.entity.*;
import com.crowdfund.exception.BusinessRuleException;
import com.crowdfund.exception.ResourceNotFoundException;
import com.crowdfund.repository.CampaignRepository;
import com.crowdfund.repository.CampaignUpdateRepository;
import com.crowdfund.repository.ContributionRepository;
import com.crowdfund.repository.MessageRepository;
import com.crowdfund.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Core business logic for campaigns: creation, approval, progress
 * tracking, updates, and lifecycle management.
 * <p>
 * The scheduled {@link #expireOverdueCampaigns()} method automatically
 * transitions active campaigns past their deadline to EXPIRED.
 * </p>
 */
@Service
@Transactional
public class CampaignService {

    private static final Logger log = LoggerFactory.getLogger(CampaignService.class);

    private final CampaignRepository campaignRepository;
    private final ContributionRepository contributionRepository;
    private final CampaignUpdateRepository updateRepository;
    private final MessageRepository messageRepository;
    private final ActivityLogService activityLogService;

    public CampaignService(CampaignRepository campaignRepository,
                           ContributionRepository contributionRepository,
                           CampaignUpdateRepository updateRepository,
                           MessageRepository messageRepository,
                           ActivityLogService activityLogService) {
        this.campaignRepository = campaignRepository;
        this.contributionRepository = contributionRepository;
        this.updateRepository = updateRepository;
        this.messageRepository = messageRepository;
        this.activityLogService = activityLogService;
    }

    /**
     * Creates a campaign in PENDING status. The creator must have the CREATOR role.
     */
    public Campaign createCampaign(String title, String description,
                                    BigDecimal goalAmount, LocalDate deadline,
                                    String category, String imageUrl, User creator) {
        if (creator == null || (creator.getRole() != Role.CREATOR && creator.getRole() != Role.ADMIN)) {
            throw new BusinessRuleException("Only creators can create campaigns");
        }
        String cleanTitle = InputSanitizer.validateAndSanitize("Campaign title", title, 3, 200);
        String cleanDesc = InputSanitizer.validateAndSanitize("Campaign description", description, 10, 5000);
        InputSanitizer.validateAmount("Goal amount", goalAmount, BigDecimal.valueOf(1.00), BigDecimal.valueOf(100000000.00));
        InputSanitizer.validateFutureDeadline(deadline, 365);

        Campaign campaign = new Campaign();
        campaign.setTitle(cleanTitle);
        campaign.setDescription(cleanDesc);
        campaign.setGoalAmount(goalAmount);
        campaign.setDeadline(deadline);
        campaign.setCategory(category != null ? InputSanitizer.sanitizeText(category) : "General");
        campaign.setImageUrl(imageUrl);
        campaign.setCreator(creator);
        campaign.setStatus(CampaignStatus.PENDING);
        campaign = campaignRepository.save(campaign);

        activityLogService.log("CAMPAIGN_CREATED",
                "Campaign created: " + cleanTitle, creator);
        log.info("Campaign created: {} by {}", cleanTitle, creator.getEmail());
        return campaign;
    }

    public Campaign createCampaign(String title, String description,
                                    BigDecimal goalAmount, LocalDate deadline,
                                    String category, User creator) {
        return createCampaign(title, description, goalAmount, deadline, category, null, creator);
    }

    @Transactional(readOnly = true)
    public Campaign findById(Long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", id));
    }

    @Transactional(readOnly = true)
    public Page<Campaign> findAll(Pageable pageable) {
        return campaignRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Campaign> findByStatus(CampaignStatus status, Pageable pageable) {
        return campaignRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public List<Campaign> findByCreator(User creator) {
        return campaignRepository.findByCreator(creator);
    }

    @Transactional(readOnly = true)
    public Page<Campaign> findPublicCampaigns(Pageable pageable) {
        return campaignRepository.findPublicCampaigns(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Campaign> searchActiveCampaigns(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return findPublicCampaigns(pageable);
        }
        return campaignRepository.searchActiveCampaigns(search, pageable);
    }

    /**
     * Admin approves a campaign, transitioning it to ACTIVE.
     */
    public Campaign approveCampaign(Long campaignId, User admin) {
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new BusinessRuleException("Only administrators can approve campaigns");
        }
        Campaign campaign = findById(campaignId);
        if (campaign.getStatus() != CampaignStatus.PENDING) {
            throw new BusinessRuleException(
                    "Only pending campaigns can be approved. Current status: " + campaign.getStatus());
        }
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaign.setRejectionReason(null);
        campaign = campaignRepository.save(campaign);

        activityLogService.log("CAMPAIGN_APPROVED",
                "Campaign approved: " + campaign.getTitle(), admin);
        return campaign;
    }

    /**
     * Admin rejects a campaign with an optional reason.
     */
    public Campaign rejectCampaign(Long campaignId, String reason, User admin) {
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new BusinessRuleException("Only administrators can reject campaigns");
        }
        Campaign campaign = findById(campaignId);
        if (campaign.getStatus() != CampaignStatus.PENDING) {
            throw new BusinessRuleException(
                    "Only pending campaigns can be rejected. Current status: " + campaign.getStatus());
        }
        campaign.setStatus(CampaignStatus.REJECTED);
        campaign.setRejectionReason(reason != null ? InputSanitizer.sanitizeText(reason) : null);
        campaign = campaignRepository.save(campaign);

        activityLogService.log("CAMPAIGN_REJECTED",
                "Campaign rejected: " + campaign.getTitle() +
                        (reason != null ? " — " + reason : ""), admin);
        return campaign;
    }

    /**
     * Updates campaign details. Only the campaign's creator or an admin may edit.
     */
    public Campaign updateCampaign(Long campaignId, String title, String description,
                                    String category, String imageUrl, User creator) {
        Campaign campaign = findById(campaignId);
        boolean isOwner = campaign.getCreator().getId().equals(creator.getId());
        boolean isAdmin = creator.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new BusinessRuleException("You can only edit your own campaigns");
        }
        campaign.setTitle(InputSanitizer.validateAndSanitize("Campaign title", title, 3, 200));
        campaign.setDescription(InputSanitizer.validateAndSanitize("Campaign description", description, 10, 5000));
        campaign.setCategory(category != null ? InputSanitizer.sanitizeText(category) : "General");
        if (imageUrl != null) {
            campaign.setImageUrl(imageUrl);
        }
        return campaignRepository.save(campaign);
    }

    public Campaign updateCampaign(Long campaignId, String title, String description,
                                    String category, User creator) {
        return updateCampaign(campaignId, title, description, category, null, creator);
    }

    /**
     * Posts a progress update on a campaign.
     */
    public CampaignUpdate postUpdate(Long campaignId, String content, User creator) {
        Campaign campaign = findById(campaignId);
        if (!campaign.getCreator().getId().equals(creator.getId())) {
            throw new BusinessRuleException("You can only post updates on your own campaigns");
        }
        CampaignUpdate update = new CampaignUpdate();
        update.setCampaign(campaign);
        update.setPostedBy(creator);
        update.setContent(content);
        update = updateRepository.save(update);

        activityLogService.log("CAMPAIGN_UPDATE",
                "Progress update on: " + campaign.getTitle(), creator);
        return update;
    }

    /**
     * Sends a message from the creator to all supporters of a campaign.
     */
    public Message sendMessage(Long campaignId, String content, User sender) {
        Campaign campaign = findById(campaignId);
        if (!campaign.getCreator().getId().equals(sender.getId())) {
            throw new BusinessRuleException("Only the campaign creator can send messages");
        }
        Message message = new Message();
        message.setCampaign(campaign);
        message.setSender(sender);
        message.setContent(content);
        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<CampaignUpdate> getUpdates(Long campaignId) {
        Campaign campaign = findById(campaignId);
        return updateRepository.findByCampaignOrderByPostedAtDesc(campaign);
    }

    @Transactional(readOnly = true)
    public List<CampaignUpdate> getUpdatesForCampaigns(List<Campaign> campaigns) {
        return updateRepository.findByCampaignInOrderByPostedAtDesc(campaigns);
    }

    @Transactional(readOnly = true)
    public List<Message> getMessages(Long campaignId) {
        Campaign campaign = findById(campaignId);
        return messageRepository.findByCampaignOrderBySentAtDesc(campaign);
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesForCampaigns(List<Campaign> campaigns) {
        return messageRepository.findByCampaignInOrderBySentAtDesc(campaigns);
    }

    /**
     * Recalculates the raised amount from contributions and checks for goal completion.
     */
    public void recalculateRaisedAmount(Campaign campaign) {
        BigDecimal total = contributionRepository.sumSuccessfulContributions(campaign);
        campaign.setRaisedAmount(total);

        if (campaign.getStatus() == CampaignStatus.ACTIVE
                && total.compareTo(campaign.getGoalAmount()) >= 0) {
            campaign.setStatus(CampaignStatus.COMPLETED);
            activityLogService.log("CAMPAIGN_COMPLETED",
                    "Campaign goal met: " + campaign.getTitle(), campaign.getCreator());
            log.info("Campaign {} completed — goal met!", campaign.getTitle());
        }
        campaignRepository.save(campaign);
    }

    /**
     * Runs every hour — marks active campaigns past their deadline as EXPIRED.
     */
    @Scheduled(fixedRate = 3600000)
    public void expireOverdueCampaigns() {
        List<Campaign> overdue = campaignRepository.findExpiredCampaigns(
                CampaignStatus.ACTIVE, LocalDate.now());
        for (Campaign c : overdue) {
            c.setStatus(CampaignStatus.EXPIRED);
            campaignRepository.save(c);
            activityLogService.log("CAMPAIGN_EXPIRED",
                    "Campaign expired: " + c.getTitle(), c.getCreator());
            log.info("Campaign expired: {}", c.getTitle());
        }
    }

    @Transactional(readOnly = true)
    public long countByStatus(CampaignStatus status) {
        return campaignRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalRaised() {
        return campaignRepository.totalRaisedAcrossAllCampaigns();
    }

    @Transactional(readOnly = true)
    public long countDistinctContributors(Campaign campaign) {
        return contributionRepository.countDistinctContributors(campaign);
    }
}
