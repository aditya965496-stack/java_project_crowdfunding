package com.crowdfund.exception;

/**
 * Thrown when a business rule is violated (e.g. contributing to
 * a non-active campaign, duplicate email on registration).
 */
public class BusinessRuleException extends CrowdFundException {

    public BusinessRuleException(String message) {
        super(message, "BUSINESS_RULE_VIOLATION");
    }
}
