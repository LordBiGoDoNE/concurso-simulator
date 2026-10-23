import importlib.util
import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


MODULE = Path(__file__).resolve().parents[1] / "build_pages.py"
SPEC = importlib.util.spec_from_file_location("build_pages", MODULE)
pages = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(pages)


class BuildPagesTests(unittest.TestCase):
    def setUp(self):
        summary = patch.dict(os.environ, {"GITHUB_STEP_SUMMARY": ""})
        summary.start()
        self.addCleanup(summary.stop)
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.home = Path(self.temporary.name)
        self.root = self.home / "source"
        self.stored = self.home / "stored"
        self.output = self.home / "output"
        self.site(self.root, "selected")

    def site(self, root, text):
        root.mkdir(parents=True)
        for name in ("index.html", ".nojekyll", "manifesto_v3.json", "catalogo_fontes_questoes.csv"):
            (root / name).write_text("<body>" + text + "</body>", encoding="utf-8")
        (root / "assets").mkdir()
        (root / "assets" / "style.css").write_text(text, encoding="utf-8")
        lesson = root / "01_Portugues"
        lesson.mkdir()
        (lesson / "aula.html").write_text("<body>" + text + "</body>", encoding="utf-8")

    def stored_preview(self, slug, text):
        path = self.stored / "previews" / slug / "index.html"
        path.parent.mkdir(parents=True)
        path.write_text(text, encoding="utf-8")

    def test_existing_main_package_call_remains_compatible(self):
        pages.build_site(self.root, self.stored, self.output, "main")
        self.assertIn("selected", (self.output / "index.html").read_text(encoding="utf-8"))
        self.assertFalse((self.output / "previews").exists())

    def test_stable_production_preserves_previews_and_excludes_stack(self):
        self.stored_preview("old", "preserved")
        for name in ("backend", "frontend"):
            (self.root / name).mkdir()
            (self.root / name / "private.txt").write_text("not public", encoding="utf-8")
        pages.build_site(self.root, self.stored, self.output, "stable", "stable")
        self.assertEqual((self.output / "index.html").read_bytes(), (self.root / "index.html").read_bytes())
        self.assertEqual((self.output / "previews/old/index.html").read_text(), "preserved")
        self.assertFalse((self.output / "backend").exists())
        self.assertFalse((self.output / "frontend").exists())

    def test_preview_keeps_stable_production_and_other_previews(self):
        self.site(self.root / "approved", "stable production")
        self.stored_preview("old", "preserved")
        slug = pages.preview_slug("melhoria/aulas")
        self.stored_preview(slug, "superseded")
        pages.build_site(self.root, self.stored, self.output, "melhoria/aulas", "stable")
        self.assertIn("stable production", (self.output / "index.html").read_text(encoding="utf-8"))
        preview = self.output / "previews" / slug
        self.assertIn("selected", (preview / "index.html").read_text(encoding="utf-8"))
        self.assertIn('href="../../"', (preview / "index.html").read_text(encoding="utf-8"))
        self.assertIn('href="../../../"', (preview / "01_Portugues/aula.html").read_text(encoding="utf-8"))
        self.assertEqual((self.output / "previews/old/index.html").read_text(), "preserved")

    def test_stable_publishing_refuses_main_before_writing_output(self):
        self.stored_preview("old", "preserved")
        with self.assertRaises(ValueError):
            pages.build_site(self.root, self.stored, self.output, "main", "stable")
        self.assertFalse(self.output.exists())

    def test_old_preview_scripts_are_not_required_or_executed(self):
        publisher = self.home / "publisher"
        self.site(publisher, "stable production")
        scripts = self.root / "scripts"
        scripts.mkdir()
        (scripts / "build_pages.py").write_text(
            "import argparse\np=argparse.ArgumentParser()\np.add_argument('--branch')\np.parse_args()\n",
            encoding="utf-8")
        (scripts / "enrich_math.py").write_text(
            "raise RuntimeError('preview code must not run')\n", encoding="utf-8")
        pages.build_site(self.root, self.stored, self.output, "melhoria/old", "stable",
                         production_source=publisher, tools_source=publisher)
        self.assertIn("stable production", (self.output / "index.html").read_text(encoding="utf-8"))
        preview = self.output / "previews" / pages.preview_slug("melhoria/old")
        self.assertIn("selected", (preview / "index.html").read_text(encoding="utf-8"))
        self.assertFalse((preview / "scripts").exists())

    def test_storage_branch_cannot_be_published(self):
        with self.assertRaises(ValueError):
            pages.build_site(self.root, self.stored, self.output, "pages-store", "stable")
        self.assertFalse(self.output.exists())


if __name__ == "__main__":
    unittest.main()
