package com.ratelimiter.exception;

import lombok.Getter;

import static com.ratelimiter.utils.ApplicationUtils.maskApiKey;

/**
 * Exception thrown when an API key is not found in the database.
 * Results in HTTP 401 Unauthorized response.
 */
@Getter
public class ApiKeyNotFoundException extends RuntimeException {

    private final String apiKey;

    public ApiKeyNotFoundException(String apiKey) {
        super("Invalid or unknown API key");
        this.apiKey = maskApiKey(apiKey);
    }

}
