"""Production backup, restore and release switch. Restored sessions never reactivate an old cookie."""
import hashlib
import json
import os
import re
import subprocess
import uuid
from datetime import datetime, timezone
from pathlib import Path

from local_app import docker
from local_app_data import SESSIONS, TABLES_BY_SCHEMA, SUPPORTED_SCHEMA_VERSIONS, private_json
from local_docker import run as docker_run
import vps_production as production

KIND = "vps-production-backup"
BACKUPS = production.DATA_ROOT / "backups"


def postgres(state, config):
    container = production.compose(state, config, "ps", "-q", "postgres").decode().strip()
    if not container or not re.fullmatch(r"[0-9a-f]+", container):
        raise ValueError("PostgreSQL deste projeto deve estar iniciado")
    return container


def sql(state, config, statement):
    return docker("exec", postgres(state, config), "psql", "-X", "-U", "concurso", "-d", "concurso_simulator",
                  "-qAt", "-v", "ON_ERROR_STOP=1", "-c", statement)


def tables(state, config):
    return set(sql(state, config, "select tablename from pg_tables where schemaname='public' order by 1").decode().splitlines())


def schema_version(state, config):
    versions = sql(state, config, "select version from flyway_schema_history where success order by installed_rank").decode().splitlines()
    if versions not in [[str(i) for i in range(1, v + 1)] for v in SUPPORTED_SCHEMA_VERSIONS]:
        raise ValueError("Histórico de schema não suportado")
    return int(versions[-1])


def fingerprints(state, config):
    expected = TABLES_BY_SCHEMA[schema_version(state, config)]
    if tables(state, config) != expected:
        raise ValueError("Conjunto de tabelas divergente do schema suportado")
    result = {}
    for table in sorted(expected - SESSIONS):
        rows = sql(state, config, f"set timezone='UTC';select row_to_json(r)::text from {table} r order by 1")
        result[table] = {"rows": len(rows.splitlines()), "sha256": hashlib.sha256(rows).hexdigest()}
    return result


def backup(state, config, restart=True, directory=None):
    """Consistent dump with the API stopped; PostgreSQL keeps running. The API restarts unless told otherwise."""
    directory = Path(directory or BACKUPS)
    production.compose(state, config, "stop", "api")
    try:
        expected = fingerprints(state, config)
        schema = schema_version(state, config)
        version = sql(state, config, "show server_version").decode().strip()
        if not version.startswith("18."):
            raise ValueError("Servidor PostgreSQL inesperado")
        sessions = int(sql(state, config, "select count(*) from spring_session"))
        directory.mkdir(parents=True, exist_ok=True, mode=0o700)
        stem = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ-") + config["project"] + "-" + uuid.uuid4().hex[:8]
        dump = directory / (stem + ".dump")
        partial = dump.with_suffix(".partial")
        try:
            with os.fdopen(os.open(partial, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), "wb") as stream:
                docker_run(["exec", postgres(state, config), "pg_dump", "-U", "concurso", "-d", "concurso_simulator",
                            "--format=custom", "--no-owner", "--no-privileges", "--no-password"],
                           stdout=stream, stderr=subprocess.PIPE, check=True)
            if fingerprints(state, config) != expected:
                raise ValueError("Dados mudaram durante o backup; o arquivo parcial não é válido")
            # The archive must be readable by pg_restore, not only checksummed.
            docker("exec", "-i", postgres(state, config), "pg_restore", "--list", data=partial.read_bytes())
        except BaseException:
            partial.unlink(missing_ok=True)
            raise
        metadata = {"version": 1, "kind": KIND, "source_project": config["project"], "source_owner": config["owner"],
                    "source_sha": config["images"]["source_sha"], "api_image": config["images"]["images"]["api"]["id"],
                    "rehearsal": config["rehearsal"], "postgres_version": version, "schema_version": schema,
                    "sessions": sessions, "tables": expected,
                    "sha256": hashlib.sha256(partial.read_bytes()).hexdigest()}
        partial.replace(dump)
        private_json(Path(str(dump) + ".json"), metadata)
    finally:
        if restart:
            production.compose(state, config, "up", "-d", "--no-build", "--wait", "--wait-timeout", "180", "api")
    print("Backup privado criado: " + str(dump))
    print("Copie o .dump e o .dump.json para o PC; segredos e configuração não fazem parte do dump.")
    return dump, metadata


def verified_backup(file, backups=None):
    backups = Path(backups or BACKUPS).resolve()
    file = Path(file)
    metadata_file = Path(str(file) + ".json")
    if file.is_symlink() or metadata_file.is_symlink() or file.resolve().parent != backups:
        raise ValueError("Use um backup da ferramenta dentro do diretório de backups")
    metadata = json.loads(metadata_file.read_text(encoding="utf-8"))
    version = metadata.get("schema_version")
    expected = TABLES_BY_SCHEMA.get(version) if type(version) is int else None
    if (metadata.get("version") != 1 or metadata.get("kind") != KIND or expected is None
            or set(metadata.get("tables", {})) != expected - SESSIONS
            or not re.fullmatch(production.NAMESPACE, metadata.get("source_project", ""))
            or not re.fullmatch(r"[0-9a-f]{32}", metadata.get("source_owner", ""))
            or not re.fullmatch(r"[0-9a-f]{40}", metadata.get("source_sha", ""))
            or not re.fullmatch(r"sha256:[0-9a-f]{64}", metadata.get("api_image", ""))
            or not isinstance(metadata.get("sessions"), int) or metadata["sessions"] < 0
            or any(not isinstance(v.get("rows"), int) or v["rows"] < 0 or not re.fullmatch(r"[0-9a-f]{64}", v.get("sha256", ""))
                   for v in metadata["tables"].values())
            or metadata.get("sha256") != hashlib.sha256(file.read_bytes()).hexdigest()):
        raise ValueError("Backup, versão ou checksum inválidos")
    return metadata


