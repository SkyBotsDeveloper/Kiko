# Colab Training Guide

Use this path when a local Linux/WSL machine is not available. The goal is to
avoid relying on Colab's global Python packages, which can change and break old
notebooks.

## 1. Start a fresh Colab notebook

Use a CPU runtime first. GPU can be tested later if the selected backend
benefits from it.

## 2. Install micromamba

```bash
!curl -Ls https://micro.mamba.pm/api/micromamba/linux-64/latest | tar -xvj bin/micromamba
!./bin/micromamba shell init -s bash -p ~/micromamba
```

Restart the shell cell after initialization if Colab asks.

## 3. Clone Kiko and create the pinned environment

```bash
!git clone https://github.com/SkyBotsDeveloper/Kiko.git
%cd Kiko
!git checkout v2-wake-word
!./bin/micromamba create -y -f tools/wake_training/environment.yml
```

If `./bin/micromamba` is not available after changing directories, use the full
path from the install cell.

## 4. Put datasets in Drive or Colab storage

Recommended temporary layout:

```text
tools/wake_training/datasets/hey_kiko/
  positive/
  negative/
  validation/
```

Do not commit datasets. Keep consent and privacy records for any voice samples.

## 5. Run the scaffold

```bash
!~/micromamba/envs/kiko-wake-training/bin/python \
  tools/wake_training/train_hey_kiko.py \
  --dataset-dir tools/wake_training/datasets/hey_kiko
```

The script currently validates setup and stops until a real backend is
implemented.

## 6. Export and check the model

After backend implementation:

```bash
!~/micromamba/envs/kiko-wake-training/bin/python \
  tools/wake_training/export_check.py \
  tools/wake_training/output/hey_kiko.tflite
```

Only install models that pass compatibility review and real-device tests.
