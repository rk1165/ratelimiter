package com.ratelimiter.controller;

import com.ratelimiter.annotation.RateLimit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Example controller demonstrating @RateLimit annotation usage with hierarchical tiers.
 * <p>
 * Rate limiting is based on user tier (resolved from API key or defaults to free):
 * <ul>
 *     <li>Free tier: Basic rate limits</li>
 *     <li>Premium tier: Higher rate limits</li>
 *     <li>Enterprise tier: Highest rate limits</li>
 * </ul>
 * <p>
 * Users can access endpoints above their tier with degraded (grace) limits.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/examples")
public class RateLimitedExampleController {

    /**
     * Free tier endpoint - accessible by all users with their tier's rate limits.
     * <p>Usage:</p>
     * <code>curl http://localhost:8080/api/v1/examples/free</code>
     * <code>curl -H "X-API-Key: your-api-key" http://localhost:8080/api/v1/examples/free</code>
     */
    @RateLimit()
    @GetMapping("/free")
    public ResponseEntity<Map<String, Object>> freeEndpoint() {
        return buildResponse("Free tier endpoint - all users get their tier's rate limits");
    }

    /**
     * Premium tier endpoint - premium and enterprise users get full access.
     * Free users get degraded (grace) access with limited requests.
     * <p>Usage:</p>
     * <code>curl -H "X-API-Key: premium-user-api-key" http://localhost:8080/api/v1/examples/premium</code>
     */
    @RateLimit(tier = "premium")
    @GetMapping("/premium")
    public ResponseEntity<Map<String, Object>> premiumEndpoint() {
        return buildResponse("Premium tier endpoint - free users get grace limits");
    }

    /**
     * Enterprise tier endpoint - only enterprise users get full access.
     * Free and premium users get degraded (grace) access.
     * <p>Usage:</p>
     * <code>curl -H "X-API-Key: enterprise-user-api-key" http://localhost:8080/api/v1/examples/enterprise</code>
     */
    @RateLimit(tier = "enterprise")
    @GetMapping("/enterprise")
    public ResponseEntity<Map<String, Object>> enterpriseEndpoint() {
        return buildResponse("Enterprise tier endpoint - lower tiers get grace limits");
    }

    /**
     * Expensive operation consuming multiple tokens.
     * Available to all tiers but costs 5 tokens per request.
     * <p>Usage:</p>
     * <code>curl -H "X-API-Key: your-api-key" http://localhost:8080/api/v1/examples/expensive</code>
     */
    @RateLimit(tokens = 5)
    @GetMapping("/expensive")
    public ResponseEntity<Map<String, Object>> expensiveOperation() {
        return buildResponse("Expensive operation - consumed 5 tokens");
    }

    /**
     * Default rate limited endpoint (uses free tier by default).
     * <p>Usage:</p>
     * <code>curl http://localhost:8080/api/v1/examples/default</code>
     */
    @RateLimit
    @GetMapping("/default")
    public ResponseEntity<Map<String, Object>> defaultRateLimit() {
        return buildResponse("Default rate limit (free tier)");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }
}
