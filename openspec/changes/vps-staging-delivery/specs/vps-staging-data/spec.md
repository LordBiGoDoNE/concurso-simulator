# vps-staging-data

## ADDED Requirements

### Requirement: Inicialização somente com o acervo congelado
O pacote inicial SHALL conter251 bases SIMULADO,255 PRATICA_AULA e conteúdo
parametrizado atual de3 famílias/111 variantes, mantendo as51 aulas. MUST NOT
copiar contas, identidades externas, sessões, tentativas, capturas ou comandos locais.
O conteúdo das questões SHALL permanecer congelado nesta entrega.

#### Scenario: Preparação do destino novo
- **WHEN** o banco da homologação é inicializado
- **THEN** Flyway e validação preparam o schema sem importar/aprovar conteúdo
- **AND** a carga posterior usa pacote explícito do acervo, sem dump completo local

### Requirement: Operação editorial deliberada e rastreável
Carga e elegibilidade SHALL usar contratos/UseCases existentes com manifesto,
fingerprints e destino identificados. Aprovação no computador MUST NOT ser tratada
como decisão já executada no banco da VPS. CLIs MUST NOT exigir exposição do PG.

#### Scenario: Decisão sobre a seleção no destino
- **WHEN** o usuário verifica manifesto e destino e executa a operação acordada
- **THEN** as decisões se vinculam às revisões/fingerprints selecionados
- **AND** finalidade, catálogo, dependências e contagens são conferidos no destino

#### Scenario: Reexecução ou divergência
- **WHEN** a carga é repetida ou detecta revisão/conteúdo inesperado
- **THEN** a repetição válida é idempotente e a divergência é recusada sem sobrescrita
- **AND** simulados/práticas continuam com acervos e correção final separados
