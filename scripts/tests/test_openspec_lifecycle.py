import contextlib
import importlib.util
import io
import os
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


MODULE = Path(__file__).resolve().parents[1] / "check_openspec_lifecycle.py"
SPEC = importlib.util.spec_from_file_location("check_openspec_lifecycle", MODULE)
lifecycle = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = lifecycle  # dataclasses resolve anotações pelo módulo
SPEC.loader.exec_module(lifecycle)

CHANGES = Path("openspec") / "changes"


class GitRepo:
    def __init__(self, root: Path):
        self.root = root
        self.git("init", "-q", "-b", "main")

    def git(self, *args):
        return subprocess.run(
            [
                "git",
                "-c", "user.name=Teste",
                "-c", "user.email=teste@example.invalid",
                "-c", "commit.gpgsign=false",
                "-c", "core.autocrlf=false",
                *args,
            ],
            cwd=self.root,
            check=True,
            capture_output=True,
            text=True,
        ).stdout

    def add_change(self, name):
        path = self.root / CHANGES / name
        path.mkdir(parents=True, exist_ok=True)
        (path / "proposal.md").write_text(f"# {name}\n", encoding="utf-8")

    def archive(self, name, date="2026-11-23"):
        source = self.root / CHANGES / name
        target = self.root / CHANGES / "archive" / f"{date}-{name}"
        target.parent.mkdir(parents=True, exist_ok=True)
        source.rename(target)

    def remove_change(self, name):
        path = self.root / CHANGES / name
        for child in path.iterdir():
            child.unlink()
        path.rmdir()

    def commit(self, message):
        self.git("add", "-A")
        self.git("commit", "-q", "--allow-empty", "-m", message)

    def branch(self, name):
        self.git("switch", "-q", "-c", name)


