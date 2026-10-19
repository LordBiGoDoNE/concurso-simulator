import io
import tarfile
import unittest
import subprocess
import tempfile
from pathlib import Path
from local_app_images import OUTPUT, committed_context, origin, restricted_tar


def archive(entries):
    data = io.BytesIO()
    with tarfile.open(fileobj=data, mode="w") as target:
        for name, value in entries.items():
            entry = tarfile.TarInfo(name)
            if value is None:
                entry.type = tarfile.SYMTYPE
                entry.linkname = "../../private"
            else:
                entry.size = len(value)
            target.addfile(entry, None if value is None else io.BytesIO(value))
    return data.getvalue()


class ContextTests(unittest.TestCase):
    def test_git_export_does_not_read_untracked_files_even_under_public_paths(self):
        OUTPUT.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(prefix="source-test-", dir=OUTPUT) as directory:
            root = Path(directory).resolve()
            self.assertTrue(root.is_relative_to(OUTPUT.resolve()))
            (root / "assets").mkdir()
            (root / "assets/public.txt").write_text("PUBLIC", encoding="utf-8")
            subprocess.run(["git", "init", "-q", str(root)], check=True)
            subprocess.run(["git", "add", "assets/public.txt"], cwd=root, check=True)
            subprocess.run(["git", "-c", "user.name=Fixture", "-c", "user.email=fixture@example.invalid",
                            "-c", "commit.gpgsign=false", "commit", "-qm", "synthetic"], cwd=root, check=True)
            (root / "assets/untracked.txt").write_text("PRIVATE_SENTINEL", encoding="utf-8")
            _, context, selected = committed_context(root=root)
            self.assertEqual(list(selected), ["assets/public.txt"])
            self.assertNotIn(b"PRIVATE_SENTINEL", context)

    def test_only_runtime_sources_and_public_material_reach_docker(self):
        entries = {"backend/src/main/App.java": b"source", "frontend/src/App.tsx": b"ui",
                   "01_Portugues/aula.html": b"lesson", "scripts/math_content.py": b"math",
                   "backend/src/test/Fixture.java": b"TEST_SENTINEL", "frontend/src/App.test.tsx": b"TEST_SENTINEL",
                   "backend/build/private.json": b"SECRET_SENTINEL", "assets/.env": b"SECRET_SENTINEL",
                   "frontend/.env.local": b"SECRET_SENTINEL", "editorial/decisions.json": b"PRIVATE_SENTINEL",
                   ".git/config": b"SECRET_SENTINEL", "infra/.env": b"SECRET_SENTINEL"}
        result, selected = restricted_tar(archive(entries))
        self.assertEqual(set(selected), {"backend/src/main/App.java", "frontend/src/App.tsx", "01_Portugues/aula.html", "scripts/math_content.py"})
        self.assertNotIn(b"SENTINEL", result)

    def test_links_and_traversal_are_rejected(self):
        for name, value in [("frontend/src/link.ts", None), ("../outside", b"bad"), ("/absolute", b"bad")]:
            with self.subTest(name=name), self.assertRaises(ValueError):
                restricted_tar(archive({name: value}))

    def test_origin_is_canonical_loopback_and_has_explicit_port(self):
        self.assertEqual(origin("http://127.0.0.1:18088/"), "http://127.0.0.1:18088")
        for value in ["http://0.0.0.0:8088", "http://localhost:8088", "https://example.com", "http://a:b@127.0.0.1:8088",
                      "http://127.0.0.1:8088/?token=x", "http://127.0.0.1:8088/#x", "http://127.0.0.1:80", "http://127.0.0.1"]:
            with self.subTest(value=value), self.assertRaises(ValueError):
                origin(value)


if __name__ == "__main__":
    unittest.main()
