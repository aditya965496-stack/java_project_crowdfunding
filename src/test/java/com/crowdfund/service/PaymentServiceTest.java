package com.crowdfund.service;

import com.crowdfund.dto.PaymentIntent;
import com.crowdfund.dto.PaymentRequest;
import com.crowdfund.entity.*;
import com.crowdfund.exception.BusinessRuleException;
import com.crowdfund.repository.ActivityLogRepository;
import com.crowdfund.repository.CampaignRepository;
import com.crowdfund.repository.ContributionRepository;
import com.crowdfund.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private ContributionRepository contributionRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private CampaignService campaignService;

    @InjectMocks
    private SimulatedPaymentService paymentService;

    private Campaign activeCampaign;
    private User contributor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "webhookSecret", "test_secret_key_12345678901234567890");

        contributor = new User("John Doe", "john@example.com", "hash", Role.CONTRIBUTOR);
        contributor.setId(10L);

        activeCampaign = new Campaign();
        activeCampaign.setId(1L);
        activeCampaign.setTitle("Save Forests");
        activeCampaign.setStatus(CampaignStatus.ACTIVE);
        activeCampaign.setGoalAmount(new BigDecimal("50000.00"));
        activeCampaign.setDeadline(LocalDate.now().plusMonths(2));
    }

    @Test
    @DisplayName("should create payment intent successfully")
    void shouldCreatePaymentIntent() {
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(activeCampaign));
        when(contributionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());

        PaymentRequest request = new PaymentRequest(1L, new BigDecimal("500.00"), "UPI", "idem-123");
        PaymentIntent intent = paymentService.createPaymentIntent(request, "john@example.com");

        assertNotNull(intent);
        assertEquals("idem-123", intent.getIdempotencyKey());
        assertEquals(new BigDecimal("500.00"), intent.getAmount());
        assertEquals("PENDING", intent.getStatus());
        assertTrue(intent.getTransactionId().startsWith("txn_"));
    }

    @Test
    @DisplayName("should enforce idempotency when processing payment")
    void shouldEnforceIdempotency() {
        Contribution existing = new Contribution();
        existing.setId(99L);
        existing.setTransactionId("TXN-EXISTING");
        existing.setIdempotencyKey("IDEM-REPEAT");
        existing.setStatus(ContributionStatus.SUCCESS);

        when(contributionRepository.findByIdempotencyKey("IDEM-REPEAT")).thenReturn(Optional.of(existing));

        Contribution result = paymentService.processSuccessfulPayment(
                1L, "john@example.com", new BigDecimal("100.00"), "UPI", "TXN-NEW", "IDEM-REPEAT");

        assertEquals(99L, result.getId());
        assertEquals("TXN-EXISTING", result.getTransactionId());
        verify(contributionRepository, never()).save(any());
        verify(campaignService, never()).recalculateRaisedAmount(any());
    }

    @Test
    @DisplayName("should calculate and verify HMAC-SHA256 signatures correctly")
    void shouldVerifyHmacSignatures() {
        String payload = "{\"event\":\"payment.succeeded\"}";
        String signature = paymentService.computeSignature(payload);

        assertNotNull(signature);
        assertTrue(paymentService.verifyWebhookSignature(payload, signature));
        assertFalse(paymentService.verifyWebhookSignature(payload, "invalid_signature"));
        assertFalse(paymentService.verifyWebhookSignature("tampered_payload", signature));
    }

    @Test
    @DisplayName("should reject payment processing for inactive campaign")
    void shouldRejectInactiveCampaign() {
        activeCampaign.setStatus(CampaignStatus.PENDING);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(activeCampaign));

        assertThrows(BusinessRuleException.class, () ->
                paymentService.processSuccessfulPayment(
                        1L, "john@example.com", new BigDecimal("100.00"), "UPI", "TXN-1", "IDEM-1"));
    }
}
