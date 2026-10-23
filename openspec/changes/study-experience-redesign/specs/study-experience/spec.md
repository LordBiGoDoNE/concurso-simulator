# study-experience

## ADDED Requirements

### Requirement: Três etapas visuais distinguíveis

A experiência proposta SHALL distinguir início, configuração e realização do simulado,
com uma ação principal reconhecível em cada etapa e navegação coerente entre elas.

#### Scenario: Iniciar um treino a partir da página inicial

- **WHEN** o estudante escolhe montar um simulado
- **THEN** a composição é apresentada separadamente da página inicial
- **AND** a realização começa somente após a ação de início na composição

### Requirement: Resumo e controles de composição

A composição SHALL manter cargo, quantidade por disciplina, assuntos opcionais e
variações opcionais, com total visível e validação de 1 a 100 questões inteiras.

#### Scenario: Quantidade fora do intervalo

- **WHEN** as quantidades resultam em zero, mais de100 ou valor não inteiro
- **THEN** a ação de início fica indisponível e o motivo aparece junto ao resumo

#### Scenario: Composição em celular

- **WHEN** a interface é apresentada em largura de390px
- **THEN** campos e resumo se organizam em uma coluna, sem corte lateral
- **AND** o total e a ação de início permanecem acessíveis durante a configuração

### Requirement: Realização focada e estados explícitos

A realização SHALL priorizar enunciado, alternativas e navegação, distinguir questões
respondidas e não respondidas e confirmar a conclusão sem antecipar correção.

#### Scenario: Selecionar e limpar uma alternativa

- **WHEN** o estudante seleciona ou limpa uma alternativa
- **THEN** a seleção e o mapa de progresso refletem a escolha
- **AND** a interface não indica acerto ou gabarito durante a realização

#### Scenario: Concluir com questões não respondidas

- **WHEN** o estudante pede a conclusão
- **THEN** a confirmação apresenta quantas questões foram respondidas
- **AND** permite continuar respondendo antes da confirmação final

### Requirement: Isolamento do estudo visual

O protótipo SHALL identificar dados demonstrativos e operar sem API, banco, login,
analytics ou armazenamento persistente. Não deve representar aceite da UI de produção.

#### Scenario: Explorar as telas do protótipo

- **WHEN** o usuário configura e realiza um exemplo
- **THEN** nenhuma tentativa, resposta ou decisão editorial é escrita na aplicação
- **AND** os exemplos não representam seleção real do acervo conforme os filtros
