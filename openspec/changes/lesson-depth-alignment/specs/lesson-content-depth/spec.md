# lesson-content-depth

## ADDED Requirements

### Requirement: Matriz de cobertura reprodutível

O projeto SHALL manter uma lista versionada dos conceitos cobrados pelas questões
de cada um dos 51 assuntos e um comando somente leitura que informa, por conceito,
se a aula do assunto o cobre, não o cobre ou se ele está fora de escopo com
justificativa.

#### Scenario: Gerar a matriz

- **WHEN** o comando de cobertura é executado na raiz do repositório
- **THEN** o relatório lista os 51 assuntos com a contagem de conceitos cobertos,
  ausentes e fora de escopo
- **AND** nenhum arquivo de aula, questão ou banco é alterado

#### Scenario: Conceito ausente sem justificativa

- **WHEN** a verificação é executada e existe conceito cobrado que a aula não cobre
  e que não tem justificativa de fora de escopo
- **THEN** a verificação falha e nomeia o assunto e o conceito

#### Scenario: Conceito sem questão de origem

- **WHEN** um conceito da lista cita uma chave de questão que não existe nos pacotes
- **THEN** a verificação falha e nomeia o conceito e a chave

### Requirement: Aula cobre o que as questões do assunto cobram

Cada aula SHALL explicar todos os conceitos da matriz do seu assunto que não
estejam marcados como fora de escopo, com definição, regra ou procedimento e ao
menos um exemplo quando o conceito for aplicado em cálculo, comando ou caso.

#### Scenario: Estado final da matriz

- **WHEN** a verificação de cobertura roda sobre a entrega completa
- **THEN** nenhum dos 51 assuntos tem conceito ausente sem justificativa

#### Scenario: Questão explicável pela aula

- **WHEN** uma questão do assunto é conferida contra a aula correspondente
- **THEN** o raciocínio da resolução pode ser reconstruído com o texto da aula

### Requirement: Dispositivos legais citados e conferidos

Aula que trate de conceito cobrado com base legal SHALL identificar o dispositivo
(norma, artigo e, quando cobrado, parágrafo ou inciso) e registrar nas referências
a fonte oficial consultada e a data de conferência. Transcrição literal MUST
corresponder ao texto vigente da fonte oficial nessa data.

#### Scenario: Dispositivo cobrado por questão

- **WHEN** questões de um assunto citam um dispositivo em sua base legal
- **THEN** a aula do assunto identifica esse dispositivo e explica o que ele determina

#### Scenario: Dispositivo sem fonte oficial conferível

- **WHEN** o texto de um dispositivo não pode ser conferido em fonte oficial
- **THEN** a aula não o transcreve
- **AND** a matriz registra o conceito como fora de escopo com a justificativa

### Requirement: Informática com versão de referência e comandos exatos

As aulas de Informática SHALL declarar a versão de referência (Windows 11 e
Microsoft 365 em pt-BR) e apresentar atalhos, caminhos de menu e funções cobrados
com o nome exato da interface; funções e fórmulas SHALL trazer exemplo com entrada
e resultado.

#### Scenario: Função cobrada em questão

- **WHEN** questões do assunto cobram o resultado de uma função de planilha
- **THEN** a aula apresenta a sintaxe da função e um exemplo com o resultado

#### Scenario: Código de macro cobrado em questão

- **WHEN** questões do assunto cobram a leitura de instruções de macro
- **THEN** a aula explica essas instruções com um exemplo comentado

### Requirement: Leitura rápida preservada

O aprofundamento SHALL preservar objetivos, esquema, exemplos resolvidos, resumo,
checklist e referências que a aula já tinha, e o resumo e o checklist SHALL passar
a citar o conteúdo acrescentado.

#### Scenario: Aula aprofundada

- **WHEN** uma aula alterada é aberta no leitor
- **THEN** as seções anteriores continuam presentes e legíveis
- **AND** o conteúdo novo aparece em seções identificáveis pelo título

### Requirement: Documentos nativos reprodutíveis

Os documentos nativos das aulas e seu manifesto SHALL corresponder às fontes
alteradas, e a verificação de reprodutibilidade do compilador MUST passar.

#### Scenario: Verificar reprodutibilidade

- **WHEN** a verificação do compilador de aulas é executada após as alterações
- **THEN** ela passa sem diferença entre a saída versionada e a recompilada

### Requirement: Aula não reproduz questões do banco

O conteúdo acrescentado MUST NOT copiar enunciado, alternativas ou gabarito de
questões do banco; exemplos são próprios da aula.

#### Scenario: Exemplo de um conceito cobrado

- **WHEN** a aula exemplifica um conceito cobrado por uma questão
- **THEN** o exemplo usa situação e valores diferentes dos da questão

### Requirement: Banco de questões e site estático inalterados

A entrega MUST NOT alterar questões, pacotes editoriais de questões, classificação
por assunto, schema, API, a branch `stable` ou o material publicado no Pages.

#### Scenario: Conferir o alcance da entrega

- **WHEN** o diff da épica contra a `main` é conferido
- **THEN** não há mudança em pacotes de questões, migrações, backend ou workflows
