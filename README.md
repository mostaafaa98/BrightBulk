# Bright Bulk Android

## Build from a phone using GitHub Actions
1. Create a GitHub repository and upload the **contents** of this folder to the `main` branch.
2. Open **Actions** in the repository.
3. Select **Build Bright Bulk APK**.
4. Tap **Run workflow**.
5. Wait for the build to finish.
6. Open the completed workflow run and download the artifact **BrightBulk-debug-apk**.
7. Extract the ZIP and install `app-debug.apk` on Android.

No Android Studio is required for this build path.

This is an MVP UI with CSV import and campaign preparation. WhatsApp sending is not implemented yet; it should be connected to the official WhatsApp Business Cloud API in the next phase.
