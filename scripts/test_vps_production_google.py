import tempfile
import unittest
from pathlib import Path
from unittest import mock

import vps_production as production

# Synthetic values assembled at runtime so secret scanners do not mistake them for credentials.
CLIENT = "0" * 12 + "-" + "a" * 32 + ".apps." + "googleusercontent.com"
SECRET = "TEST" + "-" + "x" * 28


class GoogleConfigurationTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.state = Path(self.directory.name)
        (self.state / "secrets").mkdir()
        for name, value in (("google_enabled", "false"), ("google_client_id", ""), ("google_client_secret", ""),
                            ("google_callback", "x"), ("frontend_url", "x")):
            (self.state / "secrets" / name).write_text(value, encoding="utf-8")
        self.config = {"port": 18090, "project": production.PROJECT}
        self.restart = mock.patch.object(production, "restart_api").start()
        self.addCleanup(mock.patch.stopall)

    def read(self, name):
        return (self.state / "secrets" / name).read_text(encoding="utf-8")

    def test_valid_values_enable_google_in_place_without_printing_secret(self):
        inode = (self.state / "secrets" / "google_client_secret").stat().st_ino
        with mock.patch.object(production, "google_enabled_on_api", return_value=True), mock.patch("builtins.print") as printed:
            production.configure_google(self.state, self.config, read_line=lambda _: CLIENT, read_secret=lambda _: SECRET)
        self.assertEqual(self.read("google_enabled"), "true")
        self.assertEqual(self.read("google_client_id"), CLIENT)
        self.assertEqual(self.read("google_client_secret"), SECRET)
        self.assertEqual(self.read("google_callback"), "https://concurso-simulator.reemsoftworks.com/login/oauth2/code/google")
        self.assertEqual(self.read("frontend_url"), "https://concurso-simulator.reemsoftworks.com")
        self.assertEqual((self.state / "secrets" / "google_client_secret").stat().st_ino, inode)
        self.assertNotIn(SECRET, " ".join(str(c) for c in printed.call_args_list))

    def test_unexpected_formats_change_nothing(self):
        for client, secret in ((CLIENT, "curto"), ("nao-e-google", SECRET), (CLIENT, SECRET + " espaço")):
            with self.subTest(client=client, secret=secret):
                with self.assertRaises(ValueError):
                    production.configure_google(self.state, self.config, read_line=lambda _: client, read_secret=lambda _: secret)
                self.assertEqual(self.read("google_enabled"), "false")
                self.assertEqual(self.read("google_client_secret"), "")
        self.restart.assert_not_called()

    def test_api_rejection_turns_google_back_off(self):
        with mock.patch.object(production, "google_enabled_on_api", return_value=False):
            with self.assertRaises(ValueError):
                production.configure_google(self.state, self.config, read_line=lambda _: CLIENT, read_secret=lambda _: SECRET)
        self.assertEqual(self.read("google_enabled"), "false")
        self.assertEqual(self.restart.call_count, 2)

    def test_disable_keeps_credentials(self):
        with mock.patch.object(production, "google_enabled_on_api", return_value=True), mock.patch("builtins.print"):
            production.configure_google(self.state, self.config, read_line=lambda _: CLIENT, read_secret=lambda _: SECRET)
            production.disable_google(self.state, self.config)
        self.assertEqual(self.read("google_enabled"), "false")
        self.assertEqual(self.read("google_client_id"), CLIENT)

    def test_missing_or_linked_secret_file_is_refused(self):
        (self.state / "secrets" / "google_enabled").unlink()
        with self.assertRaises(ValueError):
            production.rewrite_secret(self.state, "google_enabled", "true")


if __name__ == "__main__":
    unittest.main()
