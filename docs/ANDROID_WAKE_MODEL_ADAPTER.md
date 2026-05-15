# Android Wake Model Adapter

The current `tools/wake_training/train_hey_kiko.py` backend exports a
TensorFlow Lite classifier that expects log-mel feature input:

```text
[1, n_mels, frames, 1]
```

It does not accept raw PCM audio directly.

## Current Android State

Android now supports two local TFLite input paths:

- raw float32 PCM-shaped input: `[1, samples]`
- Kiko log-mel feature input: `[1, n_mels, frames, 1]`

For the first sanity model, the expected shape is:

```text
[1, 32, 118, 1]
```

When this model is installed locally, settings should report `Ready, log-mel`.
Manual mic and Fake/Test wake remain available.

## Implemented Adapter

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

The Kotlin extractor is an approximation of the Python/librosa training path:
it uses a periodic Hann window, Slaney-style mel filters, power-to-dB scaling,
and the same `[-1.0, 1.0]` normalization range. Exact parity with librosa still
needs device-score validation and tuning.

## Remaining Work

- Compare Android feature tensors against Python for the same WAV fixtures.
- Tune thresholds and debounce using real-phone wake scores.
- Train balanced/quality models after the pipeline is verified.
- Revisit performance if low-end phones show CPU or heat issues.

## Calibration Mode

Debug builds expose wake score calibration in Settings. Enable Wake debug mode
to log sampled inference scores and display:

- raw score
- smoothed score
- max recent score
- active threshold
- whether the debug threshold override is active

The silence/noise score check runs the installed model against generated silence
and simple deterministic noise. If silence, noise, and spoken `Hey Kiko` all
score similarly, preprocessing or the sanity model needs more investigation.
Optional `wake/debug_hey_kiko.wav` can be placed locally in app assets for a
known-sample test, but no WAV files should be committed.

## Baseline Safety

WakeDetected is blocked unless the score crosses the active threshold and rises
above the recent ambient baseline by the required margin. The default margin is
`0.12`. If silence/noise baseline is `0.45` or higher, Kiko marks calibration as
unsafe/needs a better model and blocks wake by default. This specifically
protects against sanity models that output about `0.50` for everything.

The unsafe calibration override is debug-only behavior for diagnosis. It may
false trigger and should not be used for daily wake listening.

The orbit UI reflects this distinction: the wake service can be `Listening` and
the model can be `Ready, log-mel` while the wake trigger is still blocked for
safety because calibration needs a better model. This avoids presenting the
sanity model as broken service behavior.

## Validation Before Claiming Real Wake

Do not treat the model as production-ready until:

- the model loads on Android,
- feature tensor shape matches the TFLite input,
- synthetic and real-device wake scores are sensible,
- false accepts are tested against hard negatives,
- false rejects are tested across voices/noise,
- battery and heat are measured with wake enabled.
