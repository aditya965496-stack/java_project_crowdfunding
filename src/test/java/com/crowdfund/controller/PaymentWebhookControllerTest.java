package com.crowdfund.controller;

import com.crowdfund.entity.*;
import com.crowdfund.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookControllerTest {

    @Mock
    private PaymentService paymentService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentWebhookController webhookController;

    private String validPayload;

    @BeforeEach
    void setUp() {
        validPayload = """
            {
              "eventId": "evt_001",
              "eventType": "payment.succeeded",
              "data": {
                "campaignId": 1,
                "contributorEmail": "contributor@test.com",
                "amount": 1000.00,
                "paymentMethod": "UPI",
                "transactionId": "TXN_12345",
                "idempotencyKey": "IDEM_12345"
              }
            }
            """;
    }

    @Test
    @DisplayName("should reject webhook request when signature header is missing")
    void shouldRejectMissingSignature() {
        ResponseEntity<?> response = webhookController.handlePaymentWebhook(null, validPayload);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Missing required X-Webhook-Signature"));
    }

    @Test
    @DisplayName("should reject webhook request when signature is invalid")
    void shouldRejectInvalidSignature() {
        when(paymentService.verifyWebhookSignature(anyString(), anyString())).thenReturn(false);

        ResponseEntity<?> response = webhookController.handlePaymentWebhook("fake_signature", validPayload);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Invalid webhook signature"));
    }

    @Test
    @DisplayName("should process payment.succeeded webhook successfully")
    void shouldProcessPaymentSucceeded() {
        when(paymentService.verifyWebhookSignature(anyString(), anyString())).thenReturn(true);

        Contribution mockContribution = new Contribution();
        mockContribution.setId(42L);
        mockContribution.setTransactionId("TXN_12345");
        mockContribution.setStatus(ContributionStatus.SUCCESS);

        when(paymentService.processSuccessfulPayment(eq(1L), eq("contributor@test.com"),
                eq(new BigDecimal("1000.00")), eq("UPI"), eq("TXN_12345"), eq("IDEM_12345")))
                .thenReturn(mockContribution);

        ResponseEntity<?> response = webhookController.handlePaymentWebhook("valid_sig", validPayload);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("success", body.get("status"));
        assertEquals("TXN_12345", body.get("transactionId"));
    }
}
