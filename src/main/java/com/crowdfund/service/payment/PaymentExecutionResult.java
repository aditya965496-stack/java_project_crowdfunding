package com.crowdfund.service.payment;

import com.crowdfund.entity.ContributionStatus;

/**
 * Result object returning polymorphic payment execution outcomes.
 */
public class PaymentExecutionResult {

    private final boolean successful;
    private final ContributionStatus resultingStatus;
    private final String transactionId;
    private final String message;

    public PaymentExecutionResult(boolean successful, ContributionStatus resultingStatus,
                                  String transactionId, String message) {
        this.successful = successful;
        this.resultingStatus = resultingStatus;
        this.transactionId = transactionId;
        this.message = message;
    }

    public static PaymentExecutionResult pendingVerification(String transactionId, String message) {
        return new PaymentExecutionResult(true, ContributionStatus.PENDING_VERIFICATION, transactionId, message);
    }

    public static PaymentExecutionResult success(String transactionId, String message) {
        return new PaymentExecutionResult(true, ContributionStatus.SUCCESS, transactionId, message);
    }

    public static PaymentExecutionResult failure(String message) {
        return new PaymentExecutionResult(false, ContributionStatus.FAILED, null, message);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public ContributionStatus getResultingStatus() {
        return resultingStatus;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getMessage() {
        return message;
    }
}
