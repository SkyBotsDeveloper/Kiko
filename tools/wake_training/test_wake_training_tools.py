import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

import export_check
import install_model_to_app
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
