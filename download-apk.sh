#!/data/data/com.termux/files/usr/bin/bash

echo "Installing GitHub CLI if needed..."
pkg install gh -y

echo "Authenticating with GitHub..."
gh auth login

echo ""
echo "Recent successful builds:"
gh run list --repo RachEma-ux/Builder --workflow android-ci.yml --status success --limit 5

echo ""
read -p "Enter Run ID: " RUN_ID

echo "Downloading..."
mkdir -p ~/Downloads/builder-apk
cd ~/Downloads/builder-apk
gh run download "$RUN_ID" --repo RachEma-ux/Builder

echo ""
echo "APK location:"
find . -name "*.apk"