def confirmation(metadata, config):
    return f'RESTORE {metadata["source_project"]} -> {config["project"]} {metadata["sha256"]}'


def restore(state, config, file, confirm, backups=None):
    metadata = verified_backup(file, backups)
    expected = confirmation(metadata, config)
    if confirm is None:
        print("Prévia somente; para escrever, repita com --confirm e o texto exato:")
        print(expected)
        return False
    if confirm != expected or config["owner"] == metadata["source_owner"]:
        raise ValueError("Confirmação divergente ou restauração sobre o próprio estado de origem")
    if config["images"]["images"]["api"]["id"] != metadata["api_image"]:
        raise ValueError("Restaure com a mesma imagem de API registrada no backup")
    marker = state / "recovery.json"
    if marker.exists() or production.compose(state, config, "ps", "-q", "api").strip():
        raise ValueError("API deve estar parada e sem recuperação incompleta anterior")
    production.compose(state, config, "up", "-d", "--no-build", "--wait", "postgres")
    if tables(state, config):
        raise ValueError("Destino já contém tabelas; nada foi restaurado")
    private_json(marker, {"status": "in-progress", "backup_sha256": metadata["sha256"]})
    docker("exec", "-i", postgres(state, config), "pg_restore", "-U", "concurso", "-d", "concurso_simulator",
           "--no-owner", "--no-privileges", "--exit-on-error", "--single-transaction", data=Path(file).read_bytes())
    if int(sql(state, config, "select count(*) from spring_session")) != metadata["sessions"]:
        raise ValueError("Sessões do dump divergentes")
    sql(state, config, "begin;delete from spring_session_attributes;delete from spring_session;commit")
    if (fingerprints(state, config) != metadata["tables"]
            or sql(state, config, "select count(*) from spring_session").strip() != b"0"
            or schema_version(state, config) != metadata["schema_version"]):
        raise ValueError("Restauração não validada; destino permanece bloqueado")
    marker.unlink()
    print("Destino restaurado e validado; sessões invalidadas. Use start e autentique novamente.")
    return True


def release(state, config, images_file):
    """Switch release: mandatory backup first; previous images kept for a schema-compatible rollback."""
    metadata = json.loads(Path(images_file).read_text(encoding="utf-8"))
    if metadata.get("origin") != production.ORIGIN:
        raise ValueError("Manifesto construído para outra origem")
    if not metadata.get("on_main") and not config["rehearsal"]:
        raise ValueError("Produção aceita somente releases de origin/main")
    production.validate_images(metadata)
    if metadata["base_images"].get("postgres") != config["images"]["base_images"].get("postgres"):
        raise ValueError("Troca da imagem do PostgreSQL exige restauração em alvo separado, não release")
    if metadata["images"]["api"]["id"] == config["images"]["images"]["api"]["id"] and \
            metadata["images"]["web"]["id"] == config["images"]["images"]["web"]["id"]:
        raise ValueError("Release já em uso")
    # Validate the new release's pinned compose before stopping anything (review N1).
    production.release_compose({"rehearsal": config["rehearsal"], "images": metadata})
    dump, saved = backup(state, config, restart=False)
    config["previous"] = {"images": config["images"], "backup": str(dump), "schema_version": saved["schema_version"]}
    config["images"] = metadata
    production.write_private_json(state / "state.json", config)
    production.compose(state, config, "up", "-d", "--no-build", "--wait", "--wait-timeout", "240")
    print(f"Release {metadata['source_sha'][:12]} em uso; backup prévio: {dump}")


def rollback(state, config):
    previous = config.get("previous")
    if not previous:
        raise ValueError("Não há release anterior registrada")
    production.release_compose({"rehearsal": config["rehearsal"], "images": previous["images"]})
    production.compose(state, config, "stop", "api", "web")
    if schema_version(state, config) != previous["schema_version"]:
        raise ValueError("Schema mudou desde o backup; restaure o backup prévio em um alvo de recuperação")
    config["images"], config["previous"] = previous["images"], None
    production.write_private_json(state / "state.json", config)
    production.compose(state, config, "up", "-d", "--no-build", "--wait", "--wait-timeout", "180")
    print(f"Release anterior {config['images']['source_sha'][:12]} restabelecida; schema inalterado.")


def promote(state, config):
    """Leave rehearsal mode for good: only a main release with a pinned compose can become production (review N2)."""
    if not config["rehearsal"]:
        raise ValueError("Estado já é de produção")
    if config["project"] != production.PROJECT:
        raise ValueError("Somente o projeto de produção pode ser promovido")
    images = config["images"]
    if not images.get("on_main") or not production.on_main(images["source_sha"]):
        raise ValueError("Release em uso não está em origin/main")
    if images.get("compose") is None:
        raise ValueError("Release em uso sem compose fixado")
    production.release_compose({"rehearsal": False, "images": images})
    previous = config.get("previous")
    if previous and (not previous["images"].get("on_main") or previous["images"].get("compose") is None):
        config["previous"] = None  # a rehearsal release must never become a production rollback target
    config["rehearsal"] = False
    production.write_private_json(state / "state.json", config)
    print(f"Estado promovido a produção com a release {images['source_sha'][:12]}; ensaios não são mais aceitos.")
