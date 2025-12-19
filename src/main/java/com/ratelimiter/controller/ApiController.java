package com.ratelimiter.controller;

import com.ratelimiter.configuration.RateLimitConfig;
import com.ratelimiter.model.RateLimitStatus;
import com.ratelimiter.service.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiController {

    private final RateLimiter rateLimiter;
    private final RateLimitConfig rateLimitConfig;

    /**
     * <p>Usage:
     * <p><code>curl http://localhost:8080/api/v1/test</code>
     * <p>With User ID:
     * <p><code>curl -H "X-User-Id: user123" http://localhost:8080/api/v1/test</code>
     * <p>With Tier</p>
     * <p><code>curl -H "X-User-Id: user123" -H "X-User-Tier: premium" http://localhost:8080/api/test</code>
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Request successful!");
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * Check current rate limit status without consuming tokens
     * Usage:
     * <p><code>curl http://localhost:8080/api/v1/limit/status?key=user:123</code>
     */
    @GetMapping("/limit/status")
    public ResponseEntity<Map<String, Object>> getLimitStatus(@RequestParam String key) {
        RateLimitStatus result = rateLimiter.peek(key);
        Map<String, Object> response = new HashMap<>();
        response.put("key", key);
        response.put("limit", result.getLimit());
        response.put("remaining", result.getRemainingTokens());
        response.put("resetAt", result.getResetAtSeconds());
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * Get rate limit configuration for all tiers.
     * <p>Usage:</p>
     * <p><code>curl http://localhost:8080/api/v1/limit/config</code></p>
     */
    @GetMapping("/limit/config")
    public ResponseEntity<Map<String, Object>> getLimitConfig() {
        Map<String, Object> response = new HashMap<>();
        response.put("defaultCapacity", rateLimitConfig.getDefaultCapacity());
        response.put("defaultFillRate", rateLimitConfig.getDefaultRefillRate());

        Map<String, Object> tiers = new HashMap<>();
        rateLimitConfig.getTiers().forEach((name, config) -> {
            Map<String, Object> tierInfo = new HashMap<>();
            tierInfo.put("capacity", config.getCapacity());
            tierInfo.put("refillRate", config.getRefillRate());
            tierInfo.put("description", config.getDescription());
            tiers.put(name, tierInfo);
        });
        response.put("tiers", tiers);
        return ResponseEntity.ok(response);
    }

    /**
     * Reset rate limit for a specific key
     * <p>Usage:</p>
     * <p><code>curl -X POST http://localhost:8080/api/limit/reset?key=user:123</code></p>
     */
    @PostMapping("/limit/reset")
    public ResponseEntity<Map<String, Object>> resetLimit(@RequestParam String key) {
        rateLimiter.reset(key);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Rate limit reset successfully!");
        response.put("key", key);
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }
}
