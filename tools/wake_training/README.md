# Hey Kiko Wake Model Training Toolkit

This folder contains Kiko's repo-local training workflow for a free/open-source
`Hey Kiko` wake model. It exists because hosted training routes can be paid,
unavailable, or pinned to fragile notebook environments. Kiko does not use
Picovoice Porcupine, paid SDKs, API keys, cloud wake detection, or uploaded
microphone audio.

The current backend trains a small TensorFlow/Keras CNN over log-mel features
and exports TensorFlow Lite:

```text
tools/wake_training/output/hey_kiko.tflite
tools/wake_training/output/training_report.json
```

The model is not copied into Android assets automatically. Use
`install_model_to_app.py` only after export checks and real-device testing.

## Environment

Recommended WSL2/Linux setup:

```bash
conda create -n kiko-wake python=3.10 -y
conda activate kiko-wake
cd /mnt/d/Kiko
python -m pip install --upgrade pip
python -m pip install -r tools/wake_training/requirements.txt
```

If TensorFlow does not see the GPU, keep training on CPU or install the matching
TensorFlow CUDA extras for your system. Do not change the Android app just to
work around a local training environment issue.

## Dataset layout

Put local audio here:

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

Do not commit datasets, generated samples, WAV files, checkpoints, or models.

Early real testing should use at least 50-100 positive `Hey Kiko` samples. For
better generalization, collect more voices and conditions:

- `Hey Kiko`, `Hey Kee-ko`, and natural-speed variants.
- Different distances from the phone.
- Quiet room, fan noise, traffic, and TV/music background.
- Indian English, Hindi, and Hinglish pronunciation.
- Male/female voices and low/mid-range phone microphones.

Negative data should include normal speech that is not the wake phrase, silence,
room noise, and words that sound similar.

## Recording samples

WSL microphone access is inconsistent. Try:

```bash
python tools/wake_training/record_samples.py --label positive --count 20
python tools/wake_training/record_samples.py --label negative --count 20
python tools/wake_training/record_samples.py --label background_noise --seconds 3 --count 10
```

If WSL cannot access the mic, record 16 kHz mono WAV files on a phone or audio
tool and copy them into the data folders.

## Training profiles

Sanity only verifies the pipeline and is not expected to be accurate:

```bash
python tools/wake_training/train_hey_kiko.py --profile sanity
```

Balanced is the default GTX 1650-friendly first real profile:

```bash
python tools/wake_training/train_hey_kiko.py --profile balanced
```

Quality is slower, uses more augmentation, and checkpoints progress:

```bash
python tools/wake_training/train_hey_kiko.py --profile quality --resume
```

Useful safety overrides:

```bash
python tools/wake_training/train_hey_kiko.py --profile balanced --batch-size 8 --max-vram-gb 2.5
python tools/wake_training/train_hey_kiko.py --profile quality --batch-size 6 --resume
```

The script auto-detects CUDA, prints the selected device/GPU, uses conservative
batch sizes, checkpoints to `output/checkpoints/`, and reduces batch size on
CUDA OOM without retrying forever.

## Export check

Inspect the exported TFLite model:

```bash
python tools/wake_training/export_check.py tools/wake_training/output/hey_kiko.tflite
```

The current backend exports a log-mel feature model. The compatibility status
should be `feature-input-needs-adapter` until Android implements the same
feature extractor.

## Install into Android app assets

After export checks:

```bash
python tools/wake_training/install_model_to_app.py tools/wake_training/output/hey_kiko.tflite
```

Then from Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
```

`app/src/main/assets/wake/hey_kiko.tflite` is gitignored by default.

## Android compatibility

The Android runner currently accepts raw PCM-shaped TFLite input
`[1, samples]`. This training backend exports log-mel input shaped like
`[1, n_mels, frames, 1]`. Before real wake detection can be claimed, Android
needs either:

- a Kotlin feature extractor matching this Python preprocessing exactly, or
- a TFLite model that includes preprocessing and accepts raw samples.

Manual mic and Fake/Test wake remain the reliable fallback until real-device
wake detection, threshold tuning, and battery tests pass.
