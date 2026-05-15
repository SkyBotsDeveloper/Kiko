# Kiko Wake Model Assets

Place a trained local `hey_kiko.tflite` wake-word model here for device testing.

Do not commit large, generated, private, or proprietary model files unless they
are intentionally open-source and safe to redistribute. The app handles a
missing model gracefully and keeps manual mic plus Fake/Test wake flows usable.

Expected path:

```text
app/src/main/assets/wake/hey_kiko.tflite
```

If the model was exported by `tools/wake_training/train_hey_kiko.py`, it expects
log-mel feature input. Android includes a Kiko log-mel adapter for pipeline
testing, but real wake quality still depends on balanced/quality training,
threshold tuning, and battery testing.
