package com.ratelimiter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a rate limit check
 * Contains information needed to
 * <ol>
 *     <li>Decide whether to allow/reject the request</li>
 *     <li>Populate rate limit response headers</li>
 *     <li>Tell the client when to retry</li>
 * </ol>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitResult {

    // whether the request is allowed (true) or should be rejected (false)
    private boolean allowed;

    /**
     * Number of tokens remaining after a request
     * User for X-RateLimit-Remaining header
     */
    private long remainingTokens;

    /**
     * Maximum bucket capacity
     * User for X-RateLimit-Limit header
     */
    private long limit;

    /**
     * Time in milliseconds until the bucket refills enough for a retry
     * User for X-Retry-After header when request is rejected
     * Will be 0 if the request was allowed
     */
    private long retryAfterMs;

    /**
     * Unix timestamp (seconds) when the rate limit resets
     * Used for X-RateLimit-Reset header
     */
    private long resetAtSeconds;

    // Creates a successful (allowed) result
    public static RateLimitResult allowed(long remainingTokens, long limit) {
        return RateLimitResult.builder()
                .allowed(true)
                .remainingTokens(remainingTokens)
                .limit(limit)
                .retryAfterMs(0)
                .resetAtSeconds(System.currentTimeMillis() / 1000 + 1)
                .build();
    }

    // Creates a rejected (rate limited) result
    public static RateLimitResult rejected(long remainingTokens, long limit, long retryAfterMs) {
        long resetAtSeconds = (System.currentTimeMillis() + retryAfterMs) / 1000;
        return RateLimitResult.builder()
                .allowed(false)
                .remainingTokens(remainingTokens)
                .limit(limit)
                .retryAfterMs(retryAfterMs)
                .resetAtSeconds(resetAtSeconds)
                .build();
    }

}
