# study-experience Specification

## Purpose
Organizar a experiência de estudo em etapas visuais distintas (início, configuração e realização do simulado) e numa biblioteca de aulas navegável, com linguagem visual clara, foco na realização e nenhuma correção revelada antes da conclusão.

## Requirements

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

### Requirement: Biblioteca das aulas existentes

A biblioteca SHALL apresentar as51 aulas do mapa público versionado, agrupadas nas
cinco disciplinas, com busca por assunto/disciplina e filtros locais combináveis.

#### Scenario: Buscar um assunto sem acento

- **WHEN** o usuário procura "interpretacao" na biblioteca
- **THEN** encontra a aula de Interpretação de Textos
- **AND** os resultados respeitam a disciplina selecionada

#### Scenario: Busca sem correspondência

- **WHEN** busca e filtro não encontram aula
- **THEN** a interface apresenta estado vazio e permite limpar busca/filtros

### Requirement: Leitura e ligação com a prática

O estudo visual SHALL incluir uma prévia textual identificada, índice da disciplina,
navegação entre aulas e acesso ao material completo por seu caminho de teoria conhecido.
Não deve inventar progresso pessoal nem substituir silenciosamente o conteúdo original.

#### Scenario: Ler a aula completa

- **WHEN** o usuário abre uma prévia e escolhe o material completo
- **THEN** acessa a página pública de teoria correspondente ao mapa versionado
- **AND** figuras, fórmulas e exercícios permanecem no material original

#### Scenario: Levar um assunto para a composição

- **WHEN** o usuário escolhe praticar um assunto da aula
- **THEN** a composição demonstrativa recebe a disciplina, assunto e uma questão
- **AND** nenhuma tentativa é criada no servidor antes de uma futura integração aprovada

### Requirement: Navegação de produção preserva a realização e a identidade
A interface SHALL preservar independentemente simulado e prática ao navegar entre
início, simulados, aulas/prática e consulta. SHALL ocultar conteúdo durante verificação
de identidade e limpar ambos ao trocar conta/logout. MUST NOT guardar respostas no
navegador ou anunciar salvamento antes de confirmação.

#### Scenario: Alternar entre um simulado e uma prática
- **WHEN** inicia prática de aula tendo simulado em andamento e alterna entre eles
- **THEN** ambos mantêm escolhas/filas/comandos/estados próprios e nenhum é substituído

#### Scenario: Trocar de conta com resposta tardia
- **WHEN** muda de identidade enquanto uma das realizações está oculta e responde depois
- **THEN** nenhum contexto privado anterior reaparece em qualquer das duas realizações

#### Scenario: Consultar aula com resposta pendente
- **WHEN** o estudante visita uma aula durante uma realização e volta a Simulados
- **THEN** a escolha, o estado de salvamento e o comando de retry são preservados
- **AND** cancelar uma proposta de nova prática mantém a realização anterior

#### Scenario: Trocar de conta enquanto a realização está oculta
- **WHEN** a verificação identifica outra conta ou logout em qualquer área
- **THEN** o contexto anterior é desmontado e uma resposta tardia não reaparece

### Requirement: Aulas completas na produção
A integração React SHALL oferecer as51 aulas pelo mapa conhecido, busca/filtro,
navegação e leitura completa com figuras/fórmulas/exercícios. A prática iniciada
na aula SHALL ter fluxo próprio, sem preencher ou abrir o compositor de simulados.
O acesso ao original SHALL continuar independente da API de questões.

#### Scenario: Ler teoria e praticar o assunto
- **WHEN** abre uma aula, confere cargo/assunto e escolhe Praticar
- **THEN** inicia prática própria de até cinco questões fixas exclusivas de PRATICA_AULA daquele assunto/cargo
- **AND** a correção aparece somente ao encerrar a prática, sem substituir um simulado
