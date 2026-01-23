# sunmiPrintSystem GitHub Release Procedure

This document outlines the steps to prepare and release sunmiPrintSystem to GitHub.

## 1. Clean up Repository (One-time setup)
Ensure build artifacts are not tracked by git.

```bash
# Remove build artifacts from git tracking if they exist
git rm -r --cached app/build
git rm -r --cached .idea
git rm -r --cached .gradle

# Commit the removal
git commit -m "chore: stop tracking build artifacts and IDE files"
```

## 2. Setup Signing Keys (First time only)
To build a signed release, you need a keystore and configuration in `local.properties`.

1. **Generate Keystore** (if you don't have one):
   Run this command in the `sunmiPrintSystem` root directory:
   ```bash
   keytool -genkey -v -keystore keystore.jks -alias key0 -keyalg RSA -keysize 2048 -validity 10000
   ```
   *Note: You can reuse the keystore from FLcasher if you prefer, just copy it here or point to it.*

2. **Configure `local.properties`**:
   Add the following lines to `sunmiPrintSystem/local.properties`:
   ```properties
   storePassword=YOUR_STORE_PASSWORD
   keyAlias=key0
   keyPassword=YOUR_KEY_PASSWORD
   ```

## 3. Commit Pending Changes
Commit any changes to the codebase.

```bash
git add .
git commit -m "feat: prepare for release"
```

## 4. Tag the Release
Create a tag for this version.

```bash
git tag v1.0.0
git push origin v1.0.0
```

## 5. Build Release APK
Build the signed release APK.

```bash
./gradlew assembleRelease
```
*The APK will be located in `app/build/outputs/apk/release/app-release.apk`.*

## 6. Push to GitHub
Push your commits to the main branch.

```bash
git push origin main
```

## 7. Create GitHub Release
1. Go to the GitHub repository page.
2. Draft a new release using the tag `v1.0.0`.
3. Upload the signed APK.
4. Publish.
