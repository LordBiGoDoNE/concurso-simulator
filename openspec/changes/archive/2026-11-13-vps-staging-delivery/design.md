# Design

## Context

Estado da VPS conferido pelo assistente por SSH, somente leitura, em 2026-11-13.
A impressão digital ED25519 do host foi confirmada pelo usuário no console Hostinger
antes da conexão. O IP e outros identificadores privados não entram no repositório.

- Ubuntu 24.04.5 LTS x86_64, 4 vCPUs, 15 GiB RAM, 193 GB de disco (5% usado), sem swap.
- Usuários de projeto `concurso-dev` e `reem-dev`, sem sudo, cada um com Docker
  rootless próprio (29.9.2, Compose 5.6.1) e limite de cgroup de 2 CPUs/6 GiB.
  Administração do host (sudo) é feita por conta administrativa do usuário.
- nginx do host ativo em 80/443 (IPv4 e IPv6); ufw ativo; certbot em venv própria
  com timer de renovação; webroot ACME já configurado. Agente de segurança
  Monarx (Hostinger) ativo.
- `central.conf`: serviço de administração em 443 pelo IP, com certificado de IP,
  encaminhando para 127.0.0.1:18100. Fora do escopo desta spec; preservar.
- Concurso atual (2026-10-31): main f61d57d, projeto Docker
  `concurso-simulator-app-local-vps`, perfil local HTTP, publicado na porta 80 do
  IP por nginx com `sub_filter` reescrevendo a origem do material e override de
  CORS. Banco V11 novo, sem questões, contas ou tentativas. Serviço de usuário
  `concurso-app.service`. Google desligado.
- `/opt/vps-control` (scripts de provisionamento) tem arquivos e diretórios
  graváveis por qualquer usuário (`0666`/`0777`), embora pertençam a root.
- DNS: `reemsoftworks.com` e `reemsoftworks.com.br` estão estacionados na Hostinger
  (nameservers dns-parking, A para a CDN da Hostinger). O subdomínio não existe.
- main atual: schema V17, acervo expandido e importação/curadoria administrativa
  (V12–V17). Bootstrap administrativo por usuário/senha (`ADMIN_PASSWORD`).

## Goals / Non-Goals

Publicar a main em `https://concurso-simulator.reemsoftworks.com` com HTTPS,
Google, acervo aprovado e recuperação ensaiada, substituindo a publicação HTTP pelo
IP. Não alterar conteúdo editorial, regras de negócio ou o serviço `central`.
Sem HA, SLA, RPO/RTO, CDN, backup externo automático ou monitoração paga.

## Decisions

### 1. Acesso e papéis

Decisão do usuário em 2026-11-13, substituindo as de 2026-10-25/26: o assistente
opera por SSH como `concurso-dev` a partir do PC do usuário, com
`IdentitiesOnly=yes`, sem agent forwarding e com verificação estrita da chave do host.
A chave privada fica no PC. Mudanças que exigem root (nginx, certbot, ufw,
permissões de `/opt`) são preparadas pelo assistente como arquivos/comandos
revisáveis e executadas pelo usuário na conta administrativa, ou pelo assistente
nessa conta somente se o usuário digitar a senha do sudo no prompt. Nenhuma senha,
token ou segredo passa pela conversa, pelo Git ou por logs.

Toda ação na VPS que altere estado é anunciada antes. Leitura e diagnóstico não
precisam de confirmação. Exclusão de volumes, imagens ou estado exige confirmação
explícita do usuário.

### 2. Runtime de produção

Novo diretório `infra/vps-production/`, reaproveitando as imagens e o operador de
`infra/local-app` onde for útil, sem afrouxar as recusas do operador local
(origem loopback, perfil local, endpoint remoto). Projeto Docker
`concurso-simulator-prod`, estado/segredos/volume próprios em diretório privado
do `concurso-dev`. O volume do projeto atual é preservado até a decisão do usuário.

