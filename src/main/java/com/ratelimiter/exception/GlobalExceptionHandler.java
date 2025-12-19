package com.ratelimiter.exception;

import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.utils.ApplicationConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles rate limit exceeded exceptions
     *
     * @param ex RateLimitExceededException
     * @return JSON body with error details and standard rate limit headers
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RateLimitExceededException ex) {
        ResponseEntity<Map<String, Object>> response = createResponse("Too Many Requests", ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS);

        RateLimitResult result = ex.getRateLimitResult();
        Map<String, Object> rateLimitDetails = new HashMap<>();
        rateLimitDetails.put("limit", result.getLimit());
        rateLimitDetails.put("remaining", result.getRemainingTokens());
        rateLimitDetails.put("resetAt", result.getResetAtSeconds());
        rateLimitDetails.put("retryAfterMs", result.getRetryAfterMs());
        rateLimitDetails.put("retryAfterSeconds", ex.getRetryAfterSeconds());
        response.getBody().put("rateLimitDetails", rateLimitDetails);
        log.debug("Returning 429 response for identifier: {}", ex.getIdentifier());

        response.getHeaders().add(ApplicationConstants.HEADER_RATE_LIMIT_LIMIT, String.valueOf(result.getLimit()));
        response.getHeaders().add(ApplicationConstants.HEADER_RATE_LIMIT_REMAINING, String.valueOf(result.getRemainingTokens()));
        response.getHeaders().add(ApplicationConstants.HEADER_RATE_LIMIT_RESET, String.valueOf(result.getResetAtSeconds()));
        response.getHeaders().add(ApplicationConstants.HEADER_RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()));
        return response;
    }

    /**
     * Handles disabled user access attempts
     *
     * @param ex UserDisabledException
     * @return JSON body with error details (HTTP 403 Forbidden)
     */
    @ExceptionHandler(UserDisabledException.class)
    public ResponseEntity<Map<String, Object>> handleUserDisabled(UserDisabledException ex) {
        log.warn("Disabled user attempted access: userId={}, username={}", ex.getUserId(), ex.getUsername());
        return createResponse("Forbidden", ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    /**
     * Handles invalid/unknown API key access attempts
     *
     * @param ex ApiKeyNotFoundException
     * @return JSON body with error details (HTTP 401 Unauthorized)
     */
    @ExceptionHandler(ApiKeyNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleApiKeyNotFound(ApiKeyNotFoundException ex) {
        log.warn("Unknown API key attempted access: {}", ex.getApiKey());
        return createResponse("Unauthorized", ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return createResponse("Bad Request", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Map<String, Object>> createResponse(String error, String message, HttpStatus status) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        body.put("message", message);
        body.put("status", status.value());
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity
                .status(status)
                .body(body);
    }
}
