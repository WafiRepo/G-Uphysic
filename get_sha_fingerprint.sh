#!/bin/bash

echo "========================================"
echo "Getting SHA-1 and SHA-256 Fingerprints"
echo "========================================"
echo ""

echo "[1] Getting SHA-1 fingerprint (Debug keystore)..."
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep "SHA1:"

echo ""
echo "[2] Getting SHA-256 fingerprint (Debug keystore)..."
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep "SHA256:"

echo ""
echo "========================================"
echo "Instructions:"
echo "1. Copy the SHA-1 and SHA-256 values above"
echo "2. Go to Firebase Console: https://console.firebase.google.com/"
echo "3. Select project: gphysolve"
echo "4. Go to Project Settings (gear icon)"
echo "5. Scroll down to 'Your apps' section"
echo "6. Click on your Android app (de.rwth_aachen.phyphox)"
echo "7. Click 'Add fingerprint' and paste SHA-1"
echo "8. Click 'Add fingerprint' again and paste SHA-256"
echo "9. Download the updated google-services.json"
echo "10. Replace app/google-services.json with the new file"
echo "========================================"

