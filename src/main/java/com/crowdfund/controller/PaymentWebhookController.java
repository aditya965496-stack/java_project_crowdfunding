package com.crowdfund.controller;

import com.crowdfund.dto.PaymentWebhookPayload;
import com.crowdfund.entity.Contribution;
import com.crowdfund.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Enterprise Webhook receiver for Payment Gateway callbacks.
 * Protected by HMAC-SHA256 signature verification and rate limiting.
 */
@RestController
@RequestMapping("/api/webhooks")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public PaymentWebhookController(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/payment", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handlePaymentWebhook(
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestBody String rawPayload) {

        if (signature == null || signature.isBlank()) {
            log.warn("Webhook rejected: missing X-Webhook-Signature header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Missing required X-Webhook-Signature header"));
        }

        if (!paymentService.verifyWebhookSignature(rawPayload, signature)) {
            log.warn("Webhook rejected: signature verification failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Invalid webhook signature"));
        }

        try {
            PaymentWebhookPayload payload = objectMapper.readValue(rawPayload, PaymentWebhookPayload.class);
            if (payload == null || payload.getEventType() == null || payload.getData() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Bad Request", "message", "Malformed webhook payload"));
            }

            PaymentWebhookPayload.EventData data = payload.getData();

            if ("payment.succeeded".equalsIgnoreCase(payload.getEventType())) {
                Contribution c = paymentService.processSuccessfulPayment(
                        data.getCampaignId(),
                        data.getContributorEmail(),
                        data.getAmount(),
                        data.getPaymentMethod(),
                        data.getTransactionId(),
                        data.getIdempotencyKey()
                );
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "received", true,
                        "transactionId", c.getTransactionId(),
                        "contributionId", c.getId()
                ));
            } else if ("payment.failed".equalsIgnoreCase(payload.getEventType())) {
                Contribution c = paymentService.processFailedPayment(
                        data.getCampaignId(),
                        data.getContributorEmail(),
                        data.getAmount(),
                        data.getPaymentMethod(),
                        data.getTransactionId(),
                        data.getIdempotencyKey()
                );
                return ResponseEntity.ok(Map.of(
                        "status", "failed",
                        "received", true,
                        "transactionId", c.getTransactionId()
                ));
            } else {
                log.info("Ignored unknown webhook event: {}", payload.getEventType());
                return ResponseEntity.ok(Map.of("status", "ignored", "reason", "Unhandled event type"));
            }

        } catch (Exception e) {
            log.error("Failed to process payment webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Processing error", "message", e.getMessage()));
        }
    }
}
