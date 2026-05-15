# V2 Wake Word Plan

Kiko V2 adds the foundation for the future `Hey Kiko` wake phrase without paid
SDKs, cloud audio, API keys, or private wake-word model files.

## Direction

Kiko intentionally does not use Picovoice Porcupine. The long-term direction is
a free/open-source local wake-word pipeline that can run on Android with
TensorFlow Lite.

Current engine options:

- Open-source local: AudioRecord + TFLite model runner foundation.
- Fake/Test: manual simulation for development and QA.

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

Android `SpeechRecognizer` is used only after wake detection. The local wake
engine is responsible for lightweight phrase detection, and full speech
recognition starts only after `Hey Kiko` is detected or simulated in the test
flow.

## Fixed wake phrase

The V2 wake phrase is fixed to `Hey Kiko`. The app does not expose wake-name
customization in this phase because custom wake names require separate model
training, testing, and battery/noise validation.

## Model status

The expected model path is:

```text
app/src/main/assets/wake/hey_kiko.tflite
```

No real model is committed in this phase. If the model is missing or invalid,
the open-source engine reports a clear error and Kiko keeps manual mic plus
Fake/Test wake available.

## Battery strategy

- Wake word is off by default.
- Wake listening runs only in a foreground service.
- AudioRecord uses 16 kHz mono PCM capture.
- Wake inference runs with a conservative threshold and debounce.
- Android SpeechRecognizer is not kept active while idle.
- AudioRecord is released before SpeechRecognizer starts.
- No boot auto-start or battery optimization exemption is added in this phase.

## Android limitations

- Foreground microphone services require `RECORD_AUDIO`.
- Android 14+ requires a microphone foreground service declaration and related
  foreground service permission.
- Background activity launch may be restricted. If Kiko is not foreground,
  wake detection should update the notification and let the user tap to speak.
- Notification visibility can depend on Android notification permission and OEM
  behavior.

## No cloud audio

The V2 foundation does not send microphone audio to cloud services and does not
add cloud AI. Wake-word diagnostics are local Logcat-only events with no raw
audio or frame dumps.
