# vps-staging-recovery

## ADDED Requirements

### Requirement: Backup manual com cópia independente da VPS
O procedimento SHALL produzir backup consistente e manifesto antes de atualizar
ou importar, e após validar a carga inicial. O usuário SHALL guardar cópia completa
no computador por transferência privada. Falha MUST NOT produzir backup validado.

#### Scenario: Backup deliberado
- **WHEN** o usuário pausa os escritores necessários e executa o backup
- **THEN** recebe artefato e manifesto com release/schema/PG/checksum verificáveis
- **AND** é orientado sobre parada/retomada da API e cópia no computador

### Requirement: Restauração isolada e sessões invalidadas
A restauração SHALL aceitar somente backup confiável validado e destino novo/vazio
com imagem/schema compatíveis. SHALL preservar a origem e invalidar sessões
restauradas antes de liberar a API, sem apagar histórico de negócio válido.

#### Scenario: Ensaio de recuperação
- **WHEN** o usuário restaura a cópia transferida em ambiente separado
- **THEN** confere conteúdo, acervo e funcionamento antes de liberar o destino
- **AND** cookies anteriores do backup não recuperam autenticação automaticamente

#### Scenario: Artefato inválido ou destino ocupado
- **WHEN** checksum/manifesto diverge ou o destino contém estado existente
- **THEN** a operação recusa sem sobrescrever dados nem remover volumes

### Requirement: Atualização e rollback compatíveis
Atualização/backup/restauração SHALL ser mutuamente exclusivos. O roteiro SHALL
exigir imagem/schema compatíveis ou recuperação separada para retorno. MUST NOT
tratar troca de imagem como reversão automática de Flyway ou prometer RPO/RTO.

#### Scenario: Falha na atualização
- **WHEN** a release nova falha após uma mudança de schema
- **THEN** o usuário avalia compatibilidade ou restaura separadamente pelo roteiro
- **AND** mantém imagens, dados e backup anteriores para recuperação deliberada
