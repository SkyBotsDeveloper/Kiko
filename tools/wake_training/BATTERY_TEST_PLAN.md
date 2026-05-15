# Wake Model Battery Test Plan

Battery testing is mandatory before Kiko's wake word can be treated as
production-ready.

## Baseline

1. Install the same debug APK.
2. Disable wake word.
3. Charge to a stable battery level.
4. Leave the phone idle for 30 or 60 minutes.
5. Record battery drop, temperature, and CPU behavior.

## Wake enabled

1. Install a candidate `hey_kiko.tflite`.
2. Enable wake word.
3. Confirm the foreground notification is visible.
4. Leave the phone idle for the same duration as baseline.
5. Record battery drop, temperature, CPU behavior, and wake false triggers.

## Useful commands

```bash
adb shell dumpsys batterystats --reset
adb shell dumpsys batterystats
adb shell top -o PID,CPU,RES,ARGS
adb logcat | grep KikoDiagnostics
```

## Pass criteria

- No SpeechRecognizer activity while idle.
- No log spam.
- No repeated service restart loop.
- AudioRecord stops after wake detection.
- CPU remains low while idle.
- Battery drain is acceptable versus baseline.
- No false wake in quiet conditions during the test window.
