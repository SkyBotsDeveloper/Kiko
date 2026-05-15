#!/usr/bin/env python3
"""Install a trained Hey Kiko TFLite model into Android app assets."""

from __future__ import annotations

import argparse
import shutil
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parents[1]
APP_MODEL_PATH = REPO_ROOT / "app" / "src" / "main" / "assets" / "wake" / "hey_kiko.tflite"


def install_model(source: Path, target: Path = APP_MODEL_PATH) -> Path:
    if not source.exists():
        raise FileNotFoundError(f"Source model does not exist: {source}")
    if source.suffix.lower() != ".tflite":
        raise ValueError("Expected a .tflite model file.")

    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target)
    return target


def main() -> int:
    parser = argparse.ArgumentParser(description="Install hey_kiko.tflite into app assets.")
    parser.add_argument("model", type=Path)
    args = parser.parse_args()

    try:
        target = install_model(args.model)
    except (FileNotFoundError, ValueError) as error:
        print(f"Install failed: {error}")
        return 2

    print(f"Installed model to: {target}")
    print("Note: .tflite files are gitignored by default.")
    print("Next Android commands:")
    print(r".\gradlew.bat :app:assembleDebug")
    print(r".\gradlew.bat :app:installDebug")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
