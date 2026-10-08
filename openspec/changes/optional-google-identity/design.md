# Design

## Context

Veja `proposal.md`. A base na épica geral usa Java 25, Gradle 9.8.0, Spring Boot 4.1.1, JDBC/Flyway e PostgreSQL 18.6. Hoje `SecurityConfiguration` é stateless, permite somente GET status, nega outras rotas e não concede CORS com credenciais. O frontend consulta prontidão e oferece aulas; não há identidade nem sessão. As specs da base estão em sua mudança concluída, ainda não consolidadas por arquivamento.

## Goals / Non-Goals

**Goals:** fluxo Google verificável, identidade estável mínima, sessão revogável no servidor e UI opcional; testes reproduzíveis sem Google real.

**Non-Goals:** não criar endpoints de histórico, sincronização, painel administrativo, senha, linking, avatar ou edição/exclusão de conta nesta mudança. Não prometer funcionalidades futuras prontas. Não provisionar client Google, hospedagem ou domínio; fornecer instruções para quem configurar.

## Decisions

### OIDC no backend, não tokens no navegador

Adicionar Spring Security OAuth2 Client com fluxo authorization code e PKCE S256. Usar validação OIDC da biblioteca para ID token, issuer, assinatura/JWK, audience, expiração e nonce, com state gerenciado por sessão. Escopos mínimos `openid profile`; não solicitar acesso offline, Gmail, contatos ou dados de estudo. Google é o único registration de produção; issuer esperado `https://accounts.google.com`. Testes usam provedor OIDC local controlado por perfil de teste, jamais issuer configurável por input público.

Alternativa rejeitada: JWT/bearer próprio no localStorage, que ampliaria exposição a scripts e necessidade de refresh/revogação. O frontend navega até `GET /oauth2/authorization/google`; callback é `GET /login/oauth2/code/google`, processado no backend.

### Modelo mínimo de identidade

Nova migração após V1 cria `app_user` (UUID, data de criação) e `external_identity` (usuário interno, provider, issuer normalizado, subject). Restrição única para provider/issuer/subject; criação em transação e leitura da identidade existente em caso de concorrência, sem usuários órfãos. Não usar e-mail como chave, não unir contas por e-mail e não persistir foto, nome ou e-mail nesta etapa. A UI pode dizer “Você está conectado” sem esses dados. O vínculo pseudonimizado ainda é dado pessoal e deve permanecer privado.

### Arquitetura e JPA — revisão aprovada em 2026-10-08

Aplicar `docs/architecture/adr/0001-domain-and-application-boundaries.md`: monólito modular, domínio/UseCases Java puro e portas para persistência/unidade transacional. Substituir o serviço específico Google com SQL por resolução genérica de identidade externa. O adaptador de autenticação fornece identidade verificada; VO valida apenas invariantes, sem habilitar outro provedor.

Persistência de identidade passa a JPA/Hibernate na infraestrutura, com modelos separados do domínio. Flyway permanece único dono do schema, sem alterar V1/V2; `ddl-auto=validate` e `open-in-view=false`. A porta transacional executa a unidade lookup/criação em transação independente; traduzir somente conflito da constraint de identidade e recuperar em nova transação depois do rollback. Preservar testes de migração, unicidade e ausência de órfãos. Spring Session JDBC e prontidão técnica continuam usando SQL na infraestrutura. Não criar Domain Service ou interfaces sem necessidade real.

### Sessões JDBC e principal mínimo

Usar Spring Session JDBC, sem Redis novo. Flyway cria também as tabelas e índices de sessão conforme a versão gerenciada pelo Boot; desativar inicialização automática concorrente. Sessão por necessidade, com rotação após login, inatividade de 30 minutos configurável e limpeza de sessões expiradas. Após login verificado, manter no contexto de segurança somente principal interno serializável com UUID e permissões mínimas; remover o authorized client e não reter tokens Google além da conclusão do fluxo. Não manter refresh tokens. Isso evita gravar o principal OIDC completo nas sessões JDBC.

Cookie host-only, Path=/, HttpOnly, SameSite=Lax, Secure no perfil de produção. Somente o perfil local explícito admite HTTP sem Secure. Não usar Domain amplo. Sessões em dispositivos diferentes são independentes; logout invalida apenas a atual. A identidade permanece após logout/restart. O deploy futuro deverá fornecer HTTPS e frontend/API same-site (preferencialmente reverse proxy de mesma origem); esta spec não pressupõe cookies third-party entre Pages e um domínio sem relação.

