# Revisão — repository-hygiene-guards

Issue: [#285](https://github.com/LordBiGoDoNE/concurso-simulator/issues/285).
Épica: `epic/repository-hygiene-guards` `7c3865f` (artefatos), criada da `main` `a69b8e9`.
Implementação autorizada pelo usuário em 2026-11-23 (grupos 1–3). Teste local pelo
usuário: não.

## Cadeia

| Grupo | PR | Branch | Base | Head revisado | Tree |
| --- | --- | --- | --- | --- | --- |
| 1. Ciclo de vida OpenSpec no CI | #286 | `task/repository-hygiene-guards/openspec-ci-check` | `epic/repository-hygiene-guards` | `1966eb8` | `2639520616b3` |
| 2. Registro de entregas e documentos de estado | #288 | `task/repository-hygiene-guards/delivery-registry` | branch do grupo 1 (depende de #286) | `6cc6c57` | `78412820b3fa` |
| 3. Relatório semanal | #287 | `task/repository-hygiene-guards/hygiene-report` | `epic/repository-hygiene-guards` | `e7673b6` | `82f71da638cb` |

Combinação local para a revisão do conjunto e o teste de ponta a ponta (não
publicada): `afd6ed1`, merge de `e7673b6` em `6cc6c57`, tree `8edbe4140526`.

## Revisões por grupo (`group-reviewer`, Sonnet)

| Grupo | Base → head | Achados | Triagem |
| --- | --- | --- | --- |
| 1 | `7c3865f` → `1966eb8` | Nenhum bloqueador, alto ou médio. Baixos: `npx` sem cache; `core.quotepath` em nomes não ASCII; merge ref desatualizado após troca de base (risco aceito no design) | Baixos na #289 (B4, B5); o terceiro é procedimento já previsto em `tasks.md` |
| 2 | `1966eb8` → `3302568` | **M1 (médio, documental):** o D3 previa `local-only-delivery-scope` e `pages-stable-*` como "arquivada sem entrega", mas o GitHub mostra que foram integradas (#132, #138, #135/#136; #259 com `--skip-specs`) | **Corrigido** pelo `finding-fixer` em `6cc6c57` (só documentação; validação estrita e testes OK). Correção pequena, fora de área crítica: sem rodada extra |
| 3 | `7c3865f` → `e7673b6` | Nenhum bloqueador, alto ou médio. Baixos: `is_ancestor` trata erro como "não contida"; `--limit` sem aviso; duplicada fechada conta como pendência; evidência da 3.5 (registrada no PR) | Baixos na #289 (B2, B3) |

`rules-conflict-checker` sobre o `AGENTS.md` (grupo 2): sem conflito com as regras
globais. Duas duplicações leves corrigidas no próprio grupo.

## Revisão final (`spec-reviewer`)

- Modelo: Claude Opus (perfil do `spec-reviewer`, esforço do perfil), contexto novo, somente leitura.
- Refs imutáveis: `main` `a69b8e9`, épica `7c3865f`, grupos `1966eb8`, `6cc6c57`, `e7673b6` e combinação `afd6ed1`.
- Cobertura:
  - diff do conjunto contra a épica;
  - épica contra a `main`;
  - escopo de cada PR;
  - áreas críticas: workflows, permissões, injeção de expressões, escrita no GitHub só quando habilitada, falhas que não viram aprovação;
  - contratos com `~/.claude/hooks/hygiene-guard.js`;
  - registro de entregas conferido no GitHub (37 PRs e 23 issues).
- Limitações declaradas: análise estática (o revisor não executou testes nem `act`).
- Resultado: **nenhum bloqueador, alto ou médio.** Todos os requisitos e cenários da spec cobertos. Baixos:
  - B1: a base do ciclo de vida é lida de `origin/<base>` no momento do job, e não do pai do merge ref. Num re-run antigo, isso gera falha enganosa, mas segura.
  - B2: o relatório conta como "já contida" a branch com tip igual ao da `main`, divergindo do guard; erro de `git` vira "não contida".
  - B3: títulos sem escape (`@login` pode notificar) e listas truncadas sem aviso.
- Triagem: baixos registrados na [#289](https://github.com/LordBiGoDoNE/concurso-simulator/issues/289) (`review-finding`). Sem reverificação, porque não há delta de correção.

Observação do coordenador: o revisor disse que a regex de `[SPEC]` do relatório é a
mesma do guard, mas ela difere de propósito. A regex do hook
(`.*[—–-]\s*(nome)`) é gulosa e, para `… — repository-hygiene-guards`, captura só
`guards`. O relatório exige espaço dos dois lados do separador e foi testado contra
os títulos `[SPEC]` reais. A correção do hook fica fora deste repositório.

## Verificação local do agente

- Testes:
  - `scripts/tests/test_openspec_lifecycle.py`: 24 OK;
  - `scripts/tests/test_hygiene_report.py`: 27 OK.

  No Windows, a suíte completa tem 3 erros já existentes em `test_question_extraction` (symlinks e caminhos), que não aparecem no Linux.
- Ciclo de vida contra a `main` real (`--base-ref origin/main --base-branch main`, head `afd6ed1`): sem falhas, "25 change(s) arquivada(s) com linha".
- Cenários negativos no `act`, em branches locais de teste não publicadas:
  - change válida pendente na base → job `openspec` falhou;
  - linha `subject-review` removida do registro → job `openspec` falhou.
- `act`, executado num clone local sem conversão CRLF (o `.git` de worktree não funciona no contêiner):
  - grupo 1: `openspec` em `pull_request` e em push verdes; `scripts-tests` 139 OK;
  - grupo 2: `openspec` verde; `scripts-tests` 148 OK;
  - grupo 3: `scripts-tests` 151 OK;
  - combinação `afd6ed1`: `openspec` verde (validate 33/33) e `scripts-tests` 175 OK. `act -l` lê o `hygiene-report.yml`.
- Teste de ponta a ponta: a spec não tem fluxo de aplicação. A verificação foi a cadeia completa com os scripts contra a `main` real, os cenários negativos no `act` e o `hygiene_report.py --dry-run` contra o repositório real com o `gh` do usuário, somente leitura: 0 pendências e ações previstas nenhuma, sem escrita no GitHub.
- A lógica `mentions` do guard global aplicada ao novo registro não acusa nenhuma change faltando.
- **Não verificado localmente:**
  - o job `report` do `hygiene-report.yml`, que depende de token e da API do GitHub, e o `act` não usa segredos reais;
  - `backend`, `frontend`, `identity-browser`, `practice-admin-browser`, `local-delivery` e `study-material` no `act`; esses jobs não foram alterados e passam no CI remoto.

## CI remoto

Conferido antes do pacote:

- #286 e #287: todos os checks verdes. O #286 inclui o check-run `openspec`.
- #287 não tem `openspec`, porque parte da épica, que ainda não contém o job.
- #288: estado final registrado na issue e no pacote, já que o commit deste `review.md` dispara um novo run.

## Head revisado

- Topo da cadeia revisado: `6cc6c57` (tree `78412820b3fa`); grupo 3: `e7673b6` (tree `82f71da638cb`).
- Este `review.md` é o único arquivo acrescentado depois do head revisado.
