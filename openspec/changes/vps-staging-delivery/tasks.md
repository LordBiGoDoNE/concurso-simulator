# Tasks

Issue #156. Escopo ampliado em 2026-11-13 para lançamento público em
`https://concurso-simulator.reemsoftworks.com`. Nenhum grupo autorizado para
implementação; o usuário escolhe os grupos completos antes de começar.

O antigo grupo 1 (preparação de Rocky Linux 10.2) foi superado pela reinstalação
da VPS com Ubuntu 24.04 em 2026-10-31. Seus commits locais na branch
`task/vps-staging-delivery/server-preparation` são preservados como histórico,
sem PR nem integração.

## 1. Base do host e acesso

- [ ] 1.1 Registrar inventário sanitizado do Ubuntu real (usuários, Docker rootless, limites, nginx, certbot, ufw, serviços) e o acesso SSH do assistente como `concurso-dev`.
- [ ] 1.2 Preparar e aplicar, pela conta administrativa, a correção das permissões graváveis por todos em `/opt/vps-control`, sem remover os scripts.
- [ ] 1.3 Conferir ufw (somente 22/80/443, IPv4/IPv6), sshd só por chave e conectividade IPv6 da VPS.
- [ ] 1.4 Reboot controlado com persistência de nginx, Docker rootless e serviço do concurso; evidências e docs do grupo.

## 2. Runtime de produção — depende de 1

Implementado e verificado em 2026-11-13 (#213); não integrado. Evidências: `verification/grupo-2.md`.


- [x] 2.1 Builder de imagens com origem HTTPS de produção explícita, contexto filtrado e manifesto; recusas e testes, sem afrouxar o operador local.
- [x] 2.2 Perfil de produção: cookie Secure, CORS canônico, cabeçalhos encaminhados só do proxy interno; testes da cadeia de segurança real.
- [x] 2.3 `infra/vps-production`: Compose, operador (init/start/stop/status/update), segredos por arquivo, serviço systemd de usuário, lock e verificação de ownership/colisão.
- [x] 2.4 Ensaio na VPS em projeto `concurso-simulator-prod` sem acesso público; regressões, recusas e docs do grupo.

## 3. Domínio e TLS — depende de 1

- [ ] 3.1 Registros DNS na Hostinger (subdomínio, raiz e `www` dos dois domínios), com confirmação do usuário antes de cada alteração.
- [ ] 3.2 Certificados Let's Encrypt pelo certbot existente (webroot) e hook de renovação com `nginx -t` + reload.
- [ ] 3.3 Vhosts nginx versionados: canônico com proxy e HSTS, 301 `.com.br`→`.com`, 302 da raiz, HTTP→HTTPS, hosts desconhecidos recusados, `central` preservado; acesso restrito até a abertura.
- [ ] 3.4 Testes externos de TLS, redirecionamentos, cabeçalhos e recusas; docs do grupo.

## 4. Google OAuth de produção — depende de 2 e 3

- [ ] 4.1 Roteiro para o usuário criar o client web e publicar a tela de consentimento, com callback exato.
- [ ] 4.2 Configurar segredos Google na VPS sem expô-los; startup recusa client incompleto.
- [ ] 4.3 Smoke real: login, sessão, logout, conta nova, retomada e rejeições CSRF/origem.
- [ ] 4.4 Evidências sanitizadas e docs do grupo.

## 5. Acervo — depende de 2 e 3

Expansão 2026-11 carregada e verificada em 2026-11-13 (#220); conjuntos de outubro fora do escopo por decisão do usuário. Não integrado. Evidências: `verification/grupo-5.md`.


- [x] 5.1 Preparar o pacote do acervo aprovado a partir da main da release, com contagens/digests calculados e remapeamento R1.
- [x] 5.2 Carregar pelo fluxo administrativo existente (importação e decisões em lote) no banco de produção, por HTTPS.
- [x] 5.3 Conferir idempotência, linhagem, finalidade e ausência de contas/sessões/tentativas do PC.
- [x] 5.4 Validar simulado, prática por assunto e revisão nos dois cargos; docs do grupo.

## 6. Backup, atualização e recuperação — depende de 2; ensaio final usa 5

Implementado em 2026-11-13; 6.1, 6.2 e 6.4 verificados na VPS; 6.3 coberta por testes, uso real no corte. Não integrado. Evidências: `verification/grupo-6.md`.


- [x] 6.1 Backup consistente com manifesto e cópia privada para o PC.
- [x] 6.2 Restauração em projeto/volume novo com sessões invalidadas e conferência.
- [ ] 6.3 Atualização de release com backup obrigatório, falha de migração e rollback compatível.
- [x] 6.4 Ensaio completo na VPS, com artefato inválido e destino ocupado recusados; docs do grupo.

## 7. Abertura e gate — depende de 1–6

- [ ] 7.1 Revisão final de código (Opus 5.5 medium, `spec-reviewer`), triagem e evidências em `spec-review.json`/`spec-review.md`.
- [ ] 7.2 PR `[SPEC]` da épica para `main` com autorização específica de merge.
- [ ] 7.3 Corte: release da main em produção, remoção do acesso HTTP pelo IP e da restrição do vhost; volume antigo preservado até decisão do usuário.
- [ ] 7.4 Checklist público (portas, TLS, fluxos em 390/1280 px, Google, acervo, reboot) e issue atualizada com verificado × integrado.

Cada grupo inclui testes e documentação. Dependências reais seguem Stacks
conforme AGENTS.md. Nenhum merge, publicação ou exclusão de dados é autorizado
por este documento.
