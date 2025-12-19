-- ===========================================
-- Test Users Data
-- ===========================================
-- This file contains sample users for testing different scenarios:
-- 1. Enabled users with different tiers (FREE, PREMIUM, ENTERPRISE)
-- 2. Disabled users
-- 3. Different roles (USER, ADMIN)

-- Clear existing test data (optional - comment out if you want to preserve data)
-- DELETE FROM users WHERE username LIKE 'test_%';

-- ===========================================
-- ENABLED USERS - Different Tiers
-- ===========================================

-- Free tier enabled user
INSERT INTO users (username, email, api_key, tier, role, enabled, created_at, updated_at)
VALUES ('test_free_user', 'free@test.com', 'rl_free_user_api_key_12345678', 'FREE', 'USER', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Premium tier enabled user
INSERT INTO users (username, email, api_key, tier, role, enabled, created_at, updated_at)
VALUES ('test_premium_user', 'premium@test.com', 'rl_premium_user_api_key_1234', 'PREMIUM', 'USER', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Enterprise tier enabled user
INSERT INTO users (username, email, api_key, tier, role, enabled, created_at, updated_at)
VALUES ('test_enterprise_user', 'enterprise@test.com', 'rl_enterprise_api_key_12345', 'ENTERPRISE', 'USER', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ===========================================
-- DISABLED USERS - Different Tiers
-- ===========================================

-- Disabled free tier user
INSERT INTO users (username, email, api_key, tier, role, enabled, created_at, updated_at)
VALUES ('test_disabled_free', 'disabled_free@test.com', 'rl_disabled_free_api_key_12', 'FREE', 'USER', FALSE, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Disabled premium tier user
INSERT INTO users (username, email, api_key, tier, role, enabled, created_at, updated_at)
VALUES ('test_disabled_premium', 'disabled_premium@test.com', 'rl_disabled_premium_api_123', 'PREMIUM', 'USER', FALSE, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Disabled enterprise tier user
INSERT INTO users (username, email, api_key, tier, role, enabled, created_at, updated_at)
VALUES ('test_disabled_enterprise', 'disabled_enterprise@test.com', 'rl_disabled_enterprise_123', 'ENTERPRISE', 'USER', FALSE, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ===========================================
-- ADMIN USERS
-- ===========================================

-- Enabled admin user
INSERT INTO users (username, email, api_key, tier, role, enabled, created_at, updated_at)
VALUES ('test_admin', 'admin@test.com', 'rl_admin_api_key_123456789', 'ENTERPRISE', 'ADMIN', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Disabled admin user
INSERT INTO users (username, email, api_key, tier, role, enabled, created_at, updated_at)
VALUES ('test_disabled_admin', 'disabled_admin@test.com', 'rl_disabled_admin_api_12345', 'ENTERPRISE', 'ADMIN', FALSE, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

