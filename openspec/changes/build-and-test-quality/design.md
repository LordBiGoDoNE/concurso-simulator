# Design

## Context

- Backend em Java 25, Spring Boot 4.1.1, Gradle Wrapper 9.8.0 (Kotlin DSL), um só
  módulo. `build.gradle.kts` declara o plugin do Boot com versão e repete a mesma
  versão em `platform("org.springframework.boot:spring-boot-dependencies:4.1.1")`.
  `compileJava` usa só `-parameters`. Não há Spotless, Checkstyle nem JaCoCo.
- 98 arquivos de teste; 41 classes `@Testcontainers` declaram cada uma um
  `@Container static PostgreSQLContainer("postgres:18.6")` e, nos testes Spring,
  apontam o datasource por `@DynamicPropertySource`. Outros testes montam
  `EntityManagerFactory` e `JdbcTemplate` à mão sobre o próprio contêiner. Algumas
  classes param o servidor de propósito (por exemplo `ConcursoSimulatorApplicationTests`,
  `AttemptHttpTests` e `ImportSessionOutageTests`), e as fixtures de navegador e
  operacionais são processos `main` com contêiner próprio; a lista exata sai do
  levantamento do grupo 3.
- `Clock.systemUTC()` aparece em `AdministrativeConfiguration`, `AttemptConfiguration`
  (três vezes), `AdministrativeCurationConfiguration` (duas),
  `AdministrativeImportConfiguration` (duas), `ReviewConfiguration` e
  `AdministrativeRecovery`. Os casos de uso já recebem `Clock` pelo construtor.
- Frontend em React 19, Vite 8, Vitest 5, TypeScript 5.9 e Playwright 1.63, sem
  ESLint nem Prettier. `tsconfig.json` tem `include: ["src"]`; os testes de `src`
  importam fixtures de `../test/`, que o `tsc` só alcança por esses imports.
- Três implementações de `request()` com o mesmo núcleo e políticas diferentes:

  | Módulo | Credenciais | Timeout | Erro |
  | --- | --- | --- | --- |
  | `identity-api.ts` | `include` | 5000 ms fixo | devolve resposta ao chamador; `readJson` opcional |
  | `attempt-api.ts` (usado por `lesson-practice-api.ts`) | `include` | 5000 ms fixo | `AttemptApiError(status, available)`; 401/403/404 sem ler corpo |
  | `question-api.ts` | `omit` | `VITE_QUESTION_TIMEOUT_MS` (padrão 5000) | `Error('Consulta de questões indisponível')` |
  | `App.tsx` (status) | padrão (`same-origin`) | `VITE_*` (padrão 5000) | qualquer falha → `down` |

- CI: `application.yml` (jobs `backend`, `frontend`, `study-material`,
  `identity-browser`, `local-delivery`), `pages-checks.yml` (na `main`, repete
  `check_links.py` e roda `test_build_pages.py`), `pages.yml` e `spec-review.yml`.
  `stable` tem cópia própria de `pages-checks.yml` e `pages.yml`. `main` e `stable` não
  têm proteção de branch nem ruleset; o único ruleset (`24766642`) mira a épica
  histórica `epic/application-reformulation` (consulta somente leitura em 2026-11-23).
- `playwright.admin.config.ts` e `playwright.topic-practice.config.ts` iniciam as
  fixtures Gradle com `--offline` e o segundo declara um projeto `msedge`; nenhum
  workflow, script npm ou script Python os chama.

## Goals / Non-Goals

Goals:

- Detectar no build e no CI erros de compilação suspeitos, formatação, hooks do React
  e tipos fora de `src`, antes da revisão humana.
- Reduzir o tempo dos testes do backend sem perder o banco vazio por classe.
- Ter um único ponto para versão do Boot, imagem do PostgreSQL, relógio, URL base e
  transporte HTTP do frontend.
- Exercitar no CI as suítes Playwright reais que hoje só rodam à mão.

Non-Goals:

- Mudar comportamento observável: rotas, status, corpos, mensagens, textos da
  interface, migrações, regras de negócio, timeouts e políticas de credenciais.
