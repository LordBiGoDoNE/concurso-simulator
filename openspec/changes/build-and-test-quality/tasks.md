# Tasks

Issue: [#257](https://github.com/LordBiGoDoNE/concurso-simulator/issues/257).
Épica: `epic/build-and-test-quality`. Spec formalizada em 2026-11-23.
Implementação autorizada em 2026-11-23 (grupos 1–9); teste local pelo usuário: não.
Formatador Java e largura do Prettier: valem as propostas do design. Merges dependem
da aprovação do pacote final.

Cada grupo é um PR em rascunho para a épica, com código, testes e documentação juntos.
Duas cadeias independentes:

- backend: 1 → 2 → 3 → 4 (o grupo 4 formata o código já alterado por 1–3);
- frontend e CI: 5 → 6 → 7 → 8 (o grupo 6 usa o lint do 5; o grupo 7 formata o código do 6;
  o grupo 8 altera configs do Playwright e `application.yml` já formatados pelo 7).

As duas cadeias criam `.git-blame-ignore-revs` (grupos 4 e 7); na integração da segunda
cadeia, a épica entra por merge na branch do grupo 7 e o arquivo fica com as duas linhas.

Em cada grupo: testes do projeto, CI local via `act` e revisão por grupo antes de abrir
o PR. Commits de formatação, `package-lock.json` e saídas de ferramenta são
identificados no PR como gerados. Nenhum grupo muda rota, status, corpo, mensagem,
texto da interface, migração ou regra de negócio; se um grupo precisar disso, ele para
e o caso vai ao usuário.

## 1. Build do backend (~150 linhas autorais)

- [ ] 1.1 Registrar a linha de base: `./gradlew tasks --all`, `help --task` das dez
  tarefas operacionais, `dependencies --configuration runtimeClasspath`, tempo de
  `./gradlew build` e contagem de advertências com `-Xlint:all`.
- [ ] 1.2 BOM por `SpringBootPlugin.BOM_COORDINATES` (D1).
- [ ] 1.3 Conferir `spring-boot-starter-jdbc` com `dependencyInsight` e removê-lo se
  redundante, ou comentar por que fica (D2); comparar `runtimeClasspath`.
- [ ] 1.4 `-Xlint:all` com exclusões comentadas, correção das advertências e `-Werror`
  em `compileJava` (e em `compileTestJava` se zerar); restante em issue (D3).
- [ ] 1.5 JaCoCo só como relatório (D4); documentar em `backend/README.md`.
- [ ] 1.6 Extrair as tarefas operacionais para `gradle/operational-tasks.gradle.kts`
  (D5) e comparar com a linha de base.

## 2. Relógio único — depende de 1 (~80 linhas autorais)

- [ ] 2.1 `ClockConfiguration` com o bean `Clock` e injeção nas seis configurações e
  em `AdministrativeRecovery` (D6).
- [ ] 2.2 Regra ArchUnit contra `Clock.system*` fora da configuração, com teste que
  prova a falha.
- [ ] 2.3 Abrir issue `review-finding` sobre `Instant.now()` nas classes de segurança
  (limite da proteção, D6).

## 3. PostgreSQL de teste compartilhado — depende de 2 (~300 linhas autorais)

- [ ] 3.1 Levantar as classes que param, pausam ou reconfiguram o servidor e as
  fixtures `main`; registrar a lista no PR.
- [ ] 3.2 Criar `TestPostgres` (imagem única, servidor por JVM, `max_connections`,
  `newDatabase()`) e testes do próprio suporte (bancos distintos e vazios).
- [ ] 3.3 Migrar as demais classes para `newDatabase()`; as excluídas passam a usar
  `TestPostgres.IMAGE`.
- [ ] 3.4 Medir tempo de `./gradlew test` e contêineres iniciados antes/depois; rodar a
  suíte duas vezes com `--rerun-tasks`; registrar no PR.

## 4. Spotless no backend — depende de 3 (~40 linhas autorais + formatação gerada)

- [ ] 4.1 Confirmar suporte do Spotless e do palantir-java-format ao Java 25 e ao
  Gradle 9.8 e fixar versões (D8); sem suporte, parar e chamar o usuário.
- [ ] 4.2 Commit de configuração (`spotlessCheck` em `check`).
- [ ] 4.3 Commit só de `spotlessApply`; `./gradlew build` verde sem outra mudança.
- [ ] 4.4 Commit de `.git-blame-ignore-revs` com o SHA de 4.3; documentar no
  `backend/README.md`.

## 5. Ferramentas do frontend (~250 linhas autorais + lockfile)

- [x] 5.1 ESLint flat config com `typescript-eslint`, `react-hooks` e
  `eslint-config-prettier` (D9); script `lint`.
- [x] 5.2 Corrigir as violações existentes com teste, ou suprimir na linha com
  justificativa; listar as supressões no PR.
- [x] 5.3 `tsconfig.json` com `src` e `test`; `tsconfig.node.json` para `e2e/` e
  configs; `@types/node`; script `typecheck`; corrigir os erros de tipo encontrados.
- [x] 5.4 Configuração do Prettier e `.prettierignore`; script `format:check` (sem
  aplicar ainda).
- [x] 5.5 `@vitest/coverage-v8` e script `test:coverage` sem limites; `coverage/` no
  `.gitignore`.
- [x] 5.6 Passos `lint` e `typecheck` no job `frontend`; documentar em
  `frontend/README.md`.

## 6. Cliente HTTP único — depende de 5 (~350 linhas autorais)

- [x] 6.1 `src/http-client.ts` (URL base, timeout, abort, leitura de JSON, limpeza)
  com testes de timeout, abort externo antes/durante/depois do corpo, barra final e
  limpeza de timer e ouvintes (D10).
- [x] 6.2 `src/contract.ts` com os validadores idênticos; validadores de semântica
  diferente ficam distintos.
- [x] 6.3 Migrar `identity-api.ts`, `attempt-api.ts` e `question-api.ts`, preservando
  credenciais, timeouts, mensagens e `AttemptApiError`; testes atuais sem mudança de
  asserções.
- [x] 6.4 `App.tsx` usa o cliente e valida o status; teste de corpo fora do contrato.
- [x] 6.5 Regra de lint que restringe `fetch` a `http-client.ts`.

## 7. Formatação do frontend — depende de 6 (~20 linhas autorais + formatação gerada)

- [x] 7.1 Commit só de `prettier --write` em `src`, `e2e`, `test` e configs; testes,
  `lint`, `typecheck` e build verdes sem outra mudança. CSS fica fora (os testes de
  tokens leem o CSS cru); as supressões de `exhaustive-deps` voltam à linha do array em
  commit próprio.
- [x] 7.2 Acrescentar o SHA de 7.1 a `.git-blame-ignore-revs`.
- [x] 7.3 Passo `format:check` no job `frontend`.

## 8. CI sem duplicação e suítes reais — depende de 7 (~80 linhas autorais)

- [x] 8.1 Conferir se #256 já pôs `scripts/tests` em `application.yml`; se não,
  acrescentar `test_build_pages.py` ao job `study-material`. #256 já está na `main`
  (#269, job `scripts-tests`); a cobertura chega à épica com o merge da `main` exigido
  na integração.
- [x] 8.2 Remover `.github/workflows/pages-checks.yml` da `main`; confirmar que a
  cópia de `stable` não muda.
- [x] 8.3 Configs `admin` e `topic-practice` com `--offline` só fora do CI; scripts
  `test:admin` e `test:topic-practice`.
- [x] 8.4 Job `practice-admin-browser` em `application.yml`, em Chromium; rodar as duas
  suítes localmente e via `act` (o que não rodar no `act` fica registrado).
- [x] 8.5 Atualizar `frontend/README.md` e as referências a `pages-checks.yml` na
  documentação vigente (histórico permanece; só artefatos históricos o citam).

## 9. Encerramento da spec

- [ ] 9.1 Teste de ponta a ponta local com a cadeia completa: `./gradlew build`,
  `npm run lint`, `typecheck`, `format:check`, `test`, `test:coverage`, `build` e as
  suítes Playwright padrão, `identity`, `parameterized`, `admin` e `topic-practice`.
- [ ] 9.2 Revisão final pelo `spec-reviewer`, `review.md` e pacote para o usuário.
