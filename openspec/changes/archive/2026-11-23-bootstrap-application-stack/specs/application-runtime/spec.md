# Spec Delta

## Purpose

Permitir executar, configurar e verificar a aplicação de simulados em um ambiente local reproduzível, sem depender da hospedagem do material estático.

## ADDED Requirements

### Requirement: Inicialização local documentada
A aplicação SHALL oferecer instruções e comandos reproduzíveis para iniciar frontend, backend e banco local a partir de um clone limpo.

#### Scenario: Primeiro uso local
- **WHEN** um desenvolvedor instala os pré-requisitos documentados e executa os comandos de inicialização
- **THEN** frontend e API ficam acessíveis nos endereços documentados e o backend utiliza o banco local.

### Requirement: Preparação versionada do banco
A aplicação SHALL aplicar automaticamente as migrações pendentes ao iniciar e SHALL interromper a inicialização quando uma migração falhar.

#### Scenario: Banco vazio
- **WHEN** a aplicação inicia com um banco local vazio
- **THEN** as migrações criam a estrutura inicial e registram sua versão.

#### Scenario: Reinicialização
- **WHEN** a aplicação reinicia com migrações já aplicadas
- **THEN** não reaplica migrações concluídas nem apaga dados existentes.

### Requirement: Contrato público de prontidão
A API SHALL responder a `GET /api/v1/status` com HTTP 200 e JSON `{"status":"UP"}` quando aplicação e banco estiverem disponíveis, e HTTP 503 com `{"status":"DOWN"}` quando o banco ficar indisponível durante a execução. A resposta MUST NOT revelar credenciais, configuração interna ou detalhes de exceções.

#### Scenario: Aplicação pronta
- **WHEN** um cliente consulta o endpoint com o banco disponível
- **THEN** recebe HTTP 200, conteúdo JSON e o estado UP.

#### Scenario: Perda de conexão após inicialização
- **WHEN** o banco fica indisponível após a API iniciar e o cliente consulta o endpoint
- **THEN** recebe HTTP 503 e estado DOWN, sem detalhes internos.

### Requirement: Configuração externa e origens explícitas
A aplicação SHALL receber credenciais e origens permitidas por configuração externa. Segredos MUST NOT estar em arquivos versionados. Requisições do navegador de origens não autorizadas MUST NOT receber permissão de leitura via CORS.

#### Scenario: Origem local autorizada
- **WHEN** o frontend local consulta a API a partir de uma origem configurada
- **THEN** a API permite sua leitura pelo navegador.

#### Scenario: Origem não autorizada
- **WHEN** uma origem não configurada consulta a API
- **THEN** a resposta não concede acesso CORS a essa origem; isso não substitui autenticação futura.

### Requirement: Contrato e verificações automatizadas
A aplicação SHALL documentar o endpoint inicial em OpenAPI e oferecer verificações automatizadas de build, contrato HTTP e integração com um banco real isolado, sem acessar o banco de desenvolvimento ou produção.

#### Scenario: Verificação em integração contínua
- **WHEN** uma alteração da aplicação é submetida ao fluxo de CI
- **THEN** builds e testes executam sem credenciais de produção e falham quando o contrato ou uma migração está incorreto.
