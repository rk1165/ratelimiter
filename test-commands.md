# Rate Limiter API Test Commands

Quick reference for testing the API with different user scenarios.

## Prerequisites

1. Start MySQL and ensure the database is running
2. Start the application: `./mvnw spring-boot:run`
3. The test users will be automatically created from `data.sql`

---

## Test 1: Enabled Users (Should return 200 OK)

### Free Tier User
```bash
curl -i -H "X-API-Key: rl_free_user_api_key_12345678" http://localhost:8080/api/v1/test
```

### Premium Tier User
```bash
curl -i -H "X-API-Key: rl_premium_user_api_key_1234" http://localhost:8080/api/v1/test
```

### Enterprise Tier User
```bash
curl -i -H "X-API-Key: rl_enterprise_api_key_12345" http://localhost:8080/api/v1/test
```

### Admin User
```bash
curl -i -H "X-API-Key: rl_admin_api_key_123456789" http://localhost:8080/api/v1/test
```

---

## Test 2: Disabled Users (Should return 403 Forbidden)

### Disabled Free User
```bash
curl -i -H "X-API-Key: rl_disabled_free_api_key_12" http://localhost:8080/api/v1/test
```

### Disabled Premium User
```bash
curl -i -H "X-API-Key: rl_disabled_premium_api_123" http://localhost:8080/api/v1/test
```

### Disabled Enterprise User
```bash
curl -i -H "X-API-Key: rl_disabled_enterprise_123" http://localhost:8080/api/v1/test
```

### Disabled Admin User
```bash
curl -i -H "X-API-Key: rl_disabled_admin_api_12345" http://localhost:8080/api/v1/test
```

---

## Test 3: Invalid API Keys (Should return 401 Unauthorized)

### Unknown API Key
```bash
curl -i -H "X-API-Key: rl_this_key_does_not_exist" http://localhost:8080/api/v1/test
```

### Random Invalid Key
```bash
curl -i -H "X-API-Key: invalid_random_key_12345" http://localhost:8080/api/v1/test
```

---

## Test 4: No API Key (Should return 200 OK - IP-based rate limiting)

### No Headers
```bash
curl -i http://localhost:8080/api/v1/test
```

### With User ID Only (no API key)
```bash
curl -i -H "X-User-Id: anonymous_user_123" http://localhost:8080/api/v1/test
```

---

## Test 5: Rate Limit Status Check

### Check status for a key
```bash
curl -i "http://localhost:8080/api/v1/limit/status?key=api:rl_premium_user_api_key_1234"
```

---

## Test 6: View Tier Configuration

```bash
curl -s http://localhost:8080/api/v1/limit/config | python3 -m json.tool
```

---

## Expected Responses

### 200 OK (Success)
```json
{
  "message": "Request successful!",
  "timestamp": "2025-12-19T..."
}
```

### 401 Unauthorized (Invalid API Key)
```json
{
  "error": "Unauthorized",
  "message": "Invalid or unknown API key. Please provide a valid API key.",
  "status": 401,
  "timestamp": "2025-12-19T..."
}
```

### 403 Forbidden (Disabled User)
```json
{
  "error": "Forbidden",
  "message": "User account is disabled. Please contact support to reactivate your account.",
  "status": 403,
  "timestamp": "2025-12-19T..."
}
```

### 429 Too Many Requests (Rate Limited)
```json
{
  "error": "Too Many Requests",
  "message": "Rate limit exceeded for '...'",
  "status": 429,
  "rateLimitDetails": {
    "limit": 10,
    "remaining": 0,
    "resetAt": 1734567890,
    "retryAfterMs": 1000
  }
}
```

---

## Run Full Test Suite

Execute the automated test script:
```bash
./test-api.sh
```

---

## SQL Queries for Database Verification

### View all test users
```sql
SELECT id, username, email, api_key, tier, role, enabled 
FROM users 
WHERE username LIKE 'test_%';
```

### Toggle user enabled status
```sql
-- Disable a user
UPDATE users SET enabled = FALSE WHERE username = 'test_free_user';

-- Enable a user
UPDATE users SET enabled = TRUE WHERE username = 'test_disabled_free';
```

### Check specific user
```sql
SELECT * FROM users WHERE api_key = 'rl_free_user_api_key_12345678';
```

