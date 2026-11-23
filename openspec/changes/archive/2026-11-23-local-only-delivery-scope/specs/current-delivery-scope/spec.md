# Escopo da entrega atual

## ADDED Requirements

### Requirement: Entrega atual somente local com Docker

A reformulação SHALL considerar a preparação local já integrada pelo #130 como
conclusão da etapa 7 no escopo aprovado em 2026-10-20. Hospedagem, domínio/TLS,
operação pública, backups externos, smoke Google real e publicação/rollback SHALL
ficar adiados para uma proposta futura, mantendo seus requisitos de segurança.

#### Scenario: Preparar a aceitação da entrega local

- **WHEN** a reformulação é preparada para aceitação no escopo atual
- **THEN** o roadmap exige execução local, acesso em loopback, persistência,
  recuperação de dados, segurança e regressão do material
- **AND** não exige contratar hospedagem nem publicar a aplicação externamente
- **AND** preserva Google desligado por padrão e as aprovações editoriais existentes

### Requirement: Preservar gates e autorizações de integração

A decisão de escopo SHALL preservar a revisão independente SOL 6.1 xHigh do conjunto
final em contexto novo, a aprovação expressa do PR final para `main` e as regras
existentes de publicação do material estático. Esta decisão SHALL NOT autorizar
merge, deploy, arquivamento ou alteração de workflows/proteções.

#### Scenario: Etapas do roadmap concluídas no escopo local

- **WHEN** as etapas do escopo local estão integradas na épica geral
- **THEN** o gate cumulativo final ainda precisa examinar o diff completo contra main
- **AND** CI e aprovação do PR final continuam necessários
- **AND** a aplicação Java/PostgreSQL permanece restrita à execução local
