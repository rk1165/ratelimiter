package com.ratelimiter.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ratelimiter.configuration.RateLimitConfig;
import com.ratelimiter.model.TokenBucket;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * Token Bucket Rate Limiter implementation using Caffeine cache.
 * <p> Benefits over plain ConcurrentHashMap: </br>
 * - Automatic TTL-based expiration (entries removed after inactivity)</br>
 * - Maximum size limit with LRU eviction</br>
 * - Better memory management </br>
 * - Built-in statistics
 * </p>
 * <p> Best suited for: </br>
 * - Single instance deployments with memory constraints</br>
 * - Development and testing</br>
 * - When you need TTL but don't want external dependencies like Redis
 * </p>
 */
@Slf4j
public class CaffeineTokenBucketRateLimiter extends AbstractTokenBucketRateLimiter {

    private final Cache<String, TokenBucket> bucketCache;

    public CaffeineTokenBucketRateLimiter(RateLimitConfig rateLimitConfig, long tokensPerRequest,
                                          long ttlSeconds, long maxSize) {
        super(rateLimitConfig, tokensPerRequest);

        this.bucketCache = Caffeine.newBuilder()
                .expireAfterAccess(ttlSeconds, TimeUnit.SECONDS)  // TTL after last access
                .maximumSize(maxSize)                              // Max entries (LRU eviction)
                .recordStats()                                     // Enable statistics
                .removalListener((key, value, cause) ->
                        log.debug("Cache entry removed: key={}, cause={}", key, cause))
                .build();

        log.info("Initialized Caffeine cache with TTL={}s, maxSize={}", ttlSeconds, maxSize);
    }

    /**
     * Simplified constructor with sensible defaults
     */
    public CaffeineTokenBucketRateLimiter(RateLimitConfig rateLimitConfig, long tokensPerRequest) {
        this(rateLimitConfig, tokensPerRequest,
                calculateDefaultTtl(rateLimitConfig),
                100_000);  // Default max 100k entries
    }

    private static long calculateDefaultTtl(RateLimitConfig config) {
        // TTL = 2x time to refill bucket (same logic as Redis implementation)
        return (long) Math.ceil(config.getDefaultCapacity() / config.getDefaultRefillRate() * 2);
    }

    @Override
    protected TokenBucket getOrCreateBucket(String bucketKey, long bucketCapacity,
                                            double bucketRefillRate, long currentTimeMs) {
        return bucketCache.get(bucketKey,
                k -> TokenBucket.createNew(k, bucketCapacity, bucketRefillRate, currentTimeMs));
    }

    @Override
    public void reset(String key) {
        String bucketKey = buildBucketKey(key);
        bucketCache.invalidate(bucketKey);
        log.info("Rate limit reset for key: {}", key);
    }

    /**
     * Returns the current number of cached buckets
     */
    public long getBucketCount() {
        return bucketCache.estimatedSize();
    }

    /**
     * Returns cache statistics (hit rate, eviction count, etc.)
     */
    public String getStats() {
        return bucketCache.stats().toString();
    }

    /**
     * Clears all cached buckets
     */
    public void clearAll() {
        bucketCache.invalidateAll();
        log.info("All rate limit buckets cleared");
    }

    /**
     * Triggers cleanup of expired entries
     */
    public void cleanUp() {
        bucketCache.cleanUp();
    }
}
