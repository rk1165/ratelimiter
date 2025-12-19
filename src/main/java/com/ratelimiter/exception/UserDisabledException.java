package com.ratelimiter.exception;

import lombok.Getter;

/**
 * Exception thrown when a disabled user attempts to access the API.
 * Results in HTTP 403 Forbidden response.
 */
@Getter
public class UserDisabledException extends RuntimeException {

    private final Long userId;
    private final String username;

    public UserDisabledException(Long userId, String username) {
        super(String.format("User account is disabled: %s (id=%d)", username, userId));
        this.userId = userId;
        this.username = username;
    }

}

