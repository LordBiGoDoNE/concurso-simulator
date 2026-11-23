# Proposal

Issue: [#285](https://github.com/LordBiGoDoNE/concurso-simulator/issues/285).
Épica: `epic/repository-hygiene-guards`, criada da `main` `a69b8e9` em 2026-11-23.

## Why

Uma revisão do projeto em 2026-11-23 encontrou pendências acumuladas: 14 changes
OpenSpec entregues e nunca arquivadas, documentação de estado defasada depois do
lançamento na VPS e PRs, issues e branches esquecidos. Nada no CI apontava esses
problemas, e eles só apareceram numa revisão manual.

O usuário aprovou um plano em duas camadas (`~/.claude/rules/pendencias-e-encerramento.md`):

1. um guard global do Claude Code (`~/.claude/hooks/hygiene-guard.js`, feito fora
   deste repositório), que no início da sessão lê o check-run `openspec` do head
   da `main` e a issue `[PENDÊNCIAS] Relatório semanal`, quando existem, e senão
   faz uma checagem local equivalente;
2. o CI deste projeto, que é esta spec.

Estado conferido em 2026-11-23 (somente leitura):

- `openspec/changes/` na `main` contém só `archive/` (25 changes arquivadas).
  `openspec validate --all` passa (32 itens).
- Nenhum workflow roda `openspec validate` nem verifica o ciclo de vida das changes.
- 10 pastas de `openspec/changes/archive/` não são citadas em
  `docs/application-reformulation.md` como nome isolado: `backend-http-boundaries`,
  `build-and-test-quality`, `local-only-delivery-scope`, `optional-google-identity`,
  `pages-stable-epic-sync`, `pages-stable-migration`, `parameterized-questions`,
  `question-bank-expansion` (só aparece dentro de `question-bank-expansion-curation`),
  `remove-legacy-review-gate` e `subject-review`.
- O `AGENTS.md` não tem a seção "Documentos de estado" exigida pela regra global.
- Não existe a issue `[PENDÊNCIAS] Relatório semanal` nem a label `hygiene-report`.
- `stable` não tem `application.yml`; o job novo não roda em PRs para `stable`.

## What Changes

- **Job `openspec`** no `.github/workflows/application.yml`, com esse nome exato
  (o guard global consulta `check_name=openspec`). Ele roda
  `openspec validate --all` e o novo `scripts/check_openspec_lifecycle.py`.
- **Ciclo de vida:** em PR para `main`, o script falha se uma change que já estava
  fora de `archive/` na base continua fora de `archive/` no head. O PR `[SPEC]`
  que introduz a change e o PR que a arquiva passam. PRs para épicas, tarefas e
  outras bases não são bloqueados por essa regra.
- **Registro de entregas:** nova seção "Registro de entregas" em
  `docs/application-reformulation.md`, com uma linha por pasta de
  `openspec/changes/archive/` (change, issue, `[SPEC]`, arquivamento, data,
  observação). Changes arquivadas sem entrega entram com o motivo. O script passa
  a falhar se alguma pasta de `archive/` não tiver linha no registro.
- **Documentos de estado** no `AGENTS.md`: lista dos documentos de estado,
  `Registro de entregas: docs/application-reformulation.md` e
  `Branches permanentes: main, stable, pages-store`. Checagem obrigatória pelo
  `rules-conflict-checker`.
- **Template de PR:** seção "Arquivamento (somente `chore/archive-*`)" com as
  conferências do encerramento.
- **Relatório semanal:** `scripts/hygiene_report.py` e
  `.github/workflows/hygiene-report.yml` (segunda 12:00 UTC e manual). O relatório
  mantém uma única issue `[PENDÊNCIAS] Relatório semanal` (label `hygiene-report`),
  reaberta quando há pendências e fechada quando não há. Ele só lê o restante do
  repositório: nunca fecha, apaga nem altera outros PRs, issues ou branches. A
  primeira execução que escreve no GitHub depende de autorização do usuário.

Fora do escopo:

- `delete_branch_on_merge`: apagar a branch no merge conflita com a seção 5 de
  `revisao-e-integracao.md` (branches da cadeia só são apagadas no encerramento).
- Tornar `openspec` check obrigatório, criar ruleset ou proteção de branch. Isso
  exige aprovação específica (`planejamento-e-autorizacao.md`) e fica para uma
  decisão separada do usuário.
- Ações automáticas sobre pendências (fechar issues, apagar branches, arquivar
  changes).
- O guard global em `~/.claude/hooks/`, mantido fora deste repositório.
- `stable`, Pages e previews.

## Capabilities

### New Capabilities

- `repository-hygiene`: checagens do ciclo de vida OpenSpec no CI, registro de
  entregas, documentos de estado e relatório periódico de pendências do repositório.

### Modified Capabilities

Nenhuma. `repository-ci-checks` continua cobrindo `scripts-tests` e o gate legado.

## Impact

- CI: job novo `openspec` no `application.yml`; workflow novo `hygiene-report.yml`.
- Código: `scripts/check_openspec_lifecycle.py`, `scripts/hygiene_report.py` e testes
  em `scripts/tests/` (rodam no job `scripts-tests` existente).
- Docs e regras: `docs/application-reformulation.md`, `AGENTS.md`,
  `.github/pull_request_template.md`.
- GitHub: label `hygiene-report` e issue `[PENDÊNCIAS] Relatório semanal`, criadas
  na primeira execução autorizada; variável de repositório
  `HYGIENE_REPORT_ENABLED`, definida só com autorização do usuário.
- Sem mudança em backend, frontend, schema, API, conteúdo, deploy, Pages ou rulesets.