class LifecycleCliTests(unittest.TestCase):
    def setUp(self):
        env = patch.dict(os.environ, {"GITHUB_ACTIONS": ""})
        env.start()
        self.addCleanup(env.stop)
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.repo = GitRepo(Path(temporary.name))
        old = self.repo.root / CHANGES / "archive" / "2026-11-01-antiga"
        old.mkdir(parents=True)
        (old / "proposal.md").write_text("# antiga\n", encoding="utf-8")

    def run_check(self, *args):
        stdout, stderr = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(stdout), contextlib.redirect_stderr(stderr):
            code = lifecycle.main(["-C", str(self.repo.root), *args])
        return code, stdout.getvalue() + stderr.getvalue()

    def pr_to(self, base_branch="main"):
        return self.run_check("--base-ref", "main", "--base-branch", base_branch, "--head-ref", "feature")

    def test_spec_pr_introducing_change_passes(self):
        self.repo.commit("base")
        self.repo.branch("feature")
        self.repo.add_change("nova")
        self.repo.commit("[SPEC] nova")
        code, output = self.pr_to()
        self.assertEqual(code, 0, output)
        self.assertIn("`nova` nova em relação à base", output)

    def test_archive_pr_passes(self):
        self.repo.add_change("entregue")
        self.repo.commit("base com change entregue")
        self.repo.branch("feature")
        self.repo.archive("entregue")
        self.repo.commit("arquivar")
        code, output = self.pr_to()
        self.assertEqual(code, 0, output)
        self.assertIn("`entregue` arquivada por este PR", output)

    def test_pr_with_pending_change_fails(self):
        self.repo.add_change("entregue")
        self.repo.commit("base com change entregue")
        self.repo.branch("chore/archive-entregue")
        (self.repo.root / "README.md").write_text("hotfix\n", encoding="utf-8")
        self.repo.commit("hotfix")
        code, output = self.run_check(
            "--base-ref", "main", "--base-branch", "main", "--head-ref", "chore/archive-entregue"
        )
        self.assertEqual(code, 1, output)
        self.assertIn("ERRO: Change `entregue`", output)
        self.assertIn("openspec archive entregue", output)

    def test_partial_archive_passes_with_warning(self):
        self.repo.add_change("primeira")
        self.repo.add_change("segunda")
        self.repo.commit("base com duas changes entregues")
        self.repo.branch("feature")
        self.repo.archive("primeira")
        self.repo.commit("arquivar primeira")
        code, output = self.pr_to()
        self.assertEqual(code, 0, output)
        self.assertIn("AVISO: Change `segunda`", output)
        self.assertNotIn("ERRO", output)

    def test_removal_without_archive_fails(self):
        self.repo.add_change("entregue")
        self.repo.commit("base")
        self.repo.branch("feature")
        self.repo.remove_change("entregue")
        self.repo.commit("remover")
        code, output = self.pr_to()
        self.assertEqual(code, 1, output)
        self.assertIn("remoção sem arquivamento", output)

    def test_archive_dir_with_other_name_does_not_count(self):
        self.repo.add_change("entregue")
        self.repo.commit("base")
        self.repo.branch("feature")
        self.repo.remove_change("entregue")
        self.repo.add_change("outra")
        self.repo.archive("outra")
        self.repo.commit("arquivar outra coisa")
        code, output = self.pr_to()
        self.assertEqual(code, 1, output)
        self.assertIn("Change `entregue` saiu", output)

    def test_pr_to_epic_is_not_blocked(self):
        self.repo.add_change("entregue")
        self.repo.commit("base")
        self.repo.branch("feature")
        (self.repo.root / "README.md").write_text("tarefa\n", encoding="utf-8")
        self.repo.commit("tarefa")
        code, output = self.pr_to("epic/qualquer")
        self.assertEqual(code, 0, output)
        self.assertIn("só se aplica a PR para `main`", output)

    def test_push_without_base_warns_only(self):
        self.repo.add_change("entregue")
        self.repo.commit("merge do [SPEC]")
        code, output = self.run_check()
        self.assertEqual(code, 0, output)
        self.assertIn("AVISO: Change `entregue`", output)

    def test_missing_ref_is_an_error(self):
        self.repo.commit("base")
        code, output = self.run_check("--base-ref", "origin/nao-existe", "--base-branch", "main")
        self.assertEqual(code, 2, output)
        self.assertIn("`origin/nao-existe` não existe", output)
        self.assertNotIn("sem falhas", output)

    def test_repository_without_openspec_passes(self):
        (self.repo.root / "README.md").write_text("vazio\n", encoding="utf-8")
        shutil.rmtree(self.repo.root / "openspec")
        self.repo.commit("sem openspec")
        self.repo.branch("feature")
        self.repo.commit("vazio")
        code, output = self.pr_to()
        self.assertEqual(code, 0, output)

    def test_base_and_branch_must_come_together(self):
        with contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit) as raised:
                lifecycle.main(["--base-ref", "origin/main"])
        self.assertEqual(raised.exception.code, 2)

    def test_github_annotations(self):
        self.repo.add_change("entregue")
        self.repo.commit("base")
        self.repo.branch("feature")
        self.repo.commit("vazio")
        with patch.dict(os.environ, {"GITHUB_ACTIONS": "true"}):
            code, output = self.pr_to()
        self.assertEqual(code, 1, output)
        self.assertIn("::error::Change `entregue`", output)


class LifecycleRuleTests(unittest.TestCase):
    def tree(self, active=(), archive=()):
        return lifecycle.ChangeTree(active=frozenset(active), archive_dirs=frozenset(archive))

    def test_archived_name_strips_date(self):
        self.assertEqual(lifecycle.archived_name("2026-11-23-build-and-test-quality"), "build-and-test-quality")
        self.assertEqual(lifecycle.archived_name("sem-data"), "sem-data")

    def test_new_change_with_pending_base_fails(self):
        result = lifecycle.check_lifecycle(self.tree({"antiga"}), self.tree({"antiga", "nova"}), "main")
        self.assertEqual(len(result.errors), 1)
        self.assertIn("antiga", result.errors[0])

    def test_already_archived_name_does_not_excuse_removal(self):
        base = self.tree({"x"}, {"2026-01-01-x"})
        head = self.tree(set(), {"2026-01-01-x"})
        result = lifecycle.check_lifecycle(base, head, "main")
        self.assertEqual(len(result.errors), 1)
        self.assertIn("remoção sem arquivamento", result.errors[0])


if __name__ == "__main__":
    unittest.main()
