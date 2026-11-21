# JUSU AI CALL ASSISTANT

**Powered by JUSU**

An Android-first, privacy-focused phone assistant built in Kotlin. The project prioritizes offline operation and transparent device compatibility.

## Current milestone

The repository is being established with an Android application foundation and CI build. Call audio access and automated answering are Android role/device dependent; the app will never claim call audio integration unless it is actually supported and verified.

## Build

Install Android Studio and Android SDK, then open this project. On a machine with Gradle and Android SDK configured:


```sh
gradle :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Privacy
User settings and future assistant instructions are designed to remain on-device. No cloud AI service or API key is required. Call recording is not enabled by this project foundation.

## Compatibility
Initial minimum Android version: Android 8.0 (API 26). Call-related features will be individually gated by Android version, permissions, role availability, and device support.

## Status
This is an active development project. Features are not described as complete or device-tested until verification evidence exists.
