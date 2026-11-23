# Review — backend-http-boundaries

Issue: [#261](https://github.com/LordBiGoDoNE/concurso-simulator/issues/261). Épica: `epic/backend-http-boundaries`
(`811bc83`, criada da `main` `9dea050`; `main` `2672f9a` mesclada em `515e2ed`). Implementação autorizada em
2026-11-23; teste local pelo usuário: não.

## Cadeia de PRs

| PR | Grupo | Base | Head final |
| --- | --- | --- | --- |
| #267 | 1. Matriz de caracterização | `epic/backend-http-boundaries` | `e0a3ac6` |
| #272 | 2. Políticas de rota | `task/backend-http-boundaries/route-matrix` | `8c271a2` |
| #274 | 3. Fronteiras web/infra e sessões antigas | `task/backend-http-boundaries/route-policies` | `4c2d464` |
| #277 | 4. Contrato de erro HTTP | `task/backend-http-boundaries/boundaries` | `3fc71c0` |
| #280 | 5. DTOs web administrativos | `task/backend-http-boundaries/http-errors` | topo, com este `review.md` |

Correções de grupos inferiores foram propagadas por merge (sem rebase nem push forçado).

## Revisões por grupo

Todas pelo `group-reviewer` (Sonnet), em contexto novo, sobre o diff do grupo; correções pelo `finding-fixer` (Opus).

| Grupo | Base..head | Achados | Triagem |
| --- | --- | --- | --- |
| 1 | `811bc83..deda5bd` | M1 logout com troca de senha pendente não exercido; M2 faltavam variantes de cadeia/normalização de caminho; M3 CORS e CSRF inválido parciais | Corrigidos em `607d07b` (só testes; snapshot 590 → 1445 linhas, 588 antigas idênticas). Delta reverificado: resolvidos |
| 2 | `607d07b..f87f95d` | M1 literal `"/oauth2/authorization"` em `platform` | Corrigido em `0b254bb`. Delta reverificado (área de autenticação): resolvido |
| 3 | `0b254bb..026a774` | M1 exceção ArchUnit do `SessionController` mais larga que o `CsrfToken` | Corrigido em `e3c3fbe` (só teste, +32/−17; mutações com `CsrfTokenRepository` e `PasswordEncoder` falham). Sem nova rodada: pequena e só teste |
| 4 | `e3c3fbe..9d8ba7b` | M1 `frontend/tsconfig.tsbuildinfo` versionado; M2 advices de curadoria/importação sem `TransientFailures` | Corrigidos em `7b1d389`. Delta reverificado: resolvidos |
| 5 | `7b1d389..3c10583` | M1 snapshots sem recursos, `legalBasis`, recusas, `match`/`change` variados, origem inválida no lookup e paridade de enums | Corrigido em `0f70ee8` (só testes; 5 snapshots novos gerados em `8ebbf67`, código anterior à troca de DTOs). Delta reverificado: resolvido |

Informativos sem ação: `StatusController` e `GoogleLoginController` mantêm literais nos `@GetMapping` (adaptador
web; `ApiRouteCoverageTests` liga mapeamento e declaração); `IMPORT_UNAVAILABLE` passou a ser escolhido pela área, não
por prefixo textual (muda só para caminhos inexistentes como `/question-importsX`); o atributo de sessão antigo
continua no banco e gera WARN a cada leitura até a sessão expirar.

Achados da própria implementação, antes dos PRs:

- A queda real do banco no meio da transação chega como falha de rollback (`JpaSystemException` →
  `org.hibernate.TransactionException: Unable to rollback ... Connection is closed`), sem a falha original. O
  critério transitório passou a reconhecer falha do sistema de transação sem violação de integridade
  (`TransientFailures`, com `TransientFailuresTests`).
- Os snapshots JSON gerados no Windows tinham CRLF do pretty-printer do Jackson e falhavam no Linux (`act`);
  a comparação passou a usar LF (`b974de1`), conteúdo idêntico ignorando CR.

## CI local (`act`) e testes

- Adaptações locais: cópia do `application.yml` com `bash ./gradlew` (o NTFS não preserva o bit de execução),
  `TESTCONTAINERS_RYUK_DISABLED=true` e `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`.
- Job `backend` por grupo: 363, 372, 378, 385 e 393 testes; no topo final (`36da6b9`, com a `main` e as correções da
  revisão final), 399. Em todos, as únicas falhas são as mesmas 6 CLIs locais
  (`AdministrativeAccessPersistenceTests`, `LessonPracticePackageTests`, `TemplateLocalTests`,
  `QuestionEditorialCliTests` ×2, `QuestionLocalImportTests`), que recusam banco fora do loopback por desenho:
  efeito do host sobrescrito exigido pelo `act`. Verdes nativamente e no CI remoto.
- Não rodados no `act`: `frontend` (download do Chromium falha no container por certificado da rede local),
  `identity-browser` e `local-delivery` (substituídos pela verificação nativa abaixo), `study-material`,
  `static-pages` e `spec-review-tests` (sem mudança nessas áreas).
- Suíte nativa completa (`./gradlew test`): grupo 1 na base 361; grupo 2 371; grupo 3 377; grupo 4 384; grupo 5 392;
  grupo 4 após a revisão final 389; topo final 398. Todas verdes.
- Frontend (grupo 4, nativo): Vitest 363/363, `tsc`, `npm run build` e Playwright e2e 67/67.

## Teste de ponta a ponta

No topo da cadeia (`b974de1`):

- `npm run test:identity`: 24/24 (identidade com OIDC local, questões, tentativas e revisão por assunto, PostgreSQL
  isolado). `npm run test:parameterized`: 5/5.
- `scripts/check_local_delivery.py`: `CLEAN_DELIVERY_PASS` (imagens, build, start/stop, backup/restore, restart e
  navegador a partir da exportação versionada, com nginx). Rodado do worktree principal: a primeira tentativa, a
  partir do caminho longo do scratchpad, falhou por `Input/output error` do Docker Desktop ao ler o segredo montado
  (limitação conhecida, registrada no próprio script). Projeto Compose próprio (`concurso-simulator-app-local-check-*`);
  o ambiente local do usuário (`concurso-simulator-app-local`) não foi tocado e o volume de dados foi preservado.
- Sessão anterior à mudança de pacote do `UserPrincipal`: `LegacySessionPrincipalTests` (PostgreSQL real, bytes do
  `SecurityContext` antigo) — `/me` 401, estudo como visitante, nenhuma conta criada; controle negativo sem o
  customizador falha.
- Tarefa 5.4: `scripts/vps_question_load.py` mira a produção na VPS (loopback + segredo privado) e não foi executado
  contra nenhum servidor. A compatibilidade do cliente administrativo fica coberta pelos snapshots JSON idênticos
  gerados com o código anterior e por `test_vps_question_load.py` (7/7).
- Não verificado: nginx da VPS (`infra/vps-production/nginx.conf`) só muda após deploy manual, fora desta spec.

## CI remoto

- Antes da revisão final, todos os checks verdes (`backend`, `frontend`, `identity-browser`, `local-delivery`,
  `spec-review-tests`, `static-pages`, `study-material`) em #267 `607d07b`, #272 `0b254bb`, #274 `e3c3fbe`,
  #277 `7b1d389` e #280 `b974de1`.
- Depois do merge da `main` (que removeu o gate legado: `spec-review-tests` deu lugar a `scripts-tests`) e das
  correções: todos verdes em #267 `e0a3ac6`, #272 `8c271a2`, #274 `4c2d464`, #277 `3fc71c0` e #280 `36da6b9`.
  No #272, o `local-delivery` do evento `pull_request` falhou uma vez na etapa 1 (build das imagens) enquanto o mesmo
  SHA passava completo no evento `push`; reexecutado (instabilidade de infraestrutura, sem mudança de código).
- O commit deste `review.md` dispara nova execução no #280; o pacote confere esses checks antes da aprovação.

## Revisão final

- **Revisor:** `spec-reviewer` (Claude Opus 5.5, esforço medium), contexto novo, somente leitura.
- **Base/head:** cadeia `811bc83..b974de1` e épica `9dea050..811bc83`.
- **Resultado:** nenhum bloqueador; 1 alto, 3 médios, 7 baixos.

| Achado | Severidade | Triagem |
| --- | --- | --- |
| A1 spec vigente `user-auth` (vinda da `main` atual) fixa `{"error":"unauthenticated"}` no `/me`, sem delta | Alto | `main` mesclada na épica e propagada; delta `MODIFIED` de `user-auth` e proposta corrigida (`00d8119`) |
| M1 importação validava o JSON de entrada contra o tipo do núcleo | Médio | Validação contra `AdministrativeContracts.PackageDocument` + conversão; ArchUnit e casos 422 (`67de1bc`) |
| M2 `TransientFailures` tratava toda falha do sistema de transação como 503 | Médio | Só com conexão perdida na cadeia; testes negativos; D4 e `http-endpoint-review.md` (`741b409`) |
| M3 faltavam verificador comum de erro, teste HTTP de integridade não reconhecida e criação 201 com sessão antiga | Médio | `ErrorResponses` (testes) aplicado; `AttemptHttpTests` @Order(9); `LegacySessionPrincipalTests` (`1d45983`, `4c2d464`) |
| B1 `openapi.yaml` e `backend/docs/*.md` desatualizados | Baixo | Corrigidos (`4b83c03`) |
| B2 mudanças observáveis fora da tabela da proposta | Baixo | Registradas abaixo |
| B3 tratador final de estudo é `RuntimeException` | Baixo | Confirmado sem mudança: `ServletException` (ex.: `text/plain` → 415) segue o Spring como antes |
| B4 design D1/D8 descrevia o modelo planejado | Baixo | Alinhado ao implementado (`9c898ef`) |
| B5 snapshots "rich" registram o contrato atual | Baixo | Esclarecido: foram gerados em `8ebbf67` (código anterior à troca), num worktree temporário; os base, em `6ecff76`/`8ebbf67` |
| B6 WARN da sessão antiga se repete | Baixo | Confirmado (cerca de 3 por requisição até a sessão expirar ou nova entrada); sem efeito funcional; registrado, sem mudança |
| B7 `tasks.md` desmarcado | Baixo | Marcado (`36da6b9`); 5.4 substituída, ver "Teste de ponta a ponta" |

Ajuste de requisito no mesmo delta (`36da6b9`): "Formato único de erro HTTP" passa a valer para respostas de erro com
corpo, com cenário novo: os 403 sem corpo da segurança fora das áreas com falhas JSON continuam como antes,
coerente com "Comportamento de acesso preservado" e com a matriz.

Mudanças observáveis além da tabela da proposta (B2), todas coerentes com "503 somente para indisponibilidade
transitória": na conta administrativa e no login local, falha de banco não transitória passa de 503 a 500
`ADMIN_FAILURE`; no filtro de indisponibilidade de tentativas, falha de banco não transitória na sessão deixa de virar
503 JSON e é propagada; `TypeMismatchException` nos advices de estudo é 400 `INVALID_REQUEST` explícito (antes 400 pelo
resolvedor padrão).

### Reverificação do delta

- **Revisor:** `spec-reviewer` (Claude Opus 5.5, medium), contexto novo, somente leitura, uma rodada.
- **Delta:** `b974de1..36da6b9`, incluindo o merge da `main` `2672f9a` (conflitos só de documentação: ADR 0001 e
  `.gitignore`, resolvidos mantendo os dois lados).
- **Resultado:** A1, M1, M2, M3, B1, B4 e B7 resolvidos; o merge da `main` não alterou lógica da spec (os 217 arquivos
  só da `main` ficaram idênticos). Um baixo novo: `tasks.md` aparece inteiro no diff pela conversão CRLF→LF (sem
  mudança de conteúdo; o arquivo tinha sido gravado com CRLF no commit `811bc83`).

## Head revisado e tree SHA

Registrados no PR [SPEC] e no pacote para aprovação: o head é o commit deste `review.md` no topo da cadeia; entre ele e
o estado final da épica o único arquivo que pode mudar é este.
