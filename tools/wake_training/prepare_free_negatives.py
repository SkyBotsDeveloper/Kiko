#!/usr/bin/env python3
"""Prepare free local negative and background-noise data for wake training.

The default path generates silence/noise baselines and hard-negative synthetic
phrases locally. Optional public dataset download is available but not required
for the beginner Colab workflow.
"""

from __future__ import annotations

import argparse
import random
import urllib.request
import wave
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from generate_synthetic_positives import HARD_NEGATIVE_PHRASES, SyntheticGenerationConfig, generate_samples


SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_DATA_DIR = SCRIPT_DIR / "data"
SPEECH_COMMANDS_URL = "http://download.tensorflow.org/data/speech_commands_v0.02.tar.gz"


class NegativePreparationError(RuntimeError):
    """Raised when negative/noise preparation cannot continue safely."""


@dataclass(frozen=True)
class NegativeProfile:
    noise_count: int
    background_count: int
    validation_negative_count: int
    hard_negative_count: int
    seconds: float


NEGATIVE_PROFILES: dict[str, NegativeProfile] = {
    "sanity": NegativeProfile(
        noise_count=40,
        background_count=8,
        validation_negative_count=12,
        hard_negative_count=14,
        seconds=1.2,
    ),
    "balanced": NegativeProfile(
        noise_count=500,
        background_count=80,
        validation_negative_count=120,
        hard_negative_count=280,
        seconds=1.2,
    ),
    "quality": NegativeProfile(
        noise_count=1500,
        background_count=220,
        validation_negative_count=320,
        hard_negative_count=900,
        seconds=1.4,
    ),
}


@dataclass(frozen=True)
class NegativePreparationConfig:
    profile: str = "sanity"
    data_dir: Path = DEFAULT_DATA_DIR
    sample_rate_hz: int = 16_000
    seed: int = 123
    download_speech_commands: bool = False


def build_config(argv: Iterable[str] | None = None) -> NegativePreparationConfig:
    parser = argparse.ArgumentParser(description="Prepare free negative/noise data for Hey Kiko training.")
    parser.add_argument("--profile", choices=sorted(NEGATIVE_PROFILES), default="sanity")
    parser.add_argument("--data-dir", type=Path, default=DEFAULT_DATA_DIR)
    parser.add_argument("--sample-rate", type=int, default=16_000)
    parser.add_argument("--seed", type=int, default=123)
    parser.add_argument("--download-speech-commands", action="store_true")
    args = parser.parse_args(list(argv) if argv is not None else None)
    return NegativePreparationConfig(
        profile=args.profile,
        data_dir=args.data_dir,
        sample_rate_hz=args.sample_rate,
        seed=args.seed,
        download_speech_commands=args.download_speech_commands,
    )


def prepare_negatives(config: NegativePreparationConfig) -> list[Path]:
    profile = NEGATIVE_PROFILES[config.profile]
    rng = random.Random(config.seed)
    negative_dir = config.data_dir / "negative"
    background_dir = config.data_dir / "background_noise"
    validation_positive_dir = config.data_dir / "validation" / "positive"
    validation_negative_dir = config.data_dir / "validation" / "negative"
    for path in (negative_dir, background_dir, validation_positive_dir, validation_negative_dir):
        path.mkdir(parents=True, exist_ok=True)

    generated: list[Path] = []
    generated.extend(
        generate_noise_set(
            output_dir=negative_dir,
            prefix="generated_negative",
            count=profile.noise_count,
            seconds=profile.seconds,
            sample_rate_hz=config.sample_rate_hz,
            rng=rng,
        ),
    )
    generated.extend(
        generate_noise_set(
            output_dir=background_dir,
            prefix="background_noise",
            count=profile.background_count,
            seconds=max(2.0, profile.seconds * 2),
            sample_rate_hz=config.sample_rate_hz,
            rng=rng,
        ),
    )
    generated.extend(
        generate_noise_set(
            output_dir=validation_negative_dir,
            prefix="validation_negative",
            count=profile.validation_negative_count,
            seconds=profile.seconds,
            sample_rate_hz=config.sample_rate_hz,
            rng=rng,
        ),
    )

    generated.extend(generate_hard_negatives(config, profile.hard_negative_count))

    if config.download_speech_commands:
        download_speech_commands(config)

    return generated


