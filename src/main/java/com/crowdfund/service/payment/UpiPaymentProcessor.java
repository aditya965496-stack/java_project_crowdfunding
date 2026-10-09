package com.crowdfund.service.payment;

import com.crowdfund.exception.PaymentVerificationException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Concrete polymorphic implementation for UPI Payments.
 * Enforces UPI ID (adityalive49@oksbi), validates UTR numbers, and queues for Admin Portal verification.
 */
@Component
public class UpiPaymentProcessor implements PaymentProcessor {

    public static final String DEFAULT_UPI_ID = "adityalive49@oksbi";

    @Override
    public boolean supports(String paymentMethod) {
        return paymentMethod != null &&
                (paymentMethod.equalsIgnoreCase("UPI") || paymentMethod.equalsIgnoreCase("UPI_QR"));
    }

    @Override
    public PaymentExecutionResult processPayment(PaymentContext context) {
        String utr = context.getUtrNumber();
        if (utr == null || utr.trim().length() < 6) {
            throw new PaymentVerificationException("A valid UTR (UPI Reference) number is required for verification.");
        }

        String screenshot = context.getScreenshotPath();
        if (screenshot == null || screenshot.trim().isEmpty()) {
            throw new PaymentVerificationException("Payment screenshot proof must be uploaded before submitting.");
        }

        String txId = "UPI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return PaymentExecutionResult.pendingVerification(txId,
                "UPI payment of ₹" + context.getAmount() + " with UTR " + utr + " submitted for admin verification.");
    }
}
