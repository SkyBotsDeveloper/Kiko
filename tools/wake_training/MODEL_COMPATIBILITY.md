# Model Compatibility

Android currently has a foundation runner, not a complete openWakeWord feature
pipeline.

## Current Android runner

`TfliteWakeModelRunner` supports:

- TensorFlow Lite model in app assets.
- Float32 input tensor.
- Float32 output tensor.
- Simple input shape like `[1, samples]`.
- Raw PCM 16-bit audio normalized to `[-1.0, 1.0]`.

This is intentionally narrow. Many wake models do not consume raw samples
directly.

## Common model shapes

openWakeWord/microWakeWord-style models may expect:

- mel spectrogram frames
- MFCC-like features
- embeddings from a separate model
- fixed feature windows rather than raw waveform samples

If the model expects any of those, Android needs a preprocessing adapter before
calling the TFLite wake classifier.

## Inspect a model

```bash
python tools/wake_training/export_check.py tools/wake_training/output/hey_kiko.tflite
```

The report prints input and output tensor details and warns when an adapter is
needed.

## Compatibility rule

Do not claim real wake detection works until:

- the model loads on Android,
- tensor shapes match the runner or an adapter exists,
- wake scores are meaningful on real microphone audio,
- false accepts/rejects are tested,
- battery and heat are measured.
