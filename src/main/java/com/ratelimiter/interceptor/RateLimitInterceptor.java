package com.ratelimiter.interceptor;

import com.ratelimiter.exception.RateLimitExceededException;
import com.ratelimiter.model.RateLimitStatus;
import com.ratelimiter.service.ClientIdentityResolver;
import com.ratelimiter.service.ClientIdentityResolver.ResolvedIdentity;
import com.ratelimiter.service.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// HTTP interceptor that applies rate limiting to incoming requests
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;
    private final ClientIdentityResolver clientIdentityResolver;

    /**
     * Extracts client identity and applies rate limits before the request
     * reaches the controller.
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        // Skip health check and admin endpoints
        String path = request.getRequestURI();
        if (isPathExcluded(path)) {
            return true;
        }

        // Resolve client identity
        ResolvedIdentity identity = clientIdentityResolver.resolveIdentity(request);
        /*
         * ResolvedIdentity can be of three types
         * 1. User called with valid API Key -> ResolvedIdentity("api:+key", user)
         * 2. User didn't have API Key but was registered -> ResolvedIdentity("user:+userId")
         * 3. Non-registered user called -> ResolvedIdentity("ip:+addr")
         */

        String clientId = identity.key();
        String userTier = clientIdentityResolver.resolveUserTier(request, identity.user());

        log.info("Rate limit check - clientId: {}, userTier: {}, path: {}", clientId, userTier, path);

        // Check Rate Limit
        RateLimitStatus result = rateLimiter.tryConsumeForTier(clientId, userTier);

        // Always add rate limit headers to response
        clientIdentityResolver.addRateLimitHeaders(response, result);

        if (!result.isAllowed()) {
            log.warn("Rate limit exceeded - clientId: {}, userTier: {}, path: {}", clientId, userTier, path);
            throw new RateLimitExceededException(clientId, result);
        }
        return true;
    }

    private boolean isPathExcluded(String path) {
        return path.startsWith("/actuator") ||
                path.startsWith("/admin") ||
                path.equals("/health") ||
                path.equals("/error") ||
                path.equals("/favicon.ico");
    }

}