def generate_hard_negatives(config: NegativePreparationConfig, count: int) -> list[Path]:
    if count <= 0:
        return []
    hard_negative_dir = config.data_dir / "negative" / "hard_negatives"
    tts_config = SyntheticGenerationConfig(
        count=count,
        phrase="hey kiko",
        output_dir=hard_negative_dir,
        validation_output_dir=config.data_dir / "validation" / "negative",
        validation_ratio=0.0,
        sample_rate_hz=config.sample_rate_hz,
        seed=config.seed + 99,
        backend="espeak-ng",
        hard_negative=True,
    )
    try:
        return generate_samples(tts_config)
    except Exception as error:
        print(
            "Hard-negative TTS generation skipped. Install espeak-ng to generate "
            f"{', '.join(HARD_NEGATIVE_PHRASES)}. Reason: {error}",
        )
        return []


def generate_noise_set(
    output_dir: Path,
    prefix: str,
    count: int,
    seconds: float,
    sample_rate_hz: int,
    rng: random.Random,
) -> list[Path]:
    np = import_numpy()
    output_dir.mkdir(parents=True, exist_ok=True)
    generated: list[Path] = []
    sample_count = int(seconds * sample_rate_hz)
    for index in range(count):
        output_path = output_dir / f"{prefix}_{index + 1:05d}.wav"
        if output_path.exists():
            generated.append(output_path)
            continue
        mode = rng.choice(("silence", "white", "pinkish", "tone"))
        if mode == "silence":
            audio = np.zeros(sample_count, dtype=np.float32)
        elif mode == "white":
            audio = np.asarray(rng.choices([-1.0, 1.0], k=sample_count), dtype=np.float32)
            audio *= rng.uniform(0.003, 0.035)
        elif mode == "tone":
            frequency = rng.uniform(120.0, 900.0)
            time = np.arange(sample_count, dtype=np.float32) / sample_rate_hz
            audio = np.sin(2.0 * np.pi * frequency * time).astype(np.float32) * rng.uniform(0.002, 0.02)
        else:
            audio = np.cumsum(np.random.default_rng(rng.randint(0, 999_999)).normal(0, 0.003, sample_count))
            audio = audio.astype(np.float32)
            peak = float(np.max(np.abs(audio))) if audio.size else 0.0
            if peak > 0:
                audio = audio / peak * rng.uniform(0.005, 0.03)
        write_wav_mono(output_path, audio, sample_rate_hz)
        generated.append(output_path)
    return generated


def download_speech_commands(config: NegativePreparationConfig) -> None:
    target = config.data_dir / "speech_commands_v0.02.tar.gz"
    if target.exists():
        print(f"Speech Commands archive already exists: {target}")
        return
    print("Downloading Speech Commands can be large and slow. This is optional and resumable by rerunning.")
    urllib.request.urlretrieve(SPEECH_COMMANDS_URL, target)
    print(f"Downloaded {target}. Extract selected non-wake words into {config.data_dir / 'negative'} manually.")


def write_wav_mono(path: Path, audio, sample_rate: int) -> None:
    import array
    import numpy as np

    clipped = np.clip(audio, -1.0, 1.0)
    pcm = array.array("h", [int(float(value) * 32767) for value in clipped])
    with wave.open(str(path), "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(sample_rate)
        wav.writeframes(pcm.tobytes())


def import_numpy():
    try:
        import numpy as np

        return np
    except Exception as error:
        raise NegativePreparationError(
            "Negative preparation needs numpy. Install tools/wake_training/requirements.txt.",
        ) from error


def main(argv: Iterable[str] | None = None) -> int:
    config = build_config(argv)
    try:
        generated = prepare_negatives(config)
    except NegativePreparationError as error:
        print(f"Negative preparation failed: {error}")
        return 2

    print(f"Prepared or reused {len(generated)} negative/background WAV files.")
    print(f"Data root: {config.data_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
