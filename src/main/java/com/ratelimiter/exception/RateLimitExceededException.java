package com.ratelimiter.exception;

import com.ratelimiter.model.RateLimitStatus;
import lombok.Getter;

/**
 * Exception thrown when a rate limit is exceeded
 * Contains all information to build a proper 429 response
 */
@Getter
public class RateLimitExceededException extends RuntimeException {

    private final RateLimitStatus rateLimitStatus;

    // Identifier that was rate limited (userID, ip, API key)
    private final String identifier;

    public RateLimitExceededException(String identifier, RateLimitStatus rateLimitStatus) {
        super(String.format("Rate limit exceeded for '%s'. Retry after %d ms.", identifier, rateLimitStatus.getRetryAfterMs()));
        this.identifier = identifier;
        this.rateLimitStatus = rateLimitStatus;
    }

    public long getRetryAfterSeconds() {
        return (long) Math.ceil(rateLimitStatus.getRetryAfterMs() / 1000.0);
    }

}