- Release = commit da main conferido, construído no Docker rootless da própria VPS
  a partir do clone do GitHub, com contexto filtrado (`git archive`) e manifesto
  de imagens. Sem push nem credenciais do GitHub na VPS. O `compose.yaml` do mesmo
  commit é copiado para o diretório de releases e fixado por SHA-256 no manifesto;
  `start`, `release` e `rollback` usam essa cópia, não o checkout de trabalho.
- Web com origem `https://concurso-simulator.reemsoftworks.com` embutida no build;
  sem `sub_filter`. Web publica somente em 127.0.0.1; API e PG sem portas no host.
- Perfil de produção: cookie `SESSION` Secure/HttpOnly/SameSite=Lax, CSRF atual,
  CORS somente com a origem canônica. Decisão de implementação: a API não lê
  cabeçalhos encaminhados (`SERVER_FORWARD_HEADERS_STRATEGY: none`) e os dois nginx
  os apagam. O HTTPS é garantido por configuração estática: cookie Secure fora do
  perfil `local`, callback OAuth absoluto validado como HTTPS e retorno para
  `frontend-url` fixo. Por isso o orçamento de login por origem da API vê só o
  container web; o limite por IP real fica no nginx do host (`limit_req` no login
  administrativo), e `/api/v1/admin/` é restrito a uma allowlist administrativa.
- Segredos por arquivos (configtree / `*_FILE`) montados apenas no serviço que os
  consome: senha do PG, senha do admin bootstrap, client Google.
- Serviço systemd de usuário com linger para subir após reboot; prontidão real
  dos três serviços antes de declarar sucesso.

### 3. Borda: domínio, TLS e redirecionamentos

DNS na Hostinger (painel ou MCP, com confirmação do usuário): registros A (e AAAA
se a VPS tiver IPv6 roteável) para `concurso-simulator`, raiz e `www` dos dois
domínios. TTL baixo durante a troca.

nginx do host, com arquivos de referência versionados em `infra/vps-production/edge/`:

- `concurso-simulator.reemsoftworks.com`: TLS, proxy para 127.0.0.1:18090 com
  `Host` canônico, limpeza de `Forwarded`/`X-Forwarded-*` do cliente e definição
  pelo proxy, HSTS (sem preload inicialmente), `client_max_body_size` compatível
  com a importação administrativa, timeouts finitos, sem logar cookies/query do callback.
- Hosts de `reemsoftworks.com.br` cobertos pelo certificado: 301 para o mesmo host
  e caminho em `.com`.
- Raiz e `www` de `.com`: 302 temporário para o concurso até existir outro site.
- HTTP 80: 301 para HTTPS, exceto `/.well-known/acme-challenge/`.
- Host desconhecido: na abertura, `reemsoftworks-default.conf` vira o `default_server`
  da porta 80 (444, mantendo só o desafio ACME usado pelo certificado de IP do
  `central`). Na 443 não há `ssl_reject_handshake`: o vhost `central`, acessado
  pelo IP sem SNI, é o padrão e seria derrubado. Hosts desconhecidos não chegam à
  aplicação: o vhost do concurso só atende o nome canônico e o nginx interno
  responde 421. O vhost `central` é preservado.
- Área administrativa `/api/v1/admin/`: somente IPs da allowlist administrativa
  (arquivo fora do Git), inclusive após a abertura; login com `limit_req` por IP.
- Certificados Let's Encrypt via certbot existente (webroot), com renovação
  pelo timer existente e `nginx -t` + reload no hook.

A publicação HTTP do concurso pelo IP é removida no corte (grupo 7).

### 4. Google OAuth de produção

Atualização de 2026-11-13: o lançamento sai com o Google desligado, por decisão do
usuário; o roteiro e o comando abaixo ficam prontos e a ativação segue na #221.

O usuário cria o client OAuth web no Google Cloud com o callback exato da origem
canônica (conforme `backend/docs/google-login.md`), escopos básicos
(`openid`, `profile`, os únicos que a aplicação solicita) e tela de consentimento publicada em produção, para
não limitar o acesso a usuários de teste. Os segredos ficam só nos arquivos privados
da VPS. Smoke real (login, sessão, logout, retomada e conta nova) antes da abertura.

