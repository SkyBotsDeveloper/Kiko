# Wake Battery Testing

Wake-word listening must be measured before it is treated as production-ready.

## Basic comparison

1. Charge the phone to a stable level.
2. Reboot or force stop noisy background apps if practical.
3. Run Kiko with wake word disabled for 30 or 60 minutes.
4. Record battery drop, device temperature, and obvious CPU/thermal behavior.
5. Enable `Hey Kiko` wake listening and repeat for the same duration.
6. Compare battery drain and heat against the disabled baseline.

## Android tools

If available, use:

```bash
adb shell dumpsys batterystats --reset
adb shell dumpsys batterystats
adb shell top -o PID,CPU,RES,ARGS
adb logcat | grep KikoDiagnostics
```

Watch for:

- high CPU while idle
- log spam
- repeated service restarts
- microphone errors
- wake detections while quiet
- SpeechRecognizer running while idle

## Required checks

- Wake service runs only when enabled.
- Foreground notification is visible while wake listening is active.
- Android SpeechRecognizer starts only after wake detection.
- AudioRecord stops before SpeechRecognizer starts.
- Disabling wake word stops the service and releases microphone resources.
- Missing model does not crash the app.
- Feature-input models are not used until the Android preprocessing adapter is
  implemented.
- Wake threshold and debounce are tuned conservatively before long idle tests.

## Candidate model tests

For each `hey_kiko.tflite` candidate:

1. Run `tools/wake_training/export_check.py` and save the compatibility status.
2. Confirm Android settings show the expected model status.
3. Test false wake behavior in a quiet room for at least 30 minutes.
4. Test normal speech, Hindi/Hinglish phrases, and TV/music background.
5. Compare Low/Balanced/High sensitivity only after the base threshold is safe.

Battery optimization is a release blocker for real wake detection.
