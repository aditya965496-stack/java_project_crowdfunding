package com.crowdfund.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Filter that applies HTTP 429 rate limiting to REST and webhook endpoints under /api/**.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    public RateLimitingFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Only apply rate limiting to /api/** endpoints
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();
        String limitKey = "api:" + path + ":" + clientIp;

        // Webhook endpoints allow 60/min, others 100/min
        int capacity = path.startsWith("/api/webhooks") ? 60 : 100;
        RateLimiterService.RateLimitResult result = rateLimiterService.checkRateLimit(
                limitKey, capacity, Duration.ofMinutes(1));

        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemainingTokens()));

        if (!result.isAllowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(result.getRetryAfterSeconds()));
            response.getWriter().write(String.format(
                    "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Retry after %d seconds.\"}",
                    result.getRetryAfterSeconds()));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
