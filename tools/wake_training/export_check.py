#!/usr/bin/env python3
"""Inspect a TFLite wake model and report Android runner compatibility."""

from __future__ import annotations

import argparse
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any


@dataclass
class CompatibilityReport:
    path: Path
    exists: bool
    size_bytes: int = 0
    interpreter_available: bool = False
    inputs: list[dict[str, Any]] = field(default_factory=list)
    outputs: list[dict[str, Any]] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)
    errors: list[str] = field(default_factory=list)

    @property
    def compatible_with_current_android_runner(self) -> bool:
        if not self.inputs or not self.outputs:
            return False
        first_input = self.inputs[0]
        first_output = self.outputs[0]
        return (
            first_input.get("dtype") == "float32"
            and first_output.get("dtype") == "float32"
            and first_input.get("shape_rank") == 2
            and first_input.get("shape", [None])[0] == 1
        )


def load_interpreter_class():
    try:
        from tensorflow.lite.python.interpreter import Interpreter

        return Interpreter
    except Exception:
        try:
            from tflite_runtime.interpreter import Interpreter

            return Interpreter
        except Exception:
            return None


def build_report(model_path: Path) -> CompatibilityReport:
    report = CompatibilityReport(path=model_path, exists=model_path.exists())
    if not report.exists:
        report.errors.append(f"Model file does not exist: {model_path}")
        return report

    report.size_bytes = model_path.stat().st_size
    if report.size_bytes <= 0:
        report.errors.append("Model file is empty.")
        return report

    interpreter_class = load_interpreter_class()
    if interpreter_class is None:
        report.warnings.append(
            "TensorFlow Lite interpreter is not installed. Install requirements "
            "to inspect tensor shapes.",
        )
        return report

    report.interpreter_available = True
    try:
        interpreter = interpreter_class(model_path=str(model_path))
        interpreter.allocate_tensors()
        report.inputs = [_tensor_summary(item) for item in interpreter.get_input_details()]
        report.outputs = [_tensor_summary(item) for item in interpreter.get_output_details()]
    except Exception as error:
        report.errors.append(f"Could not load TFLite model: {error}")
        return report

    if not report.compatible_with_current_android_runner:
        report.warnings.append(
            "Current Android runner supports simple float32 [1, samples] input. "
            "Models that expect mel spectrograms or embeddings need an adapter.",
        )

    return report


def print_report(report: CompatibilityReport) -> None:
    print(f"Model: {report.path}")
    print(f"Exists: {report.exists}")
    print(f"Size bytes: {report.size_bytes}")
    print(f"Interpreter available: {report.interpreter_available}")
    print("Inputs:")
    for item in report.inputs:
        print(f"  - {item}")
    print("Outputs:")
    for item in report.outputs:
        print(f"  - {item}")
    print(f"Android raw-sample runner compatible: {report.compatible_with_current_android_runner}")
    for warning in report.warnings:
        print(f"WARNING: {warning}")
    for error in report.errors:
        print(f"ERROR: {error}")


def _tensor_summary(detail: dict[str, Any]) -> dict[str, Any]:
    shape = detail.get("shape")
    shape_list = shape.tolist() if hasattr(shape, "tolist") else list(shape or [])
    dtype = detail.get("dtype")
    return {
        "name": detail.get("name"),
        "shape": shape_list,
        "shape_rank": len(shape_list),
        "dtype": getattr(dtype, "__name__", str(dtype)),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Check a Hey Kiko TFLite export.")
    parser.add_argument("model", type=Path)
    args = parser.parse_args()
    report = build_report(args.model)
    print_report(report)
    return 0 if report.exists and not report.errors else 2


if __name__ == "__main__":
    raise SystemExit(main())
