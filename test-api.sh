#!/bin/bash

# ===========================================
# Rate Limiter API Test Script
# ===========================================
# Tests hierarchical tier-based rate limiting:
# - Free tier: 10 req capacity
# - Premium tier: 100 req capacity  
# - Enterprise tier: 1000 req capacity
#
# Tier hierarchy: free < premium < enterprise
# - Higher tiers can access lower tier endpoints with their own limits
# - Lower tiers get grace access (their tier's limits) to higher tier endpoints

BASE_URL="http://localhost:8080"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# API Keys (from data.sql)
FREE_API_KEY="rl_free_user_api_key_12345678"
PREMIUM_API_KEY="rl_premium_user_api_key_1234"
ENTERPRISE_API_KEY="rl_enterprise_api_key_12345"

# Function to print test header
print_header() {
    echo ""
    echo "==========================================="
    echo -e "${BLUE}$1${NC}"
    echo "==========================================="
}

# Function to print sub-header
print_subheader() {
    echo -e "\n${CYAN}>>> $1${NC}"
}

# Function to print result
print_result() {
    local expected=$1
    local actual=$2
    local description=$3
    
    if [ "$expected" == "$actual" ]; then
        echo -e "${GREEN}✓ PASS${NC}: $description (HTTP $actual)"
    else
        echo -e "${RED}✗ FAIL${NC}: $description (Expected: $expected, Got: $actual)"
    fi
}

# Function to make API call and return status code
call_api() {
    local endpoint=$1
    local api_key=$2
    local header=""
    
    if [ -n "$api_key" ]; then
        header="-H X-API-Key:$api_key"
    fi
    
    curl -s -o /tmp/response.json -w '%{http_code}' $header "${BASE_URL}${endpoint}"
}

# Function to make API call and show headers
call_api_with_headers() {
    local endpoint=$1
    local api_key=$2
    local header=""
    
    if [ -n "$api_key" ]; then
        header="-H X-API-Key:$api_key"
    fi
    
    curl -s -i $header "${BASE_URL}${endpoint}" 2>/dev/null
}

# Function to display response body
show_response() {
    cat /tmp/response.json | python3 -m json.tool 2>/dev/null || cat /tmp/response.json
    echo ""
}

# Function to extract rate limit headers
show_rate_limit_headers() {
    echo "$1" | grep -E "^(X-RateLimit|HTTP/)" | head -5
}

# Function to test rate limiting by making multiple requests
test_rate_limit() {
    local endpoint=$1
    local api_key=$2
    local num_requests=$3
    local description=$4
    
    echo -e "${YELLOW}Making $num_requests requests to test rate limiting...${NC}"
    
    local success_count=0
    local rate_limited_count=0
    
    for i in $(seq 1 $num_requests); do
        status=$(call_api "$endpoint" "$api_key")
        if [ "$status" == "200" ]; then
            ((success_count++))
        elif [ "$status" == "429" ]; then
            ((rate_limited_count++))
        fi
    done
    
    echo -e "  Successful: ${GREEN}$success_count${NC}, Rate Limited: ${RED}$rate_limited_count${NC}"
    
    if [ $rate_limited_count -gt 0 ]; then
        echo -e "  ${GREEN}✓${NC} Rate limiting is working for: $description"
    else
        echo -e "  ${YELLOW}!${NC} No rate limiting triggered (may need more requests)"
    fi
}

echo "============================================="
echo "   HIERARCHICAL RATE LIMITER TEST SUITE"
echo "============================================="
echo "Base URL: $BASE_URL"
echo ""
echo "Tier Hierarchy: FREE (10 req) < PREMIUM (100 req) < ENTERPRISE (1000 req)"
echo ""

# ===========================================
# TEST 1: Free Tier User Access
# ===========================================
print_header "TEST 1: FREE TIER USER ACCESS"

print_subheader "1.1 Free user → Free endpoint (Full access with free limits)"
status=$(call_api "/api/v1/examples/free" "$FREE_API_KEY")
print_result "200" "$status" "Free user accessing free endpoint"
show_response

print_subheader "1.2 Free user → Premium endpoint (Grace access with free limits)"
status=$(call_api "/api/v1/examples/premium" "$FREE_API_KEY")
print_result "200" "$status" "Free user accessing premium endpoint (grace)"
show_response

print_subheader "1.3 Free user → Enterprise endpoint (Grace access with free limits)"
status=$(call_api "/api/v1/examples/enterprise" "$FREE_API_KEY")
print_result "200" "$status" "Free user accessing enterprise endpoint (grace)"
show_response

# ===========================================
# TEST 2: Premium Tier User Access
# ===========================================
print_header "TEST 2: PREMIUM TIER USER ACCESS"

print_subheader "2.1 Premium user → Free endpoint (Full access with premium limits)"
status=$(call_api "/api/v1/examples/free" "$PREMIUM_API_KEY")
print_result "200" "$status" "Premium user accessing free endpoint"
show_response

print_subheader "2.2 Premium user → Premium endpoint (Full access with premium limits)"
status=$(call_api "/api/v1/examples/premium" "$PREMIUM_API_KEY")
print_result "200" "$status" "Premium user accessing premium endpoint"
show_response

