"""Load the approved 2026-11 question expansion into production through the administrative HTTP flow.

Runs on the VPS as concurso-dev against the production web container on loopback (Host header = canonical
origin). Reads the bootstrap admin password from the private secret file; never prints it. Each package goes
through import preview (digests must match the versioned manifest), apply, editorial SUBMIT and APPROVE
(AI_ONLY, as recorded in #173/#177). A private journal makes the run resumable after any step whose response
arrived; if an apply response is lost after the server confirmed it, the rerun stops instead of guessing (#221).
The checkout must be at the commit of the release in use, and the run holds the operator lock.
"""
import argparse
import hashlib
import json
import sys
import urllib.error
import urllib.request
from pathlib import Path

import vps_production as production

ROOT = Path(__file__).resolve().parents[1]
PACKAGES = ROOT / "editorial/question-bank-expansion/2026-11/packages"
CHECKLIST = ["ACCESSIBILITY", "CLARITY", "CLASSIFICATION", "CORRECTNESS", "DISTRACTORS", "SOURCES", "STEP_BY_STEP"]
RATIONALE_SUBMIT = "Carga de produção #156: pacotes 2026-11 (#173), envio à revisão"
RATIONALE_APPROVE = ("Carga de produção #156: pacotes 2026-11 (#173) com revisão individual assistida por IA, "
                     "sem revisão humana, conforme registrado na #177")


class Client:
    """Minimal HTTP client with a manual cookie jar (the SESSION cookie is Secure; transport here is host loopback)."""

    def __init__(self, base, host=production.HOST, opener=None):
        self.base, self.host, self.cookies, self.csrf = base.rstrip("/"), host, {}, None
        self.opener = opener or urllib.request.build_opener(NoRedirect)

    def request(self, method, path, body=None):
        headers = {"Host": self.host, "Accept": "application/json", "Origin": production.ORIGIN}
        if self.cookies:
            headers["Cookie"] = "; ".join(f"{k}={v}" for k, v in self.cookies.items())
        if body is not None:
            headers["Content-Type"] = "application/json"
        if method != "GET" and self.csrf:
            headers[self.csrf[0]] = self.csrf[1]
        data = None if body is None else (body if isinstance(body, bytes) else json.dumps(body).encode())
        request = urllib.request.Request(self.base + path, data=data, method=method, headers=headers)
        try:
            response = self.opener.open(request, timeout=150)
        except urllib.error.HTTPError as error:
            response = error
        for header in response.headers.get_all("Set-Cookie") or []:
            name, _, rest = header.partition("=")
            self.cookies[name.strip()] = rest.split(";", 1)[0]
        payload = response.read()
        return response.status, (json.loads(payload) if payload else None)

    def ok(self, method, path, body=None, expected=(200,)):
        status, payload = self.request(method, path, body)
        if status not in expected:
            code = payload.get("error") if isinstance(payload, dict) else None
            raise RuntimeError(f"{method} {path} respondeu {status} {code or ''}".strip())
        return payload

    def refresh_csrf(self):
        token = self.ok("GET", "/api/v1/csrf")
        self.csrf = (token["headerName"], token["token"])

    def login(self, username, password):
        self.refresh_csrf()
        self.ok("POST", "/api/v1/admin/auth/login", {"username": username, "password": password}, expected=(204,))
        self.refresh_csrf()


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def load_journal(path):
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"format": "vps-question-load/v1", "packages": {}}


def save_journal(path, journal):
    production.write_private_json(path, journal)


def batch(client, action, review_kind, checklist, rationale, receipt):
    preview = client.ok("POST", "/api/v1/admin/editorial-decisions/preview",
                        {"action": action, "reviewKind": review_kind, "checklist": checklist,
                         "rationale": rationale, "items": None, "importReceiptId": receipt})
    if not preview.get("confirmable"):
        raise RuntimeError(f"Lote {action} não confirmável para o recibo {receipt}")
    applied = client.ok("POST", "/api/v1/admin/editorial-decisions/apply",
                        {"action": action, "reviewKind": preview["reviewKind"], "checklist": preview["checklist"],
                         "rationale": preview["rationale"], "items": preview["expected"],
                         "confirmationToken": preview["confirmationToken"]})
    return applied["receipt"]["confirmation"]["nonce"]


