#!/bin/bash
# Auditoria somente leitura do host. Executar na conta administrativa:
#   sudo bash host-audit.sh
# Não altera nada. Não imprime chaves, senhas nem conteúdo de arquivos privados.
set -uo pipefail
[[ $EUID -eq 0 ]] || { echo 'Execute com sudo.' >&2; exit 1; }

section() { printf '\n== %s\n' "$1"; }

section 'sistema'
. /etc/os-release && echo "$PRETTY_NAME $(uname -m) kernel $(uname -r)"
uptime -p

section 'firewall (ufw)'
ufw status verbose 2>&1 | sed -n '1,40p'

section 'portas em escuta (TCP)'
ss -tlnH | awk '{print $4}' | sort -u

section 'sshd efetivo'
sshd -T 2>/dev/null | grep -Ei '^(port|permitrootlogin|passwordauthentication|kbdinteractiveauthentication|pubkeyauthentication|allowagentforwarding|allowtcpforwarding|x11forwarding) '

section 'IPv6 global'
ip -6 addr show scope global 2>/dev/null | awk '/inet6/ {print $2}' || true
ip -6 route show default 2>/dev/null | head -2 || true

section 'nginx'
nginx -t 2>&1 | tail -2
ls -1 /etc/nginx/sites-enabled

section 'certificados'
ls -1 /etc/letsencrypt/live 2>/dev/null | grep -v README || echo 'nenhum'
systemctl list-timers --no-pager 2>/dev/null | grep -i cert || true

section 'arquivos graváveis por todos em /opt'
find /opt -maxdepth 4 -perm -o+w ! -type l -printf '%m %u:%g %p\n' 2>/dev/null | sort | head -50

section 'quem executa /opt/vps-control como root'
grep -rls '/opt/vps-control' /etc/systemd /etc/cron* /var/spool/cron 2>/dev/null || echo 'nenhuma referência em systemd/cron'

section 'linger dos usuários de projeto'
ls -1 /var/lib/systemd/linger 2>/dev/null
