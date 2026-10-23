"""Assemble approved production and public branch previews, without a framework."""

import argparse
import hashlib
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path


def copy_site(source, target, tools_source=None):
    tools_source = tools_source if tools_source is not None else source
    target.mkdir(parents=True, exist_ok=True)
    files = [source / "index.html", source / ".nojekyll"]
    files += sorted(source.glob("00_*.html"))
    files += [source / "manifesto_v3.json", source / "catalogo_fontes_questoes.csv"]
    directories = [source / "assets"]
    directories += sorted(p for p in source.iterdir() if p.is_dir() and re.match(r"^0[1-5]_", p.name))
    for file in files:
        shutil.copy2(file, target / file.name)
    for directory in directories:
        shutil.copytree(directory, target / directory.name, dirs_exist_ok=True)
    enrichment = tools_source / "scripts" / "enrich_math.py"
    if enrichment.exists():
        subprocess.run([sys.executable, str(enrichment.resolve()), "--source", str(source.resolve()), "--output", str(target.resolve())], check=True)
    compact_sources = tools_source / "scripts" / "compact_sources.py"
    if compact_sources.exists():
        subprocess.run([sys.executable, str(compact_sources.resolve()), "--site", str(target.resolve())], check=True)
    if enrichment.exists():
        subprocess.run([sys.executable, str((tools_source / "scripts/check_math.py").resolve()), "--source", str(source.resolve()), "--site", str(target.resolve())], check=True)
        subprocess.run([sys.executable, str((tools_source / "scripts/check_links.py").resolve()), "--root", str(target.resolve())], check=True)


def preview_slug(branch):
    name = re.sub(r"[^a-z0-9-]+", "-", branch.lower()).strip("-")[:70] or "branch"
    digest = hashlib.sha256(branch.encode()).hexdigest()[:8]
    return f"{name}-{digest}"


def build_site(root, stored, output, branch, production_branch="main", production_source=None, tools_source=None):
    if production_branch not in ("main", "stable"):
        raise ValueError("Unsupported production branch")
    if branch == "pages-store" or (production_branch == "stable" and branch == "main"):
        raise ValueError("This branch cannot be published")
    stored_previews = stored / "previews"
    if stored_previews.exists():
        shutil.copytree(stored_previews, output / "previews")
    production = root if branch == production_branch else (production_source or root / "approved")
    copy_site(production, output, tools_source)
    if branch != production_branch:
        slug = preview_slug(branch)
        target = output / "previews" / slug
        if target.exists():
            shutil.rmtree(target)
        copy_site(root, target, tools_source)
        notice = '<aside style="padding:12px;background:#fff7ed;border-bottom:2px solid #f59e0b;text-align:center">Versão de teste — alterações ainda não aprovadas. <a href="../../">Voltar ao site oficial</a></aside>'
        for page in target.rglob("*.html"):
            banner = notice if page.parent == target else notice.replace('href="../../"', 'href="../../../"')
            page.write_text(page.read_text(encoding="utf-8").replace("<body>", "<body>" + banner, 1), encoding="utf-8")
        path = f"previews/{slug}/"
        print(f"Preview: {path}")
        summary = os.environ.get("GITHUB_STEP_SUMMARY")
        if summary:
            with open(summary, "a", encoding="utf-8") as file:
                file.write(f"## Preview\n\nhttps://lordbigodone.github.io/concurso-simulator/{path}\n")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--branch", required=True)
    # Default retained for local/CI callers assembling a package, not deploying it.
    parser.add_argument("--production-branch", choices=("main", "stable"), default="main")
    parser.add_argument("--source", type=Path)
    parser.add_argument("--production-source", type=Path)
    parser.add_argument("--stored", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    publisher = Path(__file__).resolve().parents[1]
    build_site(args.source or publisher, args.stored, args.output,
               args.branch, args.production_branch, args.production_source, publisher)


if __name__ == "__main__":
    main()
