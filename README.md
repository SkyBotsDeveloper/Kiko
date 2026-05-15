# Kiko

Kiko is an offline-first Android voice assistant created by Siddhartha Abhimanyu.
It is not a chatbot app. Kiko is being built as an action-oriented assistant for
real phone control.

## Offline-first philosophy

Kiko V1 is planned around local Android capabilities: manual microphone input,
Android SpeechRecognizer, Android TextToSpeech, a local intent parser, local
memory, and direct device/app/contact actions. V1 has no login system, no cloud
AI dependency, and no API keys.

## V1 target

The first production target is a compact phone-control assistant that can listen
manually, understand a small local command set, ask clarifying questions when
needed, speak responses with Android TTS, and run basic Android actions.

## Current phase status

This branch contains only the clean initial Android scaffold:

- Kotlin Android app
- Jetpack Compose UI
- Material 3 theme
- Package name `com.skybots.kiko`
- Minimum SDK 26
- Basic dark premium placeholder screen

Future behavior such as wake word detection, speech recognition, TTS, local
memory, app launching, contacts, phone calls, and device controls is not
implemented in this phase.

## Build instructions

Open the project in Android Studio, let Gradle sync, then run the app module.

Command-line build:

```powershell
.\gradlew.bat :app:assembleDebug
```

On macOS or Linux:

```bash
./gradlew :app:assembleDebug
```

The Android SDK must include compile SDK 35, and the build requires a Java 17
compatible JDK.

## Git workflow

Development for this phase happens on `v1-core`.

- Do not push scaffold work to `main`.
- Commit only working scaffold changes.
- Push with `git push origin v1-core` after verification.
