# Spec Delta

## Purpose

Permitir retomada e consulta privada das tentativas da conta, mantendo a prática do
visitante temporária e impedindo acesso cruzado entre pessoas, sessões e contas.

## ADDED Requirements

### Requirement: Visitante com tentativa temporária
Visitantes SHALL poder realizar e corrigir simulados sem login, com tentativa acessível
somente pela sessão temporária proprietária. A tentativa SHALL expirar após 24 horas
da criação ou ficar inacessível com a sessão encerrada; dados temporários SHALL ser
removidos até uma hora após expiração, inclusive após reinício do aplicativo.

#### Scenario: Visitante conclui e revisa
- **WHEN** conclui uma tentativa antes da expiração com sua sessão válida
- **THEN** pode consultar o resultado nessa sessão, sem histórico permanente ou conta criada implicitamente.

#### Scenario: Expiração e limpeza
- **WHEN** a tentativa alcança 24 horas, ou sua sessão já não é válida
- **THEN** ela não pode ser acessada; o processo de limpeza remove seus dados até uma hora após o vencimento e recupera atrasos após reinício.

### Requirement: Salvamento confirmado na conta
Tentativas iniciadas por usuário conectado SHALL pertencer à sua identidade interna
verificada e preservar respostas confirmadas, estado e versão no servidor. O sistema
MUST NOT tratar escolha local, timeout ou falha de gravação como resposta salva.

#### Scenario: Retomar em outro navegador
- **WHEN** autentica na mesma conta em outro navegador e abre uma tentativa em andamento
- **THEN** recebe a seleção/ordens originais e as respostas já confirmadas, podendo continuar a partir da versão atual.

#### Scenario: Logout ou sessão expirada
- **WHEN** encerra a sessão ou uma operação privada recebe 401
- **THEN** a interface limpa conteúdo privado em memória, pede nova autenticação para retomar e preserva os dados confirmados da conta no servidor.

### Requirement: Histórico privado paginado
O usuário conectado SHALL consultar apenas suas tentativas em andamento e concluídas,
com paginação limitada, ordenação estável, cargo, datas, quantidade, estado e resultado
final quando concluída. O histórico MUST NOT expor nota parcial ou incluir tentativas de visitantes.

#### Scenario: Histórico da conta
- **WHEN** consulta sua segunda página de tentativas
- **THEN** recebe somente seus registros na ordem definida, sem repetição quando os dados não mudaram e sem resultado de tentativas em andamento.

#### Scenario: Visitante pede histórico da conta
- **WHEN** tenta acessar o histórico privado sem autenticação válida
- **THEN** recebe 401 JSON, sem redirecionamento automático nem exposição de registros.

### Requirement: Posse em todas as operações
Consulta, resposta, conclusão e resultado SHALL verificar o proprietário antes de
revelar conteúdo ou estado. Um ID conhecido MUST NOT conceder acesso; tentativa de
outro proprietário, ausente ou temporária expirada SHALL retornar 404 indistinguível.

#### Scenario: Conta ou sessão diferente
- **WHEN** outra conta ou outra sessão visitante conhece o UUID de uma tentativa
- **THEN** todas as leituras/escritas/resultados recebem 404, sem revelar existência, nota ou versão e sem alterar dados.

### Requirement: Proteção técnica das requisições
Operações de escrita SHALL exigir CSRF válido para visitante e conectado; origens
permitidas SHALL ser explícitas. Respostas de tentativas/histórico/resultados SHALL
usar no-store e MUST NOT persistir dados privados em storage do navegador ou cache público.

#### Scenario: CSRF inválido
- **WHEN** tenta criar, responder ou concluir sem token CSRF válido
- **THEN** recebe 403 antes do comando, sem criar tentativa ou modificar respostas.

#### Scenario: Resposta tardia depois de logout
- **WHEN** uma consulta privada iniciada antes do logout termina depois dele
- **THEN** seus dados não reaparecem na interface nem em storage/cache do navegador.

### Requirement: Troca de contexto sem transferência implícita
Login/logout ou troca de conta MUST NOT transferir uma tentativa visitante ou de outra
conta para a identidade atual. A interface SHALL informar essa limitação e separar
novas tentativas dos dados pertencentes ao contexto anterior.

#### Scenario: Login durante tentativa visitante
- **WHEN** o visitante inicia login durante uma tentativa temporária
- **THEN** é informado de que ela não será incorporada à conta; a conta não recebe histórico/respostas dessa tentativa automaticamente.

#### Scenario: Mescla solicitada pelo cliente
- **WHEN** o cliente envia proprietário, UUID de usuário ou instrução para adotar uma tentativa alheia
- **THEN** o comando é rejeitado e a identidade/propriedade verificadas no servidor permanecem intactas.
