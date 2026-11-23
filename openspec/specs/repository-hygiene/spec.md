# repository-hygiene Specification

## Purpose
Manter visível a higiene do repositório: o job `openspec` do CI valida o OpenSpec, impede que uma change entregue fique na `main` sem arquivar e exige uma linha por change arquivada no registro de entregas; o `AGENTS.md` declara os documentos de estado; e um relatório semanal lista PRs, issues, branches e changes esquecidos numa única issue, sem alterar os itens listados e só escrevendo no GitHub quando habilitado pelo usuário.

## Requirements

### Requirement: Validação OpenSpec no CI

O workflow de aplicação (`.github/workflows/application.yml`) SHALL ter um job
chamado exatamente `openspec`, executado em todo pull request e nos pushes já
cobertos pelo workflow, que roda `openspec validate --all` com a versão fixada
`@fission-ai/openspec@1.15.0` e a telemetria desligada, seguido da checagem de
ciclo de vida e do registro de entregas. O nome do check-run MUST permanecer
`openspec`, porque o guard global do Claude Code o consulta por esse nome.

#### Scenario: Artefato OpenSpec inválido

- **WHEN** um pull request contém uma spec ou change que não passa em `openspec validate --all`
- **THEN** o job `openspec` falha e o log mostra o item inválido

#### Scenario: Nome do check-run

- **WHEN** o workflow de aplicação roda num push para `main`
- **THEN** o commit recebe um check-run chamado `openspec`

### Requirement: Change entregue não fica sem arquivar

Em pull request cuja base é `main`, o job `openspec` SHALL falhar quando uma change
que já estava fora de `openspec/changes/archive/` na base continua fora de
`archive/` no head e o pull request não arquiva nenhuma change. A decisão MUST usar
o conteúdo das árvores da base e do head, não o título do PR nem o nome da branch.

#### Scenario: PR para main com change pendente

- **WHEN** a `main` tem a change `x` fora de `archive/` e um pull request para `main` não altera esse estado nem arquiva outra change
- **THEN** o job `openspec` falha e a mensagem nomeia `x` e indica o arquivamento como saída

#### Scenario: PR [SPEC] introduz a change

- **WHEN** um pull request para `main` acrescenta a change `y`, que não existia na base, e a base não tem change pendente
- **THEN** a checagem de ciclo de vida passa

#### Scenario: PR de arquivamento

- **WHEN** um pull request para `main` move a change `x` para `openspec/changes/archive/AAAA-MM-DD-x/`
- **THEN** a checagem de ciclo de vida passa
- **AND** outras changes ainda pendentes na base geram aviso, sem falha

#### Scenario: Change removida sem arquivamento

- **WHEN** um pull request para `main` remove a pasta ativa da change `x` sem criar a pasta correspondente em `archive/`
- **THEN** o job `openspec` falha e a mensagem informa que a change sumiu sem arquivamento

#### Scenario: PR para épica ou tarefa

- **WHEN** a base do pull request é uma branch `epic/**`, `task/**` ou qualquer outra diferente de `main`
- **THEN** a regra de ciclo de vida não bloqueia o pull request

#### Scenario: Push na main após o [SPEC]

- **WHEN** o merge do `[SPEC]` deixa a change fora de `archive/` na `main` até o PR de arquivamento
- **THEN** o job `openspec` do push emite aviso para a change ativa, sem falhar por ela

#### Scenario: Erro ao ler a base

- **WHEN** a ref da base não existe ou o `git` falha
- **THEN** o job falha com erro, sem relatar a checagem como aprovada

### Requirement: Registro de entregas completo

O projeto SHALL manter um registro de entregas, declarado no `AGENTS.md`, com uma
linha de tabela por pasta de `openspec/changes/archive/` (change, issue, `[SPEC]`,
arquivamento, data, observação); dados não localizados SHALL ser marcados como tal.
O job `openspec` SHALL falhar quando faltar linha para alguma pasta de `archive/`
ou quando a declaração, o arquivo ou a seção do registro faltar.

#### Scenario: Change arquivada sem linha no registro

- **WHEN** existe `openspec/changes/archive/AAAA-MM-DD-x/` e a tabela do registro não tem linha cuja primeira coluna é `x`
- **THEN** o job `openspec` falha e a mensagem nomeia `x` e o arquivo do registro

#### Scenario: PR de arquivamento atualiza o registro

