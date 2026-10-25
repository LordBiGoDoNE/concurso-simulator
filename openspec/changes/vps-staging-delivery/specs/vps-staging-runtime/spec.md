# vps-staging-runtime

## ADDED Requirements

### Requirement: Implantação guiada com acesso exclusivo do usuário
A implantação SHALL ser executada pelo usuário, com comandos, efeitos esperados
e verificações documentados. O assistente MUST NOT acessar a VPS por SSH/login,
ler/receber chaves privadas ou configurar agente/runner para operar remotamente.

#### Scenario: Retomada em outro computador
- **WHEN** o usuário retoma a issue e a branch da spec
- **THEN** encontra decisões, pré-requisitos, etapas e estado de execução explícitos
- **AND** fornece somente resultados mínimos de verificação, sem credenciais

### Requirement: Runtime privado e independente
O runtime SHALL usar projeto/volumes/segredos próprios e releases por commit e
imagens identificados. Web SHALL publicar somente em loopback; API/PG MUST NOT
publicar portas. SELinux/firewall e serviços/dados existentes SHALL ser preservados.

#### Scenario: Acesso à homologação
- **WHEN** o usuário abre o túnel SSH e acessa http://localhost:18088
- **THEN** usa SPA, API e material pela origem prevista, com sessão/CSRF atuais
- **AND** acesso direto à web/API/PG pela internet falha em IPv4 e IPv6

#### Scenario: Configuração ou recursos conflitantes
- **WHEN** existe colisão de porta, ownership inesperado ou imagem/origem divergente
- **THEN** a operação falha antes de adotar/sobrescrever recursos
- **AND** mantém o acesso SSH existente e não desabilita SELinux/firewall

### Requirement: Coexistência e evidências reais
A homologação SHALL preservar execução local e isolar suas sessões. Evidências
do Rocky real SHALL ser registradas pelo usuário separadamente das de CI.

#### Scenario: Navegação nos dois ambientes
- **WHEN** local127.0.0.1:8088 e homologação localhost:18088 são usados no computador
- **THEN** cookies/dados de um ambiente não substituem a sessão do outro
- **AND** os containers da homologação mantêm dados após reinício

#### Scenario: Falha do banco ou API
- **WHEN** a dependência fica indisponível
- **THEN** a API apresenta erro/prontidão em prazo finito, sem HTML de falso sucesso
- **AND** o material estático continua acessível sem expor dados de diagnóstico
