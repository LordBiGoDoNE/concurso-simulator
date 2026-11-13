#!/bin/bash
# Instala a borda nginx/TLS dos domínios reemsoftworks. Executar na conta administrativa,
# a partir desta pasta, em três fases (cada uma é idempotente e roda nginx -t antes do reload):
#
#   sudo bash install-edge.sh http <IP-do-usuário>   # porta 80 + allowlist de pré-lançamento
#   sudo bash install-edge.sh cert                   # certificado Let's Encrypt (DNS já propagado)
#   sudo bash install-edge.sh https                  # vhosts HTTPS e redirecionamentos
#
# Não remove nem altera concurso-public.conf (acesso antigo pelo IP) nem central.conf.
set -euo pipefail
[[ $EUID -eq 0 ]] || { echo 'Execute com sudo.' >&2; exit 1; }

here=$(cd "$(dirname "$0")" && pwd)
available=/etc/nginx/sites-available
enabled=/etc/nginx/sites-enabled
snippet=/etc/nginx/snippets/concurso-simulator-access.conf
webroot=/var/lib/central-acme
certbot=/opt/central-certbot/bin/certbot
names=(concurso-simulator.reemsoftworks.com reemsoftworks.com www.reemsoftworks.com
       reemsoftworks.com.br www.reemsoftworks.com.br concurso-simulator.reemsoftworks.com.br)

reload_nginx() {
  nginx -t
  systemctl reload nginx
}

install_site() {
  local name=$1
  install -m 0644 "$here/$name" "$available/$name"
  ln -sfn "$available/$name" "$enabled/$name"
}

public_addresses() {
  ip -o addr show scope global | awk '{split($4, a, "/"); print a[1]}'
}

check_dns() {
  local ours name resolved missing=0
  ours=$(public_addresses)
  for name in "${names[@]}"; do
    resolved=$(getent ahosts "$name" | awk '{print $1}' | sort -u)
    if [[ -z $resolved ]] || grep -qvxF -f <(printf '%s\n' $ours) <<<"$resolved"; then
      echo "DNS de $name ainda não aponta só para esta VPS: ${resolved:-sem resposta}" >&2
      missing=1
    fi
  done
  return $missing
}

case ${1:-} in
  http)
    user_ip=${2:-}
    [[ $user_ip =~ ^[0-9a-fA-F:.]+(/[0-9]{1,3})?$ ]] || { echo 'Informe o IP público do usuário para a allowlist.' >&2; exit 1; }
    mkdir -p "$(dirname "$snippet")"
    if [[ ! -e $snippet ]]; then
      umask 022
      printf '# Pré-lançamento: somente o IP do usuário. Esvaziar na abertura (grupo 7).\nallow %s;\ndeny all;\n' "$user_ip" > "$snippet"
      echo "Allowlist criada em $snippet"
    else
      echo "Allowlist já existe; mantida sem alteração: $snippet"
    fi
    [[ -d $webroot ]] || { echo "Webroot ACME $webroot ausente." >&2; exit 1; }
    install_site reemsoftworks-http.conf
    reload_nginx
    ;;
  cert)
    [[ -x $certbot ]] || { echo "certbot não encontrado em $certbot" >&2; exit 1; }
    check_dns || { echo 'Aguarde a propagação do DNS e repita.' >&2; exit 1; }
    args=()
    for name in "${names[@]}"; do args+=(-d "$name"); done
    "$certbot" certonly --webroot -w "$webroot" --cert-name reemsoftworks "${args[@]}" \
      --deploy-hook 'nginx -t && systemctl reload nginx'
    install -m 0644 "$here/reemsoftworks-certificate.service" /etc/systemd/system/
    install -m 0644 "$here/reemsoftworks-certificate.timer" /etc/systemd/system/
    systemctl daemon-reload
    systemctl enable --now reemsoftworks-certificate.timer
    ;;
  https)
    [[ -s /etc/letsencrypt/live/reemsoftworks/fullchain.pem ]] || { echo 'Certificado ausente; execute a fase cert.' >&2; exit 1; }
    [[ -e $snippet ]] || { echo 'Allowlist ausente; execute a fase http.' >&2; exit 1; }
    install_site reemsoftworks-https.conf
    reload_nginx
    ;;
  *)
    sed -n '2,9p' "$0"
    exit 1
    ;;
esac
