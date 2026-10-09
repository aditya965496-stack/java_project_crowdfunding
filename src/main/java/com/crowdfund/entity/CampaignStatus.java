package com.crowdfund.entity;

/**
 * Campaign lifecycle states.
 * <pre>
 *   PENDING ──► APPROVED ──► ACTIVE ──► COMPLETED
 *       │                        └────► EXPIRED
 *       └──► REJECTED
 * </pre>
 * A campaign transitions from APPROVED to ACTIVE when it is first
 * publicly visible. COMPLETED occurs when the goal is met; EXPIRED
 * occurs when the deadline passes without meeting the goal.
 */
public enum CampaignStatus {
    PENDING,
    APPROVED,
    ACTIVE,
    COMPLETED,
    EXPIRED,
    REJECTED
}
