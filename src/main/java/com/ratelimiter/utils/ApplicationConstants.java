package com.ratelimiter.utils;

public class ApplicationConstants {

    // HTTP Headers
    public static final String HEADER_API_KEY = "X-API-Key";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_TIER = "X-User-Tier";
    public static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";

    // Rate Limit Response Headers
    public static final String HEADER_RATE_LIMIT_LIMIT = "X-RateLimit-Limit";
    public static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_RATE_LIMIT_RESET = "X-RateLimit-Reset";
    public static final String HEADER_RETRY_AFTER = "X-Retry-After";

    public static final String KEY_PREFIX = "rate_limit:";

    // Lua Script for Token Bucket Rate Limiter
    public static final String TOKEN_BUCKET_SCRIPT = """
                -- KEYS[1] = bucket key (e.g. "rate_limit:user:123")
                -- ARGV[1] = bucket capacity (max tokens)
                -- ARGV[2] = bucket refill rate (tokens per second)
                -- ARGV[3] = tokens requested (usually 1)
                -- ARGV[4] = current time in milliseconds
            
                local key = KEYS[1]
                local capacity = tonumber(ARGV[1])
                local refillRate = tonumber(ARGV[2])
                local tokensRequested = tonumber(ARGV[3])
                local currentTimeMs = tonumber(ARGV[4])
            
                -- Get current bucket state
                local bucket = redis.call('HMGET', key, 'tokens', 'lastRefillTime')
                local tokens = tonumber(bucket[1])
                local lastRefillTime = tonumber(bucket[2])
            
                -- Initialize the bucket if it doesn't exist
                if tokens == nil then
                    tokens = capacity
                    lastRefillTime = currentTimeMs
                end
            
                -- Calculate tokens to add based on elapsed time
                local elapsedMs = currentTimeMs - lastRefillTime
                local tokensToAdd = math.floor(elapsedMs * refillRate / 1000)
            
                -- Refill tokens (capped at capacity)
                tokens = math.min(capacity, tokens + tokensToAdd)
            
                -- Update lastRefillTime only if tokens were added
                if tokensToAdd > 0 then
                    lastRefillTime = currentTimeMs
                end
            
                -- Check if we have enough tokens
                local allowed = 0
                local retryAfterMs = 0
            
                if tokens >= tokensRequested then
                    tokens = tokens - tokensRequested
                    allowed = 1
                else
                    -- Calculate when tokens will be available
                    local tokensNeeded = tokensRequested - tokens
                    retryAfterMs = math.ceil(tokensNeeded * 1000 / refillRate)
                end
            
                -- Save bucket state with TTL = 2x time to refill the bucket
                local ttl = math.ceil(capacity / refillRate * 2)
                redis.call('HMSET', key, 'tokens', tokens, 'lastRefillTime', lastRefillTime)
                redis.call('EXPIRE', key, ttl)
            
                -- Return {allowed (0/1), remaining tokens, retry after in ms}
                return {allowed, tokens, retryAfterMs}
            """;

}
