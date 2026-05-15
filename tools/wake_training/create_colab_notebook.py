#!/usr/bin/env python3
"""Create the beginner-friendly Hey Kiko Colab training notebook."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Iterable


SCRIPT_DIR = Path(__file__).resolve().parent
NOTEBOOK_PATH = SCRIPT_DIR / "Kiko_Hey_Kiko_Training_Colab.ipynb"


def notebook_json() -> dict:
    cells = [
        markdown(
            "# Kiko Hey Kiko Wake Model Training\n\n"
            "Beginner path: run these cells in order. You do not need to record your own voice. "
            "This notebook generates synthetic `Hey Kiko` positives, prepares free baseline "
            "negative/noise data, trains a local TFLite model, checks the export, and downloads "
            "`hey_kiko.tflite` plus `training_report.json`.\n\n"
            "No Picovoice, no paid SDK, no API keys, and no cloud wake detection are used."
        ),
        markdown("## Step 1: setup dependencies\nSet Runtime -> Change runtime type -> GPU before running."),
        code(
            "!git clone https://github.com/SkyBotsDeveloper/Kiko.git /content/Kiko || true\n"
            "%cd /content/Kiko\n"
            "!git fetch origin v2-wake-word\n"
            "!git checkout v2-wake-word\n"
            "!apt-get update -qq\n"
            "!apt-get install -y -qq espeak-ng\n"
            "!python -m pip install --upgrade pip\n"
            "!python -m pip install -r tools/wake_training/requirements.txt\n"
            "!python - <<'PY'\n"
            "import tensorflow as tf\n"
            "print('TensorFlow:', tf.__version__)\n"
            "print('GPUs:', tf.config.list_physical_devices('GPU'))\n"
            "PY"
        ),
        markdown("## Step 2: generate synthetic `Hey Kiko` positive samples"),
        code(
            "!python tools/wake_training/generate_synthetic_positives.py "
            "--count 1000 --phrase \"hey kiko\" --validation-ratio 0.1"
        ),
        markdown("## Step 3: prepare free negative/noise data\nSanity is small and fast. Use balanced/quality later."),
        code("!python tools/wake_training/prepare_free_negatives.py --profile sanity"),
        markdown("## Step 4: run sanity training\nThis only verifies the pipeline; it is not expected to be accurate."),
        code("!python tools/wake_training/train_hey_kiko.py --profile sanity --batch-size 8 --max-vram-gb 2.5"),
        markdown("## Step 5: optional balanced or quality training\nRun one of these after sanity succeeds."),
        code(
            "RUN_BALANCED = False\n"
            "RUN_QUALITY = False\n\n"
            "if RUN_BALANCED:\n"
            "    !python tools/wake_training/prepare_free_negatives.py --profile balanced\n"
            "    !python tools/wake_training/generate_synthetic_positives.py --count 3000 --phrase \"hey kiko\" --validation-ratio 0.1\n"
            "    !python tools/wake_training/train_hey_kiko.py --profile balanced --batch-size 12 --max-vram-gb 2.5\n\n"
            "if RUN_QUALITY:\n"
            "    !python tools/wake_training/prepare_free_negatives.py --profile quality\n"
            "    !python tools/wake_training/generate_synthetic_positives.py --count 6000 --phrase \"hey kiko\" --validation-ratio 0.1\n"
            "    !python tools/wake_training/train_hey_kiko.py --profile quality --batch-size 8 --max-vram-gb 2.5 --resume"
        ),
        markdown("## Step 6: run export check"),
        code("!python tools/wake_training/export_check.py tools/wake_training/output/hey_kiko.tflite"),
        markdown(
            "## Step 7: download model and report\n"
            "The model currently needs an Android log-mel adapter before live wake detection works."
        ),
        code(
            "from google.colab import files\n"
            "files.download('tools/wake_training/output/hey_kiko.tflite')\n"
            "files.download('tools/wake_training/output/training_report.json')"
        ),
        markdown(
            "## Next Android step\n"
            "Install the model locally with:\n\n"
            "```bash\n"
            "python tools/wake_training/install_model_to_app.py tools/wake_training/output/hey_kiko.tflite\n"
            "```\n\n"
            "Do not commit the generated model. `*.tflite`, generated audio, and datasets are gitignored."
        ),
    ]
    return {
        "cells": cells,
        "metadata": {
            "accelerator": "GPU",
            "colab": {"name": "Kiko Hey Kiko Training Colab", "provenance": []},
            "kernelspec": {"display_name": "Python 3", "name": "python3"},
            "language_info": {"name": "python"},
        },
        "nbformat": 4,
        "nbformat_minor": 5,
    }


def markdown(source: str) -> dict:
    return {"cell_type": "markdown", "metadata": {}, "source": _lines(source)}


def code(source: str) -> dict:
    return {
        "cell_type": "code",
        "execution_count": None,
        "metadata": {},
        "outputs": [],
        "source": _lines(source),
    }


def _lines(source: str) -> list[str]:
    return [line + "\n" for line in source.splitlines()]


def write_notebook(path: Path = NOTEBOOK_PATH) -> Path:
    path.write_text(json.dumps(notebook_json(), indent=2), encoding="utf-8")
    return path


def main(argv: Iterable[str] | None = None) -> int:
    del argv
    path = write_notebook()
    print(f"Wrote {path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
