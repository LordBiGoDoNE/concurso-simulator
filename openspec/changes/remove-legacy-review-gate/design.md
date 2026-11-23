# Design

## Context

- `.github/workflows/spec-review.yml` tem um único job. O nome do job é
  `spec-review` quando a base é `epic/application-reformulation` e
  `spec-review-tests` em qualquer outra base. O job roda dois passos:
  `unittest discover -s scripts/tests -v` (todos os testes, não só os do gate) e
  `scripts/check_spec_review.py` com os metadados do PR.
- `check()` (`scripts/check_spec_review.py`, ~linha 121) encerra com "Não
  aplicável" quando a base não é a épica geral. Desde o #133 (2026-10-23) nenhuma
  spec integra nessa épica, e a branch não existe mais no remoto.
- `scripts/tests/` tem 15 módulos. Só `test_spec_review.py` testa o gate. Os
  outros 14 cobrem build do Pages, aulas nativas, animações, cobertura das aulas,
  curadoria, extração e expansão de questões, artefatos de tentativa, auditoria
  parametrizada, `enrich_math`, `sketch_svg` e `review_quota`. Somente
  `test_spec_review.py` usa repositórios Git, e cria os seus próprios em diretório
  temporário. Os demais não dependem de `fetch-depth: 0`.
- `pages-checks.yml` roda só `test_build_pages.py`. O `application.yml` roda
  `scripts/test_local_app*.py` e `scripts/test_vps_*.py` (raiz de `scripts/`), mas
  não `scripts/tests`.
- Ruleset `24766642`: ativo, alvo `refs/heads/epic/application-reformulation`,
  regras `pull_request` e `required_status_checks` (`spec-review`, app `15368`,
  política estrita), sem `bypass_actors`. `main` e `stable` não têm proteção de
  branch nem regras aplicáveis. Não há rulesets de organização (a conta é pessoal).
  Consulta feita em 2026-11-23.

## Goals / Non-Goals

Goals:

- Tirar do repositório uma automação que não protege nada e que confunde a leitura
  das regras vigentes.
- Manter, sem intervalo, todo `scripts/tests` rodando no CI de todo PR.
- Preservar a evidência histórica e o histórico Git.
- Mudar regras do GitHub somente com aprovação específica do usuário.

Non-Goals:

- Reorganizar o `pages-checks.yml`, lint/format ou a seleção de testes (spec de
  qualidade de build/testes).
- Criar um novo gate automático para o `review.md`. A regra global diz que não há
  JSON de evidência nem script de digest como requisito geral.

## Decisions

### D1. Job `scripts-tests` no `application.yml`

```yaml
  scripts-tests:
    runs-on: ubuntu-latest
    timeout-minutes: 10
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.x'
      - run: python3 -m unittest discover -s scripts/tests -v
```

- Ele fica no workflow de aplicação, que já roda em todo `pull_request` e em push
  para `main`, `epic/**` e `task/**`. Hoje o `spec-review-tests` roda só em PR.
  O job novo também roda nesses pushes. Isso amplia a verificação, não a reduz.
- Usa `python-version: '3.x'`, a mesma do job atual, para não mudar o resultado
  dos testes junto com a troca de workflow. Fixar a versão é assunto da spec de
  qualidade.
- `checkout` com profundidade padrão: depois de remover `test_spec_review.py`,
  nenhum teste precisa do histórico.
- Nome `scripts-tests`, sem relação com `spec-review*`, para que ninguém leia o
  check como certificado de revisão.

Alternativa descartada: mover o passo para o `pages-checks.yml`. Esse workflow é
do material estático e está no escopo da outra spec. Misturar os dois criaria
conflito entre as specs.

### D2. Ordem: adicionar antes de remover, no mesmo PR

O grupo 1 faz as duas mudanças num único PR. Em `pull_request`, o GitHub usa os
workflows do merge ref do próprio PR. Por isso o PR já mostra `scripts-tests`
verde e deixa de publicar `spec-review-tests` antes da remoção chegar à épica.
Não há janela em que `scripts/tests` fique sem CI. Correção de 2026-11-23: a
versão anterior deste texto dizia que o PR ainda rodaria o `spec-review.yml` da
base. O run do #262 mostrou que não.

### D3. Preservação do histórico

