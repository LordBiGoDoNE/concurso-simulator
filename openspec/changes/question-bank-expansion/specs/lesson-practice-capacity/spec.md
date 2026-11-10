# Spec Delta

## Purpose

Permitir práticas mais extensas do assunto estudado sem alterar a posse, o início explícito ou a preservação das realizações e respostas que já existem.

## ADDED Requirements

### Requirement: Composição de até 25 bases próprias

Uma nova prática SHALL selecionar min(25, bases fixas elegíveis) do assunto e cargo solicitados, sem repetir identidades ou usar SIMULADO e variantes como complemento. Com zero bases, o início MUST manter o erro de insuficiência existente.

#### Scenario: Pools parcialmente preenchidos
- **WHEN** o estudante inicia uma prática com pool elegível de 1, 5 ou 24 bases
- **THEN** a realização captura respectivamente 1, 5 ou 24 bases distintas do contexto solicitado

#### Scenario: Pool atinge ou ultrapassa o limite
- **WHEN** o pool possui 25 ou mais bases elegíveis
- **THEN** a nova prática contém exatamente 25 bases distintas e mantém o embaralhamento existente de itens/alternativas

#### Scenario: Nenhuma base própria disponível
- **WHEN** o pool PRATICA_AULA do assunto/cargo está vazio, mesmo havendo questões de simulado
- **THEN** o início informa insuficiência sem criar realização ou buscar itens no outro acervo

### Requirement: Disponibilidade pública com limite 25

A consulta de disponibilidade SHALL retornar o contexto solicitado, a contagem elegível própria e maximum igual a 25. A consulta MUST continuar somente leitura e não criar práticas, expor correção ou converter falha técnica em acervo vazio.

#### Scenario: Pool maior que a prática
- **WHEN** a consulta encontra 30 bases elegíveis no assunto/cargo
- **THEN** available informa 30 e maximum informa 25, sem capturar ou iniciar uma realização

#### Scenario: Falha ou contexto inválido
- **WHEN** o contexto é inválido ou a consulta falha
- **THEN** o cliente mantém o tratamento de validação/falha e nova tentativa existente, sem apresentar available igual a zero como se fosse uma contagem confirmada

### Requirement: Início explícito e identidade da operação

A ampliação MUST manter o contrato de início por commandId, roleId e topicId, sem quantidade livre ou criação por navegação. Replay de comando antigo SHALL devolver a realização original, independentemente do novo limite ou de alterações posteriores do pool.

#### Scenario: Comando antigo criou cinco itens
- **WHEN** o mesmo proprietário repete o comando que criou uma prática de cinco itens antes da expansão
- **THEN** a mesma realização de cinco itens, IDs, ordem, respostas e estado é recuperada sem acréscimo ou novo sorteio

#### Scenario: Nova operação depois da expansão
- **WHEN** o estudante solicita explicitamente nova prática com outro commandId
- **THEN** o novo teto é aplicado ao pool corrente, respeitando confirmação de substituição e bloqueio por gravação pendente

#### Scenario: Navegação ou troca de cargo
- **WHEN** o estudante abre uma aula, consulta disponibilidade ou altera cargo/URL
- **THEN** nenhuma prática é criada ou substituída até a ação explícita de início

### Requirement: Leitura e histórico compatíveis

A aplicação SHALL ler, continuar, responder e encerrar práticas de 1 a 25 itens, preservando snapshots, comandos e histórico anteriores. A mudança MUST manter a estrutura de captura existente, sem migração, regravação histórica ou alteração das realizações de simulado.

#### Scenario: Prática antiga aberta ou encerrada
- **WHEN** o proprietário lê uma prática anterior de até cinco itens, aberta ou concluída
- **THEN** os mesmos itens e respostas permanecem, e o histórico/resultado conserva o total original

#### Scenario: Reinício com 25 respostas
- **WHEN** o servidor reinicia após gravar uma prática de 25 itens e parte de suas respostas
- **THEN** o mesmo proprietário retoma sua captura e pode concluir, sem truncamento, recomposição ou perda das respostas confirmadas

#### Scenario: Captura inválida excede o teto
- **WHEN** uma prática contém mais de 25 itens, identidades repetidas ou assunto/modalidade incompatíveis
- **THEN** a leitura/criação é recusada pelas invariantes correspondentes, sem truncar ou aceitar conteúdo inconsistente

### Requirement: Interface e cliente coerentes com o novo teto

O cliente SHALL aceitar realizações/histórico de 1 a 25 itens e validar maximum igual a 25 na disponibilidade. A interface MUST informar o novo limite e permitir responder, navegar, revisar e concluir os 25 itens, preservando foco, teclado e apresentação em celular/desktop.

#### Scenario: Percurso completo com 25 itens
- **WHEN** o estudante abre uma aula e inicia prática com pool suficiente
- **THEN** consegue acessar todos os itens e concluir/revisar o resultado, sem controles cortados, itens inacessíveis ou navegação lateral indevida

#### Scenario: Disponibilidade muda antes do clique
- **WHEN** o pool muda entre a consulta de disponibilidade e o início
- **THEN** o cliente apresenta a realização ou indisponibilidade efetivamente retornada pelo servidor, sem inventar os itens esperados na consulta anterior

### Requirement: Proteções de gravação e correção preservadas

A prática ampliada MUST preservar fila, controle de versão, posse, expiração, idempotência, limpeza por troca de identidade/logout e correção somente após encerramento. O teto novo SHALL ser aplicado sem alterar essas proteções ou misturar realizações das duas modalidades.

#### Scenario: Troca de contexto durante gravação
- **WHEN** há resposta pendente e o usuário tenta substituir a prática ou trocar identidade/sair
- **THEN** as proteções existentes são mantidas e respostas tardias não contaminam a nova identidade ou realização

#### Scenario: Conta diferente ou realização aberta
- **WHEN** outra conta acessa a prática ou o proprietário tenta consultar resultado antes de encerrá-la
- **THEN** os controles de acesso e de resultado antecipado mantêm os contratos existentes, sem revelar gabarito, resolução ou evidências corretivas
