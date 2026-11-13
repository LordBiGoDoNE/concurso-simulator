# vps-staging-recovery

## ADDED Requirements

### Requirement: Backup consistente com cópia fora da VPS
O operador SHALL produzir backup consistente com manifesto (release, schema,
PostgreSQL, hashes) antes de cada atualização e sob demanda. O usuário SHALL
manter cópia privada no PC. Backups MUST NOT ir para Git, CI ou issues. Falha
MUST NOT produzir backup validado.

#### Scenario: Backup deliberado
- **WHEN** o backup é executado
- **THEN** a API é parada, o dump é conferido e o manifesto é gravado
- **AND** a API volta somente após o término e a cópia pode ser transferida ao PC

### Requirement: Restauração isolada
A restauração SHALL aceitar somente backup validado, em projeto/volume novos e
com imagem/schema compatíveis. SHALL invalidar sessões restauradas antes de liberar
a API e preservar a origem.

#### Scenario: Ensaio de recuperação
- **WHEN** a cópia do PC é restaurada em destino separado
- **THEN** conteúdo e acervo conferem antes da liberação
- **AND** cookies anteriores não recuperam autenticação

#### Scenario: Artefato inválido ou destino ocupado
- **WHEN** checksum/manifesto diverge ou o destino tem estado
- **THEN** a operação é recusada sem sobrescrever dados nem remover volumes

### Requirement: Atualização e rollback compatíveis
Atualização, backup e restauração SHALL ser mutuamente exclusivos. Toda troca de
release SHALL ser precedida de backup. Rollback MUST usar imagem/schema compatíveis
ou restauração separada; Flyway MUST NOT ser desfeito.

#### Scenario: Falha na atualização
- **WHEN** a nova release falha após migração
- **THEN** imagem, volume e backup anteriores ficam disponíveis
- **AND** o retorno segue o roteiro de compatibilidade ou restauração separada
