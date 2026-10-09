package com.crowdfund.exception;

/**
 * Base abstract exception demonstrating OOP Exception Handling and Inheritance hierarchy.
 */
public abstract class CrowdFundException extends RuntimeException {

    private final String errorCode;

    public CrowdFundException(String message) {
        super(message);
        this.errorCode = "CROWDFUND_ERROR";
    }

    public CrowdFundException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public CrowdFundException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "CROWDFUND_ERROR";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
