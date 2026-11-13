# new-student-experience Specification

## Purpose

Orientar novos estudantes dos dois cargos de Limeira01/2026 desde a escolha do
cargo até uma aula e sua prática, com navegação pública restaurável e acesso opcional por conta.

## ADDED Requirements

### Requirement: Primeiro caminho acessível sem conta
O sistema SHALL identificar o escopo Limeira01/2026 e os dois cargos, indicar a
natureza independente do material e oferecer cargo → aula → prática, sem exigir login.

#### Scenario: API indisponível
- **WHEN** o estudante escolhe um cargo com a API indisponível
- **THEN** a biblioteca local e as aulas permanecem acessíveis
- **AND** a indisponibilidade não é apresentada como acervo confirmado vazio.

### Requirement: Contexto público restaurável
O sistema SHALL representar área e filtros públicos na URL, validar valores e
restaurar o contexto com atualização e histórico do navegador.

#### Scenario: Aula compartilhada
- **WHEN** uma URL válida de aula é aberta ou atualizada
- **THEN** o cargo, aula, disciplina e busca são restaurados
- **AND** nenhuma realização é criada por essa navegação.

#### Scenario: Endereço incompatível
- **WHEN** cargo, aula ou parâmetros são desconhecidos ou incompatíveis
- **THEN** as opções inválidas são descartadas com orientação
- **AND** dados privados não são interpretados como comandos ou estado público.

### Requirement: Oferta de prática confirmada
O sistema SHALL consultar a disponibilidade do assunto/cargo, distinguir carga,
erro e ausência confirmada e iniciar somente após ação explícita.

#### Scenario: Consulta confirmada sem questões
- **WHEN** o servidor confirma quantidade zero
- **THEN** o início permanece indisponível e a aula continua acessível.

#### Scenario: Falha ou resposta tardia
- **WHEN** a consulta falha ou pertence a seleção anterior
- **THEN** ela não libera início para a seleção atual
- **AND** uma falha oferece nova consulta sem criar atividades.

### Requirement: Preservação das realizações
O sistema SHALL preservar filas, comandos idempotentes, confirmação de substituição
e invalidação de conteúdo privado por mudança de identidade durante navegação interna.

#### Scenario: Gravação em andamento
- **WHEN** o estudante navega enquanto uma resposta está sendo gravada
- **THEN** a navegação não desmonta a realização nem descarta a fila
- **AND** a troca de conta continua limpando o conteúdo privado.

### Requirement: Prática opcional no final da aula
O sistema SHALL apresentar a prática em painel inicialmente recolhido depois do
conteúdo da aula. O botão Começar prática SHALL permanecer acima da atividade;
somente seu clique carrega as questões, sem mudar a rota da aula. O resultado
SHALL aparecer no mesmo painel após o encerramento explícito.

#### Scenario: Abrir sem iniciar
- **WHEN** o estudante abre o painel de prática
- **THEN** ele pode consultar disponibilidade e escolher o cargo
- **AND** nenhuma realização é criada e nenhuma questão da realização é carregada.

#### Scenario: Início explícito e carregamento
- **WHEN** o estudante clica em Começar prática com disponibilidade confirmada
- **THEN** o carregamento e depois as questões aparecem abaixo do botão
- **AND** o início fica bloqueado durante a operação e a rota da aula é preservada.

#### Scenario: Recolher durante gravação
- **WHEN** o estudante recolhe o painel enquanto uma resposta está sendo gravada
- **THEN** a fila continua e reabrir o painel preserva a escolha e sua confirmação
- **AND** reabrir não cria outra prática.

#### Scenario: Trocar o assunto apresentado
- **WHEN** o estudante navega para outra aula ou cargo com uma prática em andamento
- **THEN** as questões da seleção anterior não aparecem sob a nova seleção
- **AND** a realização anterior continua preservada pelas regras de substituição.

### Requirement: Índice acima da leitura
O sistema SHALL posicionar o índice de aulas da disciplina antes do título da aula,
identificar o cabeçalho como Nesta Disciplina - nome da disciplina e permitir que
o leitor utilize toda a largura disponível em sua área.

#### Scenario: Navegar pelo índice
- **WHEN** o estudante abre uma aula ou escolhe outra pelo índice
- **THEN** o índice permanece acima do leitor, com a aula atual identificada
- **AND** teclado, histórico e layout responsivo continuam disponíveis sem criar atividades.

### Requirement: Aula nativa no layout da aplicação
O sistema SHALL apresentar as 51 aulas como conteúdo nativo da página, com um
único título e a largura do leitor. Teoria, exemplos resolvidos, tabelas, fórmulas,
esquemas e fontes SHALL permanecer disponíveis sem depender da API ou do site antigo.
Os exercícios SHALL ser oferecidos somente pelo painel de prática existente.

#### Scenario: Ler uma aula convertida
- **WHEN** o estudante abre qualquer aula do catálogo
- **THEN** o conteúdo é renderizado diretamente no layout atual, sem iframe,
  cabeçalhos duplicados ou navegação de versões anteriores
- **AND** as fontes e exemplos permanecem acessíveis, com exercícios no painel recolhido.

#### Scenario: Usar o sumário e rever uma aula
- **WHEN** o estudante escolhe uma seção do sumário ou um atalho de revisão
- **THEN** o sumário mantém a rota da aula e foca a seção correspondente
- **AND** o atalho de revisão abre a aula nativa, preservando o cargo compatível.

#### Scenario: Avançar ou voltar uma aula
- **WHEN** o estudante escolhe Próxima aula ou Aula anterior no final da leitura
- **THEN** a nova aula abre no topo da página, com foco no título
- **AND** o carregamento do documento não restaura a posição inferior anterior.

#### Scenario: Falha de carregamento do documento
- **WHEN** o documento da aula não pode ser carregado
- **THEN** a página informa a falha e permite tentar novamente sem iniciar uma prática
- **AND** respostas tardias de outra aula não substituem o conteúdo selecionado.
