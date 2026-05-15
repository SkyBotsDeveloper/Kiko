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

## Model format

The preferred Android inference format is TensorFlow Lite.

Expected local test path:

```text
app/src/main/assets/wake/hey_kiko.tflite
```

The app must keep working if this file is missing. In that case, Kiko reports
the model as missing and keeps manual mic plus Fake/Test wake available.

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

## Repository safety

Do not commit large, generated, private, or proprietary model files unless they
are intentionally open-source and safe to redistribute. Keep any private
training data, generated models, and experiment artifacts out of Git.
