#!/usr/bin/env python3
"""Generate synthetic Hey Kiko wake samples without user voice recording.

The default backend uses the free/open-source `espeak-ng` command-line TTS
engine when it is installed. No API keys, paid SDKs, hosted wake trainers, or
cloud wake detection are used.
"""

from __future__ import annotations

import argparse
import random
import shutil
import subprocess
import tempfile
import wave
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_DATA_DIR = SCRIPT_DIR / "data"
DEFAULT_PHRASE = "hey kiko"
DEFAULT_SAMPLE_RATE = 16_000
PHRASE_VARIANTS = (
    "hey kiko",
    "hey kee ko",
    "hey key ko",
    "hey kiko",
)
DEFAULT_VOICES = (
    "en",
    "en-us",
    "en-gb",
    "en+f2",
    "en+f3",
    "en+m2",
    "en+m3",
)
HARD_NEGATIVE_PHRASES = (
    "hey google",
    "hey siri",
    "okay google",
    "kiko",
    "hey key",
    "hey keto",
    "hello kiko",
)


class SyntheticGenerationError(RuntimeError):
    """Raised when local synthetic sample generation cannot continue."""


@dataclass(frozen=True)
class SyntheticGenerationConfig:
    count: int = 1000
    phrase: str = DEFAULT_PHRASE
    output_dir: Path = DEFAULT_DATA_DIR / "positive" / "hey_kiko"
    validation_output_dir: Path = DEFAULT_DATA_DIR / "validation" / "positive"
    validation_ratio: float = 0.1
    sample_rate_hz: int = DEFAULT_SAMPLE_RATE
    seed: int = 42
    backend: str = "espeak-ng"
    hard_negative: bool = False


def build_config(argv: Iterable[str] | None = None) -> SyntheticGenerationConfig:
    parser = argparse.ArgumentParser(description="Generate synthetic Hey Kiko wake samples.")
    parser.add_argument("--count", type=int, default=1000)
    parser.add_argument("--phrase", default=DEFAULT_PHRASE)
    parser.add_argument("--output-dir", type=Path)
    parser.add_argument("--validation-output-dir", type=Path, default=DEFAULT_DATA_DIR / "validation" / "positive")
    parser.add_argument("--validation-ratio", type=float, default=0.1)
    parser.add_argument("--sample-rate", type=int, default=DEFAULT_SAMPLE_RATE)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--backend", default="espeak-ng", choices=["espeak-ng", "espeak"])
    parser.add_argument("--hard-negative", action="store_true")
    args = parser.parse_args(list(argv) if argv is not None else None)

    phrase_slug = _slug(args.phrase)
    default_output = DEFAULT_DATA_DIR / ("negative" if args.hard_negative else "positive") / phrase_slug

    return SyntheticGenerationConfig(
        count=args.count,
        phrase=" ".join(args.phrase.lower().split()),
        output_dir=args.output_dir or default_output,
        validation_output_dir=args.validation_output_dir,
        validation_ratio=max(0.0, min(0.5, args.validation_ratio)),
        sample_rate_hz=args.sample_rate,
        seed=args.seed,
        backend=args.backend,
        hard_negative=args.hard_negative,
    )


def generate_samples(config: SyntheticGenerationConfig) -> list[Path]:
    command = find_tts_command(config.backend)
    if command is None:
        raise SyntheticGenerationError(
            f"Could not find '{config.backend}'. In Colab run: !apt-get install -y espeak-ng. "
            "On Ubuntu/WSL run: sudo apt-get install espeak-ng. Manual voice recording is optional; "
            "this local TTS backend is the beginner path.",
        )

    rng = random.Random(config.seed)
    config.output_dir.mkdir(parents=True, exist_ok=True)
    if config.validation_ratio > 0 and not config.hard_negative:
        config.validation_output_dir.mkdir(parents=True, exist_ok=True)

    generated: list[Path] = []
    for index in range(config.count):
        phrase = choose_phrase(config, rng)
        voice = rng.choice(DEFAULT_VOICES)
        speed = rng.randint(135, 185)
        pitch = rng.randint(35, 65)
        amplitude = rng.randint(120, 190)
        filename_prefix = "hard_negative" if config.hard_negative else "hey_kiko"
        target_dir = choose_output_dir(config, index)
        output_path = target_dir / f"{filename_prefix}_{index + 1:05d}.wav"
        if output_path.exists():
            generated.append(output_path)
            continue

        with tempfile.TemporaryDirectory() as temp_dir:
            raw_path = Path(temp_dir) / "tts.wav"
            run_tts(
                command=command,
                phrase=phrase,
                voice=voice,
                speed=speed,
                pitch=pitch,
                amplitude=amplitude,
                output_path=raw_path,
            )
            audio, source_rate = read_wav_mono(raw_path)
            audio = post_process_audio(audio, source_rate, config.sample_rate_hz, rng)
            write_wav_mono(output_path, audio, config.sample_rate_hz)
        generated.append(output_path)

    return generated


