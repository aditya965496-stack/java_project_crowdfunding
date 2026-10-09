package com.crowdfund.service;

import com.crowdfund.dto.PaymentIntent;
import com.crowdfund.dto.PaymentRequest;
import com.crowdfund.entity.Contribution;

import java.math.BigDecimal;

public interface PaymentService {

    /**
     * Initializes a payment intent with a simulated payment gateway.
     */
    PaymentIntent createPaymentIntent(PaymentRequest request, String contributorEmail);

    /**
     * Confirms and persists a successful payment callback/webhook.
     * Enforces strict idempotency.
     */
    Contribution processSuccessfulPayment(Long campaignId, String contributorEmail, BigDecimal amount,
                                          String paymentMethod, String transactionId, String idempotencyKey);

    /**
     * Records a failed payment callback/webhook.
     */
    Contribution processFailedPayment(Long campaignId, String contributorEmail, BigDecimal amount,
                                      String paymentMethod, String transactionId, String idempotencyKey);

    /**
     * Validates HMAC-SHA256 signature from webhook requests.
     */
    boolean verifyWebhookSignature(String rawPayload, String signatureHeader);

    /**
     * Computes HMAC-SHA256 signature for payload using the configured webhook secret.
     */
    String computeSignature(String rawPayload);
}
