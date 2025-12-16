package com.ratelimiter.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Table("users")
@NoArgsConstructor
@Data
public class User {

    @Id
    private Long id;

    @Column("username")
    private String username;

    @Column("email")
    private String email;

    @Column("password")
    private String password;

    @Column("api_key")
    private String apiKey;

    @Column("tier")
    private String tier;

    @Column("role")
    private String role;

    @Column("enabled")
    private boolean enabled;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    // Constructor for creating new users (without password - for API key based users)
    public User(String userName, String email, String tier) {
        this.username = userName;
        this.email = email;
        this.tier = StringUtils.hasText(tier) ? tier.toUpperCase() : "FREE";
        this.role = "USER";
        this.apiKey = generateApiKey();
        this.enabled = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Constructor for creating new users with Password
    public User(String username, String email, String password, String tier, String role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.tier = StringUtils.hasText(tier) ? tier.toUpperCase() : "FREE";
        this.role = StringUtils.hasText(role) ? role.toUpperCase() : "USER";
        this.apiKey = generateApiKey();
        this.enabled = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Generates a new unique API key
    public static String generateApiKey() {
        return "rl_" + UUID.randomUUID().toString().replaceAll("-", "");
    }

    // Rotates the API key and updates the timestamp
    public void rotateApiKey() {
        this.apiKey = generateApiKey();
        this.updatedAt = Instant.now();
    }

    // Enables the user account
    public void enable() {
        this.enabled = true;
        this.updatedAt = Instant.now();
    }

    // Disables the user account
    public void disable() {
        this.enabled = false;
        this.updatedAt = Instant.now();
    }

    // Updates the tier and timestamp
    public void updateTier(String tier) {
        this.tier = tier.toUpperCase();
        this.updatedAt = Instant.now();
    }

    public String toString() {
        return "User={" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", tier='" + tier + '\'' +
                ", enabled='" + enabled +
                ", createdAt=" + createdAt +
                '}';
    }
}
