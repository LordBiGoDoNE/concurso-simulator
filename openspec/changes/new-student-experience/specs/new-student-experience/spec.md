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
