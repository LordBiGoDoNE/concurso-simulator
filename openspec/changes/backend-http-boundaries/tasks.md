# Tasks

Issue: [#261](https://github.com/LordBiGoDoNE/concurso-simulator/issues/261).
Épica: `epic/backend-http-boundaries`. Spec formalizada em 2026-11-23.
Implementação autorizada em 2026-11-23 (grupos 1–5); teste local pelo usuário:
não. Merges dependem da aprovação do pacote final.

Cada grupo é um PR em rascunho, encadeado sobre o anterior (dependências no
design, "Plano de grupos"). Em cada grupo: testes do backend e do frontend
afetado, CI local via `act` e revisão por grupo antes de abrir o PR. Nenhuma
migração Flyway. Não implementar em paralelo com o formatador da spec de qualidade
de build/testes.

## 1. Matriz de caracterização das rotas (somente testes)

- [ ] 1.1 Inventariar todas as rotas atuais (controllers, logout, OAuth2, cadeias
  administrativas) e registrar o inventário como dado do teste.
- [ ] 1.2 Escrever o teste HTTP parametrizado da matriz (D3): visitante, conta,
  conta com troca de senha pendente, sem CSRF; status, `Cache-Control`,
  `Content-Type`, corpo de erro, CORS com origem permitida e não permitida e
  criação do cookie `SESSION`.
- [ ] 1.3 Incluir variantes: método errado, barra final, segmento vazio, rota
  vizinha inexistente sob `/api/`.
- [ ] 1.4 Confirmar a matriz verde na base da épica sem alterar código de produção.

## 2. Políticas de rota por funcionalidade — depende de 1

- [ ] 2.1 Criar o contrato `ApiRoute`/`ApiRoutePolicy` e o registro agregador em
  `shared/infrastructure/http`, com falha de inicialização para declaração duplicada.
- [ ] 2.2 Declarar as políticas em `platform`, `identity`, `questionbank`,
  `mockexam` e `subjectreview`; remover `AttemptRoutes`, `PublicQuestionRoutes`,
  `SubjectReviewRoutes` e o `Set` de isenções de `ActiveAccountFilter`.
- [ ] 2.3 Recompor `SecurityConfiguration` (matchers e CORS),
  `ApiAccessFailureHandler`, `SessionIndependentRoutesFilter` e
  `ActiveAccountFilter` a partir do registro; cadeias administrativas usam o
  registro em `securityMatcher` e CORS e mantêm seus filtros.
- [ ] 2.4 Teste de cobertura mapeamento ↔ política (D2) e teste de duplicidade.
- [ ] 2.5 Matriz do grupo 1 inalterada e verde; `http-endpoint-review.md` ganha o
  item "rota nova declara política na própria funcionalidade".

## 3. Fronteiras web ↔ infraestrutura e sessões antigas — depende de 2

- [ ] 3.1 Mover `UserPrincipal` para `identity.application` e `AdministrativeSession`
  para `identity.application.port`; atualizar consumidores.
- [ ] 3.2 Mover `GoogleLoginHandlers` e `AdministrativeGoogleCompletion` para
  `identity.infrastructure`; mover o resolvedor de `@CurrentActor` para
  `mockexam.web` sem importar a infraestrutura.
- [ ] 3.3 Desserializador de sessão tolerante a classe ausente (D7) e teste em
  PostgreSQL real com bytes do `SecurityContext` antigo: `/me` 401, visitante 201,
  nenhuma conta criada, sem 500.
- [ ] 3.4 ArchUnit genérico (D8): infraestrutura não depende de web; controllers e
  advices só em `..web..`; regra única para todos os controllers; remover as regras
  nominais cobertas.
- [ ] 3.5 Registrar no ADR 0001 o refinamento (seção "Verificação e evolução") com
  causa, regra positiva, regressão e limites.

## 4. Contrato de erro HTTP — depende de 2; encadeado sobre 3

- [ ] 4.1 Criar `shared/web/ApiErrors` e `ApiError`; substituir os records de erro
  do estudo; códigos `UPPER_SNAKE_CASE` nos advices e em `ApiAccessFailureHandler`.
- [ ] 4.2 Exceções nomeadas (D4): `ParameterizedGenerationUnavailable`,
  `InvalidRequest` web e o auxiliar `RequestValues` nos controllers de estudo.
- [ ] 4.3 Remover os tratadores de `IllegalArgumentException`/`IllegalStateException`;
  tratador final `Exception → 500 INTERNAL_ERROR` com correlação; 503 restrito ao
  conjunto transitório; integridade não reconhecida → 500.
- [ ] 4.4 Atualizar `ParameterizedOperationalFixture` (500 → 503 nomeado, sem
  gravação) e os testes HTTP com os novos códigos; verificador comum de forma de erro
  nos testes HTTP.
- [ ] 4.5 Frontend: `AttemptApiError` com o código; mensagem própria em
  `AttemptComposer` para `PARAMETERIZED_GENERATION_UNAVAILABLE`; dublês de teste e
  e2e com códigos em maiúsculas.
- [ ] 4.6 `infra/local-app/nginx.conf`, `infra/vps-production/nginx.conf` e
  `scripts/check_local_images.py` com `UNAVAILABLE`; registrar no PR que a VPS só
  muda após deploy manual, fora desta spec.
- [ ] 4.7 Documentar o contrato de erro e a tabela de status em
  `http-endpoint-review.md`.

## 5. DTOs web da administração de questões — depende de 3 e 4

- [ ] 5.1 Testes de contrato JSON (requisição e resposta) dos três controllers
  administrativos, escritos antes da mudança e verdes na base do grupo.
- [ ] 5.2 Records web e mapeadores para `AdministrativeImportController`
  (incluindo o espelho do pacote de importação), `AdministrativeQuestionController` e
  `EditorialDecisionBatchController`.
- [ ] 5.3 Regra ArchUnit: records públicos de `questionbank.web` sem componentes de
  `..domain..`/`..application..`.
- [ ] 5.4 Rodar `scripts/vps_question_load.py` contra o sistema local (carga de teste)
  para confirmar o cliente administrativo sem mudança.

## Fim da spec

- [ ] F.1 Sistema local com a cadeia completa: fluxos de estudo (visitante e conta),
  login local administrativo, importação, curadoria e lote editorial de ponta a
  ponta; sessão antiga após atualização.
- [ ] F.2 Revisão final pelo `spec-reviewer` e `review.md`.
