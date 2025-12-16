package com.ratelimiter.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for rate limiting with support for multiple user tiers
 * <p>
 * Allows different rate limits for different user types
 * <ul>
 *     <li>FREE tier: Lower limits for free users</li>
 *     <li>PREMIUM tier: Higher limits for paying customers</li>
 *     <li>ENTERPRISE tier: Highest limits for enterprise customers</li>
 * </ul>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rate.limit")
public class RateLimitConfig {

    // Default bucket capacity if no tier specified
    private long defaultCapacity = 10;

    // Default refill rate (tokens/second) if no tier specified
    private double defaultRefillRate = 1.0;

    // Whether rate limiting is enabled globally
    private boolean enabled = true;

    // Storage backend for rate limiting.
    private String storage = "in-memory";

    /**
     * Map of tier name to tier configuration
     * "free" -> TierConfig(capacity=10, refillRate=1, description="free")
     */
    private Map<String, TierConfig> tiers = new HashMap<>();

    // Configuration of a specific user tier
    @Data
    public static class TierConfig {
        // maximum tokens the bucket can hold (burst capacity)
        private long capacity = 10;

        // tokens added per second (sustained rate)
        private double refillRate = 1.0;

        private String description;
    }

    /**
     * Gets the configuration for a specific tier
     * Falls back to default values if tier is not found
     *
     * @param tierName The tier name ("free", "premium", "enterprise")
     * @return TierConfig with capacity and refill rate
     */
    public TierConfig getTierConfig(String tierName) {
        if (tierName == null || !tiers.containsKey(tierName.toLowerCase())) {
            // Return default tier config
            TierConfig defaultConfig = new TierConfig();
            defaultConfig.setCapacity(defaultCapacity);
            defaultConfig.setRefillRate(defaultRefillRate);
            defaultConfig.setDescription("Default tier");
            return defaultConfig;
        }
        return tiers.get(tierName.toLowerCase());
    }

}
