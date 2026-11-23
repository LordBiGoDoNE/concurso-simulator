# repository-ci-checks Specification

## Purpose
Manter as verificações de CI do repositório verificando sem certificar revisão: todo `scripts/tests` roda no workflow de aplicação, o gate legado de revisão (`spec-review.json` + digest) não existe mais, os relatórios históricos são preservados e rulesets/checks obrigatórios só mudam com aprovação específica do usuário.

## Requirements

### Requirement: Testes de scripts no CI de aplicação

O workflow de aplicação (`.github/workflows/application.yml`) SHALL executar
`python3 -m unittest discover -s scripts/tests -v` em todo pull request e nos
pushes já cobertos pelo workflow, num job cujo nome não se confunda com
certificação de revisão.

#### Scenario: PR qualquer executa todos os testes de scripts/tests

- **WHEN** um pull request é aberto, reaberto ou recebe novos commits, qualquer que seja a base
- **THEN** o job `scripts-tests` executa todos os módulos de `scripts/tests`
- **AND** uma falha em qualquer módulo deixa o job vermelho

#### Scenario: Remoção do gate não abre janela sem testes

- **WHEN** o workflow legado de revisão é removido
- **THEN** o mesmo PR já contém o job `scripts-tests` e mostra o job verde antes do merge

### Requirement: Ausência do gate legado de revisão

O repositório SHALL NOT conter o workflow `spec-review.yml`, o script
`scripts/check_spec_review.py` nem testes do gate, e o CI SHALL NOT publicar os
checks `spec-review` ou `spec-review-tests`.

#### Scenario: PR depois da remoção

- **WHEN** um pull request é aberto depois do merge desta mudança
- **THEN** nenhum check `spec-review` ou `spec-review-tests` é publicado
- **AND** nenhum workflow lê `spec-review.json` nem calcula digest de revisão

#### Scenario: Evidência vigente de revisão

- **WHEN** o template de PR pede a evidência de integração de spec
- **THEN** ele aponta o `review.md` da spec e as regras globais de revisão
- **AND** não pede `spec-review.json`, digest nem o check `spec-review`

### Requirement: Preservação da evidência histórica

A mudança SHALL preservar sem alteração os relatórios `spec-review.json`,
`spec-review.md` e `reviews/**` das specs anteriores, abertas ou arquivadas. A
documentação histórica SHALL indicar como obter o script removido pelo histórico Git.

#### Scenario: Relatórios antigos intactos

- **WHEN** se compara a épica com a `main` de origem
- **THEN** nenhum arquivo `openspec/changes/**/spec-review.*` ou `openspec/changes/**/reviews/**` aparece no diff

#### Scenario: Recalcular digest histórico

- **WHEN** alguém precisa conferir um digest registrado num relatório antigo
- **THEN** `docs/architecture/spec-review.md` indica um commit anterior à remoção do qual obter `scripts/check_spec_review.py`

### Requirement: Checks obrigatórios e rulesets mudam só com aprovação específica

Alterações em rulesets, proteção de branch ou checks obrigatórios do GitHub SHALL
ocorrer somente com aprovação específica do usuário para aquela alteração. A
confirmação de início da spec e a aprovação do pacote não cobrem essa aprovação.
Antes de cada PR e do merge final, o estado das regras SHALL ser conferido em modo
somente leitura.

#### Scenario: Regra exige check removido

- **WHEN** a consulta encontra ruleset ou proteção de um alvo existente que exige `spec-review` ou `spec-review-tests`
- **THEN** a implementação para e pede ao usuário aprovação para trocar o check obrigatório por `scripts-tests`
- **AND** nenhuma regra é relaxada ou alterada sem essa aprovação

#### Scenario: Ruleset órfão sem aprovação

- **WHEN** o usuário não aprova especificamente desativar ou remover o ruleset `24766642`
- **THEN** o ruleset permanece como está
- **AND** o `AGENTS.md` o registra no Histórico como órfão, com alvo e check exigido

#### Scenario: Ruleset órfão com aprovação

- **WHEN** o usuário aprova especificamente desativar ou remover o ruleset `24766642`
- **THEN** a ação é feita depois do merge em `main`, com a resposta da API antes e depois registrada na issue e no `review.md`
