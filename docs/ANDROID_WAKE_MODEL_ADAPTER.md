# Android Wake Model Adapter

The current `tools/wake_training/train_hey_kiko.py` backend exports a
TensorFlow Lite classifier that expects log-mel feature input:

```text
[1, n_mels, frames, 1]
```

It does not accept raw PCM audio directly.

## Current Android State

`TfliteWakeModelRunner` currently supports raw float32 PCM-shaped input:

```text
[1, samples]
```

If a log-mel feature model is installed, Android can detect the shape and report
`Feature adapter needed`, but it must not claim real wake detection is ready.
Manual mic and Fake/Test wake remain available.

## Next Android Phase

Implement and test:

- `WakeFeatureExtractor`
- `LogMelFeatureExtractor`
- feature window buffering for 16 kHz mono `AudioRecord` frames
- the same preprocessing constants as Python:
  - sample rate: 16 kHz
  - `n_fft=400`
  - `win_length=400`
  - `hop_length=160`
  - profile-specific `n_mels`
  - log-mel dB normalization to roughly `[-1.0, 1.0]`

The Kotlin extractor must match Python output closely enough that wake scores
from live phone audio are meaningful.

## Validation Before Claiming Real Wake

Do not treat the model as production-ready until:

- the model loads on Android,
- feature tensor shape matches the TFLite input,
- synthetic and real-device wake scores are sensible,
- false accepts are tested against hard negatives,
- false rejects are tested across voices/noise,
- battery and heat are measured with wake enabled.
