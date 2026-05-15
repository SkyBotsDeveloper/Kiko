# Hey Kiko Wake Model Training Toolkit

This folder contains the repo-local training scaffold for a future free/open-
source `Hey Kiko` wake model.

It exists because public hosted training routes can be unavailable, paid, or
broken. Kiko should not depend on Picovoice Porcupine, paid SDKs, hosted
trainers, API keys, or cloud audio.

## Current status

The scripts are production-minded scaffolds and validators. They do not produce
a fake model. Real wake detection still requires a trained and tested local
TFLite model.

Expected Android model path after training:

```text
app/src/main/assets/wake/hey_kiko.tflite
```

That file is gitignored by default.

## Path A: Colab with pinned environment

Use `COLAB_TRAINING_GUIDE.md`. The guide uses micromamba/conda to avoid relying
on whatever Python and packages Colab currently preinstalls.

Target Python: 3.10.

## Path B: Local Linux / WSL

On Ubuntu/WSL:

```bash
cd tools/wake_training
python3.10 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
python train_hey_kiko.py --dataset-dir datasets/hey_kiko
```

The scaffold will validate inputs and then stop until a real backend is
implemented.

## Dataset layout

Future training data should use:

```text
tools/wake_training/datasets/hey_kiko/
  positive/
  negative/
  validation/
```

Do not commit datasets or generated samples.

## Validate an export

```bash
python export_check.py output/hey_kiko.tflite
```

## Install into Android app assets

```bash
python install_model_to_app.py output/hey_kiko.tflite
```

Then rebuild and install:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
```
