#!/usr/bin/env python3
"""Train a local open-source Hey Kiko wake-word classifier.

The backend is intentionally self-contained and local: a small TensorFlow/Keras
CNN is trained on log-mel features and exported to TensorFlow Lite. It does not
use Picovoice, cloud audio, hosted trainers, or paid SDKs.

The exported model expects log-mel feature input, not raw PCM samples. Android
must implement the same feature extractor before this model can drive real wake
detection.
"""

from __future__ import annotations

import argparse
import json
import math
import random
import re
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable


DEFAULT_PHRASE = "hey kiko"
DEFAULT_MODEL_NAME = "hey_kiko"
SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_DATA_DIR = SCRIPT_DIR / "data"
DEFAULT_OUTPUT_DIR = SCRIPT_DIR / "output"
BACKEND_NAME = "kiko_log_mel_cnn"
AUDIO_EXTENSIONS = {".wav", ".flac", ".mp3", ".ogg", ".m4a"}


class TrainingError(RuntimeError):
    """Raised when training cannot continue safely."""


@dataclass(frozen=True)
class ProfileSettings:
    epochs: int
    batch_size: int
    duration_ms: int
    n_mels: int
    model_width: int
    patience: int
    validation_split: float
    max_examples_per_class: int | None
    augment_copies: int
    learning_rate: float


PROFILES: dict[str, ProfileSettings] = {
    "sanity": ProfileSettings(
        epochs=2,
        batch_size=8,
        duration_ms=1200,
        n_mels=32,
        model_width=12,
        patience=1,
        validation_split=0.25,
        max_examples_per_class=40,
        augment_copies=0,
        learning_rate=1e-3,
    ),
    "balanced": ProfileSettings(
        epochs=25,
        batch_size=16,
        duration_ms=1200,
        n_mels=40,
        model_width=20,
        patience=5,
        validation_split=0.2,
        max_examples_per_class=2000,
        augment_copies=1,
        learning_rate=8e-4,
    ),
    "quality": ProfileSettings(
        epochs=60,
        batch_size=12,
        duration_ms=1400,
        n_mels=64,
        model_width=28,
        patience=8,
        validation_split=0.2,
        max_examples_per_class=None,
        augment_copies=2,
        learning_rate=5e-4,
    ),
}

PROFILE_DESCRIPTIONS: dict[str, str] = {
    "sanity": "small synthetic dataset; verifies the pipeline only, not accuracy",
    "balanced": "GTX 1650-safe first usable model target with more synthetic data/noise",
    "quality": "larger synthetic dataset, harder negatives, checkpoints/resume, longer training",
}


