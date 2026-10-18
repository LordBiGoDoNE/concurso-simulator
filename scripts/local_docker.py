"""Pin one validated local endpoint per process, including builds and Compose calls."""
import json
import os
import re
import subprocess
from functools import lru_cache


def environment(values=None):
    result = dict(os.environ if values is None else values)
    for key in ("DOCKER_CONTEXT", "DOCKER_HOST", "BUILDX_BUILDER"):
        result.pop(key, None)
    return result


@lru_cache(maxsize=1)
def endpoint():
    # Match Docker CLI precedence before pinning --host on every subsequent command.
    context = os.environ.get("DOCKER_CONTEXT")
    host = os.environ.get("DOCKER_HOST")
    if context or not host:
        env = environment()
        if not context:
            context = subprocess.check_output(["docker", "context", "show"], env=env, text=True).strip()
        host = json.loads(subprocess.check_output(["docker", "context", "inspect", context,
                                                  "--format", "{{json .Endpoints.docker.Host}}"], env=env, text=True))
    if not (host.startswith("unix:///") or re.fullmatch(r"npipe:/+\./pipe/[^/?#]+", host)):
        raise ValueError("Docker deve apontar para socket Unix ou named pipe deste computador")
    return host


def run(args, **kwargs):
    kwargs["env"] = environment(kwargs.get("env"))
    return subprocess.run(["docker", "--host", endpoint(), *args], **kwargs)


def check_output(args, **kwargs):
    return run(args, stdout=subprocess.PIPE, check=True, **kwargs).stdout
