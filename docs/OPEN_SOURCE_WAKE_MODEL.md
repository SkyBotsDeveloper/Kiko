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

An openWakeWord-style model can be trained for custom phrases such as `Hey
Kiko`. The model should be trained and evaluated with 16 kHz mono audio and
should be tested against:

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
