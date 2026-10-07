"""Check that local HTML links and asset references point to existing files."""

from html.parser import HTMLParser
import argparse
from pathlib import Path
from urllib.parse import unquote, urlsplit


ROOT = Path(__file__).resolve().parents[1]
APPLICATION_DIRECTORIES = {"backend", "frontend", "infra", "openspec", ".opencode", ".git"}


class References(HTMLParser):
    def __init__(self):
        super().__init__()
        self.paths = []

    def handle_starttag(self, tag, attrs):
        for name, value in attrs:
            if name in {"href", "src"} and value:
                self.paths.append(value)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=ROOT)
    root = parser.parse_args().root.resolve()
    errors = []
    checked = 0
    # Application entrypoints are resolved by Vite, not the static publication.
    # Keep checking all study/preview pages, including when --root is a built site.
    pages = list(root.glob("*.html"))
    for directory in root.iterdir():
        if directory.is_dir() and directory.name not in APPLICATION_DIRECTORIES:
            pages.extend(directory.rglob("*.html"))
    pages.sort()
    for page in pages:
        parser = References()
        parser.feed(page.read_text(encoding="utf-8"))
        for reference in parser.paths:
            url = urlsplit(reference)
            if url.scheme or url.netloc or not url.path:
                continue
            path = unquote(url.path)
            target = root / path.lstrip("/") if path.startswith("/") else page.parent / path
            checked += 1
            if not target.exists():
                errors.append(f"{page.relative_to(root)}: {reference}")
    if errors:
        print("Referências locais não encontradas:")
        print("\n".join(errors))
        raise SystemExit(1)
    print(f"OK: {len(pages)} páginas HTML e {checked} referências locais verificadas.")


if __name__ == "__main__":
    main()
