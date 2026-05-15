# Android Limitations

Kiko V1 stays offline and uses Android platform APIs directly. Some phone-control
actions are limited by Android permissions, device hardware, and OEM behavior.

## Brightness

Kiko can adjust its own app window brightness without special permission.
System-wide brightness changes require Android write-settings access
(`Settings.System.canWrite`). If that access is missing, Kiko uses the app-window
fallback and tells the user that full phone brightness needs extra permission.

## Alarms

Basic alarm creation uses Android's `AlarmClock` intent. This avoids exact-alarm
permission for the V1 flow, but the final UI and behavior depend on the device's
installed clock app.
Kiko can parse common local phrases such as `kal subah 6 baje`, but the
AlarmClock app ultimately decides how the alarm is created and displayed.

## Reminders

Kiko stores reminders locally. Reminder notification scheduling uses an inexact
`AlarmManager` flow, so exact delivery is not guaranteed. On Android 13 and newer,
notification permission is required before Kiko can alert the user.
Reminder records are stored in Room and delivery status is updated when the
notification receiver can run, but Android can still delay or suppress alarms
based on battery, standby, OEM policy, or missing notification permission.

## Flashlight

Flashlight control uses `CameraManager.setTorchMode`. Some devices may not have
flash hardware, and the torch can be unavailable if the camera is in use or the
OEM camera service rejects the request. Kiko handles those cases without crashing.

## Local Memory

Room is the primary V1 storage layer for preferences, aliases, pending
clarifications, reminders, and optional structured summaries. V2 adds explicit
Room migration from schema version `1` to `2` for wake-word preferences.

## Wake Word

V2 wake-word listening is optional and off by default. When enabled, Android
requires microphone permission and a foreground service notification. Android
14+ also requires the microphone foreground service type and related foreground
service permission.

Kiko does not keep Android `SpeechRecognizer` always running. Full speech
recognition starts only after `Hey Kiko` is detected by the local wake engine
or by the fake/manual test flow. If the app is backgrounded and Android blocks
direct UI launch, Kiko relies on the foreground notification action.

The open-source wake engine expects a trained local TFLite model at
`app/src/main/assets/wake/hey_kiko.tflite`. If the model is missing, invalid, or
not compatible with the current foundation runner, Kiko reports the problem and
keeps manual mic plus Fake/Test wake available. Production wake detection also
requires battery, heat, noise, and false-trigger testing on real phones.

## Diagnostics

Diagnostics are written to local Logcat only. They are intended for debugging
voice start/results, parser intent type, action routing, permission gaps,
reminder scheduling/storage, TTS state, and wake-word service state. Kiko does
not log raw wake audio, frame dumps, or wake transcripts, and it does not send
diagnostics to a server.
