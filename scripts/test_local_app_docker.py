import json
import os
import socket
import unittest
from unittest.mock import patch
import local_app
import local_docker


class DockerSelectionTests(unittest.TestCase):
    def tearDown(self):
        local_docker.endpoint.cache_clear()

    def test_remote_context_wins_over_local_host_and_is_refused_before_daemon_call(self):
        local_docker.endpoint.cache_clear()
        with patch.dict(os.environ, {"DOCKER_CONTEXT": "remote", "DOCKER_HOST": "unix:///var/run/docker.sock"}), patch.object(local_docker.subprocess, "check_output", return_value='"tcp://remote.invalid:2376"') as metadata, patch.object(local_docker.subprocess, "run") as command:
            with self.assertRaises(ValueError):
                local_docker.run(["ps"])
            self.assertEqual(metadata.call_args.args[0][3], "remote")
            command.assert_not_called()

    def test_selection_is_pinned_and_competing_environment_is_removed(self):
        local_docker.endpoint.cache_clear()
        with patch.dict(os.environ, {"DOCKER_CONTEXT": "desktop-linux", "DOCKER_HOST": "tcp://remote.invalid:2376", "BUILDX_BUILDER": "remote-builder"}), patch.object(local_docker.subprocess, "check_output", return_value='"unix:///var/run/docker.sock"') as metadata, patch.object(local_docker.subprocess, "run") as command:
            local_docker.run(["ps"]);local_docker.run(["compose", "ps"])
            self.assertEqual(metadata.call_count, 1)
            for call in command.call_args_list:
                self.assertEqual(call.args[0][:3], ["docker", "--host", "unix:///var/run/docker.sock"])
                self.assertFalse(set(call.kwargs["env"]) & {"DOCKER_CONTEXT", "DOCKER_HOST", "BUILDX_BUILDER"})

    def test_remote_named_pipe_is_refused(self):
        local_docker.endpoint.cache_clear()
        with patch.dict(os.environ, {"DOCKER_CONTEXT": "", "DOCKER_HOST": "npipe:////other-host/pipe/docker"}):
            with self.assertRaises(ValueError):
                local_docker.endpoint()


class NameCollisionTests(unittest.TestCase):
    def test_volume_name_is_checked_even_without_matching_project_label(self):
        project = "concurso-simulator-app-local-collision"
        for labels in ({}, {"com.docker.compose.project": "other", local_app.OWNER_LABEL: "owner"}, {"com.docker.compose.project": project}):
            def read(*args):
                if args == ("volume", "ls", "--format", "{{.Name}}"):
                    return (project + "_postgres_data\n").encode()
                if args[:2] == ("volume", "inspect"):
                    return json.dumps(labels).encode()
                return b""
            with self.subTest(labels=labels), patch.object(local_app, "docker", side_effect=read) as commands:
                with self.assertRaises(ValueError):
                    local_app.owned({"project": project, "owner": "owner"})
                self.assertTrue(any(c.args[:2] == ("volume", "inspect") for c in commands.call_args_list))


class PortProbeTests(unittest.TestCase):
    def test_live_listener_is_always_a_collision(self):
        with socket.socket() as server:
            if os.name != "nt":
                server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            server.bind(("127.0.0.1", 0));server.listen()
            with self.assertRaises(OSError):
                local_app.port_available(server.getsockname()[1])

    @unittest.skipIf(os.name == "nt", "Unix TIME_WAIT semantics; Windows keeps exclusive bind")
    def test_unix_recently_closed_listener_can_restart_without_false_collision(self):
        with socket.socket() as server:
            server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            server.bind(("127.0.0.1", 0));server.listen();port = server.getsockname()[1]
            with socket.create_connection(("127.0.0.1", port)) as client:
                accepted, _ = server.accept();accepted.close()
                self.assertEqual(client.recv(1), b"")
        local_app.port_available(port)


if __name__ == "__main__":
    unittest.main()
