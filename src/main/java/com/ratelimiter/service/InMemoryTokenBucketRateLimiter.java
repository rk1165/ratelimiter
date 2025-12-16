package com.ratelimiter.service;

import com.ratelimiter.configuration.RateLimitConfig;
import com.ratelimiter.model.TokenBucket;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory Token Bucket Rate Limiter implementation using ConcurrentHashMap.
 * <p> Best suited for: </br>
 * - Single instance deployments</br>
 * - Development and testing</br>
 * - Learning/prototyping
 *
 * <p> Limitations: </br>
 * - State is lost on restart </br>
 * - Not shared across multiple instances </br>
 * - No automatic TTL/expiration (entries stay forever)
 */
@Slf4j
public class InMemoryTokenBucketRateLimiter extends AbstractTokenBucketRateLimiter {

    private final Map<String, TokenBucket> bucketStore = new ConcurrentHashMap<>();

    public InMemoryTokenBucketRateLimiter(RateLimitConfig rateLimitConfig, long tokensPerRequest) {
        super(rateLimitConfig, tokensPerRequest);
    }

    @Override
    protected TokenBucket getOrCreateBucket(String bucketKey, long bucketCapacity,
                                            double bucketRefillRate, long currentTimeMs) {
        return bucketStore.computeIfAbsent(bucketKey,
                k -> TokenBucket.createNew(k, bucketCapacity, bucketRefillRate, currentTimeMs));
    }

    @Override
    public void reset(String key) {
        String bucketKey = buildBucketKey(key);
        bucketStore.remove(bucketKey);
        log.info("Rate limit reset for key: {}", key);
    }

    /**
     * Returns the current number of tracked buckets (for monitoring/debugging)
     */
    public int getBucketCount() {
        return bucketStore.size();
    }

    /**
     * Clears all rate limit buckets (useful for testing)
     */
    public void clearAll() {
        bucketStore.clear();
        log.info("All rate limit buckets cleared");
    }
}
