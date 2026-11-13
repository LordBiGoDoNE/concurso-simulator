import re
import shutil
import subprocess
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOST = ROOT / "infra/vps-production/host"
SCRIPTS = sorted(HOST.glob("*.sh"))
IP = re.compile(r"\b(?:\d{1,3}\.){3}\d{1,3}\b")


@unittest.skipIf(shutil.which("bash") is None, "bash ausente")
class HostScriptTests(unittest.TestCase):
    def test_scripts_parse(self):
        self.assertTrue(SCRIPTS)
        for script in SCRIPTS:
            with self.subTest(script=script.name):
                subprocess.run(["bash", "-n"], input=script.read_bytes(), check=True)

    def test_privileged_scripts_refuse_without_root(self):
        for name in ("host-audit.sh", "fix-vps-control-permissions.sh"):
            with self.subTest(script=name):
                probe = subprocess.run(["bash", "-c", "echo $EUID"], capture_output=True, text=True)
                if probe.stdout.strip() == "0":
                    self.skipTest("teste executado como root")
                result = subprocess.run(["bash", "-s"], input=(HOST / name).read_bytes(), capture_output=True)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(b"sudo", result.stderr)

    def test_permission_fix_records_before_changing_and_never_deletes(self):
        text = (HOST / "fix-vps-control-permissions.sh").read_text(encoding="utf-8")
        self.assertLess(text.index("> \"$record\""), text.index("chmod go-w"))
        self.assertNotRegex(text, r"\brm\b|-delete\b")
        self.assertIn("! -type l", text)


class NoPrivateIdentifiersTests(unittest.TestCase):
    def test_no_ip_addresses_in_production_files(self):
        for path in (ROOT / "infra/vps-production").rglob("*"):
            if path.is_file():
                with self.subTest(path=path.name):
                    found = [ip for ip in IP.findall(path.read_text(encoding="utf-8"))
                             if not ip.startswith(("127.", "0.0.0.0"))]
                    self.assertEqual(found, [])


if __name__ == "__main__":
    unittest.main()
