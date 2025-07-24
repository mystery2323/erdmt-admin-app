#!/bin/bash

echo "🔍 Starting Deep Android App Code Review..."

PROJECT_ROOT=$(pwd)
echo "📁 Project Root: $PROJECT_ROOT"
mkdir -p audit_logs

echo "✅ Checking Gradle configuration..."
if [[ -f "build.gradle.kts" ]]; then
  echo "✔️  Root build.gradle.kts found." | tee -a audit_logs/gradle_check.log
else
  echo "❌ Root build.gradle.kts missing!" | tee -a audit_logs/gradle_check.log
fi

if [[ -f "app/build.gradle.kts" ]]; then
  echo "✔️  App module build.gradle.kts found." | tee -a audit_logs/gradle_check.log
else
  echo "❌ App module build.gradle.kts missing!" | tee -a audit_logs/gradle_check.log
fi

echo "✅ Checking Firebase configuration..."
if [[ -f "app/google-services.json" ]]; then
  echo "✔️  google-services.json found." | tee -a audit_logs/firebase_check.log
else
  echo "❌ google-services.json missing!" | tee -a audit_logs/firebase_check.log
fi

echo "✅ Searching Firebase SDK usage..."
grep -ri "firebase" app/src/ > audit_logs/firebase_sdk_usage.txt
if [[ -s audit_logs/firebase_sdk_usage.txt ]]; then
  echo "✔️  Firebase SDK usage found in source code." | tee -a audit_logs/firebase_check.log
else
  echo "❌ Firebase SDK usage NOT found in source code." | tee -a audit_logs/firebase_check.log
fi

echo "🧪 Running Gradle dry-run..."
./gradlew assembleDebug --dry-run > audit_logs/gradle_dry_run.txt 2>&1
if grep -q "BUILD SUCCESSFUL" audit_logs/gradle_dry_run.txt; then
  echo "✔️  Gradle dry-run successful." | tee -a audit_logs/gradle_check.log
else
  echo "❌ Gradle dry-run failed. Check audit_logs/gradle_dry_run.txt" | tee -a audit_logs/gradle_check.log
fi

echo "🔎 Scanning for TODO/FIXME..."
grep -rniE "TODO|FIXME" app/src/ > audit_logs/todo_fixme.txt
if [[ -s audit_logs/todo_fixme.txt ]]; then
  echo "⚠️  Unfinished code found. See audit_logs/todo_fixme.txt" | tee -a audit_logs/code_check.log
else
  echo "✔️  No TODO or FIXME comments found." | tee -a audit_logs/code_check.log
fi

echo "🔐 Checking manifest for permissions..."
grep -i "<uses-permission" app/src/main/AndroidManifest.xml > audit_logs/permissions.txt
cat audit_logs/permissions.txt

echo "📦 Listing major dependencies..."
grep "implementation" app/build.gradle.kts > audit_logs/dependencies.txt
cat audit_logs/dependencies.txt

echo "✅ Deep Review Completed. Logs stored in audit_logs/ directory."
