# Tasks

Implementação autorizada pelo usuário em 2026-10-07. Cada grupo abaixo vira um PR coeso para `epic/optional-google-identity`, incluindo seus testes/documentação. Grupos dependentes aguardam integração dos anteriores. Checklist é concluído na épica somente após verificação e merge aprovado. Resultados de branches ainda não integradas ficam em `verification.md`.

## 1. Identidade persistida — branch identity-storage

- [ ] 1.1 Criar migração aditiva de usuário/identidade externa com UUID, vínculo e unicidade; testar banco novo e upgrade desde V1 em PostgreSQL 18.6 isolado, sem perda do marcador técnico.
- [ ] 1.2 Implementar resolução transacional de identidade e principal interno mínimo; testar retorno da mesma identidade, identidades diferentes e concorrência sem duplicatas/usuários órfãos.
- [ ] 1.3 Documentar modelo mínimo, ausência de e-mail como chave e escopo de dados pessoais; verificar que testes e schema não armazenam nome, foto, e-mail ou senha.

## 2. Sessões e contratos — branch session-access, após grupo 1

- [ ] 2.1 Integrar Spring Session JDBC e migração versionada de suas tabelas; testar sessão, timeout configurável, limpeza de expiradas e recuperação após restart, com principal interno sem tokens OAuth.
- [ ] 2.2 Implementar contratos me, csrf e logout com proteção CSRF, cookie seguro e negação padrão; testar 200/401/204/403, idempotência da saída, no-store, cookie anterior inválido e independência entre dispositivos. Atualizar OpenAPI e conferir JSON contra o contrato.
- [ ] 2.3 Configurar CORS explícito com credenciais para identidade/sessão e preservar prontidão pública; testar origens permitidas/desconhecidas, preflight, flags HttpOnly/SameSite/Secure e exceção HTTP somente no perfil local.
- [ ] 2.4 Documentar contratos, tempo de sessão e exemplos de consulta/saída com token CSRF; verificar os exemplos no ambiente isolado, sem introduzir autenticação mock em produção.

## 3. Login Google — branch google-oidc, após grupo 2

- [ ] 3.1 Integrar OAuth2 Client, configuração habilitável e auth/config público; testar desativado sem chamadas externas, entrada 404 desativada e startup recusado com configuração habilitada incompleta. Documentar variáveis sem segredos.
- [ ] 3.2 Implementar fluxo code com PKCE, callback, associação de identidade, rotação da sessão e principal mínimo; testar login completo com provedor OIDC local (discovery, autorização, token e JWK) e confirmar mesmo UUID em novo login/restart.
- [ ] 3.3 Implementar falha genérica e redirect fixo; testar state, nonce, issuer, audience, assinatura, expiração e replay inválidos, ausência de contas criadas em falhas, ausência de tokens em respostas/logs/sessões persistidas e rejeição de destinos externos.
- [ ] 3.4 Documentar configuração de client Google, escopos mínimos e callback local consistente; entregar roteiro de smoke Google real, sem provisionar credenciais nem exigir login Google no CI.

## 4. Interface opcional — branch login-interface, após grupo 3

- [ ] 4.1 Adicionar consulta de auth/config e me, estados visitante/autenticado/indisponível e entrada Google por navegação; testar habilitado/desativado, 401 e erros de rede, sem iniciar login automaticamente nem prometer histórico já disponível.
- [ ] 4.2 Adicionar saída com CSRF atualizado e credentials include, tratar expiração e retorno de falha; testar logout, 403 com mensagem, perda de sessão e marcador auth=failed retirado da URL, sem storage de tokens Google.
- [ ] 4.3 Documentar execução local e verificar teclado e telas de 390/1280px com Playwright, mantendo link das aulas disponível em todos os estados.

## 5. Verificação integrada — branch identity-verification, após grupo 4

- [ ] 5.1 Executar fluxo de navegador com frontend/API/provedor OIDC local/PostgreSQL reais isolados; verificar login, sessão, consulta me, expiração, logout, duas identidades isoladas e visitante sem login. Registrar evidências sem segredos.
- [ ] 5.2 Repetir Gradle clean build, testes/build frontend e regressão de material offline em clone limpo; verificar CI no PR sem credenciais Google reais e preservar os 9 módulos/90 resoluções e o workflow de preview manual.

## Workflow follow-up

- Criar issue da spec, registrar PRs e manter distinção entre implementação verificada e integração aprovada.
- Solicitar aprovação dos artefatos antes de implementar; revisar cada grupo por PR pequeno.
- Smoke Google real é complementar, exige credenciais configuradas pelo usuário e precisa ser realizado antes da publicação pública na etapa de deployment.
- Abrir PR da spec para `epic/application-reformulation` após aprovação e integração; não fazer merge em main nem arquivar sem autorização.