### 5. Acervo

Atualização de 2026-11-13: carregada somente a expansão 2026-11 pelo fluxo HTTP; os
conjuntos de outubro, que só entram pela CLI local, ficaram fora por decisão do usuário (#221).

Somente o acervo aprovado na main da release, carregado no banco novo pelas
operações administrativas/editoriais existentes (importação e decisões em lote),
autenticadas pelo admin bootstrap, pelo loopback do host (sem passar pela internet) e com o lock do operador. Sem dump do PC, contas, identidades,
sessões, tentativas ou capturas. Contagens e digests são calculados a partir da
main da release, não copiados deste documento. A recomendação R1 da revisão de
2026-10-25 continua válida: importação cria IDs novos no destino; mapear por
origem/fingerprint e recalcular digests derivados. Repetição é idempotente;
divergência falha sem sobrescrever.

### 6. Backup, atualização e recuperação

- Backup consistente (API parada, PG ativo) reaproveitando a semântica do
  `local_app.py backup`: dump custom, manifesto com release/schema/PG/hashes.
- Cópia para o PC por SCP iniciado do PC; backups são privados e nunca vão para Git/CI.
- Atualização: backup obrigatório antes de trocar a release; nova release só após
  build e inspeção; falha de migração mantém imagem/volume anteriores.
- Restauração em projeto/volume novo, sessões invalidadas, com conferência antes
  de liberar. Rollback só com imagem/schema compatíveis ou restauração separada;
  Flyway não é desfeito.
- Atualização, backup e restauração são mutuamente exclusivos (lock do operador).

### 7. Higiene do host

Corrigir as permissões graváveis por todos em `/opt/vps-control` (conta
administrativa), conferir ufw (somente 22/80/443 de entrada, IPv4/IPv6), sshd só
por chave e reboot controlado com persistência dos serviços. Manter Monarx e
demais agentes do provedor.

### 8. Ordem de entrega

A infraestrutura desta spec é desenvolvida e ensaiada na VPS a partir das branches
de tarefa, em projeto separado e sem abrir o domínio ao público. A abertura
(subdomínio servindo a aplicação para o público e remoção do acesso pelo IP)
acontece depois da revisão final e do merge `[SPEC]` autorizado, usando um commit
da main como release. Antes disso, o domínio pode ser ativado de forma restrita
para smoke (por exemplo, allowlist do IP do usuário no vhost).

## Risks

- Cookie Secure e OAuth atrás de proxy: URL de callback errada ou sessão HTTP.
  Mitigação: testes da cadeia real e smoke Google.
- Recursos compartilhados com `reem-dev` (CPU, disco, kernel). O limite de cgroup
  de 6 GiB precisa caber API + PG + build; o build pode exigir pausar a aplicação.
- Backup somente no host e no PC: perda simultânea fica fora da cobertura.
- `/opt/vps-control` gravável por todos: risco de escalonamento se root executar
  esses scripts; corrigir no grupo 1.

## Verification

Testes automatizados do builder/operador/configuração (CI, Linux), testes da
cadeia de segurança para perfil de produção, `nginx -t` e testes externos a partir
do PC: TLS válido, redirecionamentos, HSTS, portas abertas só 22/80/443, API/PG
inacessíveis, fluxos de visitante/conta/admin em 390/1280 px, reboot e
restauração. Evidências sanitizadas em `verification.md`; sem IP, segredos ou dumps.

## References

- ADR 0001, `docs/architecture/http-endpoint-review.md`, `docs/architecture/spec-review.md`.
- `infra/local-app/README.md` e `backend/docs/google-login.md`.
- [Docker rootless](https://docs.docker.com/engine/security/rootless/),
  [Spring Boot atrás de proxy](https://docs.spring.io/spring-boot/how-to/webserver.html),
  [Certbot webroot](https://eff-certbot.readthedocs.io/en/stable/using.html#webroot),
  [Backup PostgreSQL 18](https://www.postgresql.org/docs/18/backup-dump.html).
