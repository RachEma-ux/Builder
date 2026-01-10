#!/data/data/com.termux/files/usr/bin/bash

#
# Builder - One-Tap Installation Script
# Builds and installs the Builder Android app on your device
#

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

print_banner() {
    echo ""
    echo "╔════════════════════════════════════════════════════════╗"
    echo "║                                                        ║"
    echo "║              Builder - One-Tap Install                 ║"
    echo "║         Mobile Orchestration for Android               ║"
    echo "║                                                        ║"
    echo "╚════════════════════════════════════════════════════════╝"
    echo ""
}

check_termux() {
    if [ ! -d "/data/data/com.termux" ]; then
        log_error "This script must be run in Termux"
        exit 1
    fi
    log_info "Running in Termux ✓"
}

check_prerequisites() {
    log_step "Checking prerequisites..."

    local all_good=true

    # Check and install git
    if ! command -v git &> /dev/null; then
        log_warn "Git not found, installing..."
        pkg update -y && pkg install -y git || {
            log_error "Failed to install git"
            exit 1
        }
        log_info "Git installed ✓"
    else
        log_info "Git found ✓"
    fi

    # Check and install Java
    if ! command -v java &> /dev/null; then
        log_warn "Java not found, installing..."
        pkg update -y

        # Try openjdk-21 (LTS version available in Termux)
        if pkg install -y openjdk-21 2>/dev/null; then
            log_info "OpenJDK 21 installed ✓"
        elif pkg install -y openjdk-17 2>/dev/null; then
            log_info "OpenJDK 17 installed ✓"
        elif pkg install -y openjdk-25 2>/dev/null; then
            log_info "OpenJDK 25 installed ✓"
        else
            log_warn "Standard OpenJDK packages not available, checking alternatives..."
            # Try other versions
            if pkg search openjdk | grep -q openjdk; then
                log_info "Available Java packages:"
                pkg search openjdk | grep openjdk
                log_error "Please install Java manually: pkg install openjdk-21"
                exit 1
            else
                log_error "No OpenJDK packages found in repositories"
                log_info "Try: pkg update && pkg upgrade"
                exit 1
            fi
        fi
    else
        JAVA_VERSION=$(java -version 2>&1 | head -n 1)
        log_info "Java found: $JAVA_VERSION ✓"
    fi

    # Check and install ADB
    if ! command -v adb &> /dev/null; then
        log_warn "ADB not found, installing..."
        pkg install -y android-tools || {
            log_error "Failed to install android-tools"
            exit 1
        }
        log_info "ADB installed ✓"
    else
        log_info "ADB found ✓"
    fi

    log_info "All prerequisites checked ✓"
}

setup_storage() {
    log_step "Setting up storage permissions..."

    if [ ! -d "$HOME/storage" ]; then
        log_info "Requesting storage access..."
        termux-setup-storage
        log_warn "Please grant storage permission when prompted"
        sleep 3
    else
        log_info "Storage access configured ✓"
    fi
}

setup_android_sdk() {
    log_step "Setting up Android SDK..."

    # Check if ANDROID_HOME is already set and valid
    if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME" ]; then
        log_info "Android SDK found at $ANDROID_HOME ✓"
        return 0
    fi

    # Check if local.properties exists
    if [ -f "$HOME/Builder/local.properties" ] && grep -q "sdk.dir" "$HOME/Builder/local.properties"; then
        log_info "Android SDK configured in local.properties ✓"
        return 0
    fi

    log_warn "Android SDK not found, installing..."
    log_info "This will download ~2GB of data and may take 10-15 minutes"

    # Create SDK directory
    SDK_DIR="$HOME/android-sdk"
    mkdir -p "$SDK_DIR/cmdline-tools"

    # Install unzip if not present
    if ! command -v unzip &> /dev/null; then
        log_info "Installing unzip..."
        pkg install -y unzip
    fi

    # Download Android command-line tools
    log_info "Downloading Android SDK command-line tools..."
    cd "$SDK_DIR/cmdline-tools"

    CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    wget -q --show-progress "$CMDLINE_TOOLS_URL" || {
        log_error "Failed to download Android SDK tools"
        exit 1
    }

    # Extract tools
    log_info "Extracting SDK tools..."
    unzip -q commandlinetools-linux-*.zip
    mv cmdline-tools latest
    rm commandlinetools-linux-*.zip

    # Set ANDROID_HOME
    export ANDROID_HOME="$SDK_DIR"
    export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

    # Add to bashrc for persistence
    if ! grep -q "ANDROID_HOME" "$HOME/.bashrc"; then
        log_info "Adding ANDROID_HOME to ~/.bashrc..."
        echo "" >> "$HOME/.bashrc"
        echo "# Android SDK" >> "$HOME/.bashrc"
        echo "export ANDROID_HOME=\$HOME/android-sdk" >> "$HOME/.bashrc"
        echo "export PATH=\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$PATH" >> "$HOME/.bashrc"
    fi

    # Accept licenses
    log_info "Accepting Android SDK licenses..."
    yes | sdkmanager --licenses > /dev/null 2>&1

    # Install required SDK components
    log_info "Installing Android SDK platform and build tools (this may take 10 minutes)..."
    sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" || {
        log_error "Failed to install SDK components"
        exit 1
    }

    log_info "Android SDK installed successfully ✓"
    log_info "SDK location: $ANDROID_HOME"
}