- Gate de cobertura, novas versões de stack, mudanças em `stable` ou no Pages.
- O gate legado `spec-review.yml` (spec E, #256) e rotas/erros HTTP (spec C).

## Decisions

### D1 — BOM derivado do plugin

`implementation(platform(SpringBootPlugin.BOM_COORDINATES))` substitui a coordenada
com versão literal; a versão fica só no bloco `plugins`. Alternativa descartada: plugin
`io.spring.dependency-management`, que acrescenta um mecanismo de resolução a mais sem
ganho para um projeto de um módulo.

### D2 — Starter JDBC redundante

`./gradlew dependencyInsight --dependency spring-boot-starter-jdbc` (e de
`spring-boot-jdbc`) antes e depois. Se `data-jpa` ou `session-jdbc` continuarem
trazendo o mesmo artefato na mesma versão, a linha sai. Se não, fica, com comentário
dizendo por quê. `JdbcTemplate` continua disponível em `main` e nos testes; o
classpath de `runtimeClasspath` é comparado antes/depois e a diferença registrada no PR.

### D3 — `-Xlint` e `-Werror`

`compileJava` e `compileTestJava` recebem `-Xlint:all` com exclusões explícitas no
próprio `build.gradle.kts`, cada uma com comentário (candidata: `-processing`, ruído
sem processadores próprios). As advertências existentes são corrigidas no grupo 1.
`-Werror` vale para `compileJava`; para `compileTestJava` também, se o grupo terminar
com zero advertências; senão o restante fica listado no PR e numa issue
`review-finding` vinculada à #257. Nenhum `@SuppressWarnings` amplo (classe ou pacote)
para esconder advertência.

### D4 — JaCoCo só como relatório

Plugin `jacoco` com `toolVersion` que suporte Java 25 (0.8.14 ou superior),
`jacocoTestReport` com XML e HTML em `build/reports/jacoco/`, gerado depois de `test`.
Sem `jacocoTestCoverageVerification` e sem upload no CI: o relatório é local. As
tarefas `JavaExec` não recebem o agente.

### D5 — Tarefas operacionais num script Gradle

As dez tarefas operacionais (`identityBrowserFixture`, `questionBankLocal`,
`mockExamOperationalCheck`, `parameterizationLocal`, `parameterizedOperationalCheck`,
`subjectReviewOperationalCheck`, `localDeliveryFixtureClasses`,
`administrativeRecovery`, `administrativeBrowserFixture`, `topicPracticeBrowserFixture`
— nove `JavaExec` e um `Sync`) vão para `backend/gradle/operational-tasks.gradle.kts`,
aplicado por `apply(from = ...)`, com uma função auxiliar para o padrão
"fixture de teste" (`dependsOn(testClasses)`, classpath de teste, `mainClass`). Nomes,
grupos, descrições, dependências, `standardInput` e argumentos por propriedade não
mudam. Evidência: `./gradlew tasks --all` e `./gradlew help --task <nome>` de cada uma,
antes e depois, sem diferença além da origem do script. Alternativa descartada:
`buildSrc`/plugin de convenção, que pesa mais que o problema.

### D6 — Relógio único

`platform/infrastructure/ClockConfiguration` declara `@Bean Clock clock()` com
`Clock.systemUTC()`; as configurações recebem `Clock` como parâmetro do método `@Bean`;
`AdministrativeRecovery` obtém o bean do contexto que já cria. Um teste ArchUnit proíbe
chamar as fábricas `Clock.system*` no código principal fora de `ClockConfiguration`.
Domínio e aplicação continuam sem Spring: só recebem `Clock`.

Limite da proteção: `Instant.now()` sem relógio continua em três pontos de
autenticação e sessão (`AdministrativeHttpSecurity`, `AdministrativeSecurity` e
`SecurityConfiguration`, expiração de intenções OAuth). Trocá-los mexe em área crítica
e não é necessário para os objetivos desta spec; a regra ArchUnit não os cobre e o
grupo 2 abre uma issue `review-finding` vinculada à #257 para decidir isso depois.

### D7 — PostgreSQL de teste compartilhado, banco exclusivo por classe

Novo suporte de teste (`src/test/java/.../support/TestPostgres`):

- `IMAGE = "postgres:18.6"`, única declaração da imagem, usada também pelas fixtures e
  pelos testes que param o servidor;
- um servidor por JVM de teste, iniciado sob demanda e encerrado pelo Ryuk do
  Testcontainers no fim da execução, com `max_connections` elevado (os contextos Spring
  ficam em cache e cada um mantém seu pool);
- `TestPostgres.newDatabase()` cria `CREATE DATABASE` com nome aleatório e devolve URL,
  usuário e senha; cada classe chama uma vez (campo estático), e os testes Spring o
  registram no `@DynamicPropertySource` que já têm.

Assim cada classe continua vendo um banco vazio, migrado pelo próprio Flyway, sem
estado de outra classe. Testes que param, pausam ou reconfiguram o servidor e as
fixtures que são processos `main` mantêm contêiner próprio, com `TestPostgres.IMAGE`.
Alternativa descartada: `@ServiceConnection` num contêiner compartilhado, que apontaria
todas as classes para o mesmo banco e perderia o isolamento; também não dispensa o
`@DynamicPropertySource`, que esses testes usam para outras propriedades.
Evidência: tempo de `./gradlew test` e número de contêineres iniciados, antes e
depois, no PR; a suíte inteira verde duas vezes seguidas e com ordem de classes
diferente (`--rerun-tasks`).

### D8 — Spotless com palantir-java-format

Plugin Spotless com `palantirJavaFormat()` (120 colunas, adequado a lambdas e cadeias
longas do código atual), `removeUnusedImports()`, `trimTrailingWhitespace()` e
`endWithNewline()` para `src/**/*.java`. `spotlessCheck` entra em `check`, logo em
`./gradlew build` do CI. Alternativa: `googleJavaFormat()` (100 colunas, recuo de 2),
mais distante do estilo atual. As versões do plugin e do formatador são fixadas no
grupo 4 depois de confirmar suporte à sintaxe do Java 25 e ao Gradle 9.8; sem suporte,
o grupo para e vai ao usuário. Sequência do grupo: commit da configuração; commit só
de `spotlessApply` (gerado, fora da contagem de linhas autorais); commit de
`.git-blame-ignore-revs` com o SHA do commit de formatação.

### D9 — ESLint, Prettier e typecheck no frontend

- `eslint.config.js` (flat): `@eslint/js` recomendado, `typescript-eslint`
  `recommendedTypeChecked`, `eslint-plugin-react-hooks` (`rules-of-hooks` e
  `exhaustive-deps` como erro) e `eslint-config-prettier` por último. Cada violação
  existente é corrigida quando a correção é inequívoca e coberta por teste; senão,
  supressão na linha com justificativa. Nenhuma regra desligada globalmente para
  acomodar código existente; as supressões ficam listadas no PR.
- Escopo de testes (decidido no grupo 5, com a contagem real: 58 violações em `src`,
  155 nos testes unitários e 211 no e2e): em `*.test.*`, `test/` e `e2e/` ficam
  desligadas só as regras de dados não confiáveis (`no-unsafe-*`, `no-explicit-any`),
  `require-await` (mocks `async`), `unbound-method` (mocks do Vitest) e
  `no-base-to-string` (`mock.calls` tipa a URL como `RequestInfo | URL`). No código de
  produção todas continuam ativas, e as violações de `src` foram corrigidas.
- `no-unused-vars` ignora nomes com prefixo `_` (omissão intencional em desestruturação
  e parâmetros exigidos pela assinatura).
- Restrição de lint: `fetch` só em `src/http-client.ts` (ativada no grupo 6).
- Prettier com `printWidth: 120`, `singleQuote: true`, `semi: true` e `.prettierignore`
  para JSON gerados (`src/lesson-documents/`, `lesson-catalog.json`, `lesson-paths.json`)
  e saídas de build/teste. Configuração no grupo 5; aplicação no grupo 7.
- `tsconfig.json` (app) passa a incluir `src` e `test`; novo `tsconfig.node.json` para
  `e2e/`, `playwright.*.config.ts` e `vite.config.ts`, com tipos do Node
  (`@types/node` em `devDependencies`). Script `typecheck` roda os dois; `build`
  continua com `tsc --noEmit` do app antes do Vite.
- `@vitest/coverage-v8` na mesma versão do Vitest; script `test:coverage` com
  `text-summary`, `html` e `lcov` em `frontend/coverage/` (no `.gitignore`), sem
  limites.

### D10 — Cliente HTTP único, políticas por módulo

`src/http-client.ts` concentra:

- `apiBaseUrl`: `VITE_API_URL` ou `http://localhost:8080`, sem barra final;
- `positiveTimeout(valor, padrão)`: a regra hoje repetida em `question-api.ts` e
  `App.tsx`;
- `requestJson(...)`: liga o `signal` externo, aplica o timeout, corre contra o abort
  (a promessa rejeita com `AbortError` mesmo se `fetch` ignorar o sinal), chama
  `throwIfAborted()` depois do corpo e limpa timer e ouvintes no `finally`.
  Recebe `credentials`, `method`, corpo, timeout, mensagem do `AbortError` e uma
  função `interpret(response)` que cada módulo usa para mapear status e decidir se lê
  o corpo.

`src/contract.ts` reúne os validadores idênticos. No grupo 6 a comparação mostrou que só
a regex de UUID e o acesso a campo (`field`, `isObject`) são iguais: `ensure`, `row`,
`list` e `id` diferem nas mensagens (conferidas pelos testes) ou na checagem de chaves
(`in` × `Object.hasOwn`), por isso continuam em cada módulo. Validadores com semânticas diferentes continuam distintos e com nomes que as
dizem (por exemplo, `string` de `question-api` aceita texto vazio; `text` de
`attempt-api` exige texto não vazio). Cada módulo mantém credenciais, timeout,
mensagens, `AttemptApiError` e validadores de domínio. `App.tsx` usa `src/status-api.ts`,
que passa pelo cliente e valida `{ status }`; corpo inválido continua resultando em `down`.

Evidência de comportamento preservado: os testes atuais de `attempt-api`,
`identity-api`, `lesson-practice-api` e dos componentes passam sem alteração das
asserções; testes novos do cliente cobrem timeout, abort externo antes e durante,
abort depois do corpo, barra final da URL e limpeza de timer/ouvintes.

### D11 — Formatação como commit separado

Grupos 4 e 7 aplicam o formatador num commit que só contém formatação, identificado no
PR como gerado, e registram o SHA em `.git-blame-ignore-revs`. Se a `main` avançar com
conflitos nesses arquivos (por exemplo, spec C), a resolução é tomar a versão da
`main` e reaplicar o formatador, nunca editar à mão a formatação.

### D12 — CI

- Job `frontend`: `npm run lint` e `npm run typecheck` (grupo 5) e
  `npm run format:check` (grupo 7), antes dos testes.
- Job `backend`: inalterado; `build` já inclui `check` (Spotless e `-Werror`).
- `pages-checks.yml` sai da `main` (a cópia de `stable` não muda e continua valendo
  para PRs e pushes de `stable`). Pré-condição: `test_build_pages.py` roda em
  `application.yml`. Se #256 já estiver na `main`, isso está coberto pelo
  `unittest discover -s scripts/tests`; senão, o grupo 8 acrescenta a linha ao job
  `study-material`. `check_links.py` já roda lá.
- Suítes reais: scripts `test:admin` e `test:topic-practice`; os configs passam a usar
  `--offline` só fora do CI (`process.env.CI`), preservando o uso local; no CI rodam
  com `--project chromium`, pois o runner não instala o Edge. Novo job
  `practice-admin-browser` com a mesma preparação de `identity-browser`, em paralelo.
  Portas já são distintas entre os configs.

## Risks / Trade-offs

- Servidor compartilhado e conexões: muitos contextos Spring em cache podem esgotar
  conexões → `max_connections` elevado e medição no PR; se não bastar, limitar o
  tamanho do cache de contextos de teste, nunca o pool dos testes de concorrência.
- Formatação em massa conflita com specs paralelas → D11.
- `-Werror` pode bloquear atualizações futuras de dependência por depreciação → a
  correção ou a exclusão nominal da advertência entra no PR da atualização.
- Regras de hooks podem apontar efeitos com dependências intencionalmente omitidas →
  correção só com teste; senão supressão justificada na linha.
- Job novo de navegador aumenta o tempo total do CI, mas roda em paralelo.

## Migration Plan

Cada grupo é revertível sozinho. A ordem das cadeias (1→2→3→4 e 5→6→7) deixa os
commits de formatação por último, depois das mudanças de código de cada lado. O
grupo 8 é independente.

## Open Questions

- Formatador Java (D8): palantir-java-format (proposto) ou google-java-format.
- Largura do Prettier (D9): 120 colunas (proposto).
Ambas podem ser respondidas junto com a confirmação de início; sem resposta, valem as
propostas.
