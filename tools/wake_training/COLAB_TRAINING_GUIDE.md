# Colab Training Guide

The easiest training path is the checked-in notebook:

```text
tools/wake_training/Kiko_Hey_Kiko_Training_Colab.ipynb
```

Open it in Google Colab, select GPU runtime, and run the cells in order.

## What the notebook does

1. Installs pinned Python dependencies and `espeak-ng`.
2. Generates synthetic `Hey Kiko` positive samples.
3. Generates free baseline negative/noise data and hard negatives.
4. Runs sanity training.
5. Lets you opt into balanced or quality training.
6. Exports `hey_kiko.tflite`.
7. Runs `export_check.py`.
8. Downloads `hey_kiko.tflite` and `training_report.json`.

You do not need to record your own voice for the first model. Real recordings
can improve quality later, but they are optional and should never be committed.

## Why this path exists

Public hosted trainers can be paid, unavailable, or pinned to fragile notebooks.
Kiko keeps this local/Colab workflow so training does not depend on Picovoice,
paid SDKs, API keys, hosted wake-word trainers, or cloud wake detection.

## Quality choice

- Start with `sanity` to confirm the pipeline.
- Use `balanced` for the first usable model target.
- Use `quality` when you can wait longer and want more synthetic data,
  harder negatives, checkpoints, and safer threshold tuning.

The model still needs real-device validation. The current Android runtime also
needs a log-mel feature adapter before this model can run live wake detection.
