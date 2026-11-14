# Proposal

## Why

O usuário quer publicar o concurso-simulator na VPS Hostinger com domínio próprio.
O plano registrado em 2026-10-25 previa só uma homologação privada por túnel, em
Rocky Linux, operada exclusivamente pelo usuário. Em 2026-10-31 a VPS foi
reinstalada com Ubuntu 24.04 e a main antiga (f61d57d) foi publicada por HTTP no
IP, com banco vazio, perfil local e adaptação de URL no proxy. Esse arranjo
funciona, mas não usa HTTPS, cookie Secure, domínio, Google nem o acervo.

Em 2026-11-13 o usuário decidiu ampliar esta spec para o lançamento público.
Acompanhamento: #156.

## What Changes

- **Acesso:** o assistente (Claude Code no PC do usuário) MAY operar a VPS por SSH
  como `concurso-dev`, com a chave do usuário, sem agent forwarding. Operações
  com sudo (nginx, certbot, firewall) são executadas pelo usuário ou com a senha
  digitada por ele no prompt; o assistente não recebe senhas.
- **Endereço:** `https://concurso-simulator.reemsoftworks.com`.
  Os dois domínios apontam para a VPS. Qualquer host em `reemsoftworks.com.br`
  redireciona com 301 para o mesmo host e caminho em `reemsoftworks.com`.
  A raiz `reemsoftworks.com` (e `www`) fica reservada para outros projetos;
  até lá, redireciona temporariamente (302) para o concurso. O DNS é gerenciado na Hostinger.
- **Runtime de produção:** perfil não local (cookie Secure, HTTPS), release por
  commit da main, imagens construídas no Docker rootless do `concurso-dev`,
  sem portas de API/PG. Remove a substituição de URL (`sub_filter`) e a
  publicação HTTP pelo IP.
- **Borda:** nginx do host com TLS Let's Encrypt, redirecionamento HTTP→HTTPS,
  HSTS e recusa de hosts desconhecidos.
- **Google:** configuração de produção preparada (`configure-google`), mas o
  lançamento sai com o Google desligado por decisão do usuário em 2026-11-13;
  ligar e fazer o smoke real ficou para a #221.
- **Acervo:** a expansão 2026-11 aprovada (102 pacotes, 2.587 questões), carregada
  pela importação administrativa por HTTP. Os conjuntos de outubro (250 simulado,
  255 prática, famílias parametrizadas) ficaram fora por decisão do usuário (#221).
  Sem contas, sessões ou tentativas do PC.
- **Operação:** atualização com backup antes de migrar, backup manual com cópia
  no PC, restauração ensaiada em destino separado e rollback compatível.

## Capabilities

### New Capabilities

- `vps-staging-runtime`: runtime de produção, releases identificadas e operação.
- `vps-staging-data`: carga do acervo sem dados pessoais.
- `vps-staging-recovery`: backup, restauração, atualização e rollback.
- `vps-public-edge`: domínio, TLS, redirecionamentos, Google e abertura pública.

Os nomes `vps-staging-*` foram mantidos para preservar o histórico da #156.

### Modified Capabilities

Nenhuma alteração normativa das capabilities de negócio existentes. Se a
produção exigir mudança de código (por exemplo, cabeçalhos encaminhados),
ela entra como tarefa própria com testes.

## Impact

Novos artefatos em `infra/vps-production/` (Compose, nginx, roteiros e operador),
scripts e testes. Configuração do host (nginx, certbot, ufw) fora do repositório,
com versões de referência versionadas. Sem migração de negócio prevista; Flyway
continua o único dono do schema.

Mudança crítica: rede pública, segredos, OAuth, persistência e restauração.
A revisão final de código antes do merge `[SPEC]` segue `docs/architecture/spec-review.md`
(Claude Opus 5.5, esforço medium, agente novo). HA, SLA, RPO/RTO e backup externo
automático continuam fora de escopo.