def load(client, packages, journal_path, dry_run=False, out=print):
    manifest = json.loads((packages / "manifest.json").read_text(encoding="utf-8"))
    journal = load_journal(journal_path)
    done = journal["packages"]
    for entry in manifest["packages"]:
        cell = f'{entry["purpose"]}/{entry["topicId"]}'
        state = done.get(cell, {})
        if state.get("approve"):
            continue
        raw = (packages / entry["purpose"] / entry["topicId"] / "preview-request.json").read_bytes()
        if sha256(raw) != entry["requestSha256"]:
            raise RuntimeError(f"{cell}: SHA-256 da requisição diverge do manifesto")
        if not state.get("receipt"):
            request = json.loads(raw)
            preview = client.ok("POST", "/api/v1/admin/question-imports/preview", request)
            if (preview["packageDigest"] != entry["packageDigest"] or preview["selectionDigest"] != entry["selectionDigest"]
                    or not preview.get("confirmable")):
                raise RuntimeError(f"{cell}: prévia com digest divergente ou não confirmável")
            if dry_run:
                out(f"{cell}: prévia ok ({entry['items']} itens)")
                continue
            request["confirmationToken"] = preview["confirmationToken"]
            receipt = client.ok("POST", "/api/v1/admin/question-imports/apply", request)["receipt"]
            state = {"items": entry["items"], "requestSha256": entry["requestSha256"],
                     "receipt": receipt["confirmation"]["nonce"]}
            done[cell] = state
            save_journal(journal_path, journal)
        if not state.get("submit"):
            state["submit"] = batch(client, "SUBMIT", None, [], RATIONALE_SUBMIT, state["receipt"])
            save_journal(journal_path, journal)
        state["approve"] = batch(client, "APPROVE", "AI_ONLY", CHECKLIST, RATIONALE_APPROVE, state["receipt"])
        save_journal(journal_path, journal)
        out(f"{cell}: {entry['items']} itens importados e aprovados")
    return manifest, journal


def verify(client, manifest, out=print):
    """Public contracts per topic plus the administrative count of eligible expansion items."""
    expected = {}
    for entry in manifest["packages"]:
        expected.setdefault(entry["topicId"], {})[entry["purpose"]] = entry["items"]
    problems = []
    for topic, cells in sorted(expected.items()):
        practice = client.ok("GET", f"/api/v1/lesson-practices/availability?topicId={topic}")
        simulado = client.ok("GET", f"/api/v1/question-bank/questions?topicId={topic}&size=1")
        if practice["maximum"] != 5 or practice["available"] < cells.get("PRATICA_AULA", 0) or simulado["total"] < cells.get("SIMULADO", 0):
            problems.append(topic)
    eligible = client.ok("GET", "/api/v1/admin/questions?keyPrefix=admin.2026-11.&hasEligible=true&size=1")["total"]
    total = sum(e["items"] for e in manifest["packages"])
    out(f"Elegíveis da expansão: {eligible}/{total}; assuntos com cobertura abaixo do esperado: {problems or 'nenhum'}")
    return eligible == total and not problems


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--state", type=Path, default=production.DEFAULT_STATE)
    parser.add_argument("--packages", type=Path, default=PACKAGES)
    parser.add_argument("--dry-run", action="store_true", help="somente prévias; nenhuma escrita")
    parser.add_argument("--verify-only", action="store_true")
    args = parser.parse_args()
    state, config = production.load(args.state)
    try:
        require_release_checkout(config)
        with production.operation(state):  # never concurrent with backup/release/restore
            client = Client(f"http://127.0.0.1:{config['port']}")
            password = (state / "secrets" / "admin_password").read_text(encoding="utf-8").strip()
            client.login("admin", password)
            del password
            if args.verify_only:
                manifest = json.loads((args.packages / "manifest.json").read_text(encoding="utf-8"))
            else:
                manifest, _ = load(client, args.packages, state / "question-load.json", args.dry_run)
            ok = args.dry_run or verify(client, manifest)
        sys.exit(0 if ok else 2)
    except (RuntimeError, KeyError, ValueError, OSError) as error:
        sys.exit(f"Carga interrompida: {error}. Veja o diário privado antes de repetir (#221: lote sem resposta).")


def require_release_checkout(config, root=ROOT):
    """The approved packages must come from the very commit of the release in use (review L3)."""
    import subprocess
    head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip()
    if head != config["images"]["source_sha"]:
        raise RuntimeError("Checkout diferente do commit da release em uso; posicione-o no commit da release")
    if not config["rehearsal"] and not production.on_main(head, root):
        raise RuntimeError("Commit da release fora de origin/main")


if __name__ == "__main__":
    main()
