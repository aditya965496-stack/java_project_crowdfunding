package com.crowdfund.service.payment;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory demonstrating polymorphism by dynamically selecting appropriate PaymentProcessor at runtime.
 */
@Component
public class PaymentProcessorFactory {

    private final List<PaymentProcessor> processors;

    public PaymentProcessorFactory(List<PaymentProcessor> processors) {
        this.processors = processors;
    }

    public PaymentProcessor getProcessor(String paymentMethod) {
        return processors.stream()
                .filter(p -> p.supports(paymentMethod))
                .findFirst()
                .orElseGet(() -> processors.stream()
                        .filter(p -> p instanceof UpiPaymentProcessor)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No payment processor available")));
    }
}
