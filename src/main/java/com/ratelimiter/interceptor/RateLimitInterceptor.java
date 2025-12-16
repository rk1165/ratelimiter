package com.ratelimiter.interceptor;

import com.ratelimiter.ApplicationConstants;
import com.ratelimiter.configuration.RateLimitConfig;
import com.ratelimiter.exception.RateLimitExceededException;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.Tiers;
import com.ratelimiter.model.User;
import com.ratelimiter.service.RateLimiter;
import com.ratelimiter.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * HTTP interceptor that applies rate limiting to incoming requests
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;
    private final RateLimitConfig rateLimitConfig;
    private final UserService userService;

    // Thread-local to pass resolved user to tier resolution
    private static final ThreadLocal<User> resolvedUser = new ThreadLocal<>();

    /**
     *
     * Extracts client identity and applies rate limits before the request
     * reaches the controller.
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        try {
            // Skip if rate limiting is disabled
            if (!rateLimitConfig.isEnabled()) {
                return true;
            }

            // Skip health check and admin endpoints
            String path = request.getRequestURI();
            if (isPathExcluded(path)) {
                return true;
            }

            String clientId = resolveClientIdentity(request);
            String tier = resolveUserTier(request);
            log.info("Rate limit check - clientId: {}, tier: {}, path: {}", clientId, tier, path);

            // Check Rate Limit
            RateLimitResult result = rateLimiter.tryConsumeForTier(clientId, tier);

            // Always add rate limit headers to response
            addRateLimitHeaders(response, result);

            if (!result.isAllowed()) {
                log.warn("Rate limit exceeded - clientId: {}, tier: {}, path: {}", clientId, tier, path);
                throw new RateLimitExceededException(clientId, result);
            }
            return true;
        } finally {
            resolvedUser.remove();
        }
    }

    /**
     * Resolves the client's identity from the request.
     * Trying to find with which key we should rate limit.
     * Identity Resolution (in order of priority)
     * <ol>
     *     <li>X-API-Key header (for API key-based limiting) - looks up user in DB</li>
     *     <li>X-User-Id header (for authenticated users)</li>
     *     <li>X-Forwarded-For header (for clients behind proxy)</li>
     *     <li> Remote IP address (fallback)</li>
     * </ol>
     *
     * @param request http request
     * @return resolved client
     */
    private String resolveClientIdentity(HttpServletRequest request) {
        // First check for API key and lookup user
        String apiKey = request.getHeader(ApplicationConstants.HEADER_API_KEY);
        if (StringUtils.hasText(apiKey)) {
            Optional<User> userOpt = userService.getUserByApiKey(apiKey);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (!user.isEnabled()) {
                    log.warn("Disabled user attempted access: userId={}, apiKey={}...", user.getId(), apiKey.substring(0, Math.min(10, apiKey.length())));
                    // even if the user is not enabled, rate limit by API key, they will get free tier limits
                    return "api:" + apiKey;
                }
                resolvedUser.set(user); // store user for tier resolution
                // Use user ID as the rate limit key for DB users
                return "user:" + user.getId();
            }
            // Unknown API key - still rate limit by the key itself
            // Someone can use different API keys and use that multiple times? How to handle TODO
            log.debug("Unknown API key: {}...", apiKey);
            return "api:" + apiKey;
        }

        // Checking for user ID header (for internal/trusted services) TODO: check if it is needed
        String userId = request.getHeader(ApplicationConstants.HEADER_USER_ID);
        if (StringUtils.hasText(userId)) {
            return "user:" + userId;
        }

        // Checking X-Forwarded-For (might contain multiple IPs: client, proxy1, proxy2) TODO: Understand
        String forwardedFor = request.getHeader(ApplicationConstants.HEADER_FORWARDED_FOR);
        if (StringUtils.hasText(forwardedFor)) {
            // Take the first IP (original client)
            String clientIp = forwardedFor.split(",")[0].trim();
            return "ip:" + clientIp;
        }
        // By default rate limit by ip
        return "ip:" + request.getRemoteAddr();
    }

    /**
     * Tier Resolution
     * <ol>
     *     <li>User's tier from DB (when API key is provided)</li>
     *     <li>X-User-Tier header (if provided)</li>
     *     <li>Default tree (free)</li>
     * </ol>
     */
    private String resolveUserTier(HttpServletRequest request) {
        // First check if we resolved a user from API key
        User user = resolvedUser.get();
        if (user != null && user.isEnabled()) {
            return user.getTier().toLowerCase();
        }

        // Fallback to header
        String tier = request.getHeader(ApplicationConstants.HEADER_USER_TIER);
        if (StringUtils.hasText(tier)) {
            return tier.toLowerCase();
        }
        // default to free tier
        return Tiers.FREE.toString();
    }

    private boolean isPathExcluded(String path) {
        return path.startsWith("/actuator") ||
                path.startsWith("/admin") ||
                path.equals("/health") ||
                path.equals("/error") ||
                path.equals("/favicon.ico");
    }

    // Add standard rate limit headers
    private void addRateLimitHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader(ApplicationConstants.HEADER_RATE_LIMIT_LIMIT, String.valueOf(result.getLimit()));
        response.setHeader(ApplicationConstants.HEADER_RATE_LIMIT_REMAINING, String.valueOf(result.getRemainingTokens()));
        response.setHeader(ApplicationConstants.HEADER_RATE_LIMIT_RESET, String.valueOf(result.getResetAtSeconds()));
        if (!result.isAllowed()) {
            long retryAfterSeconds = (long) Math.ceil(result.getRetryAfterMs() / 1000.0);
            response.setHeader(ApplicationConstants.HEADER_RETRY_AFTER, String.valueOf(retryAfterSeconds));
        }
    }
}
