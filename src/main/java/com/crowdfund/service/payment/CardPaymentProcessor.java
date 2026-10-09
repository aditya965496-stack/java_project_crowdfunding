package com.crowdfund.service.payment;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Concrete polymorphic implementation for card and netbanking transactions.
 */
@Component
public class CardPaymentProcessor implements PaymentProcessor {

    @Override
    public boolean supports(String paymentMethod) {
        return paymentMethod != null &&
                (paymentMethod.equalsIgnoreCase("Card") ||
                 paymentMethod.equalsIgnoreCase("Net Banking") ||
                 paymentMethod.equalsIgnoreCase("Debit Card") ||
                 paymentMethod.equalsIgnoreCase("Credit Card"));
    }

    @Override
    public PaymentExecutionResult processPayment(PaymentContext context) {
        String txId = "CARD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return PaymentExecutionResult.success(txId, "Payment processed successfully via card gateway.");
    }
}
