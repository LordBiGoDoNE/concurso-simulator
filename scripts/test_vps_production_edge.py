import re
import shutil
import subprocess
import unittest
from pathlib import Path

EDGE = Path(__file__).resolve().parents[1] / "infra/vps-production/edge"
HTTP = (EDGE / "reemsoftworks-http.conf").read_text(encoding="utf-8")
HTTPS = (EDGE / "reemsoftworks-https.conf").read_text(encoding="utf-8")
DEFAULT = (EDGE / "reemsoftworks-default.conf").read_text(encoding="utf-8")
INSTALL = (EDGE / "install-edge.sh").read_text(encoding="utf-8")
NAMES = {"concurso-simulator.reemsoftworks.com", "reemsoftworks.com", "www.reemsoftworks.com",
         "reemsoftworks.com.br", "www.reemsoftworks.com.br", "concurso-simulator.reemsoftworks.com.br"}


def servers(text):
    return re.split(r"\nserver \{", text)[1:]


def block(text, opening):
    """Body of the first `opening { ... }` block (no nested braces in these locations)."""
    start = text.index(opening + " {") + len(opening) + 2
    return text[start:text.index("}", start)]


class HttpTests(unittest.TestCase):
    def test_port_80_covers_every_name_and_redirects_except_acme(self):
        names = set(re.search(r"server_name ([^;]+);", HTTP).group(1).split())
        self.assertEqual(names, NAMES)
        self.assertIn("location ^~ /.well-known/acme-challenge/", HTTP)
        self.assertIn("return 301 https://$host$request_uri;", HTTP)
        self.assertNotIn("proxy_pass", HTTP)
        self.assertNotIn("default_server", HTTP)


class HttpsTests(unittest.TestCase):
    def test_every_name_is_served_with_hsts(self):
        served = set()
        for server in servers(HTTPS):
            served |= set(re.search(r"server_name ([^;]+);", server).group(1).split())
            self.assertIn('Strict-Transport-Security "max-age=31536000" always', server)
            self.assertIn("listen [::]:443 ssl;", server)
        self.assertEqual(served, NAMES)
        self.assertNotIn("default_server", HTTPS)

    def test_application_proxy_is_loopback_canonical_and_restricted(self):
        app = next(s for s in servers(HTTPS) if "proxy_pass" in s)
        self.assertIn("server_name concurso-simulator.reemsoftworks.com;", app)
        self.assertIn("proxy_pass http://127.0.0.1:18090;", app)
        self.assertIn("proxy_set_header Host concurso-simulator.reemsoftworks.com;", app)
        for header in ("Forwarded", "X-Forwarded-For", "X-Forwarded-Host", "X-Forwarded-Proto"):
            self.assertIn(f'proxy_set_header {header} "";', app)
        self.assertIn("include /etc/nginx/snippets/concurso-simulator-access.conf;", app)

    def test_admin_area_is_restricted_and_login_rate_limited_by_real_client_ip(self):
        app = next(s for s in servers(HTTPS) if "proxy_pass" in s)
        admin = block(app, "location ^~ /api/v1/admin/")
        login = block(app, "location = /api/v1/admin/auth/login")
        for body in (admin, login):
            self.assertIn("include /etc/nginx/snippets/concurso-simulator-admin-access.conf;", body)
            self.assertIn("proxy_pass http://127.0.0.1:18090;", body)
        self.assertIn("limit_req zone=concurso_admin_login", login)
        self.assertIn("limit_req_zone $binary_remote_addr zone=concurso_admin_login", HTTPS)

    def test_access_log_omits_query_strings(self):
        log_format = re.search(r"log_format reemsoftworks_path (.+?);", HTTPS, re.S).group(1)
        self.assertIn("$uri", log_format)
        for leaking in ("$request ", "$request_uri", "$args", "$query_string", "$http_cookie"):
            self.assertNotIn(leaking, log_format + " ")

    def test_com_br_redirects_to_same_host_on_com(self):
        self.assertIn(r"~^(?<prefix>.*)reemsoftworks\.com\.br$ ${prefix}reemsoftworks.com;", HTTPS)
        self.assertIn("return 301 https://$reemsoftworks_com_host$request_uri;", HTTPS)
        self.assertIn("return 302 https://concurso-simulator.reemsoftworks.com/;", HTTPS)


class DefaultServerTests(unittest.TestCase):
    def test_port_80_default_closes_unknown_hosts_but_keeps_acme(self):
        self.assertIn("listen 80 default_server;", DEFAULT)
        self.assertIn("listen [::]:80 default_server;", DEFAULT)
        self.assertIn("return 444;", DEFAULT)
        self.assertIn("root /var/lib/central-acme;", DEFAULT)
        self.assertNotIn("proxy_pass", DEFAULT)

    def test_port_443_default_is_left_to_central_by_documented_decision(self):
        self.assertNotIn("listen 443", DEFAULT)
        self.assertIsNone(re.search(r"^\s*ssl_reject_handshake", DEFAULT, re.M))
        self.assertIn("rejeitar handshakes sem SNI o derrubaria", DEFAULT)


class InstallTests(unittest.TestCase):
    def test_never_touches_central_or_available_sites(self):
        body = INSTALL.split("set -euo pipefail", 1)[1]
        self.assertNotIn("central.conf", body)
        removals = set(re.findall(r"rm -f -- (\S+)", INSTALL))
        self.assertEqual(removals, {'"$enabled/$legacy"', '"$enabled/reemsoftworks-default.conf"'})
        self.assertIsNone(re.search(r"rm[^\n]*\$available", INSTALL))

    def test_open_restores_previous_state_when_nginx_rejects(self):
        body = INSTALL.split("  open)", 1)[1].split(";;", 1)[0]
        self.assertLess(body.index('cp -p "$snippet" "$previous"'), body.index('> "$snippet"'))
        self.assertLess(body.index("if ! nginx -t"), body.index("systemctl reload nginx"))
        self.assertIn('ln -s "$legacy_link" "$enabled/$legacy"', body)

    def test_reload_always_validates_first(self):
        self.assertIn("nginx -t\n  systemctl reload nginx", INSTALL)
        bare = re.findall(r"^(?!.*nginx -t).*systemctl reload nginx", INSTALL, re.M)
        # reload_nginx() and the open phase, both only after a successful nginx -t
        self.assertEqual(bare, ["  systemctl reload nginx", "    systemctl reload nginx"])
        self.assertIn("--deploy-hook 'nginx -t && systemctl reload nginx'", INSTALL)

    def test_certificate_requires_dns_and_existing_snippet_is_kept(self):
        self.assertLess(INSTALL.index("check_dns ||"), INSTALL.index('"$certbot" certonly'))
        self.assertIn("if [[ ! -e $snippet ]]", INSTALL)
        listed = INSTALL.split("names=(")[1].split(")")[0]
        self.assertEqual(set(re.findall(r"[a-z-]*\.?reemsoftworks\.com(?:\.br)?", listed)), NAMES)

    @unittest.skipIf(shutil.which("bash") is None, "bash ausente")
    def test_installer_parses_and_refuses_without_root(self):
        subprocess.run(["bash", "-n"], input=INSTALL.encode(), check=True)
        if subprocess.run(["bash", "-c", "echo $EUID"], capture_output=True, text=True).stdout.strip() != "0":
            result = subprocess.run(["bash", "-s", "http", "203.0.113.1"], input=INSTALL.encode(), capture_output=True)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn(b"sudo", result.stderr)


if __name__ == "__main__":
    unittest.main()
