#!/bin/bash
# ============================================
# Builder - One-Tap Installer
# ============================================
# Run with:
#   curl -sSL https://raw.githubusercontent.com/RachEma-ux/Builder/main/get-builder.sh | bash
# ============================================

set -e

REPO="RachEma-ux/Builder"

echo ""
echo "======================================"
echo "   Builder - One-Tap Installer"
echo "======================================"
echo ""

# Setup
DOWNLOAD_DIR="$HOME/Downloads"
mkdir -p "$DOWNLOAD_DIR"

# Get latest release info
echo "[1/4] Fetching latest release..."
RELEASE_JSON=$(curl -sL "https://api.github.com/repos/$REPO/releases/latest")
VERSION=$(echo "$RELEASE_JSON" | grep '"tag_name"' | head -1 | cut -d'"' -f4)
APK_URL=$(echo "$RELEASE_JSON" | grep '"browser_download_url"' | grep '\.apk"' | grep -v sha256 | head -1 | cut -d'"' -f4)
SHA_URL=$(echo "$RELEASE_JSON" | grep '"browser_download_url"' | grep 'checksums.sha256"' | head -1 | cut -d'"' -f4)

if [ -z "$APK_URL" ]; then
    echo "Error: Could not find APK"
    exit 1
fi

APK_NAME="Builder-${VERSION}.apk"
APK_PATH="$DOWNLOAD_DIR/$APK_NAME"

echo "Latest version: $VERSION"
echo ""

# Download
echo "[2/4] Downloading APK..."
curl -L --progress-bar -o "$APK_PATH" "$APK_URL"
echo ""

# Verify
echo "[3/4] Verifying checksum..."
if [ -n "$SHA_URL" ]; then
    EXPECTED=$(curl -sL "$SHA_URL" | awk '{print $1}')
    ACTUAL=$(sha256sum "$APK_PATH" 2>/dev/null | awk '{print $1}' || shasum -a 256 "$APK_PATH" | awk '{print $1}')
    if [ "$EXPECTED" = "$ACTUAL" ]; then
        echo "Checksum OK"
    else
        echo "Warning: Checksum mismatch!"
    fi
else
    echo "Skipped"
fi
echo ""

# Install
echo "[4/4] Opening installer..."
if command -v termux-open &>/dev/null; then
    termux-open "$APK_PATH"
elif command -v xdg-open &>/dev/null; then
    xdg-open "$APK_PATH"
fi

echo ""
echo "======================================"
echo "   Download Complete!"
echo "======================================"
echo ""
echo "APK: $APK_PATH"
echo ""
echo "If installer didn't open, install manually:"
echo "  1. Open Files app"
echo "  2. Go to Downloads folder"
echo "  3. Tap $APK_NAME"
echo ""
