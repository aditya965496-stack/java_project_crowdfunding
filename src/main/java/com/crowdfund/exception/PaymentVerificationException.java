package com.crowdfund.exception;

/**
 * Thrown during payment verification failures or invalid UTR/screenshot proofs.
 */
public class PaymentVerificationException extends CrowdFundException {

    public PaymentVerificationException(String message) {
        super(message, "PAYMENT_VERIFICATION_FAILED");
    }
}
