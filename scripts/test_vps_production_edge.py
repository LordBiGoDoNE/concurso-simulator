import re
import shutil
import subprocess
import unittest
from pathlib import Path

EDGE = Path(__file__).resolve().parents[1] / "infra/vps-production/edge"
HTTP = (EDGE / "reemsoftworks-http.conf").read_text(encoding="utf-8")
HTTPS = (EDGE / "reemsoftworks-https.conf").read_text(encoding="utf-8")
INSTALL = (EDGE / "install-edge.sh").read_text(encoding="utf-8")
NAMES = {"concurso-simulator.reemsoftworks.com", "reemsoftworks.com", "www.reemsoftworks.com",
         "reemsoftworks.com.br", "www.reemsoftworks.com.br", "concurso-simulator.reemsoftworks.com.br"}


def servers(text):
    return re.split(r"\nserver \{", text)[1:]


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

    def test_access_log_omits_query_strings(self):
        log_format = re.search(r"log_format reemsoftworks_path (.+?);", HTTPS, re.S).group(1)
        self.assertIn("$uri", log_format)
        for leaking in ("$request ", "$request_uri", "$args", "$query_string", "$http_cookie"):
            self.assertNotIn(leaking, log_format + " ")

    def test_com_br_redirects_to_same_host_on_com(self):
        self.assertIn(r"~^(?<prefix>.*)reemsoftworks\.com\.br$ ${prefix}reemsoftworks.com;", HTTPS)
        self.assertIn("return 301 https://$reemsoftworks_com_host$request_uri;", HTTPS)
        self.assertIn("return 302 https://concurso-simulator.reemsoftworks.com/;", HTTPS)


class InstallTests(unittest.TestCase):
    def test_never_touches_existing_sites(self):
        for protected in ("concurso-public.conf", "central.conf"):
            self.assertNotRegex(INSTALL, rf"(rm|unlink|mv)[^\n]*{re.escape(protected)}")
        self.assertNotRegex(INSTALL, r"\brm\b")

    def test_reload_always_validates_first(self):
        self.assertIn("nginx -t\n  systemctl reload nginx", INSTALL)
        bare = re.findall(r"^(?!.*nginx -t).*systemctl reload nginx", INSTALL, re.M)
        self.assertEqual(bare, ["  systemctl reload nginx"])  # only inside reload_nginx, right after nginx -t
        self.assertIn("--deploy-hook 'nginx -t && systemctl reload nginx'", INSTALL)

    def test_certificate_requires_dns_and_existing_snippet_is_kept(self):
        self.assertLess(INSTALL.index("check_dns ||"), INSTALL.index('"$certbot" certonly'))
        self.assertIn("if [[ ! -e $snippet ]]", INSTALL)
        self.assertEqual(set(re.findall(r"[a-z-]*\.?reemsoftworks\.com(?:\.br)?", INSTALL.split("names=(")[1].split(")")[0])), NAMES)

    @unittest.skipIf(shutil.which("bash") is None, "bash ausente")
    def test_installer_parses_and_refuses_without_root(self):
        subprocess.run(["bash", "-n"], input=INSTALL.encode(), check=True)
        if subprocess.run(["bash", "-c", "echo $EUID"], capture_output=True, text=True).stdout.strip() != "0":
            result = subprocess.run(["bash", "-s", "http", "203.0.113.1"], input=INSTALL.encode(), capture_output=True)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn(b"sudo", result.stderr)


if __name__ == "__main__":
    unittest.main()
