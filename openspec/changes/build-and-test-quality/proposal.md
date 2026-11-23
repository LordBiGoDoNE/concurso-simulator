# Proposal

Issue: [#257](https://github.com/LordBiGoDoNE/concurso-simulator/issues/257).
Épica: `epic/build-and-test-quality`, criada da `main` `9dea050` em 2026-11-23.

## Why

A avaliação geral do projeto feita em 2026-11-23, aprovada pelo usuário como plano
(bloco D, prioridade média), encontrou lacunas de qualidade de build e testes que
não aparecem como defeito hoje, mas tornam cada mudança mais cara de revisar e mais
fácil de quebrar:

- **Backend** (`backend/build.gradle.kts`): nenhum formatador nem `-Xlint`; nenhum
  relatório de cobertura; a versão do Spring Boot (4.1.1) repetida no plugin e no
  `platform(...)`; `spring-boot-starter-jdbc` declarado ao lado de `data-jpa` e
  `session-jdbc`, que já o trazem (o único uso de `JdbcTemplate` no código principal
  é `JdbcReadinessProbe`); nove tarefas `JavaExec` operacionais soltas no fim do
  arquivo, com o mesmo padrão repetido.
- **Testes do backend**: 52 ocorrências literais de `postgres:18.6`; 41 classes com
  `@Testcontainers` sobem cada uma o seu contêiner, o que domina o tempo de
  `./gradlew build`.
- **Relógio**: `Clock.systemUTC()` criado em seis classes de configuração e em
  `AdministrativeRecovery`, sem um bean único que um teste possa substituir.
- **Frontend** (`frontend/`): nenhum ESLint nem Prettier; `tsconfig.json` inclui só
  `src`, deixando `e2e/`, `test/` e os sete `playwright.*.config.ts` fora do `tsc`;
  nenhuma cobertura do Vitest; o mesmo `request()` (AbortController, timeout,
  corrida de abort) escrito três vezes em `attempt-api.ts`, `question-api.ts` e
  `identity-api.ts`, com validadores duplicados e a URL base resolvida em três
  arquivos e em `App.tsx`; `App.tsx` consulta `/api/v1/status` sem validar o
  contrato; linhas de até 890 caracteres em 19 arquivos de `src`.
- **CI** (`.github/workflows/`): `pages-checks.yml` repete na `main` o
  `check_links.py` do job `study-material` de `application.yml`; as suítes
  `administrative.real.spec.ts` e `topic-practice.real.spec.ts` só rodam pelos
  configs `playwright.admin.config.ts` e `playwright.topic-practice.config.ts`, que
  nenhum workflow nem script chama.

## What Changes

- Backend: BOM do Spring Boot derivado do plugin (uma só versão); remoção do starter
  redundante, confirmada por `dependencyInsight`; `-Xlint:all` com exclusões
  justificadas e `-Werror` no código principal; JaCoCo só como relatório; tarefas
  operacionais extraídas para um script Gradle próprio, com os mesmos nomes,
  grupos, descrições e argumentos.
- Backend: um bean `Clock` único, injetado nas configurações e usado pela recuperação
  administrativa.
- Testes do backend: um servidor PostgreSQL de teste compartilhado por execução, com
  um banco vazio e exclusivo por classe de teste; a imagem `postgres:18.6` declarada
  num único lugar. Testes que param, pausam ou reconfiguram o servidor continuam com
  contêiner próprio.
- Backend: Spotless com formatador Java, verificado por `./gradlew check`, aplicado em
  commit só de formatação e registrado em `.git-blame-ignore-revs`.
- Frontend: ESLint (flat config, TypeScript e regras de hooks do React), Prettier,
  `typecheck` que cobre `src`, `e2e`, `test` e os configs, e relatório de cobertura do
  Vitest sem limite mínimo.
- Frontend: um cliente HTTP único (URL base, timeout, abort, leitura de JSON) e
  validadores de contrato compartilhados; cada módulo mantém sua política de
  credenciais, mensagens e mapeamento de erros; `App.tsx` passa a validar o status.
- Frontend: formatação por Prettier em commit só de formatação e `format:check` no CI.
- CI: `lint`, `typecheck` e `format:check` no job `frontend`; remoção de
  `pages-checks.yml` da `main` (o de `stable` não muda); as suítes Playwright
  administrativa e de prática por assunto passam a rodar no CI em Chromium.

Nenhuma rota, status, corpo de resposta, migração, regra de negócio ou texto da
interface muda. A cobertura não vira gate nesta spec.

## Capabilities

### New Capabilities

- `build-quality`: verificações estáticas, relatórios de cobertura, isolamento da
  infraestrutura de testes e cobertura do CI do backend e do frontend.

### Modified Capabilities

Nenhuma.

## Impact

- `backend/build.gradle.kts`, novo `backend/gradle/operational-tasks.gradle.kts`,
  configurações em `*/infrastructure/*Configuration.java`,
  `AdministrativeRecovery.java`, classes de teste com PostgreSQL e um suporte de
  teste novo; formatação de todo `backend/src`.
- `frontend/package.json`, `package-lock.json` (gerado), configs de ESLint,
  Prettier, TypeScript e Vitest, `src/*-api.ts`, `App.tsx`, novos `src/http-client.ts`
  e `src/contract.ts`; formatação de `src`, `e2e`, `test` e configs.
- `.github/workflows/application.yml` e remoção de `.github/workflows/pages-checks.yml`
  da `main`; `.git-blame-ignore-revs`; `frontend/README.md` e `backend/README.md`.
- Áreas críticas: CI (workflows); concorrência e isolamento de testes com banco real.
  Sem autenticação, sessão, segredos, migrações ou transações de produção alteradas.

## Fora do escopo

- O gate legado `spec-review.yml`/`scripts/check_spec_review.py` (spec E, #256).
- Rotas, status e erros HTTP e fronteiras do backend (spec C, outra sessão).
- Limite mínimo de cobertura, testes de mutação e atualização de versões de stack.
- `stable`, `pages.yml`, `pages-store` e o material estático.
