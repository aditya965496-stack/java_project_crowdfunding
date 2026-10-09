package com.crowdfund.util;

import com.crowdfund.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class InputSanitizerTest {

    @Test
    @DisplayName("should strip dangerous HTML and XSS payloads")
    void shouldStripXss() {
        String dirty = "<script>alert('xss')</script>Hello <b onclick='eval()'>World</b>";
        String clean = InputSanitizer.sanitizeText(dirty);
        assertFalse(clean.contains("<script>"));
        assertFalse(clean.contains("alert"));
        assertFalse(clean.contains("onclick"));
        assertEquals("Hello World", clean);
    }

    @Test
    @DisplayName("should validate and sanitize text with length bounds")
    void shouldValidateTextBounds() {
        assertThrows(BusinessRuleException.class, () ->
                InputSanitizer.validateAndSanitize("Title", "", 3, 20));
        assertThrows(BusinessRuleException.class, () ->
                InputSanitizer.validateAndSanitize("Title", "ab", 3, 20));
        assertThrows(BusinessRuleException.class, () ->
                InputSanitizer.validateAndSanitize("Title", "a".repeat(25), 3, 20));

        String result = InputSanitizer.validateAndSanitize("Title", "Valid Title", 3, 20);
        assertEquals("Valid Title", result);
    }

    @Test
    @DisplayName("should validate email format strictly")
    void shouldValidateEmail() {
        assertEquals("test@example.com", InputSanitizer.validateEmail("  test@example.com  "));
        assertThrows(BusinessRuleException.class, () -> InputSanitizer.validateEmail("invalid-email"));
        assertThrows(BusinessRuleException.class, () -> InputSanitizer.validateEmail("test@.com"));
        assertThrows(BusinessRuleException.class, () -> InputSanitizer.validateEmail("test@example\n.com"));
    }

    @Test
    @DisplayName("should validate monetary amounts")
    void shouldValidateAmount() {
        BigDecimal valid = new BigDecimal("50.00");
        assertEquals(valid, InputSanitizer.validateAmount("Amount", valid, BigDecimal.ONE, BigDecimal.valueOf(100)));

        assertThrows(BusinessRuleException.class, () ->
                InputSanitizer.validateAmount("Amount", new BigDecimal("50.123"), BigDecimal.ONE, BigDecimal.valueOf(100)));
        assertThrows(BusinessRuleException.class, () ->
                InputSanitizer.validateAmount("Amount", BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.valueOf(100)));
    }

    @Test
    @DisplayName("should validate future deadline correctly")
    void shouldValidateDeadline() {
        assertThrows(BusinessRuleException.class, () ->
                InputSanitizer.validateFutureDeadline(LocalDate.now(), 365));
        assertThrows(BusinessRuleException.class, () ->
                InputSanitizer.validateFutureDeadline(LocalDate.now().minusDays(1), 365));
        assertThrows(BusinessRuleException.class, () ->
                InputSanitizer.validateFutureDeadline(LocalDate.now().plusDays(400), 365));

        assertDoesNotThrow(() ->
                InputSanitizer.validateFutureDeadline(LocalDate.now().plusDays(30), 365));
    }
}
