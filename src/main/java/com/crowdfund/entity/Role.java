package com.crowdfund.entity;

/**
 * Defines the three system roles.
 * <p>
 * Admin accounts are seeded at startup. Users self-register as either
 * {@code CREATOR} or {@code CONTRIBUTOR}.
 * </p>
 */
public enum Role {
    ADMIN,
    CREATOR,
    CONTRIBUTOR
}
