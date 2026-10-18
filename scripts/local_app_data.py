"""Private local dump/restore. Restored sessions never reactivate an old cookie."""
import hashlib
import json
import os
import re
import subprocess
import uuid
from datetime import datetime, timezone
from pathlib import Path
from local_app import ROOT, compose, docker, owned
from local_docker import run as docker_run

SESSIONS = {"spring_session", "spring_session_attributes"}
TABLES = {"application_marker", "app_user", "external_identity", "spring_session", "spring_session_attributes",
          "qb_discipline", "qb_role", "qb_topic", "qb_question", "qb_revision", "qb_alternative", "qb_revision_role",
          "qb_reference", "qb_decision", "me_attempt", "me_item", "me_command", "pq_family", "pq_version",
          "pq_decision", "pq_instance", "flyway_schema_history"}


def postgres(path, config):
    container = compose(path, config, "ps", "-q", "postgres").decode().strip()
    if not container or not re.fullmatch(r"[0-9a-f]+", container):
        raise ValueError("PostgreSQL deste projeto deve estar iniciado")
    return container


def sql(path, config, statement):
    return docker("exec", postgres(path, config), "psql", "-X", "-U", "concurso", "-d", "concurso_simulator",
                  "-qAt", "-v", "ON_ERROR_STOP=1", "-c", statement)


def tables(path, config):
    return set(sql(path, config, "select tablename from pg_tables where schemaname='public' order by 1").decode().splitlines())


def fingerprints(path, config):
    if tables(path, config) != TABLES:
        raise ValueError("Conjunto de tabelas divergente; não é o schema V10 esperado")
    result = {}
    for table in sorted(TABLES - SESSIONS):
        rows = sql(path, config, f"set timezone='UTC';select row_to_json(r)::text from {table} r order by 1")
        result[table] = {"rows": len(rows.splitlines()), "sha256": hashlib.sha256(rows).hexdigest()}
    return result


def private_json(path, value):
    with os.fdopen(os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), "w", encoding="utf-8") as stream:
        json.dump(value, stream, indent=2, ensure_ascii=False)
        stream.write("\n")


def backup(path, config):
    owned(config)
    # Quiesce this application's writers; other operator writes must remain paused too.
    compose(path, config, "stop", "api")
    expected = fingerprints(path, config)
    if sql(path, config, "select count(*) from flyway_schema_history where success").strip() != b"10":
        raise ValueError("Backup exige schema V10 válido")
    version = sql(path, config, "show server_version").decode().strip()
    if not version.startswith("18.6"):
        raise ValueError("Cliente/servidor devem ser PostgreSQL18.6")
    session_count = int(sql(path, config, "select count(*) from spring_session"))
    directory = path / "backups"
    directory.mkdir(exist_ok=True, mode=0o700)
    stem = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ-") + uuid.uuid4().hex[:12]
    dump = directory / (stem + ".dump")
    partial = dump.with_suffix(".partial")
    with os.fdopen(os.open(partial, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), "wb") as stream:
        docker_run(["exec", postgres(path, config), "pg_dump", "-U", "concurso", "-d", "concurso_simulator",
                        "--format=custom", "--no-owner", "--no-privileges", "--no-password"],
                       stdout=stream, stderr=subprocess.PIPE, check=True)
    if fingerprints(path, config) != expected:
        raise ValueError("Dados mudaram durante backup; mantenha todos os escritores parados")
    metadata = {"version": 1, "kind": "local-app-backup", "source_project": config["project"],
                "source_owner": config["owner"], "source_sha": config["images"]["source_sha"],
                "api_image": config["images"]["images"]["api"]["id"], "postgres_version": version,
                "sha256": hashlib.sha256(partial.read_bytes()).hexdigest(), "tables": expected,
                "sessions": session_count, "schema_version": 10}
    partial.replace(dump)
    private_json(Path(str(dump) + ".json"), metadata)
    print("Backup privado criado: " + str(dump))
    print("API permanece parada; use start para retomar. Credenciais/configuração e papéis do cluster não fazem parte do dump.")
    return dump


