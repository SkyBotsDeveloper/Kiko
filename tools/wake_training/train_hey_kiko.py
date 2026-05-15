#!/usr/bin/env python3
"""Scaffold for training a local open-source Hey Kiko wake-word model.

This script intentionally does not fake a trained model. It creates a stable
entry point for a future openWakeWord/microWakeWord-style backend and fails with
clear messages until datasets and a real backend are connected.
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


DEFAULT_PHRASE = "hey kiko"
DEFAULT_MODEL_NAME = "hey_kiko"
SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_OUTPUT_DIR = SCRIPT_DIR / "output"


class TrainingError(RuntimeError):
    """Raised when the training scaffold cannot continue safely."""


@dataclass(frozen=True)
class TrainingConfig:
    phrase: str = DEFAULT_PHRASE
    model_name: str = DEFAULT_MODEL_NAME
    output_dir: Path = DEFAULT_OUTPUT_DIR
    dataset_dir: Path | None = None
    backend: str = "scaffold"
    epochs: int = 20
    sample_rate_hz: int = 16_000

    @property
    def model_output_path(self) -> Path:
        return self.output_dir / f"{self.model_name}.tflite"


def prepare_dataset(config: TrainingConfig) -> None:
    """Validate wake/non-wake data locations.

    Expected future dataset layout:
      dataset_dir/
        positive/
        negative/
        validation/
    """
    if config.dataset_dir is None:
        raise TrainingError(
            "Dataset directory is required. Provide --dataset-dir with positive/"
            " and negative/ wake training audio.",
        )

    required = ["positive", "negative"]
    missing = [name for name in required if not (config.dataset_dir / name).is_dir()]
    if missing:
        raise TrainingError(
            "Dataset is incomplete. Missing: "
            + ", ".join(missing)
            + ". Expected positive/ and negative/ folders.",
        )


def generate_synthetic_samples(config: TrainingConfig) -> None:
    """Future hook for local/free synthetic data generation.

    TODO: Add a local/offline generation or augmentation backend. Do not call
    paid hosted services, do not upload private voice data, and do not commit
    generated samples.
    """
    raise TrainingError(
        "Synthetic sample generation is not implemented yet. Add a local/free "
        "backend before using this path.",
    )


def train_model(config: TrainingConfig) -> Path:
    """Train the wake-word model with a real backend.

    TODO: Implement an openWakeWord/microWakeWord-style backend in a pinned
    Python environment once a working local training route is selected.
    """
    if config.backend == "scaffold":
        raise TrainingError(
            "Training backend is not implemented. This scaffold validates setup "
            "but does not produce a fake model.",
        )
    raise TrainingError(f"Unsupported training backend: {config.backend}")


def export_tflite(config: TrainingConfig, trained_model_path: Path) -> Path:
    """Export a trained model to TensorFlow Lite.

    TODO: Convert the selected backend's trained model into a TFLite model that
    matches Android's expected input/output contract or has a documented adapter.
    """
    if not trained_model_path.exists():
        raise TrainingError(f"Trained model does not exist: {trained_model_path}")
    raise TrainingError("TFLite export is not implemented for the selected backend.")


def validate_output(config: TrainingConfig) -> None:
    """Validate the exported model path without pretending it works."""
    if not config.model_output_path.exists():
        raise TrainingError(
            f"No TFLite model was produced at {config.model_output_path}. "
            "Do not install a missing or dummy model into the Android app.",
        )


def build_config(argv: Iterable[str] | None = None) -> TrainingConfig:
    parser = argparse.ArgumentParser(description="Train a local Hey Kiko wake model.")
    parser.add_argument("--phrase", default=DEFAULT_PHRASE)
    parser.add_argument("--model-name", default=DEFAULT_MODEL_NAME)
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR)
    parser.add_argument("--dataset-dir", type=Path)
    parser.add_argument("--backend", default="scaffold")
    parser.add_argument("--epochs", type=int, default=20)
    args = parser.parse_args(list(argv) if argv is not None else None)

    return TrainingConfig(
        phrase=args.phrase.strip().lower(),
        model_name=args.model_name.strip(),
        output_dir=args.output_dir,
        dataset_dir=args.dataset_dir,
        backend=args.backend,
        epochs=args.epochs,
    )


def main(argv: Iterable[str] | None = None) -> int:
    config = build_config(argv)
    config.output_dir.mkdir(parents=True, exist_ok=True)

    print(f"Wake phrase: {config.phrase}")
    print(f"Model name: {config.model_name}")
    print(f"Output dir: {config.output_dir}")

    try:
        prepare_dataset(config)
        trained = train_model(config)
        export_tflite(config, trained)
        validate_output(config)
    except TrainingError as error:
        print(f"Training not run: {error}")
        return 2

    print(f"Model exported: {config.model_output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
