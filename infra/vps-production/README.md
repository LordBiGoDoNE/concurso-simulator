# Produção na VPS Hostinger

Spec `vps-staging-delivery` (#156). Destino:
`https://concurso-simulator.reemsoftworks.com`. IP, chaves e segredos nunca
entram neste repositório; use `<VPS>` como marcador.

## Papéis

| Conta | Uso | Privilégio |
| --- | --- | --- |
| `concurso-dev` | runtime da aplicação (Docker rootless), operador, builds | sem sudo |
| conta administrativa do usuário | nginx, certbot, ufw, permissões do host, reboot | sudo com senha |

O assistente acessa somente `concurso-dev`, a partir do PC do usuário:

```sh
ssh -a -i ~/.ssh/hostingerVPS -o IdentitiesOnly=yes -o ForwardAgent=no concurso-dev@<VPS>
```

A chave do host é verificada (`StrictHostKeyChecking`). Se ela mudar, pare e
confira a impressão digital no console da Hostinger
(`ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub`) antes de aceitar.

Passos com sudo são executados pelo usuário na conta administrativa. Os scripts
abaixo são revisáveis e não pedem nem imprimem segredos.

## Grupo 1 — base do host

Copie a pasta `host/` para a conta administrativa e execute:

```sh
sudo bash host-audit.sh                    # somente leitura
sudo bash fix-vps-control-permissions.sh   # remove escrita de grupo/outros em /opt/vps-control
```

`fix-vps-control-permissions.sh` grava as permissões originais em
`/root/vps-control-permissions-<data>.txt` antes de alterar e não apaga arquivos.
Para reverter um item: `chmod <modo> <caminho>` com os valores do registro.

Na conta `concurso-dev` (o assistente executa):

```sh
bash host/check-project-user.sh
```

Esperado: sem sudo, Docker rootless ativo e habilitado, linger ligado e
serviços do concurso habilitados.

Firewall esperado: entrada somente em 22, 80 e 443 (IPv4/IPv6). As portas
internas (18088, 18090, 18100, 5432) ficam em loopback ou em redes Docker.

### Reboot controlado

Antes: confirmar que não há operação do operador em andamento. O usuário
executa `sudo reboot` na conta administrativa. Depois, o assistente confere:
`check-project-user.sh`, containers saudáveis, `curl` local na porta da web e
nginx respondendo. Nenhum serviço deve exigir intervenção manual.

## Grupo 2 — runtime de produção

Checkout dedicado do operador em `~/projects/production` (clone do GitHub, sem
credenciais). O checkout antigo `~/projects/repository` e o projeto
`concurso-simulator-app-local-vps` não são tocados.

| Item | Valor |
| --- | --- |
| Projeto Docker | `concurso-simulator-prod` |
| Web (loopback) | `127.0.0.1:18090` |
| Estado e segredos | `~/.local/share/concurso-simulator/production/` (0700) |
| Manifestos de release | `~/.local/share/concurso-simulator/releases/<sha12>.json` |
| Perfil Spring | `production` (sem `local`: cookie Secure, login só HTTPS) |

```sh
export DOCKER_CONTEXT=rootless
cd ~/projects/production && git fetch origin
python3 scripts/vps_production.py build --revision <commit>
python3 scripts/vps_production.py init --images ~/.local/share/concurso-simulator/releases/<sha12>.json
python3 scripts/vps_production.py start | status | stop
```

- `build` usa o mesmo contexto filtrado do operador local (`git archive` +
  allowlist), acrescentando apenas `infra/vps-production/{web.Dockerfile,nginx.conf}`.
  O manifesto registra se o commit está em `origin/main`.
- `init` recusa estado existente, origem diferente e commit fora da main.
  `--rehearsal` permite ensaiar uma branch de tarefa; o estado fica marcado como ensaio.
- Segredos gerados em arquivos: senha do PostgreSQL, senha inicial do admin
  (`secrets/admin_password`, nunca impressa) e configuração Google desligada.
  Leia a senha do admin diretamente na VPS e troque-a após o primeiro login.
- `start` exige prontidão real dos três serviços (`--wait`). Containers usam
  `restart: unless-stopped`; o serviço de usuário `systemd/concurso-production.service`
  sobe a aplicação no boot.
- O nginx interno aceita somente o Host canônico, limpa `Forwarded`/`X-Forwarded-*`
  e libera 20 MB/120 s apenas em `/api/v1/admin/`. TLS e HSTS ficam no nginx do host.

Atualização de release, backup e restauração pertencem ao grupo 6.

## Grupo 6 — backup, atualização e recuperação

```sh
python3 scripts/vps_production.py backup        # API parada por segundos; dump + manifesto em ~/.local/share/concurso-simulator/backups/
```

Copie para o PC (no PC):

```sh
scp -i ~/.ssh/hostingerVPS concurso-dev@<VPS>:.local/share/concurso-simulator/backups/<arquivo>.dump* <pasta-privada>/
```

Restauração sempre em alvo separado e vazio, com a mesma imagem de API do backup:

```sh
R=~/.local/share/concurso-simulator/recoveries/<nome>
python3 scripts/vps_production.py --state $R init --images <manifesto-da-release> --recovery <nome> --port 18091
python3 scripts/vps_production.py --state $R restore --backup <dump>                 # prévia + texto de confirmação
python3 scripts/vps_production.py --state $R restore --backup <dump> --confirm "<texto>"
python3 scripts/vps_production.py --state $R start
```

Troca de release (somente commits de origin/main; backup automático antes):

```sh
python3 scripts/vps_production.py release --images ~/.local/share/concurso-simulator/releases/<sha12>.json
python3 scripts/vps_production.py rollback   # só se o schema não mudou; senão, restaure o backup prévio
```

Backups, estado e segredos nunca vão para Git, CI ou issues. Volumes não são removidos pela ferramenta.
