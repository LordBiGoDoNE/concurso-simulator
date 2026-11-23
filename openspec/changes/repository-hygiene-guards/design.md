# Design

## Context

- `.github/workflows/application.yml` roda em `pull_request` (todas as bases, tipos
  padrão) e em push para `main`, `epic/**` e `task/**`. O job `scripts-tests` roda
  `python3 -m unittest discover -s scripts/tests -v`. Nenhum job valida o OpenSpec.
- O guard global (`~/.claude/hooks/hygiene-guard.js`), no início da sessão:
  - consulta `repos/<slug>/commits/<head da main>/check-runs?check_name=openspec` e
    avisa quando o run terminou com resultado diferente de `success`;
  - lê a linha `Registro de entregas:` do `AGENTS.md` e procura cada change
    arquivada (nome sem o prefixo de data) no arquivo indicado, como nome isolado
    (`-` e caracteres de palavra não contam como fronteira);
  - procura uma issue aberta cujo título começa com `[PENDÊNCIAS]`. Se ela foi
    atualizada há menos de N dias, usa o relatório; senão, varre o GitHub por conta
    própria.
- Changes arquivadas: `openspec/changes/archive/AAAA-MM-DD-<change>/`. Changes
  ativas: qualquer outra pasta direta de `openspec/changes/`.
- `stable` não tem `application.yml`. PRs para `stable` não rodam o job novo.
- A conta é pessoal; `main` não tem proteção de branch nem ruleset (consulta da
  spec `remove-legacy-review-gate`, 2026-11-23).

## Goals / Non-Goals

Goals:

- Tornar visível, no próprio PR para `main`, que existe change entregue sem arquivar.
- Manter o registro de entregas completo por verificação automática.
- Dar ao guard global as duas fontes que ele já sabe ler: o check-run `openspec` e
  a issue `[PENDÊNCIAS] Relatório semanal`.
- Não executar nenhuma ação destrutiva automaticamente.

Non-Goals:

- Exigir o check `openspec` por ruleset ou proteção de branch.
- Apagar branches no merge (`delete_branch_on_merge`).
- Fechar, arquivar ou apagar pendências automaticamente.

## Decisions

### D1. Script `scripts/check_openspec_lifecycle.py`

Somente biblioteca padrão e `git` (subprocess). Interface:

```
python3 scripts/check_openspec_lifecycle.py \
  [--base-ref origin/main --base-branch main] \
  [--head-ref HEAD] [--registry docs/application-reformulation.md]
```

- Lista as changes com `git ls-tree --name-only <ref> openspec/changes/`, na base e
  no head. Ativas: pastas diretas diferentes de `archive`. Arquivadas: pastas de
  `openspec/changes/archive/`, nome sem o prefixo `AAAA-MM-DD-`.
- **Ciclo de vida** (só quando `--base-branch` é `main`; sem `--base-ref`, a etapa
  é pulada):
  - `pendentes = ativas(base) ∩ ativas(head)`: changes que já estavam na `main`
    fora de `archive/` e continuam assim;
  - `arquivadas_no_pr = ativas(base) − ativas(head)`, com a pasta datada presente em
    `archive/` no head;
  - falha quando `pendentes` não é vazio **e** `arquivadas_no_pr` é vazio;
  - se o PR arquiva ao menos uma change, as demais pendentes geram aviso
    (`::warning::`), sem falhar. Assim os arquivamentos podem entrar um por vez, e
    cada um reduz a pendência;
  - changes novas no head (`ativas(head) − ativas(base)`) passam: é o `[SPEC]` que
    as introduz;
  - uma change que some de `ativas` sem aparecer em `archive/` (remoção sem
    arquivamento) falha, com mensagem própria.
- A decisão usa o conteúdo da árvore, não o nome da branch. Um PR
  `chore/archive-*` que não arquiva nada não ganha isenção.
- Mensagens em pt-BR, uma linha por change, com o comando a executar
  (`openspec archive <change>`, ver seção 8 de `revisao-e-integracao.md`).
- Código de saída: 0 sem falhas, 1 com falhas, 2 para erro de uso ou de `git` (ref
  inexistente). Erro de `git` nunca vira "passou".

Alternativa descartada: decidir pelo título `[SPEC]` ou pelo prefixo
`chore/archive-` da branch. Nome é declaração, não evidência; o conteúdo do PR é.

### D2. Job `openspec` no `application.yml`

