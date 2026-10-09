package com.crowdfund.entity;

/**
 * Tracks the state and lifecycle of a contribution payment.
 */
public enum ContributionStatus {
    SUCCESS,
    FAILED,
    PENDING,
    PENDING_VERIFICATION,
    REJECTED
}
