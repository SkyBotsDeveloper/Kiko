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

## Current training backend

`train_hey_kiko.py` now trains a small log-mel CNN. Its expected input is:

```text
[1, n_mels, frames, 1]
```

That is a feature tensor, not raw waveform audio. The Android
`TfliteWakeModelRunner` can detect this and report `Feature adapter needed`,
but it cannot run real detection from this model until the same log-mel
preprocessing is implemented in Kotlin or bundled into the TFLite model.

See `docs/ANDROID_WAKE_MODEL_ADAPTER.md` for the next Android implementation
plan. Until that adapter exists, a trained model can be exported, inspected, and
installed for status testing, but live wake detection should not be presented as
ready.

Python preprocessing details:

- 16 kHz mono audio.
- Fixed 1.2-1.4 second window depending on profile.
- `n_fft=400`, `win_length=400`, `hop_length=160`.
- log-mel power spectrogram normalized to roughly `[-1.0, 1.0]`.

## Synthetic-first dataset

The beginner workflow does not require manual voice recording. It uses:

- synthetic positive `Hey Kiko` phrases from local/free TTS,
- generated silence/noise baseline negatives,
- generated hard negatives such as `hey google`, `hey siri`, `okay google`,
  `hey key`, `hey keto`, and `hello kiko`,
- optional public datasets or real recordings later for quality improvement.

## Inspect a model

```bash
python tools/wake_training/export_check.py tools/wake_training/output/hey_kiko.tflite
```

The report prints input/output tensor details and one of these statuses:

- `raw-audio-compatible`: can be used by the current Android runner.
- `feature-input-needs-adapter`: model found, but Android preprocessing is
  required.
- `unknown`: manual review needed.
- `invalid`: missing, empty, or not loadable.

## Compatibility rule

Do not claim real wake detection works until:

- the model loads on Android,
- tensor shapes match the runner or an adapter exists,
- wake scores are meaningful on real microphone audio,
- false accepts/rejects are tested,
- battery and heat are measured.
