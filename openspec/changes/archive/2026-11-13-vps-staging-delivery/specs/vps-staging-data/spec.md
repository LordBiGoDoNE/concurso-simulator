# vps-staging-data

## ADDED Requirements

### Requirement: Somente o acervo aprovado da release
O banco de produção SHALL receber somente o acervo aprovado na main da release,
com contagens e digests calculados a partir dessa main. MUST NOT copiar contas,
identidades externas, sessões, tentativas, capturas ou dumps do PC. O conteúdo
das questões SHALL permanecer congelado nesta entrega.

#### Scenario: Banco novo
- **WHEN** o banco de produção é inicializado
- **THEN** Flyway prepara o schema sem importar ou aprovar conteúdo no startup
- **AND** a carga posterior usa pacote explícito do acervo, sem dump do PC

### Requirement: Carga administrativa rastreável
A carga SHALL usar a importação e as decisões editoriais administrativas existentes,
autenticadas como administrador e com destino identificado; o tráfego fica no
loopback do host (sem passar pela internet) e a carga segura o lock do operador.
O checkout dos pacotes SHALL estar no commit da release em uso. IDs do destino SHALL ser mapeados por
origem/fingerprint, e digests derivados SHALL ser recalculados no destino.
Aprovação no PC MUST NOT ser tratada como decisão já executada na produção.

#### Scenario: Conferência no destino
- **WHEN** a carga termina
- **THEN** contagens, finalidade, catálogo e dependências conferem com o pacote
- **AND** simulados e práticas mantêm acervos e correção separados

#### Scenario: Reexecução ou divergência
- **WHEN** a carga é repetida ou encontra revisão inesperada
- **THEN** a repetição válida é idempotente e a divergência é recusada sem sobrescrita
