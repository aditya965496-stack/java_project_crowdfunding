package com.crowdfund.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe in-memory token bucket rate limiter.
 * Protects against brute-force login attempts, webhook flooding, and API denial-of-service.
 */
@Service
public class RateLimiterService {

    @Value("${crowdfund.ratelimit.enabled:true}")
    private boolean enabled;

    @Value("${crowdfund.ratelimit.requests-per-minute:60}")
    private int defaultRequestsPerMinute;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public static class RateLimitResult {
        private final boolean allowed;
        private final int remainingTokens;
        private final long retryAfterSeconds;

        public RateLimitResult(boolean allowed, int remainingTokens, long retryAfterSeconds) {
            this.allowed = allowed;
            this.remainingTokens = remainingTokens;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public int getRemainingTokens() {
            return remainingTokens;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    private static class Bucket {
        final int capacity;
        final double refillRatePerSecond;
        final AtomicReference<Double> tokens;
        final AtomicReference<Instant> lastRefill;

        Bucket(int capacity, Duration refillPeriod) {
            this.capacity = capacity;
            this.refillRatePerSecond = (double) capacity / refillPeriod.toSeconds();
            this.tokens = new AtomicReference<>((double) capacity);
            this.lastRefill = new AtomicReference<>(Instant.now());
        }

        synchronized RateLimitResult tryConsume(int count) {
            refill();
            double currentTokens = tokens.get();
            if (currentTokens >= count) {
                tokens.set(currentTokens - count);
                return new RateLimitResult(true, (int) Math.floor(tokens.get()), 0);
            } else {
                double needed = count - currentTokens;
                long retryAfterSec = Math.max(1, (long) Math.ceil(needed / refillRatePerSecond));
                return new RateLimitResult(false, (int) Math.floor(currentTokens), retryAfterSec);
            }
        }

        private void refill() {
            Instant now = Instant.now();
            Instant prev = lastRefill.get();
            long elapsedMillis = Duration.between(prev, now).toMillis();
            if (elapsedMillis > 0) {
                double tokensToAdd = (elapsedMillis / 1000.0) * refillRatePerSecond;
                double newTokens = Math.min(capacity, tokens.get() + tokensToAdd);
                tokens.set(newTokens);
                lastRefill.set(now);
            }
        }
    }

    /**
     * Checks rate limit using default policy (e.g. 60 req/min).
     */
    public RateLimitResult checkRateLimit(String key) {
        return checkRateLimit(key, defaultRequestsPerMinute, Duration.ofMinutes(1));
    }

    /**
     * Checks rate limit with custom capacity and time window.
     */
    public RateLimitResult checkRateLimit(String key, int capacity, Duration period) {
        if (!enabled) {
            return new RateLimitResult(true, capacity, 0);
        }
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity, period));
        return bucket.tryConsume(1);
    }

    /**
     * Specialized rate limiter for login attempts (e.g. 5 attempts / minute).
     */
    public RateLimitResult checkLoginAttempt(String ipOrUsername) {
        return checkRateLimit("login:" + ipOrUsername, 5, Duration.ofMinutes(1));
    }

    /**
     * Reset key (e.g. upon successful authentication).
     */
    public void reset(String key) {
        buckets.remove(key);
    }
}
