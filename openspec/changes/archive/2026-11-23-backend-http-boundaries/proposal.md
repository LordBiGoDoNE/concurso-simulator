# Proposal

Issue: [#261](https://github.com/LordBiGoDoNE/concurso-simulator/issues/261).
Épica: `epic/backend-http-boundaries`, criada da `main` `9dea050` em 2026-11-23.

## Why

A avaliação geral do projeto feita em 2026-11-23 encontrou no backend
(`backend/src/main/java/br/com/concursosimulator/`) quatro problemas estruturais,
confirmados por leitura do código na `main` `9dea050`:

1. **Rotas repetidas.** O caminho `/api/v1/...` aparece como literal em 10 classes de
   infraestrutura: `SecurityConfiguration` (24 ocorrências, entre cadeia padrão e
   CORS), `ApiAccessFailureHandler` (3), `AttemptRoutes` (7),
   `AdministrativeCurationSecurityConfiguration` (5), `AdministrativeHttpSecurity`
   (4), `PublicQuestionRoutes` (3), `SubjectReviewRoutes` (2),
   `ImportSecurityConfiguration` (2), `ActiveAccountFilter` (1) e
   `SessionIndependentRoutesFilter` (1). Uma rota de estudo nova exige editar de
   4 a 5 desses pontos (autorização, CORS, resposta 401/403, filtro de sessão e
   isenção de troca de senha). Esquecer um deles não quebra compilação: a rota vira
   `denyAll`, perde CORS ou responde 403 sem corpo. `PublicQuestionRoutes` e
   `SubjectReviewRoutes` moram em `shared` só para evitar ciclo entre módulos.
2. **Exceções genéricas como contrato HTTP.** `AttemptFailureAdvice`,
   `QuestionFailureAdvice` e `ReviewFailureAdvice` traduzem
   `IllegalArgumentException` → 400 e `IllegalStateException` → 500.
   `AttemptUseCases.create` lança `IllegalStateException` quando a geração
   parametrizada está desabilitada; o cliente recebe `500 internal_error`, e o
   `ParameterizedOperationalFixture` fixa esse 500. Uma
   `IllegalArgumentException` de dado persistido inconsistente vira 400, culpando o
   cliente. Os três advices mapeiam todo `DataAccessException` para 503, inclusive
   violação de integridade, que não é indisponibilidade. Os códigos de erro são
   mistos: `invalid_request`/`not_found`/`unavailable` no estudo e
   `INVALID_REQUEST`/`ADMIN_UNAVAILABLE` na administração; a segurança usa
   `unauthenticated` numa cadeia e `UNAUTHENTICATED` na outra.
3. **Infraestrutura dependendo de web.** `UserPrincipal` fica em `identity.web` e é
   usado por `AttemptAccess`, `ActiveAccountFilter`, `AdministrativeHttpSecurity`,
   `AdministrativeSecurity`, `SecurityConfiguration`,
   `ImportSecurityConfiguration` e `AdministrativeCurationSecurityConfiguration`.
   `AttemptActorConfiguration` (infraestrutura) usa `mockexam.web.CurrentActor`;
   `SecurityConfiguration` usa `identity.web.GoogleLoginHandlers`. As portas
   `AdministrativeSession` e `AdministrativeGoogleCompletion` estão em `web` e são
   implementadas pela infraestrutura. O ArchUnit não tem regra "infraestrutura não
   depende de web" nem "controllers só em `..web..`"; as regras de controller listam
   classes uma a uma e omitem `AdministrativeQuestionController`,
   `EditorialDecisionBatchController` e `StatusController`.
4. **DTOs web expondo o núcleo.** `AdministrativeImportController`,
   `AdministrativeQuestionController` e `EditorialDecisionBatchController` declaram
   requisições e respostas com tipos de `application`/`domain`
   (`QuestionImportReader.Document`, `QuestionOrigin`, `Question.Status`,
   `EditorialDecision.Action`, `ImportReceipt`, `Receipt` etc.). Renomear um enum ou
   campo do núcleo muda o JSON público sem que nenhum arquivo web seja tocado.

Essas falhas não produzem defeito visível hoje, mas tornam cada rota ou erro novo
uma edição espalhada, e o ADR 0001 e `docs/architecture/http-endpoint-review.md`
já pedem essas fronteiras. A correção mexe em contratos HTTP e migração de sessão,
então não é hotfix (`hotfix.md`).

## What Changes

- Cada funcionalidade declara a política das próprias rotas HTTP (método, padrão,
  acesso, CORS, sessão, isenção de troca de senha) na sua `infrastructure`.
  `platform` só compõe cadeias, CORS e respostas de rejeição a partir dessas
  declarações. Rota não declarada continua negada.
- Um teste de contexto Spring garante que todo mapeamento HTTP de controller tem
  exatamente uma política e que toda política aponta para um mapeamento real ou para
  uma rota do framework listada (logout, OAuth2).
- Exceções de aplicação nomeadas substituem `IllegalArgumentException`/
  `IllegalStateException` como contrato. Os advices deixam de mapear essas duas
  classes; falha inesperada vira `500 INTERNAL_ERROR` por um tratador explícito.
- Formato de erro único: objeto JSON com `error` em `UPPER_SNAKE_CASE`,
  `Cache-Control: no-store`, sem eco de mensagem, SQL ou conteúdo; campos extras
  (`correlationId`, `available`, `questionIds`, `reason`) mantidos onde já existem.
- 503 só para falha transitória de banco/transação; violação de integridade não
  reconhecida vira 500.
- `UserPrincipal` e as portas administrativas saem de `web`; `GoogleLoginHandlers` e
  o resolvedor de `@CurrentActor` vão para o lado correto da fronteira. Sessões
  persistidas antes da mudança são tratadas como sem autenticação, nunca 500.
- Regras ArchUnit genéricas: infraestrutura não depende de web; `@RestController` e
  `@RestControllerAdvice` só em `..web..`; todo controller recebe identidade
  verificada sem mecanismos de sessão/segurança e responde DTO concreto do próprio
  pacote web.
- Controllers administrativos de `questionbank` passam a ter DTOs web próprios, com
  o JSON atual preservado byte a byte por testes de contrato.

### Mudanças de comportamento observável (contrato HTTP)

| Situação | Hoje | Depois |
| --- | --- | --- |
| `POST /api/v1/attempts` com `useParameterizedQuestions=true` e geração desabilitada | `500 {"error":"internal_error"}` | `503 {"error":"PARAMETERIZED_GENERATION_UNAVAILABLE"}` |
| Violação de integridade não reconhecida nas APIs de estudo | `503 unavailable` | `500 INTERNAL_ERROR` |
| `IllegalArgumentException` vinda de dado persistido | `400 invalid_request` | `500 INTERNAL_ERROR` |
| Códigos de erro do estudo e da segurança | `invalid_request`, `not_found`, `conflict`, `insufficient_pool`, `internal_error`, `unavailable`, `unauthenticated`, `forbidden` | `INVALID_REQUEST`, `NOT_FOUND`, `CONFLICT`, `INSUFFICIENT_POOL`, `INTERNAL_ERROR`, `UNAVAILABLE`, `UNAUTHENTICATED`, `FORBIDDEN` |
| 503 do nginx com backend fora (`infra/local-app`, `infra/vps-production`) | `{"error":"unavailable"}` | `{"error":"UNAVAILABLE"}` |
| Sessão persistida antes da mudança, com `UserPrincipal` antigo | — | tratada como sem autenticação: `/me` 401, estudo segue como visitante; nova entrada necessária |

Status, cabeçalhos, CORS e corpos de sucesso das demais rotas não mudam; a matriz de
caracterização do grupo 1 prova isso antes e depois.

### Consumidores verificados

- **Frontend** (`frontend/src`): decide só por status e pelo campo `available`;
  nenhum código lê `error`. Mudam os dublês de teste que devolvem códigos em
  minúsculas (`AttemptContext.test.tsx`, `LoginPanel.test.tsx`,
  `e2e/login.spec.ts`, `e2e/study-layout.spec.ts`, `e2e/subject-review.real.spec.ts`).
  `AttemptComposer` hoje mostra "Criação não confirmada. Tente novamente com os
  mesmos critérios" para qualquer falha; com o 503 nomeado, passa a orientar a
  desmarcar as variações numéricas, porque repetir os mesmos critérios falharia de
  novo.
- **Scripts:** `scripts/check_local_images.py` compara o 503 do nginx;
  `scripts/vps_question_load.py` só imprime o código administrativo (já em
  maiúsculas) e não muda.
- **VPS:** a troca em `infra/vps-production/nginx.conf` só vale após um deploy
  manual, fora desta spec. Até lá, o nginx em produção responde `unavailable` em
  minúsculas; nenhum consumidor lê esse código.

## Capabilities

### New Capabilities

- `backend-http-boundaries`: política de rotas declarada por funcionalidade,
  contrato de erro HTTP, fronteiras entre web e infraestrutura e DTOs web próprios.

### Modified Capabilities

- `user-auth`: o requisito "Consulta privada da própria identidade" passa a fixar
  `401 {"error":"UNAUTHENTICATED"}` para `GET /api/v1/me` sem sessão válida, em vez
  de `unauthenticated`. As demais specs vigentes (`administrative-*`,
  `parameterized-questions`, `subject-performance`, `study-experience` etc.) não fixam
  códigos de erro em minúsculas nem o 500 da geração desabilitada; as que citam
  503/500 o fazem por causa (indisponibilidade ou integridade), o que esta spec mantém.

## Impact

- Backend: `platform`, `identity`, `mockexam`, `questionbank`, `subjectreview`,
  `shared` (infraestrutura e web); `AttemptUseCases` (exceção nomeada).
- Testes: matriz de rotas nova, teste de cobertura de políticas, ArchUnit, testes
  HTTP com códigos atualizados, `ParameterizedOperationalFixture` (500 → 503).
- Frontend: `attempt-api.ts` (código no erro), `AttemptComposer.tsx` (mensagem) e
  dublês de teste.
- Infra: `infra/local-app/nginx.conf`, `infra/vps-production/nginx.conf`,
  `scripts/check_local_images.py`.
- Docs: `docs/architecture/http-endpoint-review.md` e refinamento no ADR 0001.
- Specs vigentes: delta em `user-auth` (código do 401 de `GET /api/v1/me`).
- Sem migração Flyway nem mudança de schema. Sessões JDBC existentes deixam de
  autenticar (nova entrada necessária); nenhum dado de estudo é apagado.
- Áreas críticas tocadas: autenticação, sessão, CSRF/CORS e contratos de erro.

## Fora do escopo

- Formatador, lint, Postgres singleton e configuração de testes (spec de qualidade de
  build/testes, outra sessão). O formatador em massa conflitaria com quase todos os
  arquivos desta spec: as duas implementações não devem correr em paralelo, e a
  ordem fica com o usuário.
- `spec-review.yml` e `scripts/check_spec_review.py` (spec
  `remove-legacy-review-gate`, #256).
- Mudar rotas, métodos, regras de acesso, cache ou corpos de sucesso; novos
  endpoints; OpenAPI; deploy.
