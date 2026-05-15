#!/usr/bin/env python3
"""Record local wake-word training samples.

WSL microphone access varies by Windows and distribution setup. If this script
cannot see a microphone, record WAV files on a phone instead and copy them into
the data folders documented in README.md.
"""

from __future__ import annotations

import argparse
import wave
from datetime import datetime
from pathlib import Path
from typing import Iterable


SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_DATA_DIR = SCRIPT_DIR / "data"
DEFAULT_SAMPLE_RATE = 16_000


def record_samples(argv: Iterable[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Record Hey Kiko wake training samples.")
    parser.add_argument("--label", choices=["positive", "negative", "background_noise"], default="positive")
    parser.add_argument("--phrase", default="hey_kiko")
    parser.add_argument("--count", type=int, default=10)
    parser.add_argument("--seconds", type=float, default=1.5)
    parser.add_argument("--sample-rate", type=int, default=DEFAULT_SAMPLE_RATE)
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_DATA_DIR)
    args = parser.parse_args(list(argv) if argv is not None else None)

    try:
        import numpy as np
        import sounddevice as sd
    except Exception as error:
        print(f"Recording dependencies are unavailable: {error}")
        print("Install sounddevice, or record WAV files on a phone and copy them into tools/wake_training/data/.")
        return 2

    if args.label == "positive":
        target_dir = args.output_dir / "positive" / args.phrase
    elif args.label == "negative":
        target_dir = args.output_dir / "negative"
    else:
        target_dir = args.output_dir / "background_noise"
    target_dir.mkdir(parents=True, exist_ok=True)

    frame_count = int(args.seconds * args.sample_rate)
    for index in range(args.count):
        print(f"Sample {index + 1}/{args.count}: press Enter, then speak naturally.")
        input()
        try:
            recording = sd.rec(frame_count, samplerate=args.sample_rate, channels=1, dtype="float32")
            sd.wait()
        except Exception as error:
            print(f"Microphone recording failed: {error}")
            print("If this is WSL, use phone recordings and copy WAV files into the data folders.")
            return 2

        audio = np.clip(recording.reshape(-1), -1.0, 1.0)
        pcm16 = (audio * 32767.0).astype("<i2")
        timestamp = datetime.utcnow().strftime("%Y%m%d_%H%M%S")
        output_path = target_dir / f"{args.label}_{timestamp}_{index + 1:03d}.wav"
        _write_wav(output_path, pcm16.tobytes(), args.sample_rate)
        print(f"Saved {output_path}")

    return 0


def _write_wav(path: Path, pcm_bytes: bytes, sample_rate: int) -> None:
    with wave.open(str(path), "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(sample_rate)
        wav.writeframes(pcm_bytes)


if __name__ == "__main__":
    raise SystemExit(record_samples())
