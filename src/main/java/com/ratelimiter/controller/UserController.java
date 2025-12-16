package com.ratelimiter.controller;

import com.ratelimiter.dto.UserRequest;
import com.ratelimiter.dto.UserResponse;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.User;
import com.ratelimiter.service.RateLimiter;
import com.ratelimiter.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/users")
public class UserController {

    private final UserService userService;
    private final RateLimiter rateLimiter;

    public UserController(UserService userService, RateLimiter rateLimiter) {
        this.userService = userService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Create a new user.
     * POST /admin/users
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        log.info("Creating user: username={}, email={}", request.getUsername(), request.getEmail());
        User user = userService.createUser(request.getUsername(), request.getEmail(), request.getTier());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromUser(user));
    }

    /**
     * Get all users.
     * GET /admin/users
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(@RequestParam(required = false) String tier) {
        List<User> users;
        if (StringUtils.hasText(tier)) {
            users = userService.getUsersByTier(tier);
        } else {
            users = userService.getAllUsers();
        }
        List<UserResponse> responses = users.stream()
                .map(UserResponse::fromUserWithoutApiKey)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Get a user by id
     * GET /admin/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(UserResponse.fromUserWithoutApiKey(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a user.
     * PUT /admin/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@Valid @PathVariable Long id, @Valid @RequestBody UserRequest request) {
        log.info("Updating user: id={}", id);
        User user = userService.updateUser(
                id,
                request.getUsername(),
                request.getEmail(),
                request.getTier()
        );
        return ResponseEntity.ok(UserResponse.fromUserWithoutApiKey(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Deleting user: id={}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rotate-key")
    public ResponseEntity<UserResponse> rotateApiKey(@PathVariable Long id) {
        log.info("Rotating API key for user: id={}", id);
        User user = userService.rotateApiKey(id);
        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<UserResponse> enableUser(@PathVariable Long id) {
        log.info("Enabling user: id={}", id);
        User user = userService.enableUser(id);
        return ResponseEntity.ok(UserResponse.fromUserWithoutApiKey(user));
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<UserResponse> disableUser(@PathVariable Long id) {
        log.info("Disabling user: id={}", id);
        User user = userService.disableUser(id);
        return ResponseEntity.ok(UserResponse.fromUserWithoutApiKey(user));
    }

    @PatchMapping("/{id}/tier")
    public ResponseEntity<UserResponse> updateUserTier(@PathVariable Long id, @RequestParam String tier) {

        log.info("Updating user tier: id={}, tier={}", id, tier);
        User user = userService.updateTier(id, tier);
        return ResponseEntity.ok(UserResponse.fromUserWithoutApiKey(user));
    }

    @GetMapping("/{id}/usage")
    public ResponseEntity<Map<String, Object>> getUserUsage(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> {
                    String rateLimitKey = "user:" + user.getId();
                    RateLimitResult rateLimitResult = rateLimiter.peek(rateLimitKey);
                    Map<String, Object> usage = new HashMap<>();
                    usage.put("userId", user.getId());
                    usage.put("username", user.getUsername());
                    usage.put("tier", user.getTier());
                    usage.put("enabled", user.isEnabled());
                    usage.put("rateLimitKey", rateLimitKey);
                    usage.put("remainingTokens", rateLimitResult.getRemainingTokens());
                    usage.put("limit", rateLimitResult.getLimit());
                    usage.put("resetAtSeconds", rateLimitResult.getResetAtSeconds());
                    return ResponseEntity.ok(usage);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/reset-limit")
    public ResponseEntity<Map<String, Object>> resetUserRateLimit(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> {
                    String rateLimitKey = "user:" + user.getId();
                    rateLimiter.reset(rateLimitKey);
                    Map<String, Object> response = new HashMap<>();
                    response.put("userId", user.getId());
                    response.put("username", user.getUsername());
                    response.put("message", "Rate limit reset successfully");
                    log.info("Rate limit reset for user: id={}", id);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUser", userService.getAllUsers().size());
        stats.put("freeUsers", userService.countByTier("FREE"));
        stats.put("premiumUsers", userService.countByTier("PREMIUM"));
        stats.put("enterpriseUsers", userService.countByTier("ENTERPRISE"));
        return ResponseEntity.ok(stats);
    }

}
