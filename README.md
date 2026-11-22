# JUSU AI CALL ASSISTANT

**Powered by JUSU**

An Android-first, privacy-focused call assistant built in Kotlin. The project prioritizes offline operation, explicit permissions, and transparent device compatibility.

## Current implementation

- Offline rule engine for supported cellular call-screening events.
- Local Room persistence for call instructions and profiles.
- Android call-screening role onboarding and optional contacts permission.
- WhatsApp and WhatsApp Business call-notification detection through Android's notification-listener permission.
- When a WhatsApp call notification exposes an explicit **Decline** or **Reject** action, JUSU can mirror that original WhatsApp action in a separate JUSU notification. The user must tap it; JUSU does not auto-decline calls.
- Local display of the last detected WhatsApp caller label and detection time.

## WhatsApp limitations

WhatsApp calls are third-party VoIP calls, not cellular calls handled by Android's `CallScreeningService`. Detection depends on WhatsApp publishing a recognizable call notification. The decline shortcut is available only if that specific notification exposes a decline/reject action; this may vary by WhatsApp version, device, language, and call state.

JUSU cannot force WhatsApp to expose such an action, silently answer or reject a WhatsApp call, access WhatsApp call audio, or record the conversation. These features are not claimed.

## Required permissions

- **Call-screening role:** granted by the Android system for supported cellular call screening.
- **Contacts:** optional runtime permission for contact-based cellular call rules.
- **Notification access:** explicitly enabled by the user in Android Settings so JUSU can inspect WhatsApp call notifications.
- **Notifications:** runtime permission on Android 13+ so JUSU can display the optional decline shortcut.

Declining any permission leaves the related feature unavailable; permissions are not bypassed.

## Synthetic conversation dataset

A standard-library Python utility generates validated JSONL conversation examples for future model experiments. See [`tools/conversation_dataset/README.md`](tools/conversation_dataset/README.md). This is data generation only: it does not train a model, alter the Android chat engine, or download anything.

## Build

Install Android Studio and Android SDK, then open this project. On a machine with Gradle and Android SDK configured:

```sh
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Privacy

WhatsApp call notification matching and call-rule processing are performed on-device. JUSU stores only the last detected WhatsApp label and timestamp for the dashboard; it does not save the notification body or call audio. No cloud AI service or API key is required. Call recording is not enabled.

## Compatibility

Minimum Android version: Android 8.0 (API 26). Individual call-related features depend on Android version, user-granted permissions, app notification format, and device support.

## Status

Active development. CI success confirms that code compiles and tests pass; it does not replace real-device testing. WhatsApp detection and the optional decline shortcut must be tested with the installed WhatsApp version on a physical device.
