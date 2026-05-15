# Open-source Wake Model

Kiko does not use Picovoice Porcupine, a Picovoice SDK, a Picovoice AccessKey,
or `.ppn` / `.pv` model files.

The target wake-word direction is a free/open-source local wake detector. The
Android runtime foundation is designed around an openWakeWord-style pipeline:

```text
AudioRecord 16 kHz mono PCM
-> local preprocessing / embedding pipeline
-> TFLite wake model inference
-> wake score smoothing and debounce
-> WakeDetected event
```

Public hosted trainers are not reliable enough to be Kiko's only path. During
manual research, hosted openWakeWord training required credits, an older Colab
notebook broke on modern Python/dependencies, and the microWakeWord website was
temporarily blocked by a training bug. Kiko therefore keeps a repo-local
training toolkit under `tools/wake_training/`.

## Model format

The preferred Android inference format is TensorFlow Lite.

Expected local test path:

```text
app/src/main/assets/wake/hey_kiko.tflite
```

The app must keep working if this file is missing. In that case, Kiko reports
the model as missing and keeps manual mic plus Fake/Test wake available.

Use the toolkit to inspect exports:

```bash
python tools/wake_training/export_check.py tools/wake_training/output/hey_kiko.tflite
```

## Training direction

The repo now includes a practical Colab-first backend. The beginner path uses
`tools/wake_training/Kiko_Hey_Kiko_Training_Colab.ipynb` to generate synthetic
positives, prepare free negative/noise data, train, export, inspect, and
download the model without manual voice recording.

`tools/wake_training/train_hey_kiko.py` trains a small mobile-friendly
TensorFlow/Keras CNN over log-mel features and exports:

```text
tools/wake_training/output/hey_kiko.tflite
tools/wake_training/output/training_report.json
```

Supported profiles:

- `sanity`: small synthetic dataset and generated negative baseline; pipeline
  check only.
- `balanced`: more synthetic samples, more negatives/noise, GTX 1650-safe first
  usable model target.
- `quality`: larger synthetic dataset, hard negatives, more augmentation,
  checkpoint/resume, longer training without reckless GPU overload.

The current backend is not an openWakeWord hosted trainer. It is a local,
controlled path so Kiko is not blocked by paid or broken public training UIs.
Real voice samples can improve quality later, but they are optional.

## Android compatibility

The training backend exports a log-mel feature model shaped like
`[1, n_mels, frames, 1]`. Android now includes a Kotlin log-mel adapter for Kiko
trained models and still keeps the raw `[1, samples]` path for future raw-audio
models.

The first sanity model uses `[1, 32, 118, 1]`. It is useful for testing the
end-to-end Android inference path, but it is not a production-quality wake
model. Balanced/quality models still need training, phone testing, and threshold
tuning.

See `docs/ANDROID_WAKE_MODEL_ADAPTER.md` for the Android log-mel adapter plan.

## Quality tests

The model should be trained and evaluated with 16 kHz mono audio and should be
tested against:

- false accepts
- false rejects
- background noise
- different voices
- Hindi and Indian English accents
- low-end phones
- microphone differences across OEMs

## Quality bar

Do not claim production-grade wake detection until the trained model has been
tested on real devices for accuracy, CPU use, heat, and battery drain. Manual
mic input remains the reliable fallback.

See `tools/wake_training/MODEL_COMPATIBILITY.md` and
`tools/wake_training/BATTERY_TEST_PLAN.md` before installing a model into the
Android app.

## Repository safety

Do not commit large, generated, private, or proprietary model files unless they
are intentionally open-source and safe to redistribute. Keep any private
training data, generated models, and experiment artifacts out of Git.
