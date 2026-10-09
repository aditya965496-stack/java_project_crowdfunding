package com.crowdfund.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
        ReflectionTestUtils.setField(rateLimiterService, "enabled", true);
        ReflectionTestUtils.setField(rateLimiterService, "defaultRequestsPerMinute", 60);
    }

    @Test
    @DisplayName("should allow requests within rate limit capacity")
    void shouldAllowRequestsWithinLimit() {
        String key = "test-client-1";
        for (int i = 0; i < 5; i++) {
            RateLimiterService.RateLimitResult result = rateLimiterService.checkRateLimit(key, 5, Duration.ofMinutes(1));
            assertTrue(result.isAllowed());
        }
    }

    @Test
    @DisplayName("should block requests once token bucket is exhausted")
    void shouldBlockWhenExhausted() {
        String key = "test-client-exhaust";
        for (int i = 0; i < 3; i++) {
            assertTrue(rateLimiterService.checkRateLimit(key, 3, Duration.ofMinutes(1)).isAllowed());
        }

        RateLimiterService.RateLimitResult blocked = rateLimiterService.checkRateLimit(key, 3, Duration.ofMinutes(1));
        assertFalse(blocked.isAllowed());
        assertTrue(blocked.getRetryAfterSeconds() > 0);
    }

    @Test
    @DisplayName("should rate limit login attempts strictly")
    void shouldLimitLoginAttempts() {
        String ip = "192.168.1.100";
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiterService.checkLoginAttempt(ip).isAllowed());
        }
        assertFalse(rateLimiterService.checkLoginAttempt(ip).isAllowed());
    }

    @Test
    @DisplayName("should reset limits upon key reset")
    void shouldResetLimits() {
        String key = "test-client-reset";
        for (int i = 0; i < 2; i++) {
            rateLimiterService.checkRateLimit(key, 2, Duration.ofMinutes(1));
        }
        assertFalse(rateLimiterService.checkRateLimit(key, 2, Duration.ofMinutes(1)).isAllowed());

        rateLimiterService.reset(key);
        assertTrue(rateLimiterService.checkRateLimit(key, 2, Duration.ofMinutes(1)).isAllowed());
    }
}
