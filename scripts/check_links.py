"""Check that local HTML links and asset references point to existing files."""

from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit


ROOT = Path(__file__).resolve().parents[1]


class References(HTMLParser):
    def __init__(self):
        super().__init__()
        self.paths = []

    def handle_starttag(self, tag, attrs):
        for name, value in attrs:
            if name in {"href", "src"} and value:
                self.paths.append(value)


def main():
    errors = []
    checked = 0
    pages = sorted(ROOT.rglob("*.html"))
    for page in pages:
        parser = References()
        parser.feed(page.read_text(encoding="utf-8"))
        for reference in parser.paths:
            url = urlsplit(reference)
            if url.scheme or url.netloc or not url.path:
                continue
            path = unquote(url.path)
            target = ROOT / path.lstrip("/") if path.startswith("/") else page.parent / path
            checked += 1
            if not target.exists():
                errors.append(f"{page.relative_to(ROOT)}: {reference}")
    if errors:
        print("Referências locais não encontradas:")
        print("\n".join(errors))
        raise SystemExit(1)
    print(f"OK: {len(pages)} páginas HTML e {checked} referências locais verificadas.")


if __name__ == "__main__":
    main()
