#!/usr/bin/env bash
# FamilyGuard - Android Enterprise Device Owner Setup Script (Bash)

echo "=========================================================="
echo "   FamilyGuard - Device Owner Provisioning Utility (Linux/macOS) "
echo "=========================================================="

echo -e "\n[1/3] Checking connected Android devices..."
adb devices

echo -e "\n[2/3] Setting Device Owner privilege..."
RESULT=$(adb shell dpm set-device-owner org.familyguard.child/.dpc.ParentalDpcReceiver 2>&1)
echo "$RESULT"

if [[ $RESULT == *"Success"* ]]; then
    echo -e "\n[SUCCESS] FamilyGuard Child App is provisioned as Device Owner (DPC)!"
else
    echo -e "\n[TROUBLESHOOTING]"
    echo "- Ensure all Google/user accounts are removed from Settings -> Accounts."
    echo "- Ensure USB debugging is authorized."
fi
