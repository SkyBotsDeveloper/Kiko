# Colab Training Guide

Use this path when local WSL/Linux is unavailable. The goal is to avoid
Colab's global Python packages, which can change and break old notebooks.

The repo-local backend trains a small log-mel CNN and exports TFLite. It does
not use hosted wake-word trainers, Picovoice, API keys, or cloud wake detection.

## 1. Start a fresh Colab notebook

Use a CPU runtime for the first sanity run. Switch to GPU only after the
dataset layout and dependencies work.

## 2. Install micromamba

```bash
!curl -Ls https://micro.mamba.pm/api/micromamba/linux-64/latest | tar -xvj bin/micromamba
!./bin/micromamba shell init -s bash -p ~/micromamba
```

If Colab asks for a shell restart, run the next cells with the full
`~/micromamba/bin/micromamba` path.

## 3. Clone Kiko and create the pinned environment

```bash
!git clone https://github.com/SkyBotsDeveloper/Kiko.git
%cd Kiko
!git checkout v2-wake-word
!~/micromamba/bin/micromamba create -y -f tools/wake_training/environment.yml
```

Target Python is 3.10.

## 4. Add local dataset files

Recommended layout:

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

Mount Drive or upload a ZIP, then unpack into that layout. Do not commit
datasets. Keep consent records for any voice samples.

## 5. Run training

Sanity:

```bash
!~/micromamba/envs/kiko-wake-training/bin/python \
  tools/wake_training/train_hey_kiko.py --profile sanity
```

Balanced:

```bash
!~/micromamba/envs/kiko-wake-training/bin/python \
  tools/wake_training/train_hey_kiko.py --profile balanced
```

Quality with checkpoint resume:

```bash
!~/micromamba/envs/kiko-wake-training/bin/python \
  tools/wake_training/train_hey_kiko.py --profile quality --resume
```

If GPU memory is tight:

```bash
!~/micromamba/envs/kiko-wake-training/bin/python \
  tools/wake_training/train_hey_kiko.py --profile balanced --batch-size 8 --max-vram-gb 2.5
```

## 6. Check export

```bash
!~/micromamba/envs/kiko-wake-training/bin/python \
  tools/wake_training/export_check.py \
  tools/wake_training/output/hey_kiko.tflite
```

The current backend should report `feature-input-needs-adapter`; Android needs
matching log-mel preprocessing before real wake detection.