### Contratos e negação por padrão

- GET `/api/v1/auth/config`: público, googleEnabled booleano apenas.
- GET `/api/v1/me`: autenticado, UUID apenas, JSON 401 se visitante; no-store.
- GET `/api/v1/csrf`: público, entrega token vinculado à sessão (`token`, `headerName`), no-store; nunca aceitar token fixo de configuração.
- POST `/api/v1/auth/logout`: CSRF obrigatório, 204 e invalidação do cookie; sem token, 403. Obter token atualizado após login/expiração, pois o token antigo pode ser invalidado.
- GET `/api/v1/status`: contrato UP/DOWN preservado e público.
- Exceções explícitas para entrada/callback e endpoints listados. Demais rotas continuam negadas. Não abrir `/api/**` genericamente.

Não redirecionar APIs ao Google. CORS concede GET/POST e header CSRF apenas às origens configuradas, com allowCredentials=true para os endpoints de identidade/sessão. Preservar status independente de sessão. Frontend usa `credentials: include` nas chamadas de identidade/CSRF/logout, não armazena cookies ou tokens OAuth em storage e não tenta ler cookie HttpOnly.

### Configuração e experiência opcional

`AUTH_GOOGLE_ENABLED` default false permite desenvolvimento e estudo sem client real; frontend consulta auth/config e oculta o botão se desativado. Quando true, exigir `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, callback absoluto permitido e `FRONTEND_URL` fixa. Falta de configuração falha startup; nunca cair silenciosamente para autenticação simulada. Exemplos sem credenciais reais, segredos só no backend. URLs de callback devem coincidir com as registradas no Google, usando consistentemente localhost ou 127.0.0.1 no ambiente local.

Redirect pós-login para `FRONTEND_URL` fixa; falha/cancelamento usa marcador genérico `auth=failed`, consumido e retirado da URL pelo frontend. Não aceitar `returnTo` livre, não colocar código ou tokens no redirect ao frontend. Explicar que login apenas prepara identidade, sem afirmar sincronização de histórico já existente. Erros de consulta privada não bloqueiam link de aulas e não iniciam Google automaticamente.

### Testes e entrega por PRs

Separar PRs coesos: arquitetura/decisões, persistência/identidade, sessão/contratos/CSRF, fluxo OIDC, frontend e documentação/validação integrada. Cada PR inclui seus testes. O usuário autorizou PRs encadeados de rascunho para revisão ao final, sem pausas nem merges no GitHub; preservar histórico ao atualizar dependências. CI usa PostgreSQL 18.6 isolado e um provedor OIDC local com discovery, authorization, token e JWK; cobrir redirect, troca de código e cookies reais, não só mock de principal autenticado. Negativos: state/nonce/issuer/audience/assinatura/expiração inválidos, replay, CSRF, fixação, CORS, timeout e logout. Um teste manual Google real é complementar e requer credenciais do usuário, sem colocá-las no CI.

## Risks / Trade-offs

- [Google/configuração indisponível] → estudo permanece público; login desativável e falhas genéricas com nova tentativa.
- [Sessão entre sites distintos bloqueada por navegador] → validar desenho same-site na spec de deployment; não tentar contornar com tokens no storage.
- [Serialização de sessão sensível a versões] → principal mínimo e contrato estável; documentar expiração forçada de sessões em mudança incompatível.
- [Conta duplicada em login concorrente] → transação, restrição única e testes de corrida.
- [Segredos/tokens em logs ou callback] → não registrar payloads OAuth, não incluir dados sensíveis em erros; revisar log e artefatos de build.
- [Google real não disponível no CI] → provedor local exercita o fluxo completo; documentar smoke real complementar antes da entrega pública.

## Migration Plan

Implementar apenas após aprovação dos artefatos; migração aditiva sem alterar V1 nem aulas. Primeiro validar login desativado e migrações com banco novo/existente; depois habilitar em ambiente local configurado. Rollback de aplicação não apaga identidades ou tabelas; desativar login e invalidar sessões da versão nova se necessário. Specs seguintes referenciam UUID interno, não subject Google. Integrar a spec em `epic/application-reformulation` após revisão e testes, mantendo main intocada.

## Open Questions

Domínio HTTPS, proxy e client OAuth de produção serão configurados na spec de deployment. Eles não alteram o fluxo definido aqui. A disponibilidade de credenciais Google reais afeta somente o smoke complementar; testes automatizados e ambiente desativado não dependem delas.
