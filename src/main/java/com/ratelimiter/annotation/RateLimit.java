package com.ratelimiter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable rate limiting on REST controller methods.
 * <p> Rate limits are applied based on a hierarchical tier system:
 * Example usage:
 * <pre>
 * &#64;RateLimit(tier = "free")        // All users get their tier's rate limits
 * &#64;RateLimit(tier = "premium")     // Free users get grace limits, premium+ get full access
 * &#64;RateLimit(tier = "enterprise")  // Only enterprise gets full access, others get grace limits
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Minimum tier required for full access to this endpoint.
     * <p>
     * Users with a tier at or above this level get their tier's configured rate limits.
     * Users below this tier get degraded (grace) access with limited requests.
     * <p>
     * Default: "free" (all users get full access based on their tier)
     */
    String tier() default "free";

    /**
     * Number of tokens to consume per method invocation.
     * <p>
     * Can be adjusted to use higher values for expensive operations.
     * For example, a report generation endpoint might cost 10 tokens.
     */
    long tokens() default 1;
}
