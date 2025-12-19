package com.ratelimiter.service.impl;

import com.ratelimiter.configuration.RateLimitConfig;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.TokenBucket;
import com.ratelimiter.service.RateLimiter;
import lombok.extern.slf4j.Slf4j;

import static com.ratelimiter.utils.ApplicationConstants.KEY_PREFIX;

/**
 * Abstract base class for Token Bucket Rate Limiter implementations.
 * <p>
 * Uses Template Method pattern: the algorithm is defined here, subclasses only
 * provide the bucket storage/retrieval mechanism via {@link #getOrCreateBucket}.
 * </p>
 */
@Slf4j
public abstract class AbstractTokenBucketRateLimiter implements RateLimiter {

    protected final RateLimitConfig rateLimitConfig;
    protected final long tokensPerRequest;

    protected AbstractTokenBucketRateLimiter(RateLimitConfig rateLimitConfig, long tokensPerRequest) {
        this.rateLimitConfig = rateLimitConfig;
        this.tokensPerRequest = tokensPerRequest;
    }

    @Override
    public RateLimitResult tryConsume(String key) {
        return tryConsume(key, tokensPerRequest);
    }

    @Override
    public RateLimitResult tryConsume(String key, long tokens) {
        return tryConsume(key, tokens, rateLimitConfig.getDefaultCapacity(), rateLimitConfig.getDefaultRefillRate());
    }

    @Override
    public RateLimitResult tryConsumeForTier(String key, String tier) {
        return tryConsumeForTier(key, tier, tokensPerRequest);
    }

    @Override
    public RateLimitResult tryConsumeForTier(String key, String tier, long tokens) {
        RateLimitConfig.TierConfig tierConfig = rateLimitConfig.getTierConfig(tier);
        return tryConsume(key, tokens, tierConfig.getCapacity(), tierConfig.getRefillRate());
    }

    /**
     * Template method implementing the Token Bucket algorithm.
     * Subclasses provide bucket storage via {@link #getOrCreateBucket}.
     */
    @Override
    public RateLimitResult tryConsume(String key, long tokens, long bucketCapacity, double bucketRefillRate) {
        String bucketKey = buildBucketKey(key);
        long currentTimeMs = System.currentTimeMillis();

        // Get or create bucket - subclasses provide storage mechanism
        TokenBucket tokenBucket = getOrCreateBucket(bucketKey, bucketCapacity, bucketRefillRate, currentTimeMs);

        // Synchronize on the bucket to prevent race conditions during read-modify-write
        synchronized (tokenBucket) {
            // Calculate tokens to add based on elapsed time
            long elapsedMs = currentTimeMs - tokenBucket.getLastRefillTime();
            long tokensToAdd = calculateTokensToAdd(elapsedMs, bucketRefillRate);

            // Refill tokens (capped at capacity)
            tokenBucket.setAvailableTokens(Math.min(bucketCapacity, tokenBucket.getAvailableTokens() + tokensToAdd));

            // Update lastRefillTime only if tokens were added
            if (tokensToAdd > 0) {
                tokenBucket.setLastRefillTime(currentTimeMs);
            }

            // Check if we have enough tokens
            if (tokenBucket.hasTokens(tokens)) {
                // Consume tokens
                tokenBucket.setAvailableTokens(tokenBucket.getAvailableTokens() - tokens);
                log.debug("Bucket state after consume: {}", tokenBucket);
                return createAllowedResult(key, tokenBucket.getAvailableTokens(), bucketCapacity);
            } else {
                // Calculate retry-after time
                long tokensNeeded = tokens - tokenBucket.getAvailableTokens();
                long retryAfterMs = calculateRetryAfterMs(tokensNeeded, bucketRefillRate);
                log.debug("Bucket state (rate limited): {}", tokenBucket);
                return createRejectedResult(key, tokenBucket.getAvailableTokens(), bucketCapacity, retryAfterMs);
            }
        }
    }

    @Override
    public RateLimitResult peek(String key) {
        return tryConsume(key, 0);
    }

    /**
     * Gets or creates a bucket for the given key.
     * Subclasses implement this to provide their storage mechanism.
     *
     * @param bucketKey        the prefixed bucket key
     * @param bucketCapacity   bucket capacity
     * @param bucketRefillRate bucket refill rate
     * @param currentTimeMs    current time in milliseconds
     * @return the TokenBucket (existing or newly created)
     */
    protected abstract TokenBucket getOrCreateBucket(String bucketKey, long bucketCapacity,
                                                     double bucketRefillRate, long currentTimeMs);

    /**
     * Builds the bucket key with the standard prefix
     *
     * @param key the raw key
     * @return the prefixed bucket key
     */
    protected String buildBucketKey(String key) {
        return KEY_PREFIX + key;
    }

    /**
     * Calculates tokens to add based on elapsed time and refill rate.
     * Uses floor to ensure tokens are only added when full time period has elapsed.
     *
     * @param elapsedMs        time elapsed since last refill in milliseconds
     * @param bucketRefillRate tokens per second refill rate
     * @return number of tokens to add
     */
    protected long calculateTokensToAdd(long elapsedMs, double bucketRefillRate) {
        return (long) Math.floor(elapsedMs * bucketRefillRate / 1000.0);
    }

    /**
     * Calculates retry-after time when not enough tokens are available.
     *
     * @param tokensNeeded     number of additional tokens needed
     * @param bucketRefillRate tokens per second refill rate
     * @return milliseconds to wait before retrying
     */
    protected long calculateRetryAfterMs(long tokensNeeded, double bucketRefillRate) {
        return (long) Math.ceil(tokensNeeded * 1000.0 / bucketRefillRate);
    }

    /**
     * Creates an allowed result and logs the action.
     *
     * @param key             the rate limit key
     * @param remainingTokens tokens remaining after consumption
     * @param bucketCapacity  total bucket capacity
     * @return allowed RateLimitResult
     */
    protected RateLimitResult createAllowedResult(String key, long remainingTokens, long bucketCapacity) {
        log.info("Rate limit ALLOWED for key: {}, remaining: {}", key, remainingTokens);
        return RateLimitResult.allowed(remainingTokens, bucketCapacity);
    }

    /**
     * Creates a rejected result and logs the action.
     *
     * @param key             the rate limit key
     * @param remainingTokens tokens remaining (not enough for request)
     * @param bucketCapacity  total bucket capacity
     * @param retryAfterMs    milliseconds until tokens will be available
     * @return rejected RateLimitResult
     */
    protected RateLimitResult createRejectedResult(String key, long remainingTokens, long bucketCapacity, long retryAfterMs) {
        log.warn("Rate limit EXCEEDED for key: {}, retry after: {}ms", key, retryAfterMs);
        return RateLimitResult.rejected(remainingTokens, bucketCapacity, retryAfterMs);
    }
}