@dataclass(frozen=True)
class TrainingConfig:
    phrase: str = DEFAULT_PHRASE
    model_name: str = DEFAULT_MODEL_NAME
    profile: str = "balanced"
    output_dir: Path = DEFAULT_OUTPUT_DIR
    data_dir: Path = DEFAULT_DATA_DIR
    backend: str = BACKEND_NAME
    epochs: int = PROFILES["balanced"].epochs
    batch_size: int = PROFILES["balanced"].batch_size
    sample_rate_hz: int = 16_000
    duration_ms: int = PROFILES["balanced"].duration_ms
    n_mels: int = PROFILES["balanced"].n_mels
    n_fft: int = 400
    hop_length: int = 160
    win_length: int = 400
    model_width: int = PROFILES["balanced"].model_width
    patience: int = PROFILES["balanced"].patience
    validation_split: float = PROFILES["balanced"].validation_split
    max_examples_per_class: int | None = PROFILES["balanced"].max_examples_per_class
    augment_copies: int = PROFILES["balanced"].augment_copies
    learning_rate: float = PROFILES["balanced"].learning_rate
    seed: int = 42
    max_vram_gb: float | None = 3.0
    mixed_precision: bool = False
    resume: bool = False

    @property
    def model_output_path(self) -> Path:
        return self.output_dir / f"{self.model_name}.tflite"

    @property
    def keras_output_path(self) -> Path:
        return self.output_dir / f"{self.model_name}.keras"

    @property
    def report_output_path(self) -> Path:
        return self.output_dir / "training_report.json"

    @property
    def checkpoint_path(self) -> Path:
        return self.output_dir / "checkpoints" / f"{self.model_name}.keras"

    @property
    def duration_samples(self) -> int:
        return int(self.sample_rate_hz * self.duration_ms / 1000)

    @property
    def feature_frames(self) -> int:
        return max(1, 1 + ((self.duration_samples - self.n_fft) // self.hop_length))

    @property
    def feature_shape(self) -> tuple[int, int, int]:
        return (self.n_mels, self.feature_frames, 1)


@dataclass(frozen=True)
class DatasetManifest:
    positive: list[Path]
    negative: list[Path]
    validation_positive: list[Path]
    validation_negative: list[Path]
    background_noise: list[Path]


@dataclass
class PreparedDataset:
    train_x: Any
    train_y: Any
    val_x: Any
    val_y: Any
    input_shape: tuple[int, int, int]
    warnings: list[str]


@dataclass
class RuntimeInfo:
    device: str
    gpu_name: str | None
    mixed_precision: bool
    max_vram_gb: float | None


@dataclass
class TrainingArtifacts:
    model: Any
    history: dict[str, list[float]]
    metrics: dict[str, float | int | None]
    input_shape: tuple[int, ...]
    output_shape: tuple[int, ...]
    warnings: list[str]


def profile_settings(profile: str) -> ProfileSettings:
    try:
        return PROFILES[profile]
    except KeyError as error:
        raise TrainingError(f"Unknown profile '{profile}'. Choose one of: {', '.join(PROFILES)}") from error


def prepare_dataset(config: TrainingConfig) -> DatasetManifest:
    """Validate and discover local wake-word training data."""
    data_dir = config.data_dir
    positive_root = data_dir / "positive" / config.model_name
    if not positive_root.is_dir():
        fallback_positive = data_dir / "positive"
        positive_root = fallback_positive if fallback_positive.is_dir() else positive_root

    negative_root = data_dir / "negative"
    validation_positive_root = data_dir / "validation" / "positive"
    validation_negative_root = data_dir / "validation" / "negative"
    background_root = data_dir / "background_noise"

    missing = [
        path
        for path in (positive_root, negative_root)
        if not path.is_dir()
    ]
    if missing:
        expected = (
            "Expected data folders:\n"
            "  tools/wake_training/data/positive/hey_kiko/\n"
            "  tools/wake_training/data/negative/\n"
            "  tools/wake_training/data/background_noise/ optional\n"
            "  tools/wake_training/data/validation/positive/ optional\n"
            "  tools/wake_training/data/validation/negative/ optional"
        )
        raise TrainingError(
            "Dataset is incomplete. Missing: "
            + ", ".join(str(path) for path in missing)
            + "\n"
            + expected,
        )

    positive = _discover_audio(positive_root, config.max_examples_per_class, config.seed)
    negative = _discover_audio(negative_root, config.max_examples_per_class, config.seed + 1)
    validation_positive = _discover_audio(validation_positive_root, None, config.seed + 2)
    validation_negative = _discover_audio(validation_negative_root, None, config.seed + 3)
    background_noise = _discover_audio(background_root, 500, config.seed + 4)

    if not positive:
        raise TrainingError(f"No positive wake samples found in {positive_root}.")
    if not negative:
        raise TrainingError(f"No negative samples found in {negative_root}.")

    return DatasetManifest(
        positive=positive,
        negative=negative,
        validation_positive=validation_positive,
        validation_negative=validation_negative,
        background_noise=background_noise,
    )


def generate_synthetic_samples(config: TrainingConfig) -> list[Path]:
    """Reserved hook for future local/free sample generation.

    The current backend trains only on user-provided local data plus lightweight
    augmentation. It never calls hosted TTS or uploads voice data.
    """
    del config
    return []


def train_model(
    config: TrainingConfig,
    prepared: PreparedDataset | None = None,
    runtime: RuntimeInfo | None = None,
) -> TrainingArtifacts:
    """Train the mobile-friendly log-mel CNN."""
    tf = _import_tensorflow()
    if prepared is None:
        manifest = prepare_dataset(config)
        prepared = build_prepared_dataset(config, manifest)
    if runtime is None:
        runtime = configure_tensorflow(config)

    config.output_dir.mkdir(parents=True, exist_ok=True)
    config.checkpoint_path.parent.mkdir(parents=True, exist_ok=True)

    callbacks = [
        tf.keras.callbacks.ModelCheckpoint(
            filepath=str(config.checkpoint_path),
            monitor="val_loss",
            save_best_only=True,
        ),
        tf.keras.callbacks.EarlyStopping(
            monitor="val_loss",
            patience=config.patience,
            restore_best_weights=True,
        ),
        tf.keras.callbacks.ReduceLROnPlateau(
            monitor="val_loss",
            factor=0.5,
            patience=max(1, config.patience // 2),
            min_lr=1e-5,
        ),
    ]

    batch_size = max(1, config.batch_size)
    last_oom: Exception | None = None
    for attempt in range(4):
        try:
            model = _load_or_create_model(config, prepared.input_shape)
            history = model.fit(
                prepared.train_x,
                prepared.train_y,
                validation_data=(prepared.val_x, prepared.val_y),
                epochs=config.epochs,
                batch_size=batch_size,
                callbacks=callbacks,
                verbose=1,
            )
            model.save(config.keras_output_path)
            metrics = evaluate_model(model, prepared.val_x, prepared.val_y)
            return TrainingArtifacts(
                model=model,
                history={key: [float(value) for value in values] for key, values in history.history.items()},
                metrics=metrics,
                input_shape=(1,) + tuple(int(value) for value in model.input_shape[1:]),
                output_shape=tuple(1 if value is None else int(value) for value in model.output_shape),
                warnings=prepared.warnings,
            )
        except tf.errors.ResourceExhaustedError as error:
            last_oom = error
            tf.keras.backend.clear_session()
            if batch_size <= 1 or attempt == 3:
                break
            batch_size = max(1, batch_size // 2)
            print(f"CUDA OOM during training. Retrying with batch size {batch_size}.")

    raise TrainingError(
        "Training ran out of GPU memory. Try --batch-size 4, --max-vram-gb 2.5, "
        "or run --profile sanity first."
    ) from last_oom


def export_tflite(config: TrainingConfig, trained_model: Any) -> Path:
    """Export a trained Keras model to TensorFlow Lite."""
    tf = _import_tensorflow()
    if not hasattr(trained_model, "save"):
        model_path = Path(trained_model)
        if not model_path.exists():
            raise TrainingError(f"Trained model does not exist: {model_path}")
        trained_model = tf.keras.models.load_model(model_path)

    converter = tf.lite.TFLiteConverter.from_keras_model(trained_model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_bytes = converter.convert()
    config.output_dir.mkdir(parents=True, exist_ok=True)
    config.model_output_path.write_bytes(tflite_bytes)
    return config.model_output_path


def validate_output(config: TrainingConfig) -> None:
    """Validate that the exported TFLite model exists and is non-empty."""
    if not config.model_output_path.exists():
        raise TrainingError(
            f"No TFLite model was produced at {config.model_output_path}. "
            "Do not install a missing or dummy model into the Android app.",
        )
    if config.model_output_path.stat().st_size <= 0:
        raise TrainingError(f"TFLite model is empty: {config.model_output_path}")


def build_prepared_dataset(config: TrainingConfig, manifest: DatasetManifest) -> PreparedDataset:
    """Load audio, extract log-mel features, and create train/validation arrays."""
    np = _import_numpy()
    rng = np.random.default_rng(config.seed)
    random.seed(config.seed)

    background_audio = [
        load_audio_file(path, config, random_crop=True, rng=rng)
        for path in manifest.background_noise
    ]

    train_features: list[Any] = []
    train_labels: list[int] = []
    val_features: list[Any] = []
    val_labels: list[int] = []
    warnings: list[str] = []

    for path in manifest.positive:
        audio = load_audio_file(path, config, random_crop=False, rng=rng)
        train_features.append(extract_log_mel(audio, config))
        train_labels.append(1)
        for _ in range(config.augment_copies):
            augmented = augment_audio(audio, background_audio, config, rng)
            train_features.append(extract_log_mel(augmented, config))
            train_labels.append(1)

    for path in manifest.negative:
        audio = load_audio_file(path, config, random_crop=True, rng=rng)
        train_features.append(extract_log_mel(audio, config))
        train_labels.append(0)
        for _ in range(config.augment_copies):
            augmented = augment_audio(audio, background_audio, config, rng)
            train_features.append(extract_log_mel(augmented, config))
            train_labels.append(0)

    for path in manifest.validation_positive:
        val_features.append(extract_log_mel(load_audio_file(path, config, random_crop=False, rng=rng), config))
        val_labels.append(1)
    for path in manifest.validation_negative:
        val_features.append(extract_log_mel(load_audio_file(path, config, random_crop=True, rng=rng), config))
        val_labels.append(0)

    train_x = np.stack(train_features).astype("float32")
    train_y = np.asarray(train_labels, dtype="float32")

    if val_features and val_labels:
        val_x = np.stack(val_features).astype("float32")
        val_y = np.asarray(val_labels, dtype="float32")
    else:
        train_x, train_y, val_x, val_y = _split_validation(train_x, train_y, config.validation_split, rng)
        warnings.append("No explicit validation set found; using a deterministic split from training data.")

    if train_x.shape[1:] != config.feature_shape:
        warnings.append(f"Feature shape is {train_x.shape[1:]}; expected {config.feature_shape}.")

    return PreparedDataset(
        train_x=train_x,
        train_y=train_y,
        val_x=val_x,
        val_y=val_y,
        input_shape=tuple(int(value) for value in train_x.shape[1:]),
        warnings=warnings,
    )


def load_audio_file(path: Path, config: TrainingConfig, random_crop: bool, rng: Any) -> Any:
    librosa = _import_librosa()
    np = _import_numpy()
    try:
        audio, _ = librosa.load(path, sr=config.sample_rate_hz, mono=True)
    except Exception as error:
        raise TrainingError(f"Could not load audio file {path}: {error}") from error

    if audio.size == 0:
        raise TrainingError(f"Audio file is empty: {path}")

    audio, _ = librosa.effects.trim(audio, top_db=35)
    if audio.size == 0:
        audio = np.zeros(config.duration_samples, dtype="float32")
    audio = normalize_audio(audio)
    return fit_audio_window(audio, config.duration_samples, random_crop=random_crop, rng=rng)


def normalize_audio(audio: Any) -> Any:
    np = _import_numpy()
    peak = float(np.max(np.abs(audio))) if audio.size else 0.0
    if peak > 0:
        audio = audio / peak
    return audio.astype("float32")


def fit_audio_window(audio: Any, target_samples: int, random_crop: bool, rng: Any | None = None) -> Any:
    np = _import_numpy()
    if audio.size > target_samples:
        if random_crop and rng is not None:
            start = int(rng.integers(0, audio.size - target_samples + 1))
        else:
            start = max(0, (audio.size - target_samples) // 2)
        return audio[start : start + target_samples].astype("float32")
    if audio.size < target_samples:
        pad_total = target_samples - audio.size
        left = pad_total // 2
        right = pad_total - left
        return np.pad(audio, (left, right), mode="constant").astype("float32")
    return audio.astype("float32")


def augment_audio(audio: Any, background_audio: list[Any], config: TrainingConfig, rng: Any) -> Any:
    np = _import_numpy()
    augmented = audio.copy()

    gain = float(rng.uniform(0.75, 1.25))
    augmented = augmented * gain

    max_shift = max(1, int(0.12 * config.sample_rate_hz))
    shift = int(rng.integers(-max_shift, max_shift + 1))
    if shift:
        augmented = np.roll(augmented, shift)
        if shift > 0:
            augmented[:shift] = 0
        else:
            augmented[shift:] = 0

    if background_audio:
        noise = background_audio[int(rng.integers(0, len(background_audio)))]
        noise = fit_audio_window(noise, config.duration_samples, random_crop=True, rng=rng)
        audio_rms = float(np.sqrt(np.mean(np.square(augmented))) + 1e-6)
        noise_rms = float(np.sqrt(np.mean(np.square(noise))) + 1e-6)
        noise_scale = float(rng.uniform(0.01, 0.08)) * (audio_rms / noise_rms)
        augmented = augmented + (noise * noise_scale)

    return normalize_audio(augmented)


def extract_log_mel(audio: Any, config: TrainingConfig) -> Any:
    librosa = _import_librosa()
    np = _import_numpy()
    mel = librosa.feature.melspectrogram(
        y=audio,
        sr=config.sample_rate_hz,
        n_fft=config.n_fft,
        hop_length=config.hop_length,
        win_length=config.win_length,
        n_mels=config.n_mels,
        fmin=20,
        fmax=min(7600, config.sample_rate_hz // 2),
        center=False,
        power=2.0,
    )
    log_mel = librosa.power_to_db(mel, ref=np.max, top_db=80.0)
    normalized = ((log_mel + 80.0) / 80.0) * 2.0 - 1.0
    normalized = np.clip(normalized, -1.0, 1.0)
    if normalized.shape != (config.n_mels, config.feature_frames):
        normalized = _fix_feature_shape(normalized, config)
    return normalized.astype("float32")[..., np.newaxis]


def create_model(input_shape: tuple[int, int, int], config: TrainingConfig) -> Any:
    tf = _import_tensorflow()
    width = config.model_width
    inputs = tf.keras.Input(shape=input_shape, name="log_mel")
    x = tf.keras.layers.Conv2D(width, (3, 3), padding="same", use_bias=False)(inputs)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.ReLU()(x)
    x = tf.keras.layers.MaxPooling2D((2, 2))(x)
    x = tf.keras.layers.SeparableConv2D(width * 2, (3, 3), padding="same", use_bias=False)(x)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.ReLU()(x)
    x = tf.keras.layers.MaxPooling2D((2, 2))(x)
    x = tf.keras.layers.SeparableConv2D(width * 3, (3, 3), padding="same", use_bias=False)(x)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.ReLU()(x)
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dense(width * 2, activation="relu")(x)
    x = tf.keras.layers.Dropout(0.2)(x)
    outputs = tf.keras.layers.Dense(1, activation="sigmoid", dtype="float32", name="wake_score")(x)
    model = tf.keras.Model(inputs=inputs, outputs=outputs, name=f"{config.model_name}_wake_cnn")
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=config.learning_rate),
        loss="binary_crossentropy",
        metrics=[
            "accuracy",
            tf.keras.metrics.AUC(name="auc"),
            tf.keras.metrics.Precision(name="precision"),
            tf.keras.metrics.Recall(name="recall"),
        ],
    )
    return model


def evaluate_model(model: Any, val_x: Any, val_y: Any) -> dict[str, float | int | None]:
    np = _import_numpy()
    raw = model.evaluate(val_x, val_y, verbose=0, return_dict=True)
    predictions = model.predict(val_x, verbose=0).reshape(-1)
    threshold, f1 = recommend_threshold(predictions, val_y)
    metrics: dict[str, float | int | None] = {key: float(value) for key, value in raw.items()}
    metrics["recommended_threshold"] = threshold
    metrics["recommended_threshold_f1"] = f1
    metrics["validation_examples"] = int(np.asarray(val_y).shape[0])
    return metrics


def recommend_threshold(predictions: Any, labels: Any) -> tuple[float | None, float | None]:
    np = _import_numpy()
    labels = np.asarray(labels).astype("int32")
    predictions = np.asarray(predictions).astype("float32")
    if labels.size == 0 or len(np.unique(labels)) < 2:
        return None, None

    best_threshold = 0.5
    best_f1 = -1.0
    for threshold in np.linspace(0.25, 0.9, 27):
        predicted = predictions >= threshold
        true_positive = int(np.sum((predicted == 1) & (labels == 1)))
        false_positive = int(np.sum((predicted == 1) & (labels == 0)))
        false_negative = int(np.sum((predicted == 0) & (labels == 1)))
        precision = true_positive / max(1, true_positive + false_positive)
        recall = true_positive / max(1, true_positive + false_negative)
        f1 = (2 * precision * recall) / max(1e-6, precision + recall)
        if f1 > best_f1:
            best_f1 = f1
            best_threshold = float(threshold)
    return round(best_threshold, 3), round(float(best_f1), 4)


def write_training_report(
    config: TrainingConfig,
    runtime: RuntimeInfo,
    model_path: Path,
    input_shape: tuple[int, ...],
    output_shape: tuple[int, ...],
    metrics: dict[str, float | int | None],
    warnings: list[str] | None = None,
) -> Path:
    config.output_dir.mkdir(parents=True, exist_ok=True)
    report = {
        "phrase": config.phrase,
        "profile": config.profile,
        "createdAt": datetime.now(timezone.utc).isoformat(),
        "backend": config.backend,
        "device": asdict(runtime),
        "modelPath": str(model_path),
        "kerasModelPath": str(config.keras_output_path),
        "inputShape": list(input_shape),
        "outputShape": list(output_shape),
        "sampleRateHz": config.sample_rate_hz,
        "durationMs": config.duration_ms,
        "featureType": "log_mel",
        "validationMetrics": metrics,
        "thresholdRecommendation": metrics.get("recommended_threshold"),
        "warnings": warnings or [],
        "androidCompatibility": {
            "status": "feature-input-needs-adapter",
            "notes": [
                "This model expects log-mel features shaped like [1, n_mels, frames, 1].",
                "The current Android runner accepts raw PCM [1, samples].",
                "Implement the same log-mel feature extractor on Android before claiming real wake detection.",
            ],
        },
    }
    config.report_output_path.write_text(json.dumps(report, indent=2), encoding="utf-8")
    return config.report_output_path


def configure_tensorflow(config: TrainingConfig) -> RuntimeInfo:
    tf = _import_tensorflow()
    tf.keras.utils.set_random_seed(config.seed)

    gpu_name: str | None = None
    gpus = tf.config.list_physical_devices("GPU")
    if gpus:
        first_gpu = gpus[0]
        gpu_name = first_gpu.name
        try:
            if config.max_vram_gb:
                tf.config.set_logical_device_configuration(
                    first_gpu,
                    [
                        tf.config.LogicalDeviceConfiguration(
                            memory_limit=int(config.max_vram_gb * 1024),
                        ),
                    ],
                )
            else:
                for gpu in gpus:
                    tf.config.experimental.set_memory_growth(gpu, True)
        except Exception as error:
            print(f"GPU memory limit could not be applied: {error}")

        try:
            details = tf.config.experimental.get_device_details(first_gpu)
            gpu_name = details.get("device_name") or gpu_name
        except Exception:
            pass

    if config.mixed_precision and gpus:
        try:
            tf.keras.mixed_precision.set_global_policy("mixed_float16")
        except Exception as error:
            print(f"Mixed precision could not be enabled safely: {error}")

    runtime = RuntimeInfo(
        device="cuda" if gpus else "cpu",
        gpu_name=gpu_name,
        mixed_precision=bool(config.mixed_precision and gpus),
        max_vram_gb=config.max_vram_gb,
    )
    print(f"Selected device: {runtime.device}")
    if runtime.gpu_name:
        print(f"GPU: {runtime.gpu_name}")
    return runtime


def build_config(argv: Iterable[str] | None = None) -> TrainingConfig:
    parser = argparse.ArgumentParser(description="Train a local Hey Kiko wake model.")
    parser.add_argument("--phrase", default=DEFAULT_PHRASE)
    parser.add_argument("--model-name", default=DEFAULT_MODEL_NAME)
    parser.add_argument("--profile", choices=sorted(PROFILES), default="balanced")
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR)
    parser.add_argument("--data-dir", "--dataset-dir", type=Path, default=DEFAULT_DATA_DIR)
    parser.add_argument("--backend", default=BACKEND_NAME)
    parser.add_argument("--epochs", type=int)
    parser.add_argument("--batch-size", type=int)
    parser.add_argument("--max-vram-gb", type=float, default=3.0)
    parser.add_argument("--mixed-precision", action="store_true")
    parser.add_argument("--resume", action="store_true")
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--no-augment", action="store_true")
    parser.add_argument("--validation-split", type=float)
    parser.add_argument("--learning-rate", type=float)
    args = parser.parse_args(list(argv) if argv is not None else None)

    settings = profile_settings(args.profile)
    phrase = " ".join(args.phrase.strip().lower().split())
    model_name = _safe_model_name(args.model_name)
    augment_copies = 0 if args.no_augment else settings.augment_copies

    return TrainingConfig(
        phrase=phrase,
        model_name=model_name,
        profile=args.profile,
        output_dir=args.output_dir,
        data_dir=args.data_dir,
        backend=args.backend,
        epochs=args.epochs or settings.epochs,
        batch_size=args.batch_size or settings.batch_size,
        duration_ms=settings.duration_ms,
        n_mels=settings.n_mels,
        model_width=settings.model_width,
        patience=settings.patience,
        validation_split=args.validation_split if args.validation_split is not None else settings.validation_split,
        max_examples_per_class=settings.max_examples_per_class,
        augment_copies=augment_copies,
        learning_rate=args.learning_rate or settings.learning_rate,
        seed=args.seed,
        max_vram_gb=args.max_vram_gb,
        mixed_precision=args.mixed_precision,
        resume=args.resume,
    )


def main(argv: Iterable[str] | None = None) -> int:
    try:
        config = build_config(argv)
        if config.backend != BACKEND_NAME:
            raise TrainingError(f"Unsupported backend: {config.backend}")

        config.output_dir.mkdir(parents=True, exist_ok=True)
        print(f"Wake phrase: {config.phrase}")
        print(f"Model name: {config.model_name}")
        print(f"Profile: {config.profile}")
        print(f"Profile purpose: {PROFILE_DESCRIPTIONS[config.profile]}")
        print(f"Output dir: {config.output_dir}")
        print(f"Data dir: {config.data_dir}")
        if config.profile == "quality":
            print("Quality profile selected. This is GTX 1650 safe but may take longer; checkpointing is enabled.")

        runtime = configure_tensorflow(config)
        manifest = prepare_dataset(config)
        generate_synthetic_samples(config)
        prepared = build_prepared_dataset(config, manifest)
        artifacts = train_model(config, prepared, runtime)
        model_path = export_tflite(config, artifacts.model)
        validate_output(config)
        report_path = write_training_report(
            config=config,
            runtime=runtime,
            model_path=model_path,
            input_shape=artifacts.input_shape,
            output_shape=artifacts.output_shape,
            metrics=artifacts.metrics,
            warnings=artifacts.warnings,
        )
        _run_export_check(model_path)
    except TrainingError as error:
        print(f"Training failed: {error}")
        return 2

    print(f"Model exported: {config.model_output_path}")
    print(f"Training report: {report_path}")
    print("Android note: this log-mel model needs a Kotlin feature adapter before real wake detection.")
    return 0


def _discover_audio(root: Path, limit: int | None, seed: int) -> list[Path]:
    if not root.is_dir():
        return []
    files = sorted(path for path in root.rglob("*") if path.suffix.lower() in AUDIO_EXTENSIONS)
    if limit is not None and len(files) > limit:
        rng = random.Random(seed)
        files = sorted(rng.sample(files, limit))
    return files


def _split_validation(train_x: Any, train_y: Any, validation_split: float, rng: Any) -> tuple[Any, Any, Any, Any]:
    np = _import_numpy()
    train_indices: list[int] = []
    val_indices: list[int] = []
    for label in (0, 1):
        indices = np.where(train_y == label)[0]
        rng.shuffle(indices)
        if len(indices) <= 1:
            train_indices.extend(indices.tolist())
            continue
        val_count = max(1, int(math.ceil(len(indices) * validation_split)))
        val_indices.extend(indices[:val_count].tolist())
        train_indices.extend(indices[val_count:].tolist())

    if not val_indices:
        raise TrainingError("Not enough examples to create a validation split.")

    rng.shuffle(train_indices)
    rng.shuffle(val_indices)
    return train_x[train_indices], train_y[train_indices], train_x[val_indices], train_y[val_indices]


def _fix_feature_shape(features: Any, config: TrainingConfig) -> Any:
    np = _import_numpy()
    fixed = np.zeros((config.n_mels, config.feature_frames), dtype="float32")
    rows = min(config.n_mels, features.shape[0])
    cols = min(config.feature_frames, features.shape[1])
    fixed[:rows, :cols] = features[:rows, :cols]
    return fixed


def _load_or_create_model(config: TrainingConfig, input_shape: tuple[int, int, int]) -> Any:
    tf = _import_tensorflow()
    if config.resume and config.checkpoint_path.exists():
        print(f"Resuming from checkpoint: {config.checkpoint_path}")
        return tf.keras.models.load_model(config.checkpoint_path)
    return create_model(input_shape, config)


def _run_export_check(model_path: Path) -> None:
    try:
        from export_check import build_report, print_report

        print_report(build_report(model_path))
    except Exception as error:
        print(f"Export check skipped: {error}")


def _safe_model_name(value: str) -> str:
    cleaned = re.sub(r"[^a-zA-Z0-9_]+", "_", value.strip()).strip("_").lower()
    if not cleaned:
        raise TrainingError("Model name cannot be empty.")
    return cleaned


def _import_numpy():
    try:
        import numpy as np

        return np
    except Exception as error:
        raise TrainingError("numpy is required. Install tools/wake_training/requirements.txt.") from error


def _import_librosa():
    try:
        import librosa

        return librosa
    except Exception as error:
        raise TrainingError("librosa is required. Install tools/wake_training/requirements.txt.") from error


def _import_tensorflow():
    try:
        import tensorflow as tf

        return tf
    except Exception as error:
        raise TrainingError("TensorFlow is required for training/export. Install tools/wake_training/requirements.txt.") from error


if __name__ == "__main__":
    raise SystemExit(main())
