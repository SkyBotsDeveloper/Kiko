# V2 Wake Word Plan

Kiko V2 adds the foundation for the future `Hey Kiko` wake phrase without
committing a real wake-word key or private model file.

## Why a foreground service

Android limits long-running microphone work in the background. Wake-word
listening must be user-controlled, visible, and tied to a foreground service
notification. Kiko therefore runs wake listening only through
`WakeWordService`, with a persistent notification that says Kiko is listening
for `Hey Kiko`.

## Optional by default

Wake word is off by default. The user must enable it from Settings. This keeps
V1 manual mic behavior unchanged and avoids surprise microphone use.

## SpeechRecognizer is not always running

Android `SpeechRecognizer` is used only after wake detection. The wake engine
is responsible for lightweight phrase detection, and full speech recognition
starts only after `Hey Kiko` is detected or simulated in the test flow.

## Fixed wake phrase

The V2 wake phrase is fixed to `Hey Kiko`. The app does not expose wake-name
customization in this phase because custom wake names require separate model
generation, testing, and battery/noise validation.

## Battery strategy

- Wake word is off by default.
- Wake listening runs only in a foreground service.
- The fake V2 foundation engine does no microphone work.
- The future real engine should use low-power keyword spotting and avoid full
  speech recognition until wake detection.
- No boot auto-start or battery optimization exemption is added in this phase.

## Android limitations

- Foreground microphone services require `RECORD_AUDIO`.
- Android 14+ requires a microphone foreground service declaration and
  foreground service permission.
- Background activity launch may be restricted. If Kiko is not foreground,
  wake detection should update the notification and let the user tap to speak.
- Notification visibility can depend on Android notification permission and OEM
  behavior.

## Real Porcupine integration

The real `Hey Kiko` engine is planned for the next V2 phase. That work must:

- Use a Picovoice AccessKey provided at runtime or through a private local
  developer setup.
- Never hardcode or commit a real AccessKey.
- Never commit private `.ppn` wake-word model files.
- Keep all wake audio on device.
- Preserve the `WakeWordEngine` abstraction so fake tests continue to run.

## No cloud audio

The V2 foundation does not send microphone audio to cloud services and does not
add cloud AI. Wake-word diagnostics are local Logcat-only events with no raw
audio logging.
