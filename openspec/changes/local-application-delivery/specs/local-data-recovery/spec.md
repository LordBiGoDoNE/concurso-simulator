# Spec Delta

## Purpose

Demonstrar persistência e recuperação local sem alterar a cópia de dados do operador.

## ADDED Requirements

### Requirement: Volume novo e migração controlada
O ambiente SHALL usar volume próprio, Flyway como único dono do schema e Hibernate
validate/OSIVfalse. Startup MUST NOT importar, semear ou aprovar questões/templates,
copiar contas/sessões ou acessar o banco do operador. Dados reais exigem autorização
específica de origem/destino/escopo; CLIs editoriais MUST manter suas restrições.

#### Scenario: Primeira execução sem acervo
- **WHEN** inicia em volume novo sem carga deliberada
- **THEN** migra o schema e mostra estados vazios/insuficiência reais, sem dados fictícios ou aprovação editorial implícita.

### Requirement: Backup lógico deliberado e privado
Backup SHALL ser operação explícita, com origem/versões identificadas e credenciais
fora de argumentos/logs/artefatos públicos. SHALL incluir negócio, Flyway e sessões;
papéis do cluster/configuração SHALL ter procedimento separado. Dumps MUST ficar
fora de Git/imagens/bundle, com proteção local e limites documentados.

#### Scenario: Dump com operações de negócio
- **WHEN** o ensaio gera backup de banco sintético com tentativas V5/formato2 e comandos
- **THEN** obtém snapshot consistente e identifica o artefato sem expor payloads ou segredos no relatório público.

### Requirement: Restauração em destino vazio e verificação
Restore SHALL identificar origem/destino antes de escrever e recusar destino ocupado
por tabelas de negócio,sessão ou Flyway. API SHALL permanecer parada até validação.
Somente SHALL liberar uso depois de restauração bem-sucedida e validação das versões,
schema e dados. Falha MUST NOT aparentar sucesso nem modificar a origem.

### Requirement: Sessões restauradas não reativam acesso revogado
O destino SHALL invalidar todas as sessões do dump antes de liberar a API.
Contas,histórico,capturas e comandos SHALL permanecer, com nova autenticação para
retomada. Backup completo MUST NOT permitir reuso de cookie revogado após o dump.

#### Scenario: Backup anterior ao logout
- **WHEN** captura backup com sessão válida, faz logout e restaura antes da expiração
- **THEN** o cookie antigo continua recebendo401 no destino e o histórico/retomada da conta funciona após nova autenticação ou sessão sintética nova no ensaio isolado.

#### Scenario: Destino contém dados ou restauração falha
- **WHEN** o destino está ocupado ou o restore produz erro
- **THEN** recusa a operação ou mantém o novo destino indisponível, sem apagar dados existentes, escrever na origem ou apresentar aplicação parcialmente restaurada como pronta.

### Requirement: Reinício e retorno compatível
Reinício comum SHALL preservar volume, capturas/comandos/decisões e pode recuperar
sessões válidas. Restore SHALL exigir nova autenticação para retomada da conta.
Retorno SHALL usar artefato compatível identificado; MUST NOT reescrever migrações,
apagar volume, fazer downgrade de schema ou executar JAR V5 após escrita formato2.

#### Scenario: Recuperação por outro container da mesma versão compatível
- **WHEN** recria containers e restaura o backup em volume vazio separado
- **THEN** verifica capturas/hashes/comandos e resultados anteriores, retoma conta sintética e conserva a origem sem alterações.
