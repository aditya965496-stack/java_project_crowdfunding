package com.crowdfund.service;

import com.crowdfund.entity.*;
import com.crowdfund.exception.BusinessRuleException;
import com.crowdfund.repository.CampaignRepository;
import com.crowdfund.repository.CampaignUpdateRepository;
import com.crowdfund.repository.ContributionRepository;
import com.crowdfund.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private ContributionRepository contributionRepository;
    @Mock
    private CampaignUpdateRepository updateRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private CampaignService campaignService;

    private User creator;
    private User admin;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        creator = new User("Creator", "creator@test.com", "hash", Role.CREATOR);
        creator.setId(1L);

        admin = new User("Admin", "admin@test.com", "hash", Role.ADMIN);
        admin.setId(2L);

        campaign = new Campaign();
        campaign.setId(1L);
        campaign.setTitle("Test Campaign");
        campaign.setDescription("Test description for campaign");
        campaign.setGoalAmount(new BigDecimal("10000.00"));
        campaign.setRaisedAmount(BigDecimal.ZERO);
        campaign.setDeadline(LocalDate.now().plusMonths(1));
        campaign.setCategory("Education");
        campaign.setStatus(CampaignStatus.PENDING);
        campaign.setCreator(creator);
    }

    @Nested
    @DisplayName("createCampaign()")
    class CreateCampaign {

        @Test
        @DisplayName("should create a campaign in PENDING status")
        void shouldCreatePendingCampaign() {
            when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> {
                Campaign c = inv.getArgument(0);
                c.setId(1L);
                return c;
            });

            Campaign result = campaignService.createCampaign(
                    "New Campaign", "A valid description here",
                    new BigDecimal("5000"), LocalDate.now().plusMonths(2),
                    "Technology", creator);

            assertEquals(CampaignStatus.PENDING, result.getStatus());
            assertEquals("New Campaign", result.getTitle());
            verify(campaignRepository).save(any(Campaign.class));
        }

        @Test
        @DisplayName("should reject zero goal amount")
        void shouldRejectZeroGoal() {
            assertThrows(BusinessRuleException.class, () ->
                    campaignService.createCampaign("Test", "Description here",
                            BigDecimal.ZERO, LocalDate.now().plusDays(30),
                            "Cat", creator));
        }

        @Test
        @DisplayName("should reject past deadline")
        void shouldRejectPastDeadline() {
            assertThrows(BusinessRuleException.class, () ->
                    campaignService.createCampaign("Test", "Description here",
                            new BigDecimal("1000"), LocalDate.now().minusDays(1),
                            "Cat", creator));
        }

        @Test
        @DisplayName("should reject non-creator role")
        void shouldRejectNonCreator() {
            User contributor = new User("Contrib", "c@test.com", "h", Role.CONTRIBUTOR);
            assertThrows(BusinessRuleException.class, () ->
                    campaignService.createCampaign("Test", "Description here",
                            new BigDecimal("1000"), LocalDate.now().plusDays(30),
                            "Cat", contributor));
        }
    }

    @Nested
    @DisplayName("approveCampaign()")
    class ApproveCampaign {

        @Test
        @DisplayName("should transition PENDING to ACTIVE")
        void shouldApprove() {
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
            when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

            Campaign result = campaignService.approveCampaign(1L, admin);

            assertEquals(CampaignStatus.ACTIVE, result.getStatus());
        }

        @Test
        @DisplayName("should reject approval of non-pending campaign")
        void shouldRejectNonPending() {
            campaign.setStatus(CampaignStatus.ACTIVE);
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

            assertThrows(BusinessRuleException.class, () ->
                    campaignService.approveCampaign(1L, admin));
        }
    }

    @Nested
    @DisplayName("rejectCampaign()")
    class RejectCampaign {

        @Test
        @DisplayName("should transition PENDING to REJECTED with reason")
        void shouldReject() {
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
            when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

            Campaign result = campaignService.rejectCampaign(1L, "Not suitable", admin);

            assertEquals(CampaignStatus.REJECTED, result.getStatus());
            assertEquals("Not suitable", result.getRejectionReason());
        }
    }

    @Nested
    @DisplayName("updateCampaign()")
    class UpdateCampaign {

        @Test
        @DisplayName("owner should be able to edit their campaign")
        void ownerEdits() {
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
            when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

            Campaign result = campaignService.updateCampaign(1L,
                    "Updated Title", "Updated desc", "Health", creator);

            assertEquals("Updated Title", result.getTitle());
        }

        @Test
        @DisplayName("non-owner should be rejected")
        void nonOwnerRejected() {
            User other = new User("Other", "other@test.com", "h", Role.CREATOR);
            other.setId(99L);

            when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

            assertThrows(BusinessRuleException.class, () ->
                    campaignService.updateCampaign(1L, "Title", "Desc", "Cat", other));
        }
    }

    @Nested
    @DisplayName("recalculateRaisedAmount()")
    class Recalculate {

        @Test
        @DisplayName("should mark campaign COMPLETED when goal is met")
        void shouldCompleteWhenGoalMet() {
            campaign.setStatus(CampaignStatus.ACTIVE);
            when(contributionRepository.sumSuccessfulContributions(campaign))
                    .thenReturn(new BigDecimal("10000.00"));
            when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

            campaignService.recalculateRaisedAmount(campaign);

            assertEquals(CampaignStatus.COMPLETED, campaign.getStatus());
            assertEquals(new BigDecimal("10000.00"), campaign.getRaisedAmount());
        }

        @Test
        @DisplayName("should not change status when goal is not met")
        void shouldNotChangeWhenGoalNotMet() {
            campaign.setStatus(CampaignStatus.ACTIVE);
            when(contributionRepository.sumSuccessfulContributions(campaign))
                    .thenReturn(new BigDecimal("5000.00"));
            when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

            campaignService.recalculateRaisedAmount(campaign);

            assertEquals(CampaignStatus.ACTIVE, campaign.getStatus());
        }
    }

    @Nested
    @DisplayName("postUpdate()")
    class PostUpdate {

        @Test
        @DisplayName("creator should post an update on their own campaign")
        void creatorPostsUpdate() {
            when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
            when(updateRepository.save(any(CampaignUpdate.class))).thenAnswer(inv -> {
                CampaignUpdate u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            CampaignUpdate result = campaignService.postUpdate(1L, "Progress!", creator);

            assertNotNull(result);
            assertEquals("Progress!", result.getContent());
        }
    }
}
