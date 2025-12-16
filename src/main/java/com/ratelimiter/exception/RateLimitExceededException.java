package com.ratelimiter.exception;

import com.ratelimiter.model.RateLimitResult;
import lombok.Getter;

/**
 * Exception thrown when a rate limit is exceeded
 * Contains all information to build a proper 429 response
 */
@Getter
public class RateLimitExceededException extends RuntimeException {

    private final RateLimitResult rateLimitResult;

    // Identifier that was rate limited (userID, ip, API key)
    private final String identifier;

    public RateLimitExceededException(String identifier, RateLimitResult rateLimitResult) {
        super(String.format("Rate limit exceeded for '%s'. Retry after %d ms.", identifier, rateLimitResult.getRetryAfterMs()));
        this.identifier = identifier;
        this.rateLimitResult = rateLimitResult;
    }

    public long getRetryAfterSeconds() {
        return (long) Math.ceil(rateLimitResult.getRetryAfterMs() / 1000.0);
    }

}