def choose_phrase(config: SyntheticGenerationConfig, rng: random.Random) -> str:
    if config.hard_negative:
        return rng.choice(HARD_NEGATIVE_PHRASES)
    variants = tuple(dict.fromkeys((config.phrase, *PHRASE_VARIANTS)))
    return rng.choice(variants)


def choose_output_dir(config: SyntheticGenerationConfig, index: int) -> Path:
    if config.hard_negative or config.validation_ratio <= 0:
        return config.output_dir
    validation_count = int(round(config.count * config.validation_ratio))
    if index < validation_count:
        return config.validation_output_dir
    return config.output_dir


def find_tts_command(preferred: str) -> str | None:
    if preferred == "espeak-ng":
        return shutil.which("espeak-ng") or shutil.which("espeak")
    return shutil.which("espeak") or shutil.which("espeak-ng")


def run_tts(
    command: str,
    phrase: str,
    voice: str,
    speed: int,
    pitch: int,
    amplitude: int,
    output_path: Path,
) -> None:
    args = [
        command,
        "-v",
        voice,
        "-s",
        str(speed),
        "-p",
        str(pitch),
        "-a",
        str(amplitude),
        "-w",
        str(output_path),
        phrase,
    ]
    result = subprocess.run(args, capture_output=True, text=True, check=False)
    if result.returncode != 0:
        fallback_args = args.copy()
        fallback_args[fallback_args.index("-v") + 1] = "en"
        result = subprocess.run(fallback_args, capture_output=True, text=True, check=False)
    if result.returncode != 0:
        raise SyntheticGenerationError(
            "TTS generation failed. Try installing espeak-ng or using a different voice. "
            f"stderr: {result.stderr.strip()}",
        )


def read_wav_mono(path: Path) -> tuple[list[float], int]:
    with wave.open(str(path), "rb") as wav:
        channels = wav.getnchannels()
        sample_width = wav.getsampwidth()
        sample_rate = wav.getframerate()
        frames = wav.readframes(wav.getnframes())

    if sample_width != 2:
        raise SyntheticGenerationError(f"Unsupported TTS WAV sample width: {sample_width}")

    import array

    pcm = array.array("h")
    pcm.frombytes(frames)
    if channels > 1:
        mono = []
        for offset in range(0, len(pcm), channels):
            mono.append(sum(pcm[offset : offset + channels]) / channels)
        pcm_values = mono
    else:
        pcm_values = pcm
    return [max(-1.0, min(1.0, value / 32768.0)) for value in pcm_values], sample_rate


def post_process_audio(audio: list[float], source_rate: int, target_rate: int, rng: random.Random) -> list[float]:
    np, resample_poly = import_audio_dependencies()
    samples = np.asarray(audio, dtype=np.float32)
    if source_rate != target_rate:
        gcd = _gcd(source_rate, target_rate)
        samples = resample_poly(samples, target_rate // gcd, source_rate // gcd).astype(np.float32)

    gain = rng.uniform(0.75, 1.2)
    samples *= gain

    left_pad = int(rng.uniform(0.02, 0.18) * target_rate)
    right_pad = int(rng.uniform(0.02, 0.22) * target_rate)
    samples = np.concatenate([np.zeros(left_pad, dtype=np.float32), samples, np.zeros(right_pad, dtype=np.float32)])

    peak = float(np.max(np.abs(samples))) if samples.size else 0.0
    if peak > 0.98:
        samples = samples / peak * 0.98
    return samples.astype(np.float32).tolist()


def write_wav_mono(path: Path, audio: list[float], sample_rate: int) -> None:
    import array

    path.parent.mkdir(parents=True, exist_ok=True)
    pcm = array.array("h", [int(max(-1.0, min(1.0, value)) * 32767) for value in audio])
    with wave.open(str(path), "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(sample_rate)
        wav.writeframes(pcm.tobytes())


def import_audio_dependencies():
    try:
        import numpy as np
        from scipy.signal import resample_poly

        return np, resample_poly
    except Exception as error:
        raise SyntheticGenerationError(
            "Synthetic generation needs numpy and scipy. Install tools/wake_training/requirements.txt.",
        ) from error


def _slug(value: str) -> str:
    return "_".join("".join(ch if ch.isalnum() else " " for ch in value.lower()).split()) or "hey_kiko"


def _gcd(left: int, right: int) -> int:
    while right:
        left, right = right, left % right
    return max(1, left)


def main(argv: Iterable[str] | None = None) -> int:
    config = build_config(argv)
    try:
        generated = generate_samples(config)
    except SyntheticGenerationError as error:
        print(f"Synthetic generation failed: {error}")
        return 2

    print(f"Generated or reused {len(generated)} WAV files.")
    print(f"Train output: {config.output_dir}")
    if config.validation_ratio > 0 and not config.hard_negative:
        print(f"Validation output: {config.validation_output_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
