# Grupo 1 — base do host: evidências

Executado em 2026-11-13. IP, chaves e senhas omitidos.

## Auditoria (usuário, `host-audit.sh`, conta root)

- Ubuntu 24.04.5 LTS, kernel 6.8.0-147, uptime de 13 dias antes do reboot.
- ufw ativo, entrada negada por padrão; permitidos 22, 80 e 443 em IPv4 e IPv6.
- Em escuta externa: 22, 80, 443 (IPv4/IPv6). Loopback: 18088, 18090, 18100, 65529, DNS local.
- sshd: chave apenas (`passwordauthentication no`, `kbdinteractiveauthentication no`),
  `permitrootlogin without-password`, `allowagentforwarding yes`, `x11forwarding no`.
- IPv6 global configurado com rota padrão.
- nginx válido; vhosts `central.conf` e `concurso-public.conf`; certificado `central-ip`.
- 21 itens graváveis por todos em `/opt/vps-control` (20 arquivos 0666 e o diretório
  `scripts` 0777), todos de root. Nenhuma referência em systemd/cron.
- Linger ligado para `central`, `concurso-dev` e `reem-dev`.

## Correção (usuário, `fix-vps-control-permissions.sh`)

- Permissões originais registradas em `/root/vps-control-permissions-20261113164550.txt`.
- Restantes graváveis por grupo/outros: 0. Após o reboot: diretórios 755, arquivos 644.

## Conta do projeto (assistente, `check-project-user.sh`)

- `concurso-dev` sem sudo; cgroup do usuário com `CPUQuotaPerSecUSec=2s`,
  `MemoryHigh=5 GiB`, `MemoryMax=6 GiB`.
- Docker rootless 29.9.2 / Compose 5.6.1 ativo e habilitado; linger ligado.

## Reboot controlado

Executado pelo usuário. Sem intervenção manual, após cerca de 1 minuto:
nginx ativo; Docker rootless ativo; `concurso-production.service` e
`concurso-app.service` ativos; os seis containers healthy; HTTPS do domínio 200 e
`/api/v1/status` UP; timers `central-certificate` e `reemsoftworks-certificate` agendados.

## Fora do escopo (registrado para decisão do usuário)

Login de root por chave e encaminhamento de agente continuam permitidos no sshd.
Restringi-los pode cortar o acesso atual; fica como endurecimento opcional.
