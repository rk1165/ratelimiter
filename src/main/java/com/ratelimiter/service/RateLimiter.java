package com.ratelimiter.service;

import com.ratelimiter.model.RateLimitStatus;

/**
 * Interface for rate limiting implementations.
 * Implementations can use different storage backends (in-memory, Redis, etc.)
 */
public interface RateLimiter {

    /**
     * Attempts to consume tokens for a given key using default limits
     *
     * @param key Unique identifier (e.g. user ID, IP address, API Key)
     * @return RateLimitResult indicating if request is allowed and rate limit info
     */
    RateLimitStatus tryConsume(String key);

    /**
     * Attempts to consume tokens for a given key using default limits
     *
     * @param key    Unique identifier (e.g. user ID, IP address, API Key)
     * @param tokens Number of tokens to consume (for weighted rate limiting)
     * @return RateLimitResult indicating if request is allowed and rate limit info
     */
    RateLimitStatus tryConsume(String key, long tokens);

    /**
     * Attempts to consume tokens for a user with a specific tier
     *
     * @param key  Unique identifier (e.g. user ID, IP address, API Key)
     * @param tier User tier name (free, premium, enterprise)
     * @return RateLimitResult indicating if request is allowed and rate limit info
     */
    RateLimitStatus tryConsumeForTier(String key, String tier);

    /**
     * Attempts to consume tokens for a user with a specific tier
     *
     * @param key    Unique identifier (e.g. user ID, IP address, API Key)
     * @param tier   User tier name (free, premium, enterprise)
     * @param tokens Number of tokens to consume
     * @return RateLimitResult indicating if request is allowed and rate limit info
     */
    RateLimitStatus tryConsumeForTier(String key, String tier, long tokens);

    /**
     * Attempts to consume tokens with custom bucket configuration
     *
     * @param key              Unique identifier (e.g. user ID, IP address, API Key)
     * @param tokens           Number of tokens to consume
     * @param bucketCapacity   custom bucket capacity (different for each tier)
     * @param bucketRefillRate custom refill rate (different for each tier)
     * @return RateLimitResult indicating if request is allowed and rate limit info
     */
    RateLimitStatus tryConsume(String key, long tokens, long bucketCapacity, double bucketRefillRate);

    /**
     * Get the current bucket state without consuming tokens
     * Useful for displaying rate limit info to users.
     *
     * @param key Unique identifier
     * @return RateLimitResult with current state (always marked as allowed)
     */
    RateLimitStatus peek(String key);

    /**
     * Resets the rate limit for a specific key
     *
     * @param key Unique identifier
     */
    void reset(String key);
}

