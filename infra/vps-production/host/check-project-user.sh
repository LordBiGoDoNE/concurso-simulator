#!/bin/bash
# Verificação somente leitura na conta concurso-dev (usada pelo assistente via SSH).
# Não imprime segredos, conteúdo de estado nem logs da aplicação.
set -uo pipefail
[[ $(id -un) == concurso-dev ]] || { echo 'Use a conta concurso-dev.' >&2; exit 1; }
export DOCKER_CONTEXT=rootless

section() { printf '\n== %s\n' "$1"; }

section 'identidade'
id -un; id -Gn
sudo -n true 2>/dev/null && echo 'ALERTA: sudo sem senha' || echo 'sem sudo (esperado)'

section 'recursos'
nproc; free -h | sed -n 2p; df -h / | tail -1
systemctl show "user-$(id -u).slice" -p MemoryMax -p MemoryHigh -p CPUQuotaPerSecUSec 2>/dev/null || true

section 'docker rootless'
docker version --format 'server {{.Server.Version}} os {{.Server.Os}}' 2>&1
docker compose version 2>&1 | head -1
docker info --format 'rootless={{range .SecurityOptions}}{{.}} {{end}}' 2>&1 | grep -o 'name=rootless' || echo 'ALERTA: daemon não é rootless'
systemctl --user is-enabled docker 2>&1; systemctl --user is-active docker 2>&1

section 'containers'
docker ps -a --format '{{.Names}} {{.Status}} {{.Ports}}'

section 'serviços de usuário do concurso'
systemctl --user list-unit-files --no-pager 'concurso*' 2>/dev/null | grep -E '\.service' || echo 'nenhum'
for unit in $(systemctl --user list-unit-files --no-legend 'concurso*' 2>/dev/null | awk '{print $1}'); do
  printf '%s: %s/%s\n' "$unit" "$(systemctl --user is-enabled "$unit")" "$(systemctl --user is-active "$unit")"
done

section 'linger'
loginctl show-user "$(id -un)" -p Linger 2>/dev/null
