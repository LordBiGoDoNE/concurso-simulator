# lesson-practice-by-topic Specification

## Purpose

Oferecer prática do assunto da aula sem seleção ou vínculo novo de cargo,
preservando dados históricos, segurança e separação dos simulados.

## ADDED Requirements

### Requirement: Prática identificada pelo assunto
O sistema SHALL iniciar prática por assunto, sem cargo explícito ou implícito.
O cargo SHALL continuar disponível na organização do edital e nos simulados.

#### Scenario: Aula aberta sem cargo
- **WHEN** o estudante abre uma aula por link direto ou exploração livre
- **THEN** pode consultar e iniciar a prática sem escolher cargo
- **AND** o servidor não atribui um cargo substituto à nova realização.

### Requirement: Seleção independente de cargo
O sistema SHALL contar e selecionar somente questões fixas aprovadas do assunto,
exclusivas de PRATICA_AULA, sem filtrar pela classificação editorial de cargos.
A classificação e os fingerprints existentes MUST NOT ser regravados por essa mudança.

#### Scenario: Questões com cargos diferentes no mesmo assunto
- **WHEN** há questões elegíveis do assunto classificadas para cargos diferentes
- **THEN** todas participam do mesmo pool de prática por assunto
- **AND** a realização seleciona até cinco bases distintas, sem usar questões de simulados.

#### Scenario: Pool pequeno ou vazio
- **WHEN** há de uma a quatro questões elegíveis
- **THEN** a prática usa todas as bases disponíveis daquele assunto
- **AND** se o pool for zero, informa insuficiência sem criar realização parcial ou trocar assunto.

### Requirement: Contratos próprios sem cargo de realização
A API de prática SHALL receber commandId/topicId no início e topicId na consulta
de disponibilidade. Visualização e histórico SHALL identificar o assunto, sem
roleId da realização. Contratos de simulado MUST continuar exigindo cargo.

#### Scenario: Pedido válido e contrato antigo
- **WHEN** o cliente envia início ou disponibilidade pelo novo contrato por assunto
- **THEN** a API valida e responde sem exigir cargo
- **AND** pedidos de início/disponibilidade que ainda enviem roleId recebem 400.

#### Scenario: Prática projetada sem cargo
- **WHEN** o proprietário consulta uma prática, seu encerramento ou histórico próprio
- **THEN** recebe a projeção por assunto sem roleId de realização
- **AND** os metadados editoriais das questões e os contratos dos simulados são preservados.

### Requirement: Compatibilidade histórica imutável
Novas práticas SHALL usar snapshot formato 4 sem Composition por cargo. O sistema
SHALL preservar acesso, respostas, conclusão, resultados e replay das capturas
anteriores, sem reescrever registros, hashes ou formatos históricos.

#### Scenario: Retomar prática formato 3
- **WHEN** a conta retoma uma prática antiga, em andamento ou concluída
- **THEN** recebe suas mesmas questões, alternativas, respostas e correção capturadas
- **AND** o contrato público omite cargo sem apagar a composição histórica persistida.

#### Scenario: Migração sobre dados existentes
- **WHEN** a nova migração Flyway é aplicada
- **THEN** registros anteriores permanecem válidos e o formato 4 passa a ser aceito
- **AND** travas de modalidade, assunto, itens, origem e imutabilidade permanecem ativas.

### Requirement: Idempotência por assunto e segurança
Novos comandos de criação SHALL identificar modalidade e assunto. Replays SHALL
verificar posse/expiração e manter a captura, sem reinterpretar comandos históricos.
Sessão, CSRF e autorização técnica SHALL permanecer na infraestrutura de segurança.

#### Scenario: Confirmação de criação perdida
- **WHEN** a criação foi confirmada no banco e o cliente repete comando e assunto
- **THEN** recebe a mesma prática e a mesma ordem capturada
- **AND** não há duplicação; reutilizar o comando para intenção incompatível gera 409.

#### Scenario: Acesso privado e correção antecipada
- **WHEN** outro ator ou uma sessão expirada solicita leitura, resposta, conclusão ou replay
- **THEN** o acesso é recusado sem devolver conteúdo privado
- **AND** antes de encerrar o proprietário também não recebe gabarito ou resolução.

### Requirement: Painel e execução preservados
O painel SHALL manter início explícito, questões abaixo do botão e resultado ao
encerrar. Correspondência e estado público da prática SHALL usar assunto, sem cargo.
Filas e escolhas MUST ser preservadas ao recolher/navegar, com limpeza por identidade.

#### Scenario: Cargo público muda na mesma aula
- **WHEN** o estudante altera o cargo da navegação e permanece no mesmo assunto
- **THEN** a prática e suas escolhas continuam no painel
- **AND** nenhuma nova prática é criada nem a realização anterior é ocultada pelo cargo.

#### Scenario: Recolher durante gravação e voltar à aula
- **WHEN** o estudante recolhe a prática ou visita outra área durante uma gravação
- **THEN** a fila continua e reabrir preserva a escolha e sua confirmação
- **AND** o retorno à aula usa o assunto, preservando cargo público compatível ou exploração livre.

### Requirement: Isolamento de modos e identidade
Práticas SHALL permanecer separadas de simulados em execução, acervo, histórico,
janela e métricas. Mudança de identidade SHALL invalidar ambos os contextos privados.

#### Scenario: Simulado em andamento e prática por assunto
- **WHEN** o estudante inicia prática tendo simulado com resposta pendente
- **THEN** a prática não substitui o simulado nem descarta sua fila
- **AND** práticas novas e antigas continuam fora das métricas e do histórico de simulados.

#### Scenario: Troca de conta durante resposta pendente
- **WHEN** a identidade muda enquanto uma resposta da prática está pendente
- **THEN** o conteúdo privado anterior é limpo e respostas tardias não reaparecem
- **AND** práticas visitantes não são transferidas automaticamente à conta.
