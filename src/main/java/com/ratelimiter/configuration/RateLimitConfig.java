package com.ratelimiter.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for rate limiting with support for multiple user tiers.
 * <p>
 * Supports hierarchical tier access where higher tiers can access lower tier endpoints
 * with their own rate limits, and lower tiers get degraded (grace) access to higher tier endpoints.
 * <p>
 * Tier hierarchy (lowest to highest): free → premium → enterprise
 * <ul>
 *     <li>FREE tier: Basic limits for free users</li>
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

    // Storage backend for rate limiting.
    private String storage = "in-memory";

    // Tier hierarchy from lowest to highest priority.
    private List<String> tierHierarchy = List.of("free", "premium", "enterprise");

    /**
     * Map of tier name to tier configuration
     * "free" -> TierConfig(capacity=10, refillRate=1, description="free")
     */
    private Map<String, TierConfig> tiers = new HashMap<>();

    /**
     * Configuration of a specific user tier.
     * <p>
     * The tier's capacity and refillRate define:
     * <ul>
     *     <li>Full rate limits when accessing endpoints at or below this tier</li>
     *     <li>Grace limits when accessing endpoints above this tier</li>
     * </ul>
     */
    @Data
    public static class TierConfig {
        // Maximum tokens the bucket can hold (burst capacity)
        private long capacity = 10;

        // Tokens added per second (sustained rate)
        private double refillRate = 1.0;

        private String description;
    }

    /**
     * Gets the configuration for a specific tier.
     * Falls back to default values if tier is not found.
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

    /**
     * Gets the priority level of a tier (higher = more privileged).
     *
     * @param tierName The tier name
     * @return Priority level (0 = lowest), or -1 if tier not in hierarchy
     */
    public int getTierPriority(String tierName) {
        if (tierName == null) {
            return 0; // Treat null as lowest tier (free)
        }
        int index = tierHierarchy.indexOf(tierName.toLowerCase());
        return Math.max(index, 0); // Default to lowest if not found
    }

    /**
     * Checks if a user's tier meets or exceeds the required tier.
     *
     * @param userTier     The user's tier
     * @param requiredTier The tier required by the endpoint
     * @return true if user has sufficient tier level
     */
    public boolean hasAccess(String userTier, String requiredTier) {
        return getTierPriority(userTier) >= getTierPriority(requiredTier);
    }

}
