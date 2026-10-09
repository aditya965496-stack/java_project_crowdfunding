package com.crowdfund.exception;

/**
 * Thrown when a requested resource (user, campaign, etc.) is not found.
 */
public class ResourceNotFoundException extends CrowdFundException {

    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " not found with id: " + id, "RESOURCE_NOT_FOUND");
    }
}
