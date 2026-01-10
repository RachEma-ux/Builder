#!/bin/bash
# GitHub OAuth Verification Script for Builder
# This script checks if GitHub OAuth is correctly configured

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo ""
echo "========================================"
echo "  GitHub OAuth Configuration Checker"
echo "========================================"
echo ""

# Step 1: Check if CLIENT_ID is configured in code
echo "📋 Step 1: Checking CLIENT_ID in code..."
CLIENT_ID_LINE=$(grep "const val CLIENT_ID" data/remote/github/GitHubOAuthService.kt | grep -v "^//" || true)

if [ -z "$CLIENT_ID_LINE" ]; then
    echo -e "${RED}❌ ERROR: Could not find CLIENT_ID in GitHubOAuthService.kt${NC}"
    exit 1
fi

# Extract the CLIENT_ID value
CLIENT_ID=$(echo "$CLIENT_ID_LINE" | sed -n 's/.*CLIENT_ID = "\([^"]*\)".*/\1/p')

echo "   Found: $CLIENT_ID"
echo ""

# Step 2: Check if it's still a placeholder
echo "🔍 Step 2: Verifying CLIENT_ID is not a placeholder..."

if [[ "$CLIENT_ID" == *"PLACEHOLDER"* ]] || [[ "$CLIENT_ID" == *"placeholder"* ]] || [[ "$CLIENT_ID" == *"UPDATE_ME"* ]]; then
    echo -e "${RED}❌ FAIL: CLIENT_ID is still a placeholder!${NC}"
    echo ""
    echo "   Current value: $CLIENT_ID"
    echo ""
    echo "   📝 ACTION REQUIRED:"
    echo "   1. Register GitHub OAuth app: https://github.com/settings/applications/new"
    echo "   2. Copy your Client ID (starts with 'Ov' or 'Iv1.')"
    echo "   3. Update line 31 in: data/remote/github/GitHubOAuthService.kt"
    echo ""
    echo "   For detailed instructions, see: docs/GITHUB_OAUTH_SETUP.md"
    echo ""
    exit 1
fi

# Step 3: Validate CLIENT_ID format
echo "✅ CLIENT_ID is configured (not a placeholder)"
echo ""
echo "🔍 Step 3: Validating CLIENT_ID format..."

# GitHub Client IDs start with "Ov" (new format) or "Iv1." (old format)
if [[ "$CLIENT_ID" =~ ^Ov[a-zA-Z0-9]{16,}$ ]] || [[ "$CLIENT_ID" =~ ^Iv1\.[a-f0-9]{16}$ ]]; then
    echo -e "${GREEN}✅ PASS: CLIENT_ID format looks valid${NC}"
    echo "   Format: GitHub OAuth Client ID"
else
    echo -e "${YELLOW}⚠️  WARNING: CLIENT_ID format seems unusual${NC}"
    echo "   Expected: Starts with 'Ov' or 'Iv1.'"
    echo "   Found: $CLIENT_ID"
    echo ""
    echo "   This might still work, but double-check your Client ID from:"
    echo "   https://github.com/settings/developers"
fi

echo ""

# Step 4: Test OAuth endpoint (optional, requires network)
echo "🌐 Step 4: Testing GitHub OAuth endpoint..."

