package com.ratelimiter.service;

import com.ratelimiter.model.User;
import com.ratelimiter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing users and their API keys
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // Creates a new user with the given details
    public User createUser(String username, String email, String tier) {
        log.info("Creating new user: username={}, email={}, tier={}", username, email, tier);

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists!" + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists!" + email);
        }

        User user = new User(username, email, tier);
        User savedUser = userRepository.save(user);
        log.info("User created successfully: id={}, apiKey={}", savedUser.getId(), savedUser.getApiKey());
        return savedUser;
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByApiKey(String apiKey) {
        return userRepository.findByApiKey(apiKey);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return (List<User>) userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<User> getUsersByTier(String tier) {
        return userRepository.findByTier(tier.toUpperCase());
    }

    public User updateUser(Long id, String username, String email, String tier) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found!" + id));

        if (username != null && !username.equals(user.getUsername())) {
            if (userRepository.existsByUsername(username)) {
                throw new IllegalArgumentException("Username already exists!" + username);
            }
            user.setUsername(username);
        }

        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("Email already exists!" + email);
            }
            user.setEmail(email);
        }

        if (tier != null) {
            user.updateTier(tier);
        }

        log.info("Updating user: id={}", id);
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found!" + id);
        }
        log.info("Deleting user: id={}", id);
        userRepository.deleteById(id);
    }

    public User rotateApiKey(Long id) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found!" + id));
        String oldKey = user.getApiKey();
        user.rotateApiKey();
        User savedUser = userRepository.save(user);
        log.info("API key rotate for user: id={}, oldKey={}..., newKey={}...",
                id, oldKey.substring(0, 10), savedUser.getApiKey().substring(0, 10));
        return savedUser;
    }

    public User enableUser(Long id) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found!" + id));
        user.enable();
        log.info("User enabled: id={}", id);
        return userRepository.save(user);
    }

    public User disableUser(Long id) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found!" + id));
        user.disable();
        log.info("User disabled: id={}", id);
        return userRepository.save(user);
    }

    public User updateTier(Long id, String tier) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found!" + id));
        user.updateTier(tier);
        log.info("Updating tier for user: id={}, tier={}", id, tier);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public long countByTier(String tier) {
        return userRepository.countByTier(tier.toUpperCase());
    }

    @Transactional(readOnly = true)
    public boolean isApiKeyValid(String apiKey) {
        return userRepository.findByApiKey(apiKey)
                .map(User::isEnabled)
                .orElse(false);
    }
}
