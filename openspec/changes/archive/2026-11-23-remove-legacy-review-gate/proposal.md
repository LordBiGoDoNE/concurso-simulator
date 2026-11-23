# Proposal

Issue: [#256](https://github.com/LordBiGoDoNE/concurso-simulator/issues/256).
Épica: `epic/remove-legacy-review-gate`, criada da `main` `9dea050` em 2026-11-23.

## Why

O `AGENTS.md` ("Automação legada de revisão") registra que `spec-review.yml` e
`scripts/check_spec_review.py` aplicam o gate estrutural (`spec-review.json` +
digest) somente à antiga `epic/application-reformulation`. A evidência vigente de
revisão é o `review.md` da spec (`revisao-e-integracao.md`, seção 7). O mesmo
arquivo exige PR e aprovação próprios para alterar essa automação. Esta spec é
esse caminho.

Estado conferido em 2026-11-23 (somente leitura):

- `check()` em `scripts/check_spec_review.py` retorna "Não aplicável" para
  qualquer base diferente de `epic/application-reformulation`. Essa branch não
  existe mais no remoto (`git ls-remote` vazio), e o `AGENTS.md` diz que ela é
  histórica e não é reutilizada.
- Em todo PR o workflow publica `spec-review-tests`, que roda
  `python3 -m unittest discover -s scripts/tests -v`. **É o único ponto do CI que
  executa todo `scripts/tests`.** O `pages-checks.yml` roda só
  `test_build_pages.py`, e o `application.yml` não roda `scripts/tests`. Remover
  o gate sem mover esses testes tiraria do CI os testes de aulas nativas,
  animações, cobertura, curadoria e expansão de questões. A issue #242 já aponta
  essa dependência.
- O ruleset `24766642` ("Spec integration requires independent review evidence",
  ativo) exige PR e o check `spec-review` (app `15368`) somente em
  `refs/heads/epic/application-reformulation`, sem bypass. `main` e `stable` não
  têm proteção de branch nem regras (`rules/branches/main` e `.../stable` vazios).
  Nenhum ruleset ou proteção exige `spec-review-tests`.
- O template de PR (`.github/pull_request_template.md`) ainda pede
  `spec-review.json/md`, digest e o check `spec-review` nos PRs `[SPEC]`.

## What Changes

- Novo job `scripts-tests` no `application.yml`, que roda
  `python3 -m unittest discover -s scripts/tests -v` em todo PR e nos pushes já
  cobertos pelo workflow. Ele entra antes de remover o gate, para que os testes
  não fiquem fora do CI em nenhum momento.
- Remover `.github/workflows/spec-review.yml`, `scripts/check_spec_review.py` e
  `scripts/tests/test_spec_review.py`. O histórico Git guarda o script para quem
  precisar recalcular um digest antigo.
- Preservar sem alteração os `spec-review.json/md` e `reviews/` das specs
  anteriores, abertas ou arquivadas, como histórico verdadeiro.
- `AGENTS.md`: a seção "Automação legada de revisão" sai das regras vigentes e
  vira uma linha no Histórico. Checagem obrigatória pelo `rules-conflict-checker`.
- `docs/architecture/spec-review.md`: nota no cabeçalho histórico dizendo que o
  workflow e o script foram removidos por esta spec, e como obter o script pelo
  histórico Git.
- Template de PR: a seção "Integração de spec" passa a apontar a evidência vigente
  (`review.md`) e as regras globais, sem repeti-las.
- Ruleset `24766642`: sem mudança **a menos que** o usuário aprove especificamente
  desativá-lo (ou removê-lo). Sem essa aprovação ele permanece como está, registrado
  como órfão. Remover o workflow não afeta nenhum PR atual, porque a branch-alvo
  não existe.

Fora do escopo:

- `pages-checks.yml` (inclusive a execução duplicada de `test_build_pages.py`),
  lint e format: spec de qualidade de build/testes, conduzida em outra sessão.
- O teste com `lesson_coverage.py --check` da #242. Esta spec cobre só a parte
  "garantir que `scripts/tests` rode no workflow de aplicação".
- Testes `scripts/test_*.py` fora de `scripts/tests` que nenhum workflow executa
  (por exemplo `scripts/test_compile_lesson_practice.py`).
- `scripts/review_quota.py` e `docs/review-quota.md` (medição encerrada, histórico).
- `.opencode/agents/spec-reviewer.md`, `docs/stacks-pilot.md` e
  `docs/implementation-batches.md`, que já se declaram históricos.
- Proteção de `main`/`stable`, deploy, Pages e previews.

## Capabilities

### New Capabilities

- `repository-ci-checks`: verificações de CI do repositório. Esta spec cobre os
  testes de `scripts/tests`, a ausência do gate legado, a preservação da evidência
  histórica e a troca de checks obrigatórios só com aprovação específica.

### Modified Capabilities

Nenhuma.

## Impact

- CI: `.github/workflows/application.yml` (job novo); `.github/workflows/spec-review.yml`
  removido. O check `spec-review-tests` deixa de aparecer nos PRs e `scripts-tests`
  passa a aparecer.
- Código: `scripts/check_spec_review.py` e `scripts/tests/test_spec_review.py` removidos.
- Docs e regras: `AGENTS.md`, `docs/architecture/spec-review.md`,
  `.github/pull_request_template.md`.
- GitHub: ruleset `24766642` só com aprovação específica. Nenhuma outra regra muda.
- Sem mudança em backend, frontend, schema, API, conteúdo, deploy ou Pages.
