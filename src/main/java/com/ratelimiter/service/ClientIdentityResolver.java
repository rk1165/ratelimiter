package com.ratelimiter.service;

import com.ratelimiter.exception.ApiKeyNotFoundException;
import com.ratelimiter.exception.UserDisabledException;
import com.ratelimiter.model.RateLimitStatus;
import com.ratelimiter.model.Tiers;
import com.ratelimiter.model.User;
import com.ratelimiter.utils.ApplicationConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Resolves client identity from HTTP requests for rate limiting.
 * <p>
 * Provides methods to extract client identifiers (user ID, IP, API key)
 * from request headers for use as rate limit keys.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientIdentityResolver {

    private final UserService userService;

    /**
     * Result of client identity resolution.
     * Contains both the rate limit key and optionally the resolved user.
     */
    public record ResolvedIdentity(String key, User user) {
        public ResolvedIdentity(String key) {
            this(key, null);
        }
    }

    /**
     * Resolves client identity with full user lookup.
     *
     * @param request the HTTP request
     * @return ResolvedIdentity containing the key and optionally the user (for tier lookup)
     */
    public ResolvedIdentity resolveIdentity(HttpServletRequest request) {
        // Check if user is having a valid API Key - Only a registered user can have valid API key
        String apiKey = request.getHeader(ApplicationConstants.HEADER_API_KEY);
        if (StringUtils.hasText(apiKey)) {
            Optional<User> userOpt = userService.getUserByApiKey(apiKey);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (!user.isEnabled()) {
                    log.warn("Disabled user attempted access: userId={}, username={}", user.getId(), user.getUsername());
                    throw new UserDisabledException(user.getId(), user.getUsername());
                }
                // Valid API key → rate limit by API key, and return user to check their tier
                return new ResolvedIdentity("api:" + apiKey, user);
            }
            // Unknown API key → reject request
            log.warn("Unknown API key {} attempted access", apiKey);
            throw new ApiKeyNotFoundException(apiKey);
        } else {
            // A registered user making a request will have HEADER_USER_ID
            log.warn("No API key provided. Checking to see if user is registered.");
        }

        // Registered user makes a call without API key → rate limit by user ID with free tier
        String userId = request.getHeader(ApplicationConstants.HEADER_USER_ID);
        if (StringUtils.hasText(userId)) {
            return new ResolvedIdentity("user:" + userId);
        } else {
            log.warn("User is not registered. Falling back to rate limiting by IP");
        }

        // Non-registered user → rate limit by IP with free tier
        return new ResolvedIdentity(resolveIp(request));
    }

    /**
     * Resolves client IP from request.
     * <p>
     * Checks X-Forwarded-For header first (for clients behind proxy),
     * then falls back to remote address.
     *
     * @param request the HTTP request
     * @return rate limit key prefixed with "ip:"
     */
    public String resolveIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(ApplicationConstants.HEADER_FORWARDED_FOR);
        if (StringUtils.hasText(forwardedFor)) {
            // Take the first IP (original client)
            String clientIp = forwardedFor.split(",")[0].trim();
            return "ip:" + clientIp;
        }
        return "ip:" + request.getRemoteAddr();
    }

    /**
     * Tier Resolution
     * <ol>
     *     <li>User's tier from DB (when API key is provided)</li>
     *     <li>X-User-Tier header (if provided)</li>
     *     <li>Default tier (free)</li>
     * </ol>
     */
    public String resolveUserTier(HttpServletRequest request, User user) {
        // First check if we have a resolved user
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

    /**
     * Adds standard rate limit headers to the HTTP response.
     *
     * @param response the HTTP response (may be null)
     * @param result   the rate limit result containing limit info
     */
    public void addRateLimitHeaders(HttpServletResponse response, RateLimitStatus result) {
        if (response == null || result == null) {
            log.warn("Cannot add rate limit headers: response={}, result={}", response, result);
            return;
        }
        
        response.setHeader(ApplicationConstants.HEADER_RATE_LIMIT_LIMIT, String.valueOf(result.getLimit()));
        response.setHeader(ApplicationConstants.HEADER_RATE_LIMIT_REMAINING, String.valueOf(result.getRemainingTokens()));
        response.setHeader(ApplicationConstants.HEADER_RATE_LIMIT_RESET, String.valueOf(result.getResetAtSeconds()));
        
        if (!result.isAllowed()) {
            long retryAfterSeconds = (long) Math.ceil(result.getRetryAfterMs() / 1000.0);
            response.setHeader(ApplicationConstants.HEADER_RETRY_AFTER, String.valueOf(retryAfterSeconds));
        }
    }
}
