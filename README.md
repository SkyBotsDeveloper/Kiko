# Kiko

Kiko is an offline-first Android voice assistant created by Siddhartha
Abhimanyu. It is not a chatbot app. Kiko V1 is an action-oriented assistant for
real phone control using manual microphone input.

## V1 Status

Kiko V1 core features passed real-device manual QA on a Samsung SM-A556E running
Android 16. No major crash or major slow/weird behavior was found during the V1
manual test pass.

## V2 Wake-word Foundation

The `v2-wake-word` branch adds the safe foundation for future `Hey Kiko` wake
support. Wake word is optional and off by default, runs only through a visible
foreground service, and uses a free/open-source local direction. Kiko
intentionally does not use Picovoice Porcupine, Picovoice SDKs, Picovoice
AccessKeys, or `.ppn` / `.pv` files.

The foundation does not add cloud AI, login, Accessibility Service automation,
or always-on Android SpeechRecognizer. Full voice recognition starts only after
manual mic input or a wake-detected event.

The open-source local engine expects a trained TFLite model at
`app/src/main/assets/wake/hey_kiko.tflite`. Until that model is available and
compatible, Kiko reports the model status clearly and keeps manual mic plus
Fake/Test wake available. The repo includes `tools/wake_training/` for a
beginner-friendly Colab-first training path, so Kiko is not dependent on paid or
broken hosted trainers. The first model can be trained from synthetic positives
and free/generated negatives without manually recording the user's voice.

The current training backend exports a log-mel TFLite classifier. Android now
has a log-mel adapter for Kiko-trained models, so the sanity model can be used
for real-phone pipeline testing. It is still not a production-quality wake
model until balanced/quality training, threshold tuning, false-trigger testing,
and battery testing pass.

Debug builds include wake score calibration controls in Settings. They show raw,
smoothed, and max recent scores, allow a temporary low-threshold override for
testing, and provide a silence/noise score check. Lowering the threshold is for
diagnosis only and may false trigger.

Kiko blocks WakeDetected when the model baseline is unsafe. If silence/noise is
already near `0.50`, the sanity model is treated as pipeline validation only and
balanced/quality training is required before daily wake use. Manual mic, fake
wake, and real wake use a compact in-app orbit listening UI; no overlay
permission is used in this phase.

The app is now orbit-first: opening Kiko shows a compact glowing assistant orbit
with mic, wake status, and settings controls instead of a full settings-style
home screen. Full settings remain available from the gear or wake status pill.

V2 also adds an optional floating orbit mode. Android requires the user to grant
Draw over other apps permission before Kiko can show a draggable assistant
bubble over other apps. Kiko does not request this aggressively; if permission is
missing, the in-app orbit remains the fallback. The floating orbit can open a
compact panel, route to settings, and start the existing manual mic flow.

To protect battery on low/mid-range phones, V2 pauses wake inference when the
phone screen turns off or the device locks. The wake preference stays enabled,
and the foreground notification changes to show that Kiko wake is paused while
locked. Wake listening resumes after unlock/user-present when the user had
enabled it. Android still requires the foreground notification whenever active
background microphone wake listening is running.

## V1 Features

- Manual mic voice input using Android SpeechRecognizer.
- Voice replies using Android TextToSpeech.
- Local deterministic parser for English, Hinglish, and Hindi command variants.
- Installed app opening, including learned app aliases.
- Contact calling with contact clarification and multi-number clarification.
- Dialer fallback when `CALL_PHONE` permission is missing.
- Direct calling when `CALL_PHONE` permission is granted.
- Flashlight, media volume, brightness, alarm, and reminder actions.
- Brightness handling that respects Android write-settings limitations.
- Basic inexact local reminder scheduling with notification permission handling.
- Room-backed local memory for preferences, aliases, reminders, and pending
  clarification state.
- Local app/contact alias learning after user approval.
- Language/style mirroring for English, Hinglish, and Hindi responses.
- Offline creator identity answer for Siddhartha Abhimanyu.
- Settings screen for voice replies, language, reply style, personalization,
  local memory clear/export/import, and permission guidance.

## Offline-first Philosophy

Kiko V1 has no login system, no cloud AI dependency, no API keys, and no account
sync. Contacts, installed app lists, reminders, preferences, and learned memory
stay on the device. Full raw conversations are not stored by default, and JSON
export/import is local and user-controlled.

## Android Limitations

Kiko uses Android platform APIs directly, so some behavior depends on device
permissions, OEM policy, and installed system apps:

- Direct calls need `CALL_PHONE`; without it Kiko opens the dialer.
- System-wide brightness needs write-settings permission; without it Kiko adjusts
  only its own screen brightness.
- Reminder alarms are inexact and notification alerts need notification
  permission on Android 13+.
- Flashlight control can fail gracefully if hardware is unavailable or camera is
  in use.
- Wake-word listening, when enabled in V2, requires microphone permission and a
  foreground service notification.

See `docs/ANDROID_LIMITATIONS.md`, `docs/PRIVACY.md`,
`docs/DB_MIGRATION_POLICY.md`, `docs/V1_MANUAL_QA.md`, and
`docs/V2_WAKE_WORD_PLAN.md` for details. Wake-model training and battery testing
notes live in `docs/OPEN_SOURCE_WAKE_MODEL.md` and
`docs/WAKE_BATTERY_TESTING.md`. Training toolkit details live in
`tools/wake_training/README.md`, and the Android log-mel adapter plan is in
`docs/ANDROID_WAKE_MODEL_ADAPTER.md`.

## Build Instructions

Open the project in Android Studio, let Gradle sync, then run the app module.

Command-line build on Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

Run checks:

```powershell
.\gradlew.bat check
```

On macOS or Linux:

```bash
./gradlew :app:assembleDebug
./gradlew check
```

The Android SDK must include compile SDK 35, and the build requires a Java 17
compatible JDK.

## Roadmap

- V2: "Hey Kiko" wake phrase support. The current foundation adds the
  foreground service, fake engine, open-source AudioRecord/TFLite engine
  foundation, settings, notification, diagnostics, and tests. Real detection
  requires a trained local `Hey Kiko` TFLite model, real-phone wake score
  tuning, and battery validation.
- V3: Accessibility automation for deeper app interaction after safety and user
  controls are mature.
- Later: optional cloud AI and premium voice features, without weakening the
  offline-first V1 foundation.

## Git Workflow

V1 development happened on `v1-core`. Final verified V1 code can be merged to
`main` only after `:app:assembleDebug`, `check`, and safety scans pass. Do not
force push.