if command -v curl &> /dev/null; then
    echo "   Sending test request to GitHub..."

    # Make request to GitHub device flow endpoint
    RESPONSE=$(curl -s -X POST \
        -H "Accept: application/json" \
        -d "client_id=$CLIENT_ID&scope=repo" \
        https://github.com/login/device/code)

    # Check if response contains error
    if echo "$RESPONSE" | grep -q '"error"'; then
        ERROR_TYPE=$(echo "$RESPONSE" | grep -o '"error":"[^"]*"' | cut -d'"' -f4)
        ERROR_DESC=$(echo "$RESPONSE" | grep -o '"error_description":"[^"]*"' | cut -d'"' -f4)

        echo -e "${RED}❌ FAIL: GitHub API returned an error${NC}"
        echo ""
        echo "   Error: $ERROR_TYPE"
        echo "   Description: $ERROR_DESC"
        echo ""

        if [ "$ERROR_TYPE" = "invalid_client" ]; then
            echo "   📝 SOLUTION:"
            echo "   - Verify your Client ID is correct"
            echo "   - Check https://github.com/settings/developers"
            echo "   - Ensure the OAuth app wasn't deleted"
        fi

        echo ""
        exit 1
    fi

    # Check if we got a device code (success)
    if echo "$RESPONSE" | grep -q '"device_code"'; then
        echo -e "${GREEN}✅ PASS: GitHub OAuth endpoint responded successfully${NC}"

        # Extract device code for verification
        if command -v jq &> /dev/null; then
            DEVICE_CODE=$(echo "$RESPONSE" | jq -r '.device_code')
            USER_CODE=$(echo "$RESPONSE" | jq -r '.user_code')

            echo "   Device Code: ${DEVICE_CODE:0:20}..."
            echo "   User Code: $USER_CODE"
        fi
    else
        echo -e "${YELLOW}⚠️  WARNING: Unexpected response from GitHub${NC}"
        echo "   Response: $RESPONSE"
    fi
else
    echo -e "${YELLOW}⚠️  SKIP: curl not available, skipping network test${NC}"
fi

echo ""

# Step 5: Check callback URL in AndroidManifest
echo "📱 Step 5: Checking Android deep link configuration..."

if [ -f "app/src/main/AndroidManifest.xml" ]; then
    if grep -q 'android:scheme="builder"' app/src/main/AndroidManifest.xml && \
       grep -q 'android:host="oauth"' app/src/main/AndroidManifest.xml; then
        echo -e "${GREEN}✅ PASS: Deep link configured in AndroidManifest${NC}"
        echo "   Callback URL: builder://oauth/callback"
    else
        echo -e "${YELLOW}⚠️  WARNING: Deep link configuration not found${NC}"
        echo "   Make sure AndroidManifest.xml includes:"
        echo '   <data android:scheme="builder" android:host="oauth"/>'
    fi
else
    echo -e "${YELLOW}⚠️  SKIP: AndroidManifest.xml not found${NC}"
fi

echo ""

# Final Summary
echo "========================================"
echo "  Verification Summary"
echo "========================================"
echo ""
echo -e "${GREEN}✅ CLIENT_ID is configured${NC}"
echo "   Value: $CLIENT_ID"
echo ""

if echo "$RESPONSE" | grep -q '"device_code"' 2>/dev/null; then
    echo -e "${GREEN}✅ GitHub OAuth app is correctly registered${NC}"
    echo -e "${GREEN}✅ API communication is working${NC}"
    echo ""
    echo "🎉 SUCCESS! GitHub OAuth is properly configured."
    echo ""
    echo "Next steps:"
    echo "1. Build the app: ./gradlew assembleDebug"
    echo "2. Install on device: adb install -r app/build/outputs/apk/debug/app-debug.apk"
    echo "3. Test OAuth flow in the app"
else
    echo -e "${BLUE}ℹ️  Configuration appears correct${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Build and test the app: ./gradlew assembleDebug"
    echo "2. Verify OAuth flow works in the app"
    echo "3. If issues occur, see: docs/GITHUB_OAUTH_VERIFICATION.md"
fi

echo ""
echo "========================================"
echo ""

# Check if GitHub OAuth app settings match
echo "📝 Reminder: Verify these settings in your GitHub OAuth app:"
echo "   (https://github.com/settings/developers)"
echo ""
echo "   Application name: Builder - Mobile Orchestration (or similar)"
echo "   Homepage URL: https://github.com/RachEma-ux/Builder"
echo "   Authorization callback URL: builder://oauth/callback"
echo ""

exit 0
