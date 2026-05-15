import tempfile
import unittest
from pathlib import Path

import export_check
import install_model_to_app
import train_hey_kiko


class WakeTrainingToolsTest(unittest.TestCase):
    def test_training_config_defaults_to_hey_kiko(self):
        config = train_hey_kiko.build_config([])

        self.assertEqual("hey kiko", config.phrase)
        self.assertEqual("hey_kiko", config.model_name)

    def test_export_check_handles_missing_file(self):
        report = export_check.build_report(Path("missing_model.tflite"))

        self.assertFalse(report.exists)
        self.assertTrue(report.errors)

    def test_install_refuses_missing_source(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            missing = Path(temp_dir) / "missing.tflite"

            with self.assertRaises(FileNotFoundError):
                install_model_to_app.install_model(missing)


if __name__ == "__main__":
    unittest.main()
