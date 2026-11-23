# Tasks

Issue: [#285](https://github.com/LordBiGoDoNE/concurso-simulator/issues/285).
Épica: `epic/repository-hygiene-guards`. Spec formalizada em 2026-11-23.
Implementação: aguardando a confirmação única de início. Teste local pelo usuário:
a perguntar junto com a confirmação. Merges dependem da aprovação do pacote final.

Cada grupo é um PR em rascunho. O grupo 2 parte da branch do grupo 1 e aponta para
ela, porque o script do grupo 1 passa a exigir o registro que o grupo 2 completa.
O grupo 3 é independente e aponta para a épica. Em cada grupo: testes do projeto,
CI local via `act` e revisão por grupo (`group-reviewer`) antes de abrir o PR.
CI/deploy é área crítica (`revisao-e-integracao.md`, seção 1).

## 1. Ciclo de vida OpenSpec no CI

- [ ] 1.1 Criar `scripts/check_openspec_lifecycle.py` (stdlib + `git`), com a
  listagem de changes ativas e arquivadas por `git ls-tree` e a regra de ciclo de
  vida de D1: falha em PR para `main` com change pendente e sem arquivamento no PR;
  aviso para pendentes restantes quando o PR arquiva alguma; falha para remoção sem
  arquivamento; código 2 para erro de `git` ou de uso.
- [ ] 1.2 Criar `scripts/tests/test_openspec_lifecycle.py` com repositórios `git`
  temporários: `[SPEC]` que introduz change, PR de arquivamento, PR comum com change
  pendente, arquivamento parcial, remoção sem arquivamento, PR para épica, push sem
  base e ref inexistente (D7).
- [ ] 1.3 Adicionar o job `openspec` ao `.github/workflows/application.yml` (D2):
  nome exato `openspec`, `fetch-depth: 0`, `openspec validate --all` com
  `@fission-ai/openspec@1.15.0` e `OPENSPEC_TELEMETRY=0`, script com base do PR por
  `env`.
- [ ] 1.4 Verificar: `python -m unittest discover -s scripts/tests -v`; o script
  passa na `main` atual (`--base-ref origin/main --base-branch main` com head na
  épica); falha numa branch local de teste em que a base tem uma change ativa que
  o head mantém (a branch de teste não é publicada).
- [ ] 1.5 Rodar com `act` os jobs `openspec` (evento `pull_request` simulado com base
  `main`) e `scripts-tests`. Registrar no PR o que não rodou localmente.
- [ ] 1.6 No PR, mostrar o check-run `openspec` verde com esse nome exato.

## 2. Registro de entregas e documentos de estado — depende de 1

- [ ] 2.1 Criar a seção `## Registro de entregas` em `docs/application-reformulation.md`
  (D3), com uma linha para cada uma das 25 pastas de `openspec/changes/archive/`,
  inclusive as 10 hoje sem menção (`backend-http-boundaries`,
  `build-and-test-quality`, `local-only-delivery-scope`, `optional-google-identity`,
  `pages-stable-epic-sync`, `pages-stable-migration`, `parameterized-questions`,
  `question-bank-expansion`, `remove-legacy-review-gate`, `subject-review`). Issue,
  `[SPEC]` e arquivamento conferidos no GitHub e nos artefatos; o que não for
  localizado fica "não localizado". Não entregues: "arquivada sem entrega" e motivo.
- [ ] 2.2 Estender o script: ler `Registro de entregas:` do `AGENTS.md` (ou
  `--registry`) e falhar se alguma pasta de `archive/` não tiver linha na tabela da
  seção; falhar com mensagem se o registro, a seção ou a declaração faltar. Testes
  correspondentes em `test_openspec_lifecycle.py`.
- [ ] 2.3 `AGENTS.md`: seção `## Documentos de estado` (D4) com
  `docs/application-reformulation.md`, `README.md`, `openspec/config.yaml`,
  `Registro de entregas: docs/application-reformulation.md` e
  `Branches permanentes: main, stable, pages-store`, referenciando
  `pendencias-e-encerramento.md` sem repeti-la.
- [ ] 2.4 Disparar o `rules-conflict-checker` sobre o `AGENTS.md` alterado, contra as
  regras globais, antes do commit. Conflitos pequenos corrigidos no grupo; exceção
  nova vai ao usuário.
- [ ] 2.5 `.github/pull_request_template.md`: seção "Arquivamento (somente
  `chore/archive-*`)" com as três caixas de D4, apontando a seção 8 de
  `revisao-e-integracao.md`.
- [ ] 2.6 Verificar: testes; script passa no head do grupo e falha se uma linha do
  registro for removida numa cópia local; `scripts/check_links.py`; `act` nos jobs
  `openspec` e `scripts-tests`; o guard global, rodado no worktree do grupo, não
  aponta change arquivada sem linha no registro.

## 3. Relatório semanal de pendências — independente

- [ ] 3.1 Criar `scripts/hygiene_report.py` (D5): coleta com `gh`/`git`, detecção em
  funções puras para os 7 itens, exclusão das branches permanentes lidas do
  `AGENTS.md` (padrão `main, stable, pages-store` enquanto o grupo 2 não estiver
  integrado) e das branches em PR aberto.
- [ ] 3.2 Saída: `--dry-run` imprime (e grava em `$GITHUB_STEP_SUMMARY` quando
  existir); sem `--dry-run`, mantém a issue única `[PENDÊNCIAS] Relatório semanal`
  (label `hygiene-report`): cria, atualiza, reabre ou fecha conforme D5, com data e
  hora UTC no corpo. Nunca altera outros PRs, issues ou branches.
- [ ] 3.3 Criar `scripts/tests/test_hygiene_report.py` com fixtures JSON e dublê do
  `gh` (D7), cobrindo cada item, as exclusões e as decisões sobre a issue.
- [ ] 3.4 Criar `.github/workflows/hygiene-report.yml` (D6): cron `0 12 * * 1`,
  `workflow_dispatch` com `dry-run` padrão `true`, permissões `contents: read`,
  `pull-requests: read`, `issues: write`, escrita só com
  `HYGIENE_REPORT_ENABLED == 'true'`.
- [ ] 3.5 Verificar: testes; `python scripts/hygiene_report.py --dry-run` contra o
  repositório real com o `gh` do usuário (somente leitura), resultado registrado no
  PR; `act` no job `scripts-tests`. O relatório não roda no `act` (depende de token
  e da API do GitHub): limitação registrada. Opcional, só com `dry-run`: disparo
  manual na épica.

## Verificação final

- [ ] Teste de ponta a ponta do agente (`teste-local.md`): a spec não tem fluxo de
  aplicação. A verificação é a cadeia completa com `scripts/tests`, o script de
  ciclo de vida contra a `main` e `act` nos jobs `openspec` e `scripts-tests`, mais o
  `--dry-run` do relatório.
- [ ] O PR `[SPEC]` mostra `openspec` verde: a change `repository-hygiene-guards` é
  nova em relação à `main`.
- [ ] Depois de cada troca de base de PR da cadeia e antes do merge, rodar de novo os
  checks (push ou fechar/reabrir) e conferir que o run usa a base atual.
- [ ] Encerramento (seção 8): o PR de arquivamento acrescenta a linha de
  `repository-hygiene-guards` ao registro e passa no job `openspec`.
- [ ] Primeira execução real do relatório e definição de `HYGIENE_REPORT_ENABLED`:
  somente com autorização específica do usuário, depois do merge em `main`.
  Registrar na issue a execução e a issue `[PENDÊNCIAS]` criada.
