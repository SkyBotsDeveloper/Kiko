# Kiko

Kiko is an offline-first Android voice assistant created by Siddhartha
Abhimanyu. It is not a chatbot app. Kiko V1 is an action-oriented assistant for
real phone control using manual microphone input.

## V1 Status

Kiko V1 core features passed real-device manual QA on a Samsung SM-A556E running
Android 16. No major crash or major slow/weird behavior was found during the V1
manual test pass.

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

See `docs/ANDROID_LIMITATIONS.md`, `docs/PRIVACY.md`,
`docs/DB_MIGRATION_POLICY.md`, and `docs/V1_MANUAL_QA.md` for details.

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

- V2: "Hey Kiko" wake phrase support after core runtime and battery behavior are
  ready.
- V3: Accessibility automation for deeper app interaction after safety and user
  controls are mature.
- Later: optional cloud AI and premium voice features, without weakening the
  offline-first V1 foundation.

## Git Workflow

V1 development happened on `v1-core`. Final verified V1 code can be merged to
`main` only after `:app:assembleDebug`, `check`, and safety scans pass. Do not
force push.
