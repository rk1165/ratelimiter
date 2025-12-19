package com.ratelimiter.aspect;

import com.ratelimiter.annotation.RateLimit;
import com.ratelimiter.configuration.RateLimitConfig;
import com.ratelimiter.exception.RateLimitExceededException;
import com.ratelimiter.model.RateLimitStatus;
import com.ratelimiter.service.ClientIdentityResolver;
import com.ratelimiter.service.ClientIdentityResolver.ResolvedIdentity;
import com.ratelimiter.service.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Aspect that provides hierarchical tier-based rate limiting for methods annotated with {@link RateLimit}.
 * <p>
 * Rate limiting behavior:
 * <ul>
 *     <li>If user's tier >= endpoint's required tier: Apply user's tier rate limits</li>
 *     <li>If user's tier < endpoint's required tier: Apply degraded (grace) rate limits</li>
 * </ul>
 * <p>
 * If the rate limit is exceeded, the aspect throws a {@link RateLimitExceededException}.
 *
 * @see RateLimit
 * @see RateLimiter
 */
@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimiter rateLimiter;
    private final RateLimitConfig rateLimitConfig;
    private final ClientIdentityResolver clientIdentityResolver;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    /**
     * Around advice that applies rate limiting before method execution.
     * Only applies to methods within @RestController classes.
     */
    @Around("@annotation(com.ratelimiter.annotation.RateLimit) && " +
            "within(@org.springframework.web.bind.annotation.RestController *)")
    public Object rateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RateLimit rateLimit = AnnotationUtils.findAnnotation(method, RateLimit.class);

        // Resolve client identity (clientId + optional user for tier lookup)
        ResolvedIdentity identity = clientIdentityResolver.resolveIdentity(request);
        String clientId = identity.key();
        String userTier = clientIdentityResolver.resolveUserTier(request, identity.user());
        String requiredTier = rateLimit.tier();
        long tokens = rateLimit.tokens();

        // Apply rate limit based on tier comparison
        RateLimitStatus result = applyRateLimit(clientId, userTier, requiredTier, tokens);

        // Add rate limit headers to response
        clientIdentityResolver.addRateLimitHeaders(response, result);

        log.debug("Rate limit check for clientId '{}': userTier={}, requiredTier={}, allowed={}, remaining={}",
                clientId, userTier, requiredTier, result.isAllowed(), result.getRemainingTokens());

        if (!result.isAllowed()) {
            String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
            log.warn("Rate limit exceeded for method '{}' with clientId '{}'. Retry after {}ms",
                    methodName, clientId, result.getRetryAfterMs());
            throw new RateLimitExceededException(clientId, result);
        }

        return joinPoint.proceed();
    }

    /**
     * Applies rate limiting based on user tier vs required tier.
     *
     * @param key          The rate limit key (user ID, API key, or IP)
     * @param userTier     The user's tier
     * @param requiredTier The tier required by the endpoint
     * @param tokens       Number of tokens to consume
     * @return RateLimitResult indicating if request is allowed
     */
    private RateLimitStatus applyRateLimit(String key, String userTier, String requiredTier, long tokens) {
        if (rateLimitConfig.hasAccess(userTier, requiredTier)) {
            // User has sufficient tier - apply their tier's rate limits
            log.debug("User tier '{}' has full access to '{}' tier endpoint", userTier, requiredTier);
            return rateLimiter.tryConsumeForTier(key, userTier, tokens);
        } else {
            // User is below required tier - apply their tier's limits as grace limits
            log.debug("User tier '{}' below required '{}' - applying grace limits", userTier, requiredTier);
            RateLimitConfig.TierConfig tierConfig = rateLimitConfig.getTierConfig(userTier);
            return rateLimiter.tryConsume(key, tokens, tierConfig.getCapacity(), tierConfig.getRefillRate());
        }
    }

}
