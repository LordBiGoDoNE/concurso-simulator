# Revisão — build-and-test-quality

Issue: [#257](https://github.com/LordBiGoDoNE/concurso-simulator/issues/257).
Épica: `epic/build-and-test-quality` (`42c22a5`, criada da `main` `9dea050`).
Implementação autorizada em 2026-11-23; teste local pelo usuário: não.

## Cadeia de PRs

| Grupo | PR | Branch | Base | Head revisado |
| --- | --- | --- | --- | --- |
| 1 Build do backend | #265 | `task/build-and-test-quality/backend-build` | `epic/build-and-test-quality` | `8209a6f` |
| 2 Relógio único | #268 | `.../clock` | `.../backend-build` | `ba6c0e0` |
| 3 PostgreSQL de teste | #271 | `.../test-postgres` | `.../clock` | `03b0176` |
| 4 Spotless | #273 | `.../spotless` | `.../test-postgres` | `99abdd9` |
| 5 Ferramentas do frontend | #275 | `.../frontend-tooling` | `epic/build-and-test-quality` | `9a685e2` |
| 6 Cliente HTTP único | #276 | `.../http-client` | `.../frontend-tooling` | `809554f` |
| 7 Formatação do frontend | #278 | `.../frontend-format` | `.../http-client` | `76fecfe` |
| 8 CI | #279 | `.../ci` | `.../frontend-format` | `ddd3acf` |

O grupo 7 recebeu por merge a cadeia do backend (`76fecfe`). As duas cadeias criam
`.git-blame-ignore-revs`, e a resolução (união dos SHAs `096fe8d` e `1b64ba0`) já está no
head revisado. Ordem de integração: 1→2→3→4, depois 5→6→7→8.

Head revisado (topo): `ddd3acfec46793e8249b9698c1b16bec1ca6d791`, árvore
`36f872e9e15286c77fb1e3b91a3557ec2d08af0d`. Este `review.md` é o único arquivo acrescentado
depois dele.

## Revisões por grupo (`group-reviewer`, Sonnet)

| Grupo | Base → head revisado | Achados | Triagem |
| --- | --- | --- | --- |
| 1 | `42c22a5` → `9c22641`/`c3504b6` | nenhum | — |
| 2 | `backend-build` → `c7cac71` | nenhum | — |
| 3 | `clock` → `26544a3` | 1 baixo: `withCommand` perde o `fsync=off` padrão (só desempenho) | registrado, sem ação |
| 4 | `test-postgres` → `66fdc90` | nenhum | — |
| 5 | `42c22a5` → `ba15506` | 1 médio (M1): `playwright.config.ts` só incluído de forma transitiva no `tsconfig.node.json` | `finding-fixer` confirmou e corrigiu em `9a685e2` |
| 6 | `frontend-tooling` → `21c9119` | nenhum | — |
| 7 | `21c9119` → `6da92a2` | nenhum | — |
| 8 | `6da92a2` → `cd7c86b` | nenhum | — |

Nenhuma correção de grupo passou de ~100 linhas nem tocou área crítica; não houve rodada extra.

## CI remoto antes da revisão final (achados, `finding-fixer`)

- **`local-delivery` nos PRs 1–4 (origem: grupo 1).** O contexto da imagem do backend é
  filtrado por lista branca em `scripts/local_app_images.py`, que não incluía
  `backend/gradle/operational-tasks.gradle.kts`, aplicado pelo build. Confirmado
  reproduzindo o `docker build`. Correção `8209a6f`: o caminho exato entra na lista, e um
  teste prova que outro `backend/gradle/*.gradle.kts` continua excluído. Propagado por
  merge até o grupo 4.
- **`spec-review-tests` no PR 4 (origem: grupo 4).** As definições em
  `editorial/parameterized-questions/2026-10/` fixam por SHA-256 os bytes de
  `NumericGenerator.java`, e a formatação os mudou ("Proveniência divergente"). Correção
  `55318ee`: o arquivo sai do Spotless (`targetExclude`) e os bytes voltam ao original
  (blob `37cbf29`). As definições editoriais não foram alteradas, e não há outro arquivo
  formatado com bytes fixados.

## Revisão final (`spec-reviewer`, Claude Opus 5.5, esforço do perfil)

- **Base e heads:** épica `42c22a5`, `origin/main` `2672f9a`, topos `57f40d7`
  (cadeia A) e `cd7c86b` (cadeia B).
- **Cobertura:** os diffs completos das duas cadeias contra a épica, a épica contra a
  `main` e a simulação de merge entre as cadeias e a `main` (`git merge-tree`). Os
  commits gerados `096fe8d` e `1b64ba0` e o `package-lock.json` foram vistos por
  amostragem e por regra. Sem execução de testes.
- **Resultado:** 0 bloqueador, 0 alto, 1 médio, 6 baixos.

| Achado | Severidade | Triagem |
| --- | --- | --- |
| M1: exceção nominal da regra de `fetch` (`native-lesson.ts`) sem registro no design e no cenário do spec | médio | corrigido (`809554f`, D9/D10 e cenário) |
| B1: D10 descrevia `requestJson`/`interpret`; o código tem `withAbortScope`/`apiFetch` | baixo | corrigido (`809554f`) |
| B2: tarefa 2.3 desmarcada | baixo | corrigido (`ba6c0e0`, com #266) |
| B3: `tasks.md` versionado com CRLF, consistente em todas as linhas | baixo | registrado; converter agora conflitaria com todas as branches |
| B4: `TestPostgres.withCommand` perde o `fsync=off` | baixo | registrado (melhoria de desempenho possível) |
| B5: `native-lesson.ts` passa a recusar atributos que não são texto; nenhum documento real usa | baixo | registrado (endurecimento declarado no grupo 5) |
| B6: `test_build_pages.py` só roda na épica depois do merge da `main` (job `scripts-tests`) | baixo | condição da integração: merge da `main` antes do `[SPEC]` |
| B7: a chave de cache Gradle dos jobs não inclui `backend/gradle/operational-tasks.gradle.kts` | baixo | registrado (só cache) |

### Reverificação do delta (`spec-reviewer`, uma rodada)

- **Delta:**
  - cadeia A `57f40d7` → `99abdd9`: só a tarefa 2.3;
  - cadeia B, comparada com a união dos heads revisados: a resolução de
    `.git-blame-ignore-revs`, o `809554f` e a tarefa 2.3;
  - `backend` e `scripts` idênticos entre `99abdd9` e `ddd3acf`.
- **Resultado:** M1, B1, B2 e a integração resolvidos, sem bloqueador.
  `.git-blame-ignore-revs` contém exatamente os dois SHAs completos, e o blob de
  `NumericGenerator.java` é `37cbf29`.
- **Divergência anotada:** o revisor leu o `tasks.md` como LF. O coordenador confirmou
  com `git cat-file`: o blob tem CRLF em todas as linhas (125 CR em `ddd3acf`). Permanece
  o B3.

## CI local (`act`)

| Job | Resultado |
| --- | --- |
| `frontend` (grupos 5, 6, 7) | `npm ci`, `lint`, `typecheck`, `format:check` (7), `test` e `build` passaram no Linux. O `test:e2e` falha por tempo limite em conjuntos que variam a cada execução (7–8 testes, com 6 workers); a épica sem mudanças falha igual no `act`, então o e2e não é declarado verificado pelo `act` |
| `backend` | não verificado: no Windows o `act` copia o repositório sem bit de execução (`./gradlew: Permission denied`). Com `--bind`, o contêiner foi morto com código 137 enquanto outro `act` do mesmo job rodava; execuções simultâneas do mesmo job derrubam uma à outra |
| `practice-admin-browser`, `identity-browser`, `local-delivery` | não executados no `act` (Gradle + Testcontainers dentro do contêiner, mesma limitação) |

## Teste de ponta a ponta local (Windows; estado final = topos das duas cadeias + `origin/main`)

- `openspec validate build-and-test-quality --strict`: válido.
- `./gradlew clean build`: verde, 366 testes passaram e 1 foi pulado (link simbólico no
  Windows); `spotlessCheck` e `-Werror` ativos.
- `python3 -m unittest discover -s scripts/tests`: 124 testes OK no Linux (`python:3.14`).
  `test_local_app*.py`: 24 OK. No Windows, testes de `scripts/` falham por ambiente
  (links simbólicos, identidade do Git), sem relação com a spec.
- Frontend:
  - `npm run lint`, `typecheck`, `format:check` e `build`: ok;
  - Vitest 381/381;
  - `test:coverage`: instruções 90,6%, sem limite.
- Playwright:
  - padrão 67/67;
  - `test:identity` 24/24;
  - `test:parameterized` 5/5;
  - `test:admin` 1/1 (também com `CI=1`, sem `--offline`);
  - `test:topic-practice` 30/30 (Chromium e Edge).
- **Medições do grupo 3** (`gradle test --rerun-tasks`, mesma máquina): 457 s e 29
  contêineres PostgreSQL antes; 360 s e 7 depois. O commit `096fe8d` tem bytecode
  idêntico (`javap -c -p -constants` das 568 classes).

## Issues abertas durante a spec

- #264 (baixo): trocar o construtor depreciado do `Pbkdf2PasswordEncoder` com teste de
  compatibilidade dos hashes.
- #266 (baixo): `Instant.now()` sem o relógio da aplicação nas classes de segurança.

## Áreas críticas tocadas

CI (workflows: `pages-checks.yml` removido da `main`, job novo, passos de verificação) e
o empacotamento local (lista branca do contexto da imagem). Autenticação: só o
isolamento, sem mudança de parâmetros, do construtor do encoder do ADM. Sem
migrações, sessão, segredos, transações de produção ou deploy.

## Integração

A `main` avançou (`2672f9a`, spec `remove-legacy-review-gate`). Antes do `[SPEC]`:
1. fazer merge da `main` na épica (simulado sem conflito: só `application.yml` e
   `.gitignore`, mesclados automaticamente);
2. rodar os testes e o `act`;
3. reverificar o delta desse merge com o `spec-reviewer`;
4. registrar aqui.
