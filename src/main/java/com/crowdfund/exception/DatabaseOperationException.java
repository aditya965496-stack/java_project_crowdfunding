package com.crowdfund.exception;

/**
 * Thrown when low-level JDBC database connectivity or query execution fails.
 */
public class DatabaseOperationException extends CrowdFundException {

    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