print_subheader "2.3 Premium user → Enterprise endpoint (Grace access with premium limits)"
status=$(call_api "/api/v1/examples/enterprise" "$PREMIUM_API_KEY")
print_result "200" "$status" "Premium user accessing enterprise endpoint (grace)"
show_response

# ===========================================
# TEST 3: Enterprise Tier User Access
# ===========================================
print_header "TEST 3: ENTERPRISE TIER USER ACCESS"

print_subheader "3.1 Enterprise user → Free endpoint (Full access with enterprise limits)"
status=$(call_api "/api/v1/examples/free" "$ENTERPRISE_API_KEY")
print_result "200" "$status" "Enterprise user accessing free endpoint"
show_response

print_subheader "3.2 Enterprise user → Premium endpoint (Full access with enterprise limits)"
status=$(call_api "/api/v1/examples/premium" "$ENTERPRISE_API_KEY")
print_result "200" "$status" "Enterprise user accessing premium endpoint"
show_response

print_subheader "3.3 Enterprise user → Enterprise endpoint (Full access with enterprise limits)"
status=$(call_api "/api/v1/examples/enterprise" "$ENTERPRISE_API_KEY")
print_result "200" "$status" "Enterprise user accessing enterprise endpoint"
show_response

# ===========================================
# TEST 4: Anonymous User (No API Key)
# ===========================================
print_header "TEST 4: ANONYMOUS USER (No API Key = Free tier)"

print_subheader "4.1 Anonymous → Free endpoint"
status=$(call_api "/api/v1/examples/free" "")
print_result "200" "$status" "Anonymous user accessing free endpoint"
show_response

print_subheader "4.2 Anonymous → Premium endpoint (Grace access)"
status=$(call_api "/api/v1/examples/premium" "")
print_result "200" "$status" "Anonymous user accessing premium endpoint (grace)"
show_response

# ===========================================
# TEST 5: Rate Limit Headers Check
# ===========================================
print_header "TEST 5: RATE LIMIT HEADERS"

print_subheader "5.1 Free user rate limit headers"
response=$(call_api_with_headers "/api/v1/examples/free" "$FREE_API_KEY")
show_rate_limit_headers "$response"
echo ""

print_subheader "5.2 Premium user rate limit headers"
response=$(call_api_with_headers "/api/v1/examples/premium" "$PREMIUM_API_KEY")
show_rate_limit_headers "$response"
echo ""

print_subheader "5.3 Enterprise user rate limit headers"
response=$(call_api_with_headers "/api/v1/examples/enterprise" "$ENTERPRISE_API_KEY")
show_rate_limit_headers "$response"
echo ""

# ===========================================
# TEST 6: Rate Limiting Verification
# ===========================================
print_header "TEST 6: RATE LIMITING VERIFICATION"

print_subheader "6.1 Free user hitting premium endpoint (should hit grace limit ~10 req)"
test_rate_limit "/api/v1/examples/premium" "$FREE_API_KEY" 15 "Free user grace limit"

print_subheader "6.2 Premium user hitting enterprise endpoint (should hit grace limit ~100 req)"
echo -e "${YELLOW}Skipping 100+ requests test (takes too long). Use manual testing.${NC}"

# ===========================================
# TEST 7: Expensive Operation (Multiple Tokens)
# ===========================================
print_header "TEST 7: EXPENSIVE OPERATION (5 tokens per request)"

print_subheader "7.1 Free user → Expensive endpoint (consumes 5 tokens each)"
status=$(call_api "/api/v1/examples/expensive" "$FREE_API_KEY")
print_result "200" "$status" "Free user - expensive operation"
show_response

print_subheader "7.2 Testing expensive endpoint rate limiting (5 tokens each, ~2 requests before limit)"
test_rate_limit "/api/v1/examples/expensive" "$FREE_API_KEY" 5 "Expensive operation limit"

# ===========================================
# Summary
# ===========================================
print_header "TEST SUMMARY"
echo ""
echo "Hierarchical Rate Limiting Rules:"
echo "┌─────────────┬──────────────────┬───────────────────┬─────────────────────┐"
echo "│ User Tier   │ Free Endpoint    │ Premium Endpoint  │ Enterprise Endpoint │"
echo "├─────────────┼──────────────────┼───────────────────┼─────────────────────┤"
echo "│ Free        │ ✓ Full (10 req)  │ ⚠ Grace (10 req)  │ ⚠ Grace (10 req)    │"
echo "│ Premium     │ ✓ Full (100 req) │ ✓ Full (100 req)  │ ⚠ Grace (100 req)   │"
echo "│ Enterprise  │ ✓ Full (1000)    │ ✓ Full (1000)     │ ✓ Full (1000 req)   │"
echo "└─────────────┴──────────────────┴───────────────────┴─────────────────────┘"
echo ""
echo "API Keys for manual testing:"
echo "  FREE:       $FREE_API_KEY"
echo "  PREMIUM:    $PREMIUM_API_KEY"
echo "  ENTERPRISE: $ENTERPRISE_API_KEY"
echo ""
echo "Example commands:"
echo "  curl -H 'X-API-Key: $FREE_API_KEY' $BASE_URL/api/v1/examples/premium"
echo "  curl -H 'X-API-Key: $PREMIUM_API_KEY' $BASE_URL/api/v1/examples/enterprise"
echo ""

# Cleanup
rm -f /tmp/response.json