- Não alterar nenhum `spec-review.json/md`, `reviews/**` ou arquivo em
  `openspec/changes/archive/**`. Comandos registrados neles
  (`check_spec_review.py --snapshot`) continuam verdadeiros para o commit em que
  foram executados.
- `docs/architecture/spec-review.md` ganha uma nota: o script foi removido por
  `remove-legacy-review-gate`; para recalcular um digest histórico, usar
  `git show 9dea050:scripts/check_spec_review.py` (ou qualquer commit anterior à
  remoção).
- `AGENTS.md`: a seção "Automação legada de revisão" sai das regras vigentes. No
  Histórico entra uma linha com a data da remoção, a spec e a observação de que os
  relatórios antigos são preservados. A frase "alterar ou remover essa automação
  exige PR e aprovação próprios" fica cumprida por esta spec e deixa de valer.

### D4. Template de PR

A seção "Integração de spec (somente PR `[SPEC]`)" do template passa a pedir:
link para o `review.md` da spec, head revisado e tree SHA, e confirmação de que
merge depende da aprovação do pacote. A seção não repete o processo: aponta
`~/.claude/rules/revisao-e-integracao.md` (seções 4, 5 e 7), conforme a higiene de
`precedencia-de-regras.md`. Sai a menção ao check `spec-review`.

### D5. Checks obrigatórios e ruleset órfão

- Antes de abrir cada PR e de novo antes do merge `[SPEC]`, consultar em modo
  somente leitura `rulesets`, `rules/branches/main`, `rules/branches/stable` e a
  proteção de `main`/`stable`. Se alguma regra passar a exigir `spec-review-tests`
  ou `spec-review` num alvo que exista, parar. A troca para `scripts-tests` só
  acontece com aprovação específica do usuário.
- Ruleset `24766642`: depois da remoção, o check `spec-review` deixa de existir.
  Se alguém recriar `epic/application-reformulation`, o ruleset bloqueia o merge
  nela. O efeito é mais restritivo, nunca mais permissivo. Por isso, mantê-lo não
  abre brecha. Desativá-lo ou removê-lo é relaxar uma proteção (regra global) e
  exige aprovação específica, separada da confirmação de início e da aprovação do
  pacote. Opções, da menos para a mais destrutiva:
  1. manter como está e registrá-lo como órfão no Histórico do `AGENTS.md`;
  2. desativar (`enforcement: disabled`), que é reversível e preserva a configuração;
  3. remover, com a configuração (JSON da API) registrada no PR/issue antes.
- Sem aprovação, vale a opção 1. Qualquer ação aprovada é feita pela API do GitHub
  depois do merge `[SPEC]` em `main`, quando o workflow já não existe. Ela é
  registrada na issue e no `review.md`, com a resposta da API antes e depois.

### D6. Regras e checagem de conflitos

A mudança no `AGENTS.md` dispara o `rules-conflict-checker` antes do commit do
grupo 2 (`precedencia-de-regras.md`). Conflitos são resolvidos no mesmo grupo.
Se exigirem exceção, ela vai ao usuário.

## Risks / Trade-offs

- **Um teste de `scripts/tests` falha no job novo por diferença de ambiente.**
  Mitigação: mesmo runner e mesma versão de Python do job atual. O PR do grupo 1
  mostra os dois resultados lado a lado.
- **Uma spec antiga, ainda aberta, cita `check_spec_review.py` nas tarefas.**
  Esses artefatos registram o processo da época (Histórico do `AGENTS.md`). A
  execução restante segue as regras globais, que não exigem o script.
- **Duplicação de `test_build_pages.py`** (roda no `pages-checks.yml` e no job
  novo). A duplicação já existe hoje e o custo é pequeno. A outra spec trata disso.
- **Desaparecimento do check `spec-review-tests` em PRs abertos.** Não há PR aberto
  em 2026-11-23. PRs abertos depois do merge passam a mostrar `scripts-tests`.

## Migration Plan

1. Grupo 1 (CI) e grupo 2 (docs/regras), PRs em rascunho para a épica.
2. Revisões, pacote, aprovação e merge `[SPEC]` conforme as regras globais.
3. Grupo 3 (ruleset), somente se aprovado especificamente, depois do merge em `main`.

Rollback: reverter o merge commit restaura o workflow e o script. O ruleset, se
desativado, volta com `enforcement: active`.

## Open Questions

- Decisão do usuário sobre o ruleset `24766642` (manter, desativar ou remover).
