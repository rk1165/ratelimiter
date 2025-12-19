package com.ratelimiter.service.impl;

import com.ratelimiter.configuration.RateLimitConfig;
import com.ratelimiter.model.RateLimitStatus;
import com.ratelimiter.model.TokenBucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.List;

/**
 * Token Bucket Rate Limiter implementation using Redis.
 * <p>
 * This provides distributed rate limiting by storing bucket state in Redis and
 * using Lua script for atomic operations.
 * </p>
 * <p>
 * Best suited for: </br>
 * - Multi-instance deployments </br>
 * - Production environments </br>
 * - Distributed systems
 * </p>
 * <p>
 * Benefits: </br>
 * - Shared state across instances </br>
 * - Survives application restarts </br>
 * - Atomic operations via Lua scripts </br>
 * - Automatic key expiration (TTL)
 * </p>
 */
@Slf4j
public class RedisTokenBucketRateLimiter extends AbstractTokenBucketRateLimiter {

    private final RedisTemplate<String, Long> redisTemplate;
    private final RedisScript<List<Long>> tokenBucketScript;

    public RedisTokenBucketRateLimiter(RateLimitConfig rateLimitConfig, long tokensPerRequest,
                                       RedisTemplate<String, Long> redisTemplate, RedisScript<List<Long>> tokenBucketScript) {
        super(rateLimitConfig, tokensPerRequest);
        this.redisTemplate = redisTemplate;
        this.tokenBucketScript = tokenBucketScript;
    }

    @Override
    public RateLimitStatus tryConsume(String key, long tokens, long bucketCapacity, double bucketRefillRate) {
        String bucketKey = buildBucketKey(key);
        long currentTimeMs = System.currentTimeMillis();

        try {
            // Execute Lua script atomically
            List<Long> result = redisTemplate.execute(
                    tokenBucketScript,
                    Collections.singletonList(bucketKey),
                    bucketCapacity,
                    (long) bucketRefillRate,
                    tokens,
                    currentTimeMs
            );

            if (result == null || result.size() < 3) {
                log.error("Unexpected result from Redis script for key: {}", key);
                // Fail open - allow request if Redis returns unexpected result
                return RateLimitStatus.allowed(bucketCapacity, bucketCapacity);
            }

            long allowed = result.get(0);
            long remainingTokens = result.get(1);
            long retryAfterMs = result.get(2);

            if (allowed == 1) {
                return createAllowedResult(key, remainingTokens, bucketCapacity);
            } else {
                return createRejectedResult(key, remainingTokens, bucketCapacity, retryAfterMs);
            }
        } catch (Exception e) {
            log.error("Redis error during rate limit check for key: {}", key, e);
            // Fail open - allow request if Redis is unavailable
            // Prefer availability over strict rate limiting
            return RateLimitStatus.allowed(bucketCapacity, bucketCapacity);
        }
    }

    @Override
    protected TokenBucket getOrCreateBucket(String bucketKey, long bucketCapacity, double bucketRefillRate, long currentTimeMs) {
        // Redis implementation relies on the Lua script path and should never call
        // the in-memory bucket creation used by other implementations. If this method
        // is invoked, it indicates an unexpected code path; fail fast to avoid
        // silently bypassing Redis.
        throw new UnsupportedOperationException("RedisTokenBucketRateLimiter does not use local TokenBucket instances");
    }

    @Override
    public void reset(String key) {
        String bucketKey = buildBucketKey(key);
        redisTemplate.delete(bucketKey);
        log.info("Rate limit reset for key: {}", key);
    }
}
