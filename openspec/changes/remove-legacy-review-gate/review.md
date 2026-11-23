# Review — remove-legacy-review-gate

Issue: [#256](https://github.com/LordBiGoDoNE/concurso-simulator/issues/256).
Evidência conforme `~/.claude/rules/revisao-e-integracao.md`, seção 7.
Teste local pelo usuário: não. Ruleset `24766642`: mantido por decisão do
usuário (2026-11-23).

## Cadeia

| PR | Branch | Base | Head |
| --- | --- | --- | --- |
| [#262](https://github.com/LordBiGoDoNE/concurso-simulator/pull/262) | `task/remove-legacy-review-gate/ci` | `epic/remove-legacy-review-gate` (`eb0b957`) | `a1c653e` |
| [#263](https://github.com/LordBiGoDoNE/concurso-simulator/pull/263) | `task/remove-legacy-review-gate/docs` | `task/remove-legacy-review-gate/ci` | `2a5ac56`, mais o commit deste `review.md` |

Épica `eb0b957`, criada da `main` `9dea050`, com só os artefatos da spec.

## Revisões por grupo

| Grupo | Base..head | Modelo | Achados | Triagem |
| --- | --- | --- | --- | --- |
| 1. CI | `eb0b957..a1c653e` | `group-reviewer` (Sonnet), contexto novo | nenhum bloqueador/alto/médio | Nota sem severidade sobre `.opencode/agents/spec-reviewer.md`: fora do escopo, o arquivo já se declara histórico |
| 2. Docs e regras | `a1c653e..d6e5d48` | `group-reviewer` (Sonnet), contexto novo | nenhum bloqueador/alto/médio | Inclusão do ADR 0001 considerada adequada e mínima |

`rules-conflict-checker` (grupo 2, `precedencia-de-regras.md`):

- Não encontrou conflito nem exceção malformada.
- Apontou R1 (referência obsoleta no ADR 0001), R2 (caminho e rótulo "Vigente" no `spec-review.md`), R3 (seções históricas no presente) e D1 (duplicação leve no `AGENTS.md`).
- Os quatro foram corrigidos em `d6e5d48`. A reverificação do delta pelo mesmo agente confirmou que estão resolvidos e que não surgiu conflito novo.

## Revisão final

- **Modelo:** `spec-reviewer`, perfil global Claude Opus 5.5, esforço medium, em contexto novo e somente leitura.
- **SHAs imutáveis:** `main` `9dea050`, épica `eb0b957`, topo `cddfe04`.
- **Diffs:** `eb0b957..cddfe04`, `9dea050..eb0b957` e o conjunto `9dea050..cddfe04`.
- **Cobertura:**
  - requisitos da spec;
  - testes de CI perdidos (nenhum: os 14 módulos restantes não dependem do gate);
  - ausência de referências executáveis ao que foi removido, inclusive em `origin/stable`;
  - preservação de `spec-review.*`, `reviews/**` e `archive/**` (nenhum aparece no diff);
  - regras vigentes e rulesets;
  - coerência da documentação.
- **Limitações declaradas pelo revisor:** sem rede nem execução. Não verificou o estado real dos rulesets, o CI remoto, o `act` nem a issue. Essas verificações ficaram com o coordenador (abaixo).

| ID | Severidade | Achado | Triagem |
| --- | --- | --- | --- |
| M1 | médio | `application.yml` usa `pull_request` com os tipos padrão. Trocar a base gera só `edited`, então `scripts-tests` não roda de novo contra a nova base. O `spec-review.yml` reagia a `edited`. D1 dizia "amplia, não reduz". | **Corrigido** pelo `finding-fixer` em `2a5ac56`: D1 exato, risco registrado, cenário ajustado ("aberto, reaberto ou recebe novos commits") e tarefa de rodar os checks de novo depois de cada troca de base. Os gatilhos não mudam, porque afetariam os jobs pesados e ficam fora do escopo. |
| B1 | baixo | ADR 0001 alterado fora do Impact da proposta | **Registrado**: correção de referência obsoleta ao processo removido, indicada pelo `rules-conflict-checker`. Nenhuma decisão arquitetural muda (PR #263). |
| B2 | baixo | Caminho histórico `.claude/agents/spec-reviewer.md` reescrito em `spec-review.md` | **Corrigido** pelo `finding-fixer` em `2a5ac56`: caminho original restaurado, com "hoje `~/.claude/agents/spec-reviewer.md`". |
| B3 | baixo | O `checkout` do `scripts-tests` não usa `persist-credentials: false`, que o job antigo usava | **Registrado**: segue o padrão dos outros jobs do `application.yml`. O token tem `contents: read`. Endurecer todos os jobs fica para a spec de qualidade de build/testes, se o usuário quiser. |
| Obs. | — | `docs/application-reformulation.md:50` ainda cita o "gate" em `spec-review.md` no presente | **Registrado**: parágrafo histórico do roadmap, fora do escopo |

Reverificação do delta `cddfe04..2a5ac56`: não executada. Não houve bloqueador nem
alto, e médio/baixo não exigem nova revisão (seção 3). O coordenador conferiu o
diff (só documentação, 4 arquivos), `openspec validate --strict` e `check_links.py`.

## Verificação do coordenador

- **Regras do GitHub** (somente leitura, 2026-11-23):
  - `rulesets`: só `24766642`, ativo, alvo `refs/heads/epic/application-reformulation`, check `spec-review`.
  - `rules/branches/main`, `rules/branches/stable` e `rules/branches/epic/remove-legacy-review-gate`: `[]`.
  - Proteção de `main`/`stable`: "Branch not protected".
  - A consulta será repetida antes do merge `[SPEC]`.
- **Testes locais em Windows (Python 3.14.7):** `unittest discover -s scripts/tests`, 124 testes. Há 2 erros de ambiente, que passam em Linux:
  - `test_question_extraction`: symlink sem privilégio (WinError 1314);
  - `test_review_quota`: o subprocesso chama `python3`.
- **`act` 0.2.88** (`catthehacker/ubuntu:act-latest`), em `a1c653e` e no topo `d6e5d48`:
  - `application.yml` / `scripts-tests`: CPython 3.15.2, 124 testes, OK;
  - `application.yml` / `study-material`: OK;
  - `pages-checks.yml` / `static-pages`: OK.
  - **Não executados localmente:** `backend`, `frontend`, `identity-browser` e `local-delivery`. A spec não os altera e eles são pesados (Gradle com PostgreSQL isolado, Playwright, Docker). Ficam cobertos só pelo CI remoto.
- **Teste de ponta a ponta:** a spec não tem fluxo de aplicação. A verificação é a cadeia completa com `act` e o CI remoto.
- **`openspec validate remove-legacy-review-gate --strict`** (1.15.0): válido em `2a5ac56`.
- **`scripts/check_links.py`:** OK (110 páginas, 1661 referências).

## CI remoto

Conferido em 2026-11-23, antes do pacote:

- **#262** (head `a1c653e`): 13 checks verdes (eventos `pull_request` e `push`).
  - `backend`, `frontend`, `identity-browser`, `local-delivery`, `scripts-tests` e `study-material`: 2 cada.
  - `static-pages`: 1.
- **#263** (head `2a5ac56`): os mesmos 13 checks, todos verdes.
- Nenhum check `spec-review` ou `spec-review-tests` publicado nos dois PRs.
- O push deste `review.md` dispara novos runs no #263. Eles serão conferidos antes da integração, e de novo depois de cada troca de base (D1).

## Head revisado e tree

- **Head revisado pela revisão final:** `cddfe04150afac63a587b19ec450cec54174c10b`.
- **Correções posteriores, sem reverificação** (médio/baixo): `2a5ac566250f97a32a9134a92e0ada4d2ef5e761`, tree `5780e03a6843781ecb002f3618d98311aeccc5ca`.
- **Estado final:** o commit deste `review.md` sobre `2a5ac56`. Entre `2a5ac56` e o estado final da épica, o único arquivo que pode mudar é este `review.md`.

## Integração na épica e merge da `main`

Aprovação do pacote pelo usuário em 2026-11-23.

- **#262:** marcado como pronto e integrado com merge commit, em `824b32b`. O tree de `824b32b` é igual ao de `a1c653e` (`2990d37`).
- **#263:**
  - A base trocou para a épica. O diff do PR mostrou só os arquivos do grupo 2 e da spec.
  - Os checks rodaram de novo depois da troca e os 13 ficaram verdes. Depois veio o merge commit `f7d0c5d`, com tree `a41aef8`, igual ao de `fbb435d`.
- **Limitação do procedimento de D1** (baixo, registrado):
  - No GitHub, um re-run reaproveita o SHA original do evento, então não testa o novo merge ref. Só um push gera um merge ref novo.
  - Neste caso, a verificação continua válida: o tree da nova base (`824b32b`) é idêntico ao da base antiga (`a1c653e`), e o run de push testou o próprio head.
  - Em trocas de base futuras, prefira um push (ou fechar e reabrir o PR) a um re-run.
- **A `main` avançou** de `9dea050` para `7862195`, com o arquivamento retroativo das changes antigas e docs pós-VPS:
  - Merge da `main` na épica, sem rebase, em `b745426`.
  - Sem conflito. O delta é idêntico a `git diff 9dea050 7862195`.
  - Testes em Windows: os mesmos 124, com os 2 erros de ambiente já descritos.
  - `act`: `scripts-tests` (124 OK), `study-material` e `static-pages` verdes.
  - `openspec validate --strict` válido e `check_links.py` OK.
- **Reverificação do delta do merge** pelo `spec-reviewer` (Opus 5.5 medium, em contexto novo, somente leitura), sobre `git diff fbb435d b745426`, excluído este arquivo:
  - Nenhum achado e nenhum conflito.
  - Os 26 `spec-review.json/md` movidos pela `main` são renames `R100`, com conteúdo inalterado.
  - `repository-ci-checks` não colide com as 29 capabilities de `openspec/specs/`.
  - Nada da `main` reintroduz o gate.
- **Observação, fora do escopo:** depois do arquivamento da `main`, alguns docs históricos citam os caminhos antigos `openspec/changes/<spec>/spec-review.*`. São eles `docs/application-reformulation.md:73`, `docs/handoffs/new-student-experience.md:162` e `docs/architecture/spec-review.md:194`. Os relatórios agora ficam em `openspec/changes/archive/2026-11-23-<spec>/`.