build_app() {
    log_step "Building Builder app..."

    cd ~/Builder || {
        log_error "Builder directory not found at ~/Builder"
        exit 1
    }

    # Make gradlew executable
    chmod +x gradlew

    log_info "Starting Gradle build (this may take 5-10 minutes)..."
    log_info "First build will download dependencies..."

    # Build debug APK
    ./gradlew assembleDebug --warning-mode all 2>&1 | tee build.log
    BUILD_EXIT_CODE=${PIPESTATUS[0]}

    if [ $BUILD_EXIT_CODE -eq 0 ]; then
        log_info "Build successful! ✓"

        # Find the APK
        APK_PATH=$(find app/build/outputs/apk/debug -name "*.apk" | head -1)

        if [ -n "$APK_PATH" ]; then
            APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
            log_info "APK generated: $APK_PATH ($APK_SIZE)"
            echo "$APK_PATH" > .last_apk_path
        else
            log_error "APK not found after build"
            exit 1
        fi
    else
        log_error "Build failed! Check build.log for details"
        tail -50 build.log
        exit 1
    fi
}

install_app() {
    log_step "Installing Builder app..."

    APK_PATH=$(cat .last_apk_path 2>/dev/null)

    if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
        log_error "APK not found. Build first."
        exit 1
    fi

    # Check if ADB is working
    log_info "Checking ADB connection..."
    adb devices

    # Wait for device
    log_info "Waiting for device..."
    adb wait-for-device

    # Install APK
    log_info "Installing APK..."
    adb install -r "$APK_PATH"

    if [ $? -eq 0 ]; then
        log_info "Installation successful! ✓"
        log_info "App package: com.builder"

        # Ask if user wants to launch
        echo ""
        read -p "Launch Builder app now? [Y/n] " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]] || [[ -z $REPLY ]]; then
            log_info "Launching Builder..."
            adb shell am start -n com.builder/.MainActivity
            log_info "Builder launched! ✓"
        fi
    else
        log_error "Installation failed"
        exit 1
    fi
}

copy_to_downloads() {
    log_step "Copying APK to Downloads folder..."

    APK_PATH=$(cat .last_apk_path 2>/dev/null)

    if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
        log_warn "APK not found, skipping copy"
        return
    fi

    DOWNLOADS="/sdcard/Download"
    if [ -d "$DOWNLOADS" ]; then
        cp "$APK_PATH" "$DOWNLOADS/builder-debug.apk"
        log_info "APK copied to Downloads/builder-debug.apk ✓"
        log_info "You can share or install this APK manually"
    else
        log_warn "Downloads folder not found, skipping copy"
    fi
}

show_summary() {
    echo ""
    echo "╔════════════════════════════════════════════════════════╗"
    echo "║                  Installation Complete!                ║"
    echo "╚════════════════════════════════════════════════════════╝"
    echo ""
    log_info "Builder app is now installed on your device"
    log_info "Package name: com.builder"
    echo ""
    log_info "Next steps:"
    echo "  1. Open the Builder app from your app drawer"
    echo "  2. Grant required permissions"
    echo "  3. Navigate to 'GitHub Packs'"
    echo "  4. Sign in with GitHub to browse repositories"
    echo ""
    log_info "To rebuild and reinstall:"
    echo "  $ ./install.sh"
    echo ""
}

# Main installation flow
main() {
    print_banner

    # Parse arguments
    BUILD_ONLY=false
    INSTALL_ONLY=false

    while [[ $# -gt 0 ]]; do
        case $1 in
            --build-only)
                BUILD_ONLY=true
                shift
                ;;
            --install-only)
                INSTALL_ONLY=true
                shift
                ;;
            --help|-h)
                echo "Usage: ./install.sh [OPTIONS]"
                echo ""
                echo "Options:"
                echo "  --build-only      Only build the APK, don't install"
                echo "  --install-only    Only install existing APK, don't build"
                echo "  --help, -h        Show this help message"
                echo ""
                echo "Examples:"
                echo "  ./install.sh                # Build and install"
                echo "  ./install.sh --build-only   # Just build"
                echo "  ./install.sh --install-only # Just install"
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                echo "Use --help for usage information"
                exit 1
                ;;
        esac
    done

    # Run checks
    check_termux

    if [ "$INSTALL_ONLY" = false ]; then
        check_prerequisites
        setup_storage
        setup_android_sdk
        build_app
        copy_to_downloads
    fi

    if [ "$BUILD_ONLY" = false ]; then
        install_app
    fi

    if [ "$INSTALL_ONLY" = false ] && [ "$BUILD_ONLY" = false ]; then
        show_summary
    fi

    log_info "Done! ✓"
}

# Run main function
main "$@"
