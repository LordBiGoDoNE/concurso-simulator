# Spec Delta

## Purpose

Manter práticas curtas com sorteio de até cinco questões de um acervo de pelo menos
25 bases por assunto/modalidade. Esclarecimento do usuário em 2026-11-11: 25 é a
meta do acervo, não o tamanho de cada realização.

## ADDED Requirements

### Requirement: Sorteio de até cinco bases do acervo completo

Uma nova prática SHALL selecionar aleatoriamente min(5, bases fixas elegíveis) do assunto solicitado, considerando todo o pool PRATICA_AULA. MUST NOT repetir identidades dentro da prática ou complementar com SIMULADO/templates. A meta editorial de 25 por assunto/modalidade MUST permanecer independente desse limite.

#### Scenario: Acervo com 25 ou mais bases
- **WHEN** o estudante inicia uma prática de assunto com 25 ou mais bases elegíveis
- **THEN** recebe cinco bases sorteadas do pool completo, com o embaralhamento existente de questões/alternativas

#### Scenario: Pool parcial ou vazio
- **WHEN** o pool tem de uma a quatro bases, ou nenhuma
- **THEN** captura as bases disponíveis sem repetição; com zero mantém insuficiência e não cria realização

#### Scenario: Outra prática explícita
- **WHEN** o estudante solicita nova prática com outro commandId
- **THEN** executa novo sorteio de até cinco; coincidências entre sorteios são permitidas e não há garantia de esgotar o acervo antes de repetir

### Requirement: Disponibilidade distingue acervo e tamanho da prática

A consulta SHALL informar available como a contagem elegível real e maximum igual a 5. MUST continuar somente leitura, sem iniciar prática, expor correção ou apresentar falha técnica como acervo vazio.

#### Scenario: Acervo maior que o treino
- **WHEN** há 25 ou 30 bases elegíveis do assunto
- **THEN** available informa 25 ou 30 e maximum informa 5, sem limitar ou reduzir o acervo

#### Scenario: Consulta falha
- **WHEN** o contexto é inválido ou ocorre falha técnica
- **THEN** o cliente preserva validação/falha e nova tentativa, sem converter a falha em available zero

### Requirement: Identidade do sorteio e preservação das capturas

O início SHALL manter commandId/topicId sem cargo, quantidade livre ou criação por navegação. Repetir um comando SHALL recuperar sua captura original, sem ressorteio. A aplicação MUST preservar respostas, comandos e capturas existentes, incluindo as de 25 itens criadas na etapa anterior.

#### Scenario: Repetição da mesma operação
- **WHEN** o mesmo proprietário repete commandId e assunto compatíveis
- **THEN** recupera os mesmos itens, IDs, ordem, respostas e estado, independentemente do pool corrente

#### Scenario: Captura anterior com 25 itens
- **WHEN** o proprietário retoma ou repete a criação de uma captura anterior de 25 itens
- **THEN** ela permanece legível e operável com seu tamanho original; somente um novo comando cria sorteio de até cinco

#### Scenario: Navegação e contexto público
- **WHEN** o estudante abre aula, muda cargo público/URL ou consulta disponibilidade
- **THEN** nenhuma prática é criada ou substituída sem ação explícita

### Requirement: Compatibilidade persistida sem regravação

A aplicação SHALL manter formato 4 e leitura dos formatos anteriores. MUST preservar V1–V15 já usadas nos ambientes locais, sem truncar dados ou editar migrações para esta correção. A V15 conserva compatibilidade com capturas anteriores de até 25; o domínio MUST recusar novas criações com mais de cinco.

#### Scenario: Gravação após reinício
- **WHEN** o servidor reinicia com prática parcialmente respondida
- **THEN** preserva a captura imutável e permite responder/encerrar sem reordenar metadados, recompor itens ou perder respostas

#### Scenario: Criação nova excede cinco
- **WHEN** uma nova prática é construída com seis ou mais itens
- **THEN** o domínio recusa a criação sem truncamento ou persistência parcial

#### Scenario: Recuperação local
- **WHEN** o ambiente é restaurado de backup verificado em destino novo
- **THEN** preserva práticas/capturas anteriores e o inventário fechado de tabelas da versão, sem regravação ou downgrade

### Requirement: Interface coerente com práticas curtas

A interface SHALL informar até cinco questões por nova prática e o cliente SHALL validar maximum igual a 5. Leitura, resultado e histórico MUST continuar aceitando as capturas anteriores de até 25, sem apresentar essa compatibilidade como quantidade de um novo sorteio.

#### Scenario: Jornada de cinco itens
- **WHEN** o estudante inicia prática com acervo de pelo menos 25
- **THEN** navega/responde/conclui até cinco itens por teclado e em celular/desktop, recebendo correção somente ao encerrar

#### Scenario: Pool muda antes do início
- **WHEN** o pool muda entre disponibilidade e clique
- **THEN** o cliente apresenta a realização ou insuficiência efetivamente devolvida pelo servidor, sem inventar questões

### Requirement: Proteções de identidade e gravação preservadas

A correção MUST preservar posse, versão, expiração, filas, idempotência, limpeza na troca de identidade/logout e correção após conclusão. MUST NOT misturar modos ou reintroduzir cargo na prática.

#### Scenario: Resposta pendente ou identidade alterada
- **WHEN** há gravação pendente, substituição ou troca de identidade
- **THEN** as proteções existentes impedem perda silenciosa e respostas tardias não contaminam outra identidade/realização

#### Scenario: Acesso indevido ou correção antecipada
- **WHEN** outra conta acessa a prática ou o proprietário pede resultado antes de concluir
- **THEN** mantém os contratos de rejeição e não expõe gabarito, resolução ou dados de outra conta
