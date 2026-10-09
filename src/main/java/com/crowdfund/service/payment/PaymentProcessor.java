package com.crowdfund.service.payment;

/**
 * Interface demonstrating OOP Polymorphism and Interface segregation.
 * Different payment processors implement distinct transaction workflows.
 */
public interface PaymentProcessor {

    /**
     * Checks whether this processor handles the specified payment method.
     */
    boolean supports(String paymentMethod);

    /**
     * Polymorphic execution of payment logic according to specific payment instrument rules.
     */
    PaymentExecutionResult processPayment(PaymentContext context);
}
