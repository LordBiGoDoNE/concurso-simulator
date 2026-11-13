#!/bin/bash
# Instala a borda nginx/TLS dos domínios reemsoftworks. Executar na conta administrativa,
# a partir desta pasta, em fases (cada uma roda nginx -t antes do reload):
#
#   sudo bash install-edge.sh http <IP-do-usuário>   # porta 80 + allowlist de pré-lançamento
#   sudo bash install-edge.sh admin <IP-do-usuário>  # allowlist da área administrativa (permanente)
#   sudo bash install-edge.sh cert                   # certificado Let's Encrypt (DNS já propagado)
#   sudo bash install-edge.sh https                  # vhosts HTTPS e redirecionamentos
#   sudo bash install-edge.sh open                   # abertura: público, sem o acesso antigo pelo IP
#
# central.conf nunca é alterado. concurso-public.conf (acesso antigo pelo IP) só tem o link
# em sites-enabled removido na fase open; o arquivo em sites-available é preservado.
set -euo pipefail
[[ $EUID -eq 0 ]] || { echo 'Execute com sudo.' >&2; exit 1; }

here=$(cd "$(dirname "$0")" && pwd)
available=/etc/nginx/sites-available
enabled=/etc/nginx/sites-enabled
snippet=/etc/nginx/snippets/concurso-simulator-access.conf
admin_snippet=/etc/nginx/snippets/concurso-simulator-admin-access.conf
legacy=concurso-public.conf
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
  admin)
    admin_ip=${2:-}
    [[ $admin_ip =~ ^[0-9a-fA-F:.]+(/[0-9]{1,3})?$ ]] || { echo 'Informe o IP público do administrador.' >&2; exit 1; }
    mkdir -p "$(dirname "$admin_snippet")"
    umask 022
    printf '# Área administrativa: somente estes IPs, também após a abertura.\nallow %s;\ndeny all;\n' "$admin_ip" > "$admin_snippet.new"
    mv "$admin_snippet.new" "$admin_snippet"
    echo "Allowlist administrativa gravada em $admin_snippet"
    if [[ -e $enabled/reemsoftworks-https.conf ]]; then reload_nginx; fi
    ;;
  https)
    [[ -s /etc/letsencrypt/live/reemsoftworks/fullchain.pem ]] || { echo 'Certificado ausente; execute a fase cert.' >&2; exit 1; }
    [[ -e $snippet ]] || { echo 'Allowlist ausente; execute a fase http.' >&2; exit 1; }
    [[ -e $admin_snippet ]] || { echo 'Allowlist administrativa ausente; execute a fase admin.' >&2; exit 1; }
    install_site reemsoftworks-https.conf
    reload_nginx
    ;;
  open)
    [[ -e $enabled/reemsoftworks-https.conf && -e $admin_snippet ]] || { echo 'Execute antes as fases https e admin.' >&2; exit 1; }
    previous=$(mktemp)
    cp -p "$snippet" "$previous"
    legacy_link=""
    if [[ -L $enabled/$legacy ]]; then legacy_link=$(readlink "$enabled/$legacy"); fi
    printf '# Aberto ao público em %s. A área administrativa segue restrita.\n' "$(date -u +%Y-%m-%dT%H:%MZ)" > "$snippet"
    [[ -z $legacy_link ]] || rm -f -- "$enabled/$legacy"
    install_site reemsoftworks-default.conf
    if ! nginx -t; then
      echo 'nginx -t falhou; restaurando estado anterior.' >&2
      rm -f -- "$enabled/reemsoftworks-default.conf"
      [[ -z $legacy_link ]] || ln -s "$legacy_link" "$enabled/$legacy"
      cp -p "$previous" "$snippet"
      exit 1
    fi
    systemctl reload nginx
    echo "Aberto ao público. Allowlist anterior preservada em $previous"
    ;;
  *)
    sed -n '2,13p' "$0"
    exit 1
    ;;
esac
