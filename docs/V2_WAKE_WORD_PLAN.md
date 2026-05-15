# V2 Wake Word Plan

Kiko V2 adds the foundation for the future `Hey Kiko` wake phrase without paid
SDKs, cloud audio, API keys, or private wake-word model files.

## Direction

Kiko intentionally does not use Picovoice Porcupine. The long-term direction is
a free/open-source local wake-word pipeline that can run on Android with
TensorFlow Lite.

The project now includes `tools/wake_training/` so Kiko is not blocked by paid
or unavailable hosted training UIs. The primary path is now a beginner-friendly
Colab notebook that generates synthetic positives, prepares free negative/noise
data, trains, exports, checks, and downloads `hey_kiko.tflite`. Manual voice
recording is optional, not required.

The current backend trains a local log-mel CNN with TensorFlow/Keras and exports
TFLite, with sanity/balanced/quality profiles for safe GTX 1650-class
experimentation.

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

The Android runner now supports both raw-sample TFLite input and Kiko log-mel
feature input. The repo-local sanity model shape `[1, 32, 118, 1]` can be loaded
through the log-mel adapter for real-phone pipeline testing. Do not claim
production wake detection until balanced/quality models, threshold tuning, false
trigger testing, and battery testing pass.

The next Android runtime step is documented in
`docs/ANDROID_WAKE_MODEL_ADAPTER.md`.

## Debug and calibration

Debug builds include a Wake debug and calibration section in Settings. It shows
latest wake score, smoothed score, recent max score, threshold, and lets testers
temporarily lower the threshold. Low thresholds are for diagnosis only and may
false trigger.

The wake pipeline now uses baseline-aware gating before WakeDetected. A score
must cross the threshold and rise meaningfully above the recent ambient baseline.
If silence/noise sits near `0.50`, the sanity model is marked unsafe/needs a
better model and WakeDetected is blocked by default. An unsafe override exists
only for debugging and still requires margin above baseline.

The sanity model may not detect reliably. Use debug mode to confirm whether
`Hey Kiko` produces scores above silence/noise, then train balanced/quality
models and tune threshold/debounce on real phones.

Manual mic and wake detection arbitrate microphone use: Kiko stops the wake
AudioRecord path before starting Android SpeechRecognizer, then restarts wake
listening after the command/TTS flow when wake word remains enabled.

Manual mic, fake wake, and real wake all use the same compact in-app orbit
listening UI. This is intentionally not an overlay service yet, so no overlay
permission is required.

## Orbit-first app experience

The default Kiko screen is now a compact assistant orbit rather than a large
settings-style interface. The main screen keeps only the glowing orbit, mic
control, wake status pill, and settings gear visible. Tapping the orbit or mic
starts manual SpeechRecognizer input. Tapping the gear opens full settings, and
back/close returns to the orbit screen.

Orbit states stay lightweight:

- idle: subtle glow
- listening: stronger pulse
- processing: small rotating ring
- speaking: soft pulse
- unsafe calibration: amber ring and safety wording
- locked/screen-off pause: dimmed orbit state

Fake/Test wake and real WakeDetected use the same orbit listening route. The
orbit is in-app only in this phase; Kiko does not request overlay permission.

## Battery strategy

- Wake word is off by default.
- Wake listening runs only in a foreground service.
- AudioRecord uses 16 kHz mono PCM capture.
- Wake inference runs with a conservative threshold and debounce.
- Android SpeechRecognizer is not kept active while idle.
- AudioRecord is released before SpeechRecognizer starts.
- Wake AudioRecord/TFLite inference pauses when the phone screen turns off or
  the device locks by default.
- If wake was enabled, listening resumes automatically on unlock/user-present.
- The foreground notification clearly switches between active, unsafe-model,
  and paused-while-locked wording.
- No boot auto-start or battery optimization exemption is added in this phase.

## Android limitations

- Foreground microphone services require `RECORD_AUDIO`.
- Android 14+ requires a microphone foreground service declaration and related
  foreground service permission.
- Background activity launch may be restricted. If Kiko is not foreground,
  wake detection should update the notification and let the user tap to speak.
- Kiko attempts to foreground the orbit screen on safe WakeDetected, but falls
  back to the notification action if Android or the lock screen blocks launch.
- Notification visibility can depend on Android notification permission and OEM
  behavior.

## No cloud audio

The V2 foundation does not send microphone audio to cloud services and does not
add cloud AI. Wake-word diagnostics are local Logcat-only events with no raw
audio or frame dumps.
