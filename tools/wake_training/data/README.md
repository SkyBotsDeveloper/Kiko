# Wake Training Data

Keep local training audio here. Do not commit recorded voices, generated
samples, datasets, or experiment outputs.

Manual voice recording is optional. The beginner path uses
`generate_synthetic_positives.py` and `prepare_free_negatives.py` to populate
these folders without recording your own voice.

Expected layout:

```text
tools/wake_training/data/
  positive/
    hey_kiko/
  negative/
  background_noise/
  validation/
    positive/
    negative/
```

Use 16 kHz mono WAV when possible. Phone recordings are acceptable if WSL
microphone access is unreliable; copy them into the matching folders before
running `train_hey_kiko.py`.