```yaml
  openspec:
    runs-on: ubuntu-latest
    timeout-minutes: 10
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - uses: actions/setup-node@v4
        with:
          node-version: '24'
      - uses: actions/setup-python@v5
        with:
          python-version: '3.x'
      - name: Validate OpenSpec
        run: npx --yes @fission-ai/openspec@1.15.0 validate --all
        env:
          OPENSPEC_TELEMETRY: '0'
      - name: OpenSpec lifecycle and delivery registry
        env:
          EVENT_NAME: ${{ github.event_name }}
          BASE_BRANCH: ${{ github.base_ref }}
        run: |
          if [ "$EVENT_NAME" = "pull_request" ]; then
            python3 scripts/check_openspec_lifecycle.py \
              --base-ref "origin/$BASE_BRANCH" --base-branch "$BASE_BRANCH"
          else
            python3 scripts/check_openspec_lifecycle.py
          fi
```

- Nome do job exatamente `openspec`, sem `name:` próprio, para o check-run se chamar
  `openspec`. O guard global depende disso.
- `fetch-depth: 0` traz `origin/<base>`; o checkout do `pull_request` é o merge ref,
  que representa o que entraria na base.
- Valores de contexto passam por `env`, não interpolados no script.
- Em push (inclusive na `main`) não há base de PR: o script valida só o registro e
  emite aviso para changes ativas. Depois do merge do `[SPEC]`, a `main` tem a change
  fora de `archive/` até o PR de arquivamento; isso é esperado e não deixa o
  check-run do head da `main` vermelho. O guard global já lista changes ativas pela
  checagem local.
- Versão do OpenSpec fixada em `1.15.0`, a mesma usada para validar os artefatos.
- PR para épica ou tarefa: valida o OpenSpec e o registro; o ciclo de vida não se
  aplica.

### D3. Registro de entregas

- Seção `## Registro de entregas` em `docs/application-reformulation.md`, logo
  depois de "Estado atual", com uma tabela:

  | Change | Issue | [SPEC] | Arquivamento | Data | Observação |

  `Change` traz o nome sem prefixo de data, entre crases. `Data` é a data do
  arquivamento (prefixo da pasta). Changes de épica combinada apontam o `[SPEC]` do
  conjunto. Changes arquivadas sem entrega (`local-only-delivery-scope`,
  `pages-stable-epic-sync`, `pages-stable-migration`, a confirmar pelos artefatos)
  usam "arquivada sem entrega" e o motivo na observação. Dados não confirmados pelo
  GitHub ou pelos artefatos ficam "não localizado", nunca inventados.
- O script (D1) lê o caminho do registro na linha `Registro de entregas:` do
  `AGENTS.md` (ou de `--registry`) e falha se alguma pasta de `archive/` não tiver
  linha na tabela da seção `Registro de entregas`. A busca é mais estrita que a do
  guard global: só conta a linha da tabela cuja primeira coluna é a change.
- Registro ausente ou seção inexistente: falha com mensagem, nunca passa calado.
- O PR de arquivamento atualiza o registro no mesmo PR
  (`pendencias-e-encerramento.md`). Por isso o job falha no PR de arquivamento que
  esquece a linha.

### D4. Documentos de estado e template de PR

- `AGENTS.md` ganha a seção `## Documentos de estado`:
  - `docs/application-reformulation.md` (estado entregue e registro),
    `README.md` e `openspec/config.yaml`;
  - `Registro de entregas: docs/application-reformulation.md`;
  - `Branches permanentes: main, stable, pages-store`.
  A seção não repete a regra global; referencia `pendencias-e-encerramento.md`.
  Alterar o `AGENTS.md` dispara o `rules-conflict-checker` antes do commit
  (`precedencia-de-regras.md`).
- `.github/pull_request_template.md` ganha a seção "Arquivamento (somente
  `chore/archive-*`)", com caixas: diff de `openspec/specs/` conferido e registrado
  no PR; registro de entregas e documentos de estado atualizados; issue da spec a
  fechar depois do merge. A seção aponta a seção 8 de `revisao-e-integracao.md`.

### D5. Relatório semanal `scripts/hygiene_report.py`

Coleta com `gh` (JSON) e `git`; a lógica de detecção fica em funções puras que
recebem os dados já carregados, testadas com fixtures JSON.

Itens:

1. PRs em rascunho sem atualização há mais de 14 dias.
2. PRs abertos cuja base não existe mais no remoto.
3. Issues abertas `[SPEC] … — <change>` cuja change já está em `archive/` na `main`
   (mesma extração de nome do guard global).
4. Branches remotas já contidas na `main` (`git merge-base --is-ancestor`), exceto
   as permanentes do `AGENTS.md` e as que são base ou head de PR aberto.
5. Branches remotas sem PR aberto e sem commit há mais de 30 dias (exceto as
   permanentes).
6. Changes fora de `archive/` na `main`.
7. Issues `review-finding` abertas há mais de 60 dias (data de criação).

Saída:

- `--dry-run`: imprime o relatório (Markdown) e não escreve no GitHub. Em Actions,
  também o grava em `$GITHUB_STEP_SUMMARY`.
