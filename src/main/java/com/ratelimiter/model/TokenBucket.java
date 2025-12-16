package com.ratelimiter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the state of a Token Bucket for rate limiting
 * <p>
 * Token Bucket algorithm
 * <p>A bucket holds tokens upto a maximum capacity </br>
 * Tokens are added at a fixed refill rate. (e.g. 1 token/second)</br>
 * Each request consumes token (usually 1). If there are not enough tokens,</br>
 * the request is rejected</p>
 *
 * <p>Example</p>
 * <p>capacity = 10, refillRate = 1 token/sec</p>
 * <ol>
 *     <li>Bucket starts with 10 tokens</li>
 *     <li>User makes 10 requests instantly -> all allowed, bucket empty</li>
 *     <li>User waits 5 seconds -> bucket refills to 5 tokens</li>
 *     <li>User makes 3 more requests -> allowed, 2 tokens remain</li>
 * </ol>
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenBucket {

    // Unique identifier for the bucket (e.g. "user:123, "ip:192.168.1.1")
    private String key;

    /**
     * Maximum number of tokens the bucket can hold.
     * This defines the burst capacity - how many requests can be made at once
     */

    private long capacity;
    /**
     * Number of tokens added per second
     * This defines the sustained rate limit.
     */
    private double refillRate;

    // Current number of available tokens
    private long availableTokens;

    // Timestamp (in milliseconds) when tokens were last refilled
    private long lastRefillTime;

    // Creates a new bucket with full capacity
    public static TokenBucket createNew(String key, long capacity, double refillRate, long lastRefillTime) {
        return TokenBucket.builder()
                .key(key)
                .capacity(capacity)
                .refillRate(refillRate)
                .availableTokens(capacity)
                .lastRefillTime(lastRefillTime)
                .build();
    }

    // Checks if bucket has enough tokens for the request
    public boolean hasTokens(long tokensRequired) {
        return availableTokens >= tokensRequired;
    }

    @Override
    public String toString() {
        return "TokenBucket{" +
                "availableTokens=" + availableTokens +
                ", refillRate=" + refillRate +
                ", capacity=" + capacity +
                ", key='" + key + '\'' +
                '}';
    }
}
