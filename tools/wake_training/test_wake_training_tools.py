import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

import export_check
import create_colab_notebook
import generate_synthetic_positives
import install_model_to_app
import prepare_free_negatives
import train_hey_kiko


HAS_NUMPY = importlib.util.find_spec("numpy") is not None
HAS_LIBROSA = importlib.util.find_spec("librosa") is not None
HAS_TENSORFLOW = importlib.util.find_spec("tensorflow") is not None


class WakeTrainingToolsTest(unittest.TestCase):
    def test_training_config_defaults_to_hey_kiko_balanced(self):
        config = train_hey_kiko.build_config([])

        self.assertEqual("hey kiko", config.phrase)
        self.assertEqual("hey_kiko", config.model_name)
        self.assertEqual("balanced", config.profile)
        self.assertEqual(train_hey_kiko.PROFILES["balanced"].batch_size, config.batch_size)
        self.assertIn("first usable", train_hey_kiko.PROFILE_DESCRIPTIONS["balanced"])

    def test_synthetic_generator_defaults_to_hey_kiko(self):
        config = generate_synthetic_positives.build_config([])

        self.assertEqual("hey kiko", config.phrase)
        self.assertEqual(1000, config.count)
        self.assertEqual("espeak-ng", config.backend)
        self.assertFalse(config.hard_negative)

    def test_hard_negative_phrase_list_contains_similar_wakes(self):
        hard_negatives = set(generate_synthetic_positives.HARD_NEGATIVE_PHRASES)

        self.assertIn("hey google", hard_negatives)
        self.assertIn("hey siri", hard_negatives)
        self.assertIn("hey keto", hard_negatives)
        self.assertIn("hello kiko", hard_negatives)

    def test_prepare_free_negatives_defaults_to_sanity_profile(self):
        config = prepare_free_negatives.build_config([])

        self.assertEqual("sanity", config.profile)
        self.assertGreater(prepare_free_negatives.NEGATIVE_PROFILES["quality"].hard_negative_count, 0)

    def test_prepare_free_negatives_missing_dependency_behavior(self):
        original = prepare_free_negatives.import_numpy
        prepare_free_negatives.import_numpy = lambda: (_ for _ in ()).throw(
            prepare_free_negatives.NegativePreparationError("missing numpy"),
        )
        try:
            with tempfile.TemporaryDirectory() as temp_dir:
                with self.assertRaises(prepare_free_negatives.NegativePreparationError):
                    prepare_free_negatives.generate_noise_set(
                        output_dir=Path(temp_dir),
                        prefix="test",
                        count=1,
                        seconds=0.1,
                        sample_rate_hz=16_000,
                        rng=__import__("random").Random(1),
                    )
        finally:
            prepare_free_negatives.import_numpy = original

    def test_colab_notebook_exists_and_generator_has_required_steps(self):
        notebook = create_colab_notebook.notebook_json()
        source = "\n".join(
            "".join(cell.get("source", []))
            for cell in notebook["cells"]
        )

        self.assertTrue(create_colab_notebook.NOTEBOOK_PATH.exists())
        self.assertIn("generate_synthetic_positives.py", source)
        self.assertIn("prepare_free_negatives.py", source)
        self.assertIn("export_check.py", source)

    def test_dataset_validation_reports_missing_folders(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            config = train_hey_kiko.build_config(["--data-dir", temp_dir])

            with self.assertRaises(train_hey_kiko.TrainingError):
                train_hey_kiko.prepare_dataset(config)

    @unittest.skipUnless(HAS_NUMPY and HAS_LIBROSA, "numpy/librosa are not installed")
    def test_preprocessing_shape(self):
        import numpy as np

        config = train_hey_kiko.build_config(["--profile", "sanity"])
        audio = np.zeros(config.duration_samples, dtype="float32")

        features = train_hey_kiko.extract_log_mel(audio, config)

        self.assertEqual(config.feature_shape, features.shape)

    @unittest.skipUnless(HAS_TENSORFLOW, "tensorflow is not installed")
    def test_model_creation_shape(self):
        config = train_hey_kiko.build_config(["--profile", "sanity"])

        model = train_hey_kiko.create_model(config.feature_shape, config)

        self.assertEqual((None,) + config.feature_shape, model.input_shape)
        self.assertEqual((None, 1), model.output_shape)

    def test_training_report_generation(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            output_dir = Path(temp_dir)
            config = train_hey_kiko.build_config(["--output-dir", str(output_dir)])
            runtime = train_hey_kiko.RuntimeInfo(
                device="cpu",
                gpu_name=None,
                mixed_precision=False,
                max_vram_gb=None,
            )
            report_path = train_hey_kiko.write_training_report(
                config=config,
                runtime=runtime,
                model_path=output_dir / "hey_kiko.tflite",
                input_shape=config.feature_shape,
                output_shape=(1, 1),
                metrics={"recommended_threshold": 0.7},
                warnings=["unit test"],
            )

            report = json.loads(report_path.read_text(encoding="utf-8"))

            self.assertEqual("hey kiko", report["phrase"])
            self.assertEqual("feature-input-needs-adapter", report["androidCompatibility"]["status"])
            self.assertNotIn("rawConversation", json.dumps(report))

    def test_export_check_handles_missing_file(self):
        report = export_check.build_report(Path("missing_model.tflite"))

        self.assertFalse(report.exists)
        self.assertTrue(report.errors)
        self.assertEqual("invalid", report.compatibility_status)

    def test_install_refuses_missing_source(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            missing = Path(temp_dir) / "missing.tflite"

            with self.assertRaises(FileNotFoundError):
                install_model_to_app.install_model(missing)


if __name__ == "__main__":
    unittest.main()
