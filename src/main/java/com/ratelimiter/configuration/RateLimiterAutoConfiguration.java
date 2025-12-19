package com.ratelimiter.configuration;

import com.ratelimiter.service.impl.CaffeineTokenBucketRateLimiter;
import com.ratelimiter.service.impl.InMemoryTokenBucketRateLimiter;
import com.ratelimiter.service.RateLimiter;
import com.ratelimiter.service.impl.RedisTokenBucketRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

/**
 * Auto-configuration for Rate Limiter implementations.
 * <p>
 * Use the property {@code rate.limit.storage} to select the implementation:
 * <ul>
 *   <li>{@code redis} - Uses Redis for distributed rate limiting (production)</li>
 *   <li>{@code caffeine} - Uses Caffeine cache with TTL support (single instance with auto-expiry)</li>
 *   <li>{@code in-memory} - Uses ConcurrentHashMap (simple, no TTL)</li>
 * </ul>
 * </p>
 */
@Slf4j
@Configuration
public class RateLimiterAutoConfiguration {

    @Value("${rate.limit.bucket.tokens-per-request:1}")
    private long tokensPerRequest;

    /**
     * Creates Redis-based rate limiter when storage is set to 'redis'.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "rate.limit.storage", havingValue = "redis")
    public RateLimiter redisRateLimiter(RateLimitConfig rateLimitConfig,
                                        RedisTemplate<String, Long> redisTemplate,
                                        RedisScript<List<Long>> tokenBucketScript) {
        log.info("Initializing Redis-based Token Bucket Rate Limiter");
        return new RedisTokenBucketRateLimiter(rateLimitConfig, tokensPerRequest, redisTemplate, tokenBucketScript);
    }

    /**
     * Creates Caffeine-based rate limiter when storage is set to 'caffeine'.
     * Best choice for single-instance with automatic TTL expiration.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "rate.limit.storage", havingValue = "caffeine")
    public RateLimiter caffeineRateLimiter(RateLimitConfig rateLimitConfig) {
        log.info("Initializing Caffeine-based Token Bucket Rate Limiter");
        return new CaffeineTokenBucketRateLimiter(rateLimitConfig, tokensPerRequest);
    }

    /**
     * Creates In-Memory rate limiter when storage is set to 'in-memory'.
     * Simple implementation using ConcurrentHashMap (no TTL).
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "rate.limit.storage", havingValue = "in-memory")
    public RateLimiter inMemoryRateLimiter(RateLimitConfig rateLimitConfig) {
        log.info("Initializing In-Memory Token Bucket Rate Limiter");
        return new InMemoryTokenBucketRateLimiter(rateLimitConfig, tokensPerRequest);
    }

    /**
     * Fallback: Creates In-Memory rate limiter when no storage type is specified.
     * This ensures the application can start even without explicit configuration.
     */
    @Bean
    @ConditionalOnMissingBean(RateLimiter.class)
    public RateLimiter defaultRateLimiter(RateLimitConfig rateLimitConfig) {
        log.warn("No rate.limit.storage configured. Defaulting to In-Memory Rate Limiter. " +
                "Set 'rate.limit.storage=redis' for production use.");
        return new InMemoryTokenBucketRateLimiter(rateLimitConfig, tokensPerRequest);
    }
}