- Sem `--dry-run`: localiza a issue com a label `hygiene-report` e o título exato
  `[PENDÊNCIAS] Relatório semanal`, em qualquer estado.
  - com pendências: cria a issue (e a label, se faltar), ou atualiza o corpo e a
    reabre se estiver fechada;
  - sem pendências: se a issue estiver aberta, comenta "sem pendências" e a fecha;
    se não existir, não cria.
  - o corpo traz a data e hora UTC da geração, para que `updatedAt` mude a cada
    execução e o guard global veja o relatório como recente;
  - se houver mais de uma issue com a label e o título, usa a de menor número e
    lista as outras como pendência, sem fechá-las.
- O script nunca fecha, edita, apaga ou comenta outros PRs, issues ou branches.
- Código de saída diferente de 0 só em erro de coleta; pendências não são erro.

### D6. Workflow `.github/workflows/hygiene-report.yml`

```yaml
on:
  schedule:
    - cron: '0 12 * * 1'
  workflow_dispatch:
    inputs:
      dry-run:
        type: boolean
        default: true
permissions:
  contents: read
  pull-requests: read
  issues: write
```

- `checkout` com `fetch-depth: 0` (itens 4 e 5). `GH_TOKEN: ${{ github.token }}`.
- Habilitação: a execução agendada só escreve no GitHub quando a variável de
  repositório `HYGIENE_REPORT_ENABLED` vale `true`; sem ela, roda com `--dry-run` e
  deixa o relatório no resumo do job. Disparo manual usa o input `dry-run`
  (padrão `true`). Assim, o merge em `main` não cria a issue sozinho: a primeira
  execução real e a criação da variável dependem de autorização do usuário.
- `schedule` roda só na branch padrão; até o merge, o workflow é testado por
  `workflow_dispatch` na épica, somente com `dry-run`.

### D7. Testes

- `scripts/tests/test_openspec_lifecycle.py`: repositórios `git` temporários
  (`tempfile`), com commits de base e head simulando: `[SPEC]` que introduz change;
  PR que arquiva; PR comum com change pendente; arquivamento parcial com duas
  pendentes; remoção sem arquivamento; PR para épica; push; ref inexistente;
  registro completo, faltando linha, sem seção e sem a linha no `AGENTS.md`.
- `scripts/tests/test_hygiene_report.py`: fixtures JSON de PRs, issues e branches,
  cobrindo cada item de D5, as exclusões (permanentes, branches em PR aberto) e a
  decisão de criar, atualizar, reabrir, fechar ou não fazer nada com a issue, com o
  `gh` substituído por um dublê que registra as chamadas.
- Os dois módulos rodam no job `scripts-tests` existente, sem workflow novo para testes.

## Risks / Trade-offs

- **Bloqueio de PR não relacionado para `main`.** Um hotfix para `main` falha no
  job enquanto houver change entregue sem arquivar. É o comportamento pedido: a
  pendência aparece no PR. O job não é obrigatório; o merge continua sendo decisão
  humana. Saída: arquivar antes ou registrar a pendência no PR.
- **Merge ref desatualizado após troca de base.** Como no `scripts-tests`, trocar a
  base não roda o job de novo (lição registrada na spec `remove-legacy-review-gate`).
  Mesmo procedimento: push ou fechar/reabrir o PR antes do merge.
- **`act` sem `origin/<base>`.** Localmente o `act` pode não ter as refs remotas.
  Mitigação: rodar o job com evento `pull_request` simulado e a base disponível; o
  que não rodar fica registrado como limitação.
- **Relatório depende de token e da API.** Não roda no `act`. Verificação: testes com
  fixtures e `--dry-run` contra o repositório real com o `gh` do usuário (somente
  leitura).
- **Divergência com o guard global.** O registro do script é mais estrito que o do
  guard; um texto que satisfaz o script satisfaz o guard. O nome do check e o
  título da issue são contratos com o guard e ficam nos requisitos.

## Migration Plan

1. Grupo 1 (job e script) → PR em rascunho para a épica.
2. Grupo 2 (registro, `AGENTS.md`, template), encadeado sobre o grupo 1, porque o
   script passa a exigir o registro.
3. Grupo 3 (relatório), independente, PR para a épica.
4. Revisões, pacote, aprovação e `[SPEC]` conforme as regras globais. O próprio
   `[SPEC]` introduz `repository-hygiene-guards` como change nova e passa no job.
5. Depois do merge: arquivamento com a linha da própria change no registro;
   primeira execução real do relatório somente com autorização do usuário.

Rollback: reverter o merge commit remove o job, os scripts e o workflow. A issue
`[PENDÊNCIAS]` e a label, se criadas, ficam no GitHub e podem ser fechadas à mão.

## Open Questions

- Autorização para a primeira execução real do relatório e para definir
  `HYGIENE_REPORT_ENABLED` (depois do merge em `main`).
- Tornar `openspec` check obrigatório: fora desta spec; decisão separada.
