#!/bin/bash
#
# Builder - One-Tap Install Script
# Downloads and installs the latest Builder APK from GitHub Releases
#

set -e

REPO="RachEma-ux/Builder"
DOWNLOAD_DIR="$HOME/Downloads"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo ""
echo "======================================"
echo "    Builder - One-Tap Installer"
echo "======================================"
echo ""

# Create download directory if it doesn't exist
mkdir -p "$DOWNLOAD_DIR"

# Check for required tools
check_requirements() {
    local missing=()

    if ! command -v curl &> /dev/null; then
        missing+=("curl")
    fi

    if ! command -v jq &> /dev/null; then
        missing+=("jq")
    fi

    if [ ${#missing[@]} -gt 0 ]; then
        echo -e "${YELLOW}Installing required packages: ${missing[*]}${NC}"
        pkg install -y "${missing[@]}"
    fi
}

# Get latest release info
get_latest_release() {
    echo -e "${YELLOW}Fetching latest release info...${NC}"

    RELEASE_INFO=$(curl -s "https://api.github.com/repos/$REPO/releases/latest")

    if [ -z "$RELEASE_INFO" ] || echo "$RELEASE_INFO" | grep -q "Not Found"; then
        echo -e "${RED}Error: Could not fetch release info${NC}"
        exit 1
    fi

    VERSION=$(echo "$RELEASE_INFO" | jq -r '.tag_name')
    APK_URL=$(echo "$RELEASE_INFO" | jq -r '.assets[] | select(.name | endswith(".apk") and (endswith(".sha256") | not)) | .browser_download_url')
    SHA_URL=$(echo "$RELEASE_INFO" | jq -r '.assets[] | select(.name | endswith(".sha256")) | .browser_download_url')
    APK_NAME=$(echo "$RELEASE_INFO" | jq -r '.assets[] | select(.name | endswith(".apk") and (endswith(".sha256") | not)) | .name')

    if [ -z "$APK_URL" ] || [ "$APK_URL" == "null" ]; then
        echo -e "${RED}Error: No APK found in latest release${NC}"
        exit 1
    fi

    echo -e "${GREEN}Latest version: $VERSION${NC}"
}

# Download APK and checksum
download_apk() {
    APK_PATH="$DOWNLOAD_DIR/$APK_NAME"
    SHA_PATH="$DOWNLOAD_DIR/${APK_NAME}.sha256"

    echo ""
    echo -e "${YELLOW}Downloading $APK_NAME...${NC}"
    curl -L -# -o "$APK_PATH" "$APK_URL"

    if [ -n "$SHA_URL" ] && [ "$SHA_URL" != "null" ]; then
        echo -e "${YELLOW}Downloading checksum...${NC}"
        curl -sL -o "$SHA_PATH" "$SHA_URL"
    fi

    echo -e "${GREEN}Download complete!${NC}"
}

# Verify checksum
verify_checksum() {
    if [ -f "$SHA_PATH" ]; then
        echo ""
        echo -e "${YELLOW}Verifying checksum...${NC}"

        cd "$DOWNLOAD_DIR"
        EXPECTED=$(cat "$SHA_PATH" | awk '{print $1}')
        ACTUAL=$(sha256sum "$APK_NAME" | awk '{print $1}')

        if [ "$EXPECTED" == "$ACTUAL" ]; then
            echo -e "${GREEN}Checksum verified!${NC}"
        else
            echo -e "${RED}Checksum mismatch!${NC}"
            echo "Expected: $EXPECTED"
            echo "Actual:   $ACTUAL"
            echo ""
            read -p "Continue anyway? (y/N) " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                exit 1
            fi
        fi
    else
        echo -e "${YELLOW}No checksum file available, skipping verification${NC}"
    fi
}

# Install APK
install_apk() {
    echo ""
    echo -e "${YELLOW}Installing APK...${NC}"
    echo ""

    # Check if termux-open is available
    if command -v termux-open &> /dev/null; then
        termux-open "$APK_PATH"
    else
        # Try using am command (Android Activity Manager)
        if command -v am &> /dev/null; then
            am start -a android.intent.action.VIEW -d "file://$APK_PATH" -t application/vnd.android.package-archive
        else
            # Fallback: just open the file
            echo -e "${YELLOW}Opening APK with default handler...${NC}"
            xdg-open "$APK_PATH" 2>/dev/null || open "$APK_PATH" 2>/dev/null || {
                echo ""
                echo -e "${GREEN}APK downloaded to:${NC}"
                echo "$APK_PATH"
                echo ""
                echo "Please install manually by:"
                echo "1. Open your file manager"
                echo "2. Navigate to: $DOWNLOAD_DIR"
                echo "3. Tap on: $APK_NAME"
            }
        fi
    fi

    echo ""
    echo -e "${GREEN}======================================"
    echo "    Installation initiated!"
    echo "======================================${NC}"
    echo ""
    echo "If the installer doesn't open automatically:"
    echo "  1. Open your file manager"
    echo "  2. Go to: $DOWNLOAD_DIR"
    echo "  3. Tap: $APK_NAME"
    echo ""
}

# Cleanup old versions
cleanup() {
    echo -e "${YELLOW}Cleaning up old versions...${NC}"
    find "$DOWNLOAD_DIR" -name "Builder-v*.apk" -not -name "$APK_NAME" -delete 2>/dev/null || true
    find "$DOWNLOAD_DIR" -name "Builder-v*.apk.sha256" -not -name "${APK_NAME}.sha256" -delete 2>/dev/null || true
}

# Main
main() {
    check_requirements
    get_latest_release
    download_apk
    verify_checksum
    cleanup
    install_apk
}

main "$@"
