package com.crowdfund.service;

import com.crowdfund.entity.*;
import com.crowdfund.exception.BusinessRuleException;
import com.crowdfund.repository.ContributionRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContributionServiceTest {

    @Mock
    private ContributionRepository contributionRepository;
    @Mock
    private CampaignService campaignService;
    @Mock
    private SystemSettingService systemSettingService;
    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private com.crowdfund.service.payment.PaymentProcessorFactory paymentProcessorFactory;
    @Mock
    private com.crowdfund.concurrency.ConcurrentPaymentVerificationQueue verificationQueue;
    @Mock
    private com.crowdfund.dao.jdbc.ContributionJdbcDao contributionJdbcDao;

    @InjectMocks
    private ContributionService contributionService;

    private User contributor;
    private Campaign activeCampaign;

    @BeforeEach
    void setUp() {
        contributor = new User("Contributor", "contrib@test.com", "hash", Role.CONTRIBUTOR);
        contributor.setId(1L);

        User creator = new User("Creator", "creator@test.com", "hash", Role.CREATOR);
        creator.setId(2L);

        activeCampaign = new Campaign();
        activeCampaign.setId(1L);
        activeCampaign.setTitle("Active Campaign");
        activeCampaign.setDescription("Test campaign description");
        activeCampaign.setGoalAmount(new BigDecimal("10000.00"));
        activeCampaign.setRaisedAmount(BigDecimal.ZERO);
        activeCampaign.setDeadline(LocalDate.now().plusMonths(1));
        activeCampaign.setStatus(CampaignStatus.ACTIVE);
        activeCampaign.setCreator(creator);
    }

    @Nested
    @DisplayName("contribute()")
    class Contribute {

        @Test
        @DisplayName("should accept a valid contribution to an active campaign")
        void shouldAcceptValidContribution() {
            when(campaignService.findById(1L)).thenReturn(activeCampaign);
            when(systemSettingService.getMinContributionAmount())
                    .thenReturn(new BigDecimal("5.00"));
            when(contributionRepository.save(any(Contribution.class)))
                    .thenAnswer(inv -> {
                        Contribution c = inv.getArgument(0);
                        c.setId(1L);
                        return c;
                    });

            Contribution result = contributionService.contribute(
                    1L, contributor, new BigDecimal("100.00"), "UPI");

            assertNotNull(result);
            assertEquals(new BigDecimal("100.00"), result.getAmount());
            assertEquals(ContributionStatus.SUCCESS, result.getStatus());
            assertEquals("UPI", result.getPaymentMethod());

            verify(campaignService).recalculateRaisedAmount(activeCampaign);
        }

        @Test
        @DisplayName("should reject contribution to non-active campaign")
        void shouldRejectNonActive() {
            activeCampaign.setStatus(CampaignStatus.PENDING);
            when(campaignService.findById(1L)).thenReturn(activeCampaign);

            assertThrows(BusinessRuleException.class, () ->
                    contributionService.contribute(1L, contributor,
                            new BigDecimal("100.00"), "UPI"));

            verify(contributionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject contribution below minimum amount")
        void shouldRejectBelowMinimum() {
            when(campaignService.findById(1L)).thenReturn(activeCampaign);
            when(systemSettingService.getMinContributionAmount())
                    .thenReturn(new BigDecimal("50.00"));

            assertThrows(BusinessRuleException.class, () ->
                    contributionService.contribute(1L, contributor,
                            new BigDecimal("10.00"), "UPI"));

            verify(contributionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject contribution past deadline")
        void shouldRejectPastDeadline() {
            activeCampaign.setDeadline(LocalDate.now().minusDays(1));
            when(campaignService.findById(1L)).thenReturn(activeCampaign);

            assertThrows(BusinessRuleException.class, () ->
                    contributionService.contribute(1L, contributor,
                            new BigDecimal("100.00"), "UPI"));

            verify(contributionRepository, never()).save(any());
        }
    }
}
