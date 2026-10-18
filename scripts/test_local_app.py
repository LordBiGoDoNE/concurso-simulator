import unittest
from unittest.mock import patch
from pathlib import Path
import local_app


class OwnershipTests(unittest.TestCase):
    def test_foreign_container_blocks_project_before_any_mutation(self):
        replies = iter([b"container-id\n", b"wrong-owner\n"])
        with patch.object(local_app, "docker", side_effect=lambda *a: next(replies)) as call:
            with self.assertRaises(ValueError):
                local_app.owned({"project": "concurso-simulator-app-local-test", "owner": "expected"})
            self.assertEqual([c.args[:2] for c in call.call_args_list], [("ps", "-aq"), ("container", "inspect")])

    def test_cannot_target_another_project_or_state_tree(self):
        with patch.object(local_app, "docker") as call:
            with self.assertRaises(ValueError):
                local_app.owned({"project": "concurso-simulator", "owner": "x"})
            call.assert_not_called()
        for path in [local_app.ROOT, local_app.ROOT / "infra", Path.home()]:
            with self.subTest(path=path), self.assertRaises(ValueError):
                local_app.state_path(path)

    def test_partial_restore_cannot_start_api(self):
        with patch.object(Path, "exists", return_value=True), patch.object(local_app, "compose") as call:
            with self.assertRaises(ValueError):
                local_app.start(local_app.OUTPUT / "environments/test", {})
            call.assert_not_called()


if __name__ == "__main__":
    unittest.main()
