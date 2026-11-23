# Tasks

Issue: [#256](https://github.com/LordBiGoDoNE/concurso-simulator/issues/256).
Épica: `epic/remove-legacy-review-gate`. Spec formalizada em 2026-11-23.
Implementação autorizada em 2026-11-23 (grupos 1–3). Teste local pelo usuário:
não. Ruleset `24766642`: o usuário decidiu **manter** (2026-11-23); não há
aprovação para desativar ou remover. Merges dependem da aprovação do pacote final.

Cada grupo é um PR em rascunho para a épica. O grupo 2 parte da branch do grupo 1
e aponta para ela, porque documenta o resultado do grupo 1 (job `scripts-tests`,
arquivos removidos). Em cada grupo: testes do projeto, CI local via `act` e
revisão por grupo (`group-reviewer`) antes de abrir o PR. CI/deploy é área
crítica (`revisao-e-integracao.md`, seção 1). O grupo 3 não tem PR: é uma ação no
GitHub, condicionada à aprovação específica.

## 1. CI: testes de `scripts/tests` no `application.yml` e remoção do gate

- [x] 1.1 Conferir em modo somente leitura `rulesets`, `rules/branches/main`,
  `rules/branches/stable` e a proteção de `main`/`stable`. Se alguma regra exigir
  `spec-review` ou `spec-review-tests` num alvo existente, parar e pedir aprovação
  específica (D5).
- [x] 1.2 Adicionar o job `scripts-tests` ao `.github/workflows/application.yml`
  (D1): `checkout`, `setup-python` `3.x`, `unittest discover -s scripts/tests -v`,
  `timeout-minutes: 10`.
- [x] 1.3 Remover `.github/workflows/spec-review.yml`, `scripts/check_spec_review.py`
  e `scripts/tests/test_spec_review.py`.
- [x] 1.4 Confirmar com `git grep` que nenhum código, workflow ou teste vigente
  importa ou executa `check_spec_review`. Referências em
  `openspec/changes/**` e em docs históricos ficam como estão.
- [x] 1.5 Rodar localmente `python -m unittest discover -s scripts/tests -v` e o
  job `scripts-tests` com `act`. Registrar no PR os jobs não executados localmente.
- [x] 1.6 No PR, mostrar `scripts-tests` verde no próprio PR (D2) e confirmar que
  nenhum arquivo `spec-review.*` ou `reviews/**` está no diff.

## 2. Documentação, template de PR e regras do projeto — depende de 1

- [x] 2.1 `AGENTS.md`: tirar a seção "Automação legada de revisão" das regras
  vigentes e registrar no Histórico a remoção (data, spec, preservação dos
  relatórios antigos) e o estado do ruleset `24766642` conforme a decisão do usuário.
- [x] 2.2 Disparar o subagente `rules-conflict-checker` sobre o `AGENTS.md`
  alterado, contra as regras globais, antes do commit. Corrigir no grupo
  duplicações e referências obsoletas pequenas. Exceção nova vai ao usuário (D6).
- [x] 2.3 `docs/architecture/spec-review.md`: nota no cabeçalho histórico sobre a
  remoção e o commit de onde obter o script (D3), sem reescrever o texto histórico.
- [x] 2.4 `.github/pull_request_template.md`: seção "Integração de spec" passa a
  pedir link do `review.md`, head revisado e tree SHA, apontando as regras globais.
  Sai `spec-review.json/md`, digest e check `spec-review` (D4).
- [x] 2.5 Conferir links e referências dos arquivos alterados (`scripts/check_links.py`
  quando aplicável) e rodar `act` nos workflows afetados.

## 3. Ruleset órfão `24766642` — somente com aprovação específica

- [x] 3.1 Registrar na issue a decisão do usuário (manter, desativar ou remover)
  com data. Sem aprovação explícita para desativar ou remover, este grupo termina
  aqui com "mantido".
- [x] 3.2 Não aplicável: decisão "manter" (2026-11-23), sem ação no GitHub. Se aprovado, e somente depois do merge `[SPEC]` em `main`: salvar na
  issue o JSON atual do ruleset (`gh api repos/{owner}/{repo}/rulesets/24766642`),
  executar a ação aprovada e registrar a resposta da API e o estado final na issue.
  Se o `review.md` ainda não estiver integrado, registrar também nele.
- [ ] 3.3 Conferir de novo, somente leitura, que `main` e `stable` continuam sem
  alteração de proteção ou regras.

## Verificação final

- [x] Teste de ponta a ponta do agente (`teste-local.md`): a spec não tem fluxo de
  aplicação. A verificação é a cadeia completa com `scripts/tests` e `act` nos
  workflows `application.yml` e `pages-checks.yml`, mais o PR do grupo 1 com
  `scripts-tests` verde no GitHub.
- [ ] Repetir a consulta de regras da tarefa 1.1 antes do merge `[SPEC]` em `main`.
- [ ] Depois de cada troca de base de PR da cadeia e antes do merge, rodar de novo
  `scripts-tests` e os demais checks (re-run do run `pull_request` mais recente ou
  push na branch) e conferir que o run novo usa a base atual e está verde (D1).
