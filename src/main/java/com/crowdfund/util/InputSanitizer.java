package com.crowdfund.util;

import com.crowdfund.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Enterprise utility for rigorous input validation, sanitization,
 * and XSS / injection defense.
 */
public final class InputSanitizer {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>", Pattern.DOTALL);
    private static final Pattern DANGEROUS_ATTRIBUTES = Pattern.compile("(?i)(javascript:|onerror|onload|onclick|onmouseover|eval\\()", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    private InputSanitizer() {
        // utility class
    }

    /**
     * Sanitizes plain text by removing HTML markup, dangerous scripts, and leading/trailing whitespace.
     */
    public static String sanitizeText(String input) {
        if (input == null) {
            return null;
        }
        // Remove entire script and style tags along with their content
        String cleaned = input.replaceAll("(?is)<script.*?</script>", "")
                              .replaceAll("(?is)<style.*?</style>", "");
        // Remove remaining HTML tags
        cleaned = HTML_TAG_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = DANGEROUS_ATTRIBUTES.matcher(cleaned).replaceAll("");
        return cleaned.trim();
    }

    /**
     * Validates that a string is non-empty, within bounds, and contains no dangerous content.
     */
    public static String validateAndSanitize(String fieldName, String value, int minLen, int maxLen) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessRuleException(fieldName + " is required and cannot be blank.");
        }
        String sanitized = sanitizeText(value);
        if (sanitized.length() < minLen || sanitized.length() > maxLen) {
            throw new BusinessRuleException(fieldName + " must be between " + minLen + " and " + maxLen + " characters.");
        }
        return sanitized;
    }

    /**
     * Strict email validation.
     */
    public static String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessRuleException("Email address is required.");
        }
        String cleanEmail = email.trim().toLowerCase();
        if (cleanEmail.length() > 150 || !EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new BusinessRuleException("Please provide a valid, well-formed email address.");
        }
        return cleanEmail;
    }

    /**
     * Password validation: minimum 4 characters, non-blank.
     */
    public static void validatePasswordStrength(String password) {
        if (password == null || password.trim().length() < 4) {
            throw new BusinessRuleException("Password must be at least 4 characters long.");
        }
    }

    /**
     * Monetary amount validation.
     */
    public static BigDecimal validateAmount(String fieldName, BigDecimal amount, BigDecimal min, BigDecimal max) {
        if (amount == null) {
            throw new BusinessRuleException(fieldName + " is required.");
        }
        if (amount.scale() > 2) {
            throw new BusinessRuleException(fieldName + " cannot have more than 2 decimal places.");
        }
        if (min != null && amount.compareTo(min) < 0) {
            throw new BusinessRuleException(fieldName + " must be at least " + min.toPlainString());
        }
        if (max != null && amount.compareTo(max) > 0) {
            throw new BusinessRuleException(fieldName + " cannot exceed " + max.toPlainString());
        }
        return amount;
    }

    /**
     * Validates a campaign deadline date.
     */
    public static void validateFutureDeadline(LocalDate deadline, int maxDays) {
        if (deadline == null) {
            throw new BusinessRuleException("Deadline date is required.");
        }
        LocalDate now = LocalDate.now();
        if (!deadline.isAfter(now)) {
            throw new BusinessRuleException("Campaign deadline must be at least one day in the future.");
        }
        if (deadline.isAfter(now.plusDays(maxDays))) {
            throw new BusinessRuleException("Campaign duration cannot exceed " + maxDays + " days.");
        }
    }
}