- **WHEN** um PR de arquivamento move a change `x` para `archive/` e acrescenta a linha de `x` ao registro
- **THEN** o job `openspec` passa

#### Scenario: Registro não declarado

- **WHEN** o `AGENTS.md` não tem a linha `Registro de entregas:` e nenhum `--registry` é informado
- **THEN** o job `openspec` falha com mensagem que aponta a seção "Documentos de estado"

#### Scenario: Change arquivada sem entrega

- **WHEN** uma change foi arquivada sem ter sido entregue
- **THEN** a linha dela no registro diz "arquivada sem entrega" e traz o motivo

### Requirement: Documentos de estado declarados

O `AGENTS.md` SHALL ter a seção "Documentos de estado", listando os documentos que
descrevem o estado entregue, a linha `Registro de entregas: <caminho>` e a linha
`Branches permanentes: main, stable, pages-store`. O template de PR SHALL ter uma
seção para PRs de arquivamento com as conferências do encerramento.

#### Scenario: Leitura das declarações

- **WHEN** uma ferramenta lê o `AGENTS.md` da `main`
- **THEN** encontra o caminho do registro de entregas e a lista de branches permanentes em linhas próprias

#### Scenario: PR de arquivamento

- **WHEN** alguém abre um PR `chore/archive-*`
- **THEN** o template pede a conferência do diff de `openspec/specs/`, a atualização do registro e dos documentos de estado e a issue da spec a fechar

### Requirement: Itens do relatório semanal de pendências

O repositório SHALL ter um relatório de pendências, semanal e sob demanda, que
lista: rascunhos parados há mais de 14 dias; PRs sem base; issues `[SPEC]` abertas
com a change arquivada; branches contidas na `main`; branches sem PR e sem commit
há mais de 30 dias; changes ativas na `main`; issues `review-finding` com mais de
60 dias. Branches permanentes e de PR aberto SHALL ficar fora das listas.

#### Scenario: Branch permanente

- **WHEN** `main`, `stable` ou `pages-store` está contida na `main` ou sem PR há mais de 30 dias
- **THEN** ela não aparece no relatório

#### Scenario: Branch usada por PR aberto

- **WHEN** uma branch contida na `main` é base ou head de um PR aberto
- **THEN** ela não aparece como branch a apagar

### Requirement: Issue única do relatório

O relatório SHALL manter uma única issue com o título exato
`[PENDÊNCIAS] Relatório semanal` e a label `hygiene-report`. O relatório SHALL NOT
fechar, editar, comentar ou apagar outros PRs, issues ou branches.

#### Scenario: Pendências encontradas

- **WHEN** o relatório roda sem `--dry-run` e encontra pendências
- **THEN** cria a issue `[PENDÊNCIAS] Relatório semanal`, ou atualiza o corpo da existente e a reabre se estiver fechada
- **AND** o corpo traz a data e hora UTC da geração

#### Scenario: Nenhuma pendência

- **WHEN** o relatório roda sem `--dry-run` e não encontra pendências
- **THEN** fecha a issue do relatório se estiver aberta, com um comentário, e não cria issue nova

#### Scenario: Execução de verificação

- **WHEN** o relatório roda com `--dry-run`
- **THEN** imprime o relatório e não faz nenhuma escrita no GitHub

#### Scenario: Pendência sobre outro item

- **WHEN** o relatório lista uma branch, um PR ou uma issue como pendência
- **THEN** esse item não é alterado pelo relatório

### Requirement: Execução real do relatório só com autorização

O workflow do relatório SHALL ter permissões limitadas a `contents: read`,
`pull-requests: read` e `issues: write`. A execução agendada SHALL escrever no
GitHub somente quando a variável de repositório `HYGIENE_REPORT_ENABLED` valer
`true`; sem ela, SHALL rodar como verificação, sem escrita. O disparo manual SHALL
usar verificação sem escrita por padrão. A primeira execução com escrita e a
definição da variável MUST depender de autorização específica do usuário.

#### Scenario: Merge sem habilitação

- **WHEN** o workflow chega à `main` e a variável `HYGIENE_REPORT_ENABLED` não está definida
- **THEN** a execução agendada roda como verificação e não cria label nem issue

#### Scenario: Disparo manual padrão

- **WHEN** alguém dispara o workflow manualmente sem mudar o input `dry-run`
- **THEN** o relatório roda como verificação, sem escrita no GitHub
