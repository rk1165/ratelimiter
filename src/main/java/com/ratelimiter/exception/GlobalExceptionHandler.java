package com.ratelimiter.exception;

import com.ratelimiter.ApplicationConstants;
import com.ratelimiter.model.RateLimitResult;
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
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Too Many Requests");
        body.put("message", ex.getMessage());
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("timestamp", Instant.now().toString());

        RateLimitResult result = ex.getRateLimitResult();
        Map<String, Object> rateLimitDetails = new HashMap<>();
        rateLimitDetails.put("limit", result.getLimit());
        rateLimitDetails.put("remaining", result.getRemainingTokens());
        rateLimitDetails.put("resetAt", result.getResetAtSeconds());
        rateLimitDetails.put("retryAfterMs", result.getRetryAfterMs());
        rateLimitDetails.put("retryAfterSeconds", ex.getRetryAfterSeconds());
        body.put("rateLimitDetails", rateLimitDetails);
        log.debug("Returning 429 response for identifier: {}", ex.getIdentifier());

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header(ApplicationConstants.HEADER_RATE_LIMIT_LIMIT, String.valueOf(result.getLimit()))
                .header(ApplicationConstants.HEADER_RATE_LIMIT_REMAINING, String.valueOf(result.getRemainingTokens()))
                .header(ApplicationConstants.HEADER_RATE_LIMIT_RESET, String.valueOf(result.getResetAtSeconds()))
                .header(ApplicationConstants.HEADER_RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }
}
