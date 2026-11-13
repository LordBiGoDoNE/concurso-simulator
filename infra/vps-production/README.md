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

Se o host reiniciar durante `backup`, `release` ou `restore`, fica um
`operation.lock` no estado e o serviço `concurso-production` não sobe (a API
parada por `compose stop` não volta sozinha). A ferramenta nunca quebra o lock:
confira que nenhum processo `vps_production.py` está vivo (`pgrep -af vps_production`),
que não há `.partial` em `backups/` e o estado dos containers; só então remova o
lock deliberadamente e rode `start`.

## Grupo 2 — runtime de produção

Checkout dedicado do operador em `~/projects/production` (clone do GitHub, sem
credenciais). O checkout antigo `~/projects/repository` e o projeto
`concurso-simulator-app-local-vps` não são tocados.

| Item | Valor |
| --- | --- |
| Projeto Docker | `concurso-simulator-prod` |
| Web (loopback) | `127.0.0.1:18090` |
| Estado e segredos | `~/.local/share/concurso-simulator/production/` (0700) |
| Manifestos de release | `~/.local/share/concurso-simulator/releases/<sha12>.json` e `<sha12>.compose.yaml` |
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
  O manifesto registra se o commit está em `origin/main` e fixa por SHA-256 o
  `compose.yaml` daquele commit; o runtime sempre usa essa cópia, nunca o checkout.
- `init` recusa estado existente, origem diferente e commit fora da main.
  `--rehearsal` permite ensaiar uma branch de tarefa; o estado fica marcado como ensaio.
- Segredos gerados em arquivos: senha do PostgreSQL, senha inicial do admin
  (`secrets/admin_password`, nunca impressa) e configuração Google desligada.
  A senha do admin vem desse arquivo e não é temporária. Se for trocada pela
  interface, atualize o arquivo, porque `vps_question_load.py` o usa para entrar.
- `start` exige prontidão real dos três serviços (`--wait`). Containers usam
  `restart: unless-stopped`; o serviço de usuário `systemd/concurso-production.service`
  sobe a aplicação no boot.
- O nginx interno aceita somente o Host canônico, limpa `Forwarded`/`X-Forwarded-*`
  e libera 20 MB/120 s apenas em `/api/v1/admin/`. TLS e HSTS ficam no nginx do host.

Atualização de release, backup e restauração pertencem ao grupo 6.

## Grupo 5 — acervo

Carga da expansão 2026-11 aprovada (102 pacotes, 2.587 questões) pela importação
administrativa HTTP, no loopback do host. Faça um backup antes.

```sh
python3 scripts/vps_question_load.py --dry-run   # só prévias, digests conferidos com o manifesto versionado
python3 scripts/vps_question_load.py             # importa, SUBMIT e APPROVE AI_ONLY por pacote; confere no fim
python3 scripts/vps_question_load.py --verify-only
```

O checkout precisa estar no commit da release em uso, e a carga segura o lock do
operador (não roda junto com backup/release/restore). O diário privado
`production/question-load.json` retoma a partir do último passo confirmado; se a
resposta de um `apply` se perder depois de o servidor confirmar, a reexecução
para em vez de adivinhar (limitação registrada na #221). Os conjuntos de outubro
e o Google ficaram fora desta entrega (#221).

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
python3 scripts/vps_production.py promote    # uma vez: tira o estado do modo ensaio
```

`release` e `rollback` conferem o compose fixado da release de destino antes de
parar qualquer serviço. O estado usado no ensaio foi criado com `--rehearsal`;
na abertura, depois da `release` de um commit da main com compose fixado,
`promote` grava `rehearsal: false` e descarta como alvo de rollback uma release
anterior que não seja da main. A partir daí só releases da main são aceitas.

Backups, estado e segredos nunca vão para Git, CI ou issues. Volumes não são removidos pela ferramenta.

## Grupo 3 — domínio e TLS

### DNS (Hostinger)

Nas zonas `reemsoftworks.com` e `reemsoftworks.com.br`, TTL 300:

| Nome | Tipo | Valor |
| --- | --- | --- |
| `@` | A / AAAA | IPv4 / IPv6 da VPS (substitui o estacionamento da Hostinger) |
| `concurso-simulator` | A / AAAA | IPv4 / IPv6 da VPS |
| `www` | CNAME | mantido (aponta para `@`) |

Cada alteração é confirmada pelo usuário antes. A Hostinger guarda snapshots da
zona (`dns_snapshots_list`/`restore`) para desfazer.

### nginx e certificado

Copie a pasta `edge/` para a conta administrativa e execute, nesta ordem:

```sh
sudo bash install-edge.sh http <IP-público-do-usuário>   # porta 80 + allowlist de pré-lançamento
sudo bash install-edge.sh admin <IP-público-do-usuário>  # allowlist da área administrativa
sudo bash install-edge.sh cert                           # após o DNS propagar
sudo bash install-edge.sh https
sudo bash install-edge.sh open                           # abertura (grupo 7)
```

- Um certificado `reemsoftworks` cobre os seis nomes; renovação pelo timer
  `reemsoftworks-certificate.timer` com `nginx -t` antes do reload.
- `concurso-simulator.reemsoftworks.com` encaminha para `127.0.0.1:18090` com
  Host fixo, cabeçalhos encaminhados limpos, HSTS de um ano (sem `includeSubDomains`
  nem preload) e log sem query string.
- `*.reemsoftworks.com.br` → 301 para o mesmo host/caminho em `.com`;
  raiz e `www` de `.com` → 302 para o concurso.
- Até a abertura (grupo 7), `/etc/nginx/snippets/concurso-simulator-access.conf`
  permite só o IP do usuário; demais recebem 403. O arquivo fica fora do Git.
- `/api/v1/admin/` só aceita IPs de `concurso-simulator-admin-access.conf`, também
  após a abertura; o login administrativo tem `limit_req` pelo IP real (a API
  atrás dos proxies só vê o container web). Se o IP do administrador mudar, repita
  a fase `admin`.
- `open`: esvazia a allowlist de pré-lançamento (cópia anterior preservada),
  remove só o link `sites-enabled/concurso-public.conf` (acesso antigo pelo IP) e
  instala `reemsoftworks-default.conf` (porta 80: 444 para Host desconhecido,
  mantendo o desafio ACME do `central`). Se `nginx -t` falhar, tudo volta como estava.
- Porta 443 sem `ssl_reject_handshake`: o `central`, acessado pelo IP sem SNI, é o
  padrão da 443. Hosts desconhecidos não chegam à aplicação (vhost só do nome
  canônico; nginx interno responde 421).
- `central.conf` nunca é alterado. De `concurso-public.conf` (acesso antigo por HTTP
  no IP), só o link em `sites-enabled` é removido, e apenas na fase `open`.
- As fases `admin` e `open` guardam a allowlist anterior ao lado
  (`*.bak-<data>` em `/etc/nginx/snippets/`) e voltam a ela se `nginx -t` falhar.
