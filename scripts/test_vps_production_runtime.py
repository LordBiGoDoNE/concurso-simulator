import json
import re
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import vps_production as production
from local_app_images import permitted

ROOT = Path(__file__).resolve().parents[1]
INFRA = ROOT / "infra/vps-production"


class ContextTests(unittest.TestCase):
    def test_production_allowlist_adds_only_the_two_container_files(self):
        self.assertTrue(production.production_permitted("infra/vps-production/web.Dockerfile"))
        self.assertTrue(production.production_permitted("infra/vps-production/nginx.conf"))
        for name in ("infra/vps-production/compose.yaml", "infra/vps-production/README.md",
                     "infra/vps-production/host/host-audit.sh", "infra/local-app/.local/secrets/db_password",
                     "infra/vps-production/.env"):
            with self.subTest(name=name):
                self.assertFalse(production.production_permitted(name))

    def test_local_allowlist_is_unchanged(self):
        self.assertFalse(permitted("infra/vps-production/web.Dockerfile"))
        self.assertTrue(permitted("infra/local-app/web.Dockerfile"))


class ConfigurationTests(unittest.TestCase):
    compose = (INFRA / "compose.yaml").read_text(encoding="utf-8")

    def test_only_web_publishes_and_only_on_loopback(self):
        ports = re.findall(r"^\s+- (\S+:\$\{PROD_HTTP_PORT[^}]*\}:8080)$", self.compose, re.M)
        self.assertEqual(ports, ["127.0.0.1:${PROD_HTTP_PORT:?Use scripts/vps_production.py}:8080"])
        self.assertEqual(self.compose.count("ports:"), 1)

    def test_api_uses_non_local_profile_and_canonical_origin(self):
        self.assertIn("SPRING_PROFILES_ACTIVE: production", self.compose)
        self.assertNotRegex(self.compose, r"SPRING_PROFILES_ACTIVE:.*local")
        self.assertIn("CORS_ORIGINS: https://concurso-simulator.reemsoftworks.com\n", self.compose)
        self.assertIn("SERVER_FORWARD_HEADERS_STRATEGY: none", self.compose)

    def test_every_secret_is_a_file_and_admin_password_is_configured(self):
        for name in production.SECRET_NAMES:
            with self.subTest(name=name):
                self.assertIn(f"{name}: {{file: '${{PROD_SECRET_DIR}}/{name}'}}", self.compose)
        self.assertIn("target: app.admin.bootstrap.password", self.compose)

    def test_database_network_is_internal(self):
        self.assertRegex(self.compose, r"database:\n\s+internal: true")

    def test_web_container_accepts_only_canonical_host_and_clears_forwarded_headers(self):
        nginx = (INFRA / "nginx.conf").read_text(encoding="utf-8")
        self.assertIn("if ($host != concurso-simulator.reemsoftworks.com) { return 421; }", nginx)
        for header in ("Forwarded", "X-Forwarded-For", "X-Forwarded-Host", "X-Forwarded-Proto"):
            self.assertEqual(nginx.count(f'proxy_set_header {header} "";'), 2, header)
        self.assertNotIn("sub_filter", nginx)

    def test_web_build_refuses_non_canonical_material_origin(self):
        dockerfile = (INFRA / "web.Dockerfile").read_text(encoding="utf-8")
        self.assertIn("u.protocol!=='https:'", dockerfile)
        self.assertIn("u.hostname!=='concurso-simulator.reemsoftworks.com'", dockerfile)
        self.assertIn("COPY infra/vps-production/nginx.conf", dockerfile)


class StateTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.base = Path(self.directory.name).resolve()
        patches = [mock.patch.object(production, "linux_engine"),
                   mock.patch.object(production, "validate_images"),
                   mock.patch.object(production, "port_available"),
                   mock.patch.object(production, "owned")]
        for patch in patches:
            patch.start()
            self.addCleanup(patch.stop)
        self.addCleanup(self.directory.cleanup)

    def manifest(self, **changes):
        value = {"origin": production.ORIGIN, "on_main": True, "source_sha": "a" * 40,
                 "images": {"api": {"id": "sha256:" + "1" * 64}, "web": {"id": "sha256:" + "2" * 64}},
                 "base_images": {"postgres": "postgres@sha256:" + "3" * 64}}
        value.update(changes)
        path = self.base / "release.json"
        path.write_text(json.dumps(value), encoding="utf-8")
        return path

    def test_state_must_be_absolute_and_outside_checkout(self):
        with self.assertRaises(ValueError):
            production.state_path("relative/state")
        with self.assertRaises(ValueError):
            production.state_path(ROOT / "infra/vps-production/state")

    def test_init_creates_private_secrets_without_printing_them(self):
        state = self.base / "state"
        with mock.patch("builtins.print") as printed:
            production.initialize(state, self.manifest(), rehearsal=False)
        secrets_dir = state / "secrets"
        self.assertEqual(sorted(p.name for p in secrets_dir.iterdir()), sorted(production.SECRET_NAMES))
        self.assertEqual((secrets_dir / "google_enabled").read_text(), "false")
        self.assertEqual((secrets_dir / "google_callback").read_text(),
                         "https://concurso-simulator.reemsoftworks.com/login/oauth2/code/google")
        admin = (secrets_dir / "admin_password").read_text()
        self.assertGreaterEqual(len(admin), 24)
        output = " ".join(str(call) for call in printed.call_args_list)
        self.assertNotIn(admin, output)
        self.assertNotIn((secrets_dir / "db_password").read_text(), output)
        config = json.loads((state / "state.json").read_text())
        self.assertEqual((config["project"], config["port"], config["rehearsal"]), (production.PROJECT, 18090, False))

    def test_init_refuses_existing_state(self):
        state = self.base / "state"
        state.mkdir()
        (state / "anything").write_text("x")
        with self.assertRaises(ValueError):
            production.initialize(state, self.manifest(), rehearsal=False)
        self.assertFalse((state / "secrets").exists())

    def test_init_refuses_other_origin(self):
        with self.assertRaises(ValueError):
            production.initialize(self.base / "state", self.manifest(origin="http://127.0.0.1:8088"), rehearsal=False)

    def test_commit_outside_main_requires_rehearsal_flag(self):
        with self.assertRaises(ValueError):
            production.initialize(self.base / "state", self.manifest(on_main=False), rehearsal=False)
        production.initialize(self.base / "rehearsal", self.manifest(on_main=False), rehearsal=True)
        config = json.loads((self.base / "rehearsal/state.json").read_text())
        self.assertTrue(config["rehearsal"])

    def test_load_rejects_tampered_state(self):
        state = self.base / "state"
        production.initialize(state, self.manifest(), rehearsal=False)
        config = json.loads((state / "state.json").read_text())
        config["port"] = 18088
        (state / "state.json").write_text(json.dumps(config))
        with self.assertRaises(ValueError):
            production.load(state)

    def test_import_secrets_are_generated_and_missing_ones_added_without_rotation(self):
        import base64
        state = self.base / "state"
        production.initialize(state, self.manifest(), rehearsal=False)
        secrets_dir = state / "secrets"
        self.assertEqual((secrets_dir / "question_import_destination").read_text(), production.PROJECT)
        key = (secrets_dir / "question_import_key").read_text()
        self.assertGreaterEqual(len(base64.b64decode(key)), 32)
        (secrets_dir / "question_import_destination").unlink()
        config = json.loads((state / "state.json").read_text())
        with mock.patch("builtins.print"):
            production.ensure_generated_secrets(state, config)
        self.assertEqual((secrets_dir / "question_import_destination").read_text(), production.PROJECT)
        self.assertEqual((secrets_dir / "question_import_key").read_text(), key)

    def test_editorial_cli_stays_on_the_database_namespace_and_never_takes_secrets_as_args(self):
        config = {"project": production.PROJECT, "images": {"images": {"api": {"id": "sha256:" + "1" * 64}}}}
        command = production.editorial_cli_command(config, "questions", ["preview", "--editorial-local", "true"], self.base)
        self.assertIn("container:concurso-simulator-prod-postgres-1", command)
        self.assertIn("DB_PASSWORD", command)
        self.assertFalse(any(part.startswith("DB_PASSWORD=") for part in command))
        self.assertTrue(any("localhost:5432" in part for part in command))
        self.assertNotIn("-p", command)
        for launcher, args in (("questions", ["register"]), ("templates", ["import"]), ("outro", ["preview"]),
                               ("questions", []), ("questions", ["import", "--yes"])):
            with self.subTest(launcher=launcher, args=args):
                with self.assertRaises(ValueError):
                    production.editorial_cli_command(config, launcher, args, self.base)

    def test_operation_lock_is_exclusive(self):
        state = self.base / "state"
        state.mkdir()
        with production.operation(state):
            with self.assertRaises(FileExistsError):
                with production.operation(state):
                    pass
        self.assertFalse((state / "operation.lock").exists())


if __name__ == "__main__":
    unittest.main()
