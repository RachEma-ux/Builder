#!/data/data/com.termux/files/usr/bin/bash
pkg install gh -y 2>/dev/null
gh auth status || gh auth login
gh workflow run android-ci.yml --repo RachEma-ux/Builder --ref main
sleep 5
RUN_ID=$(gh run list --repo RachEma-ux/Builder --workflow android-ci.yml --limit 1 --json databaseId --jq '.[0].databaseId')
echo "Build: https://github.com/RachEma-ux/Builder/actions/runs/$RUN_ID"
gh run watch "$RUN_ID" --repo RachEma-ux/Builder
mkdir -p ~/Downloads/builder-apk && cd ~/Downloads/builder-apk
gh run download "$RUN_ID" --repo RachEma-ux/Builder
find . -name "*.apk" -exec adb install -r {} \;