def verified_backup(file):
    file = Path(file)
    if file.is_symlink() or not file.resolve().is_relative_to(ROOT.resolve()) or "backups" not in file.parts:
        raise ValueError("Use um backup local confiável da ferramenta dentro do projeto")
    metadata_file = Path(str(file) + ".json")
    if metadata_file.is_symlink():
        raise ValueError("Metadados não podem ser link")
    metadata = json.loads(metadata_file.read_text(encoding="utf-8"))
    if (metadata.get("version") != 1 or metadata.get("kind") != "local-app-backup" or metadata.get("schema_version") != 10
            or set(metadata["tables"]) != TABLES - SESSIONS
            or not re.fullmatch(r"concurso-simulator-app-local(?:-[a-z0-9]{1,24})?", metadata["source_project"])
            or not re.fullmatch(r"[0-9a-f]{32}", metadata["source_owner"])
            or not re.fullmatch(r"[0-9a-f]{40}", metadata["source_sha"])
            or not re.fullmatch(r"sha256:[0-9a-f]{64}", metadata["api_image"])
            or not isinstance(metadata["sessions"], int) or metadata["sessions"] < 0
            or any(not isinstance(value["rows"], int) or value["rows"] < 0
                   or not re.fullmatch(r"[0-9a-f]{64}", value["sha256"]) for value in metadata["tables"].values())
            or not metadata["postgres_version"].startswith("18.6")
            or metadata["sha256"] != hashlib.sha256(file.read_bytes()).hexdigest()):
        raise ValueError("Backup/versão/checksum inválidos")
    return metadata


def confirmation(metadata, config):
    return f'RESTORE {metadata["source_project"]} -> {config["project"]} {metadata["sha256"]}'


def restore_backup(path, config, file, confirm):
    owned(config)
    metadata = verified_backup(file)
    expected = confirmation(metadata, config)
    if confirm is None:
        print("Prévia somente; antes de escrever informe --confirm com o texto exato:")
        print(expected)
        return False
    if confirm != expected or config["project"] == metadata["source_project"] or config["owner"] == metadata["source_owner"]:
        raise ValueError("Confirmação/destino divergente; restauração in-place recusada")
    if config["images"]["images"]["api"]["id"] != metadata["api_image"]:
        raise ValueError("Restaure usando a mesma imagem compatível identificada no backup")
    marker = path / "recovery.json"
    if marker.exists() or compose(path, config, "ps", "-q", "api").strip():
        raise ValueError("API deve estar parada e destino sem recuperação anterior incompleta")
    compose(path, config, "up", "-d", "--no-build", "--wait", "postgres")
    if tables(path, config):
        raise ValueError("Destino já contém tabelas; nenhuma restauração efetuada")
    private_json(marker, {"status": "in-progress", "backup_sha256": metadata["sha256"]})
    # Atomic restore; on any later validation failure, marker keeps start blocked.
    docker("exec", "-i", postgres(path, config), "pg_restore", "-U", "concurso", "-d", "concurso_simulator",
           "--no-owner", "--no-privileges", "--exit-on-error", "--single-transaction", data=Path(file).read_bytes())
    if int(sql(path, config, "select count(*) from spring_session")) != metadata["sessions"]:
        raise ValueError("Sessões do dump divergentes")
    sql(path, config, "begin;delete from spring_session_attributes;delete from spring_session;commit")
    if (fingerprints(path, config) != metadata["tables"]
            or sql(path, config, "select count(*) from spring_session").strip() != b"0"
            or sql(path, config, "select count(*) from flyway_schema_history where success").strip() != b"10"
            or sql(path, config, "select count(*) from pg_indexes where schemaname='public' and indexname='me_completed_subject_window_idx'").strip() != b"1"):
        raise ValueError("Recuperação não validada; destino permanece bloqueado")
    marker.unlink()
    print("Destino restaurado e validado; sessões invalidadas. Use start e uma nova autenticação.")
    return True
