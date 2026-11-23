# backend-http-boundaries Specification

## Purpose
Manter as fronteiras HTTP do backend com um único proprietário por decisão: políticas de rota declaradas pela funcionalidade dona e compostas pela plataforma, formato e códigos de erro uniformes, 503 somente para indisponibilidade transitória, adaptadores web separados da infraestrutura, sessões anteriores toleradas e contratos JSON administrativos em DTOs web próprios.

## Requirements

### Requirement: Política de rota declarada pela funcionalidade

Cada rota HTTP da API SHALL ter sua política (método, padrão, acesso, CORS, sessão e
isenção de troca de senha) declarada uma única vez, na infraestrutura da
funcionalidade dona. Configurações de `platform` SHALL compor cadeias de segurança,
CORS, respostas de rejeição, desvio de sessão e isenções somente a partir dessas
declarações, sem literais de rota próprios.

#### Scenario: Rota de estudo nova

- **WHEN** uma funcionalidade adiciona um endpoint e declara sua política
- **THEN** autorização, CORS, resposta 401/403 e tratamento de sessão da rota seguem a
  declaração sem edição em `platform` ou em outra funcionalidade

#### Scenario: Declaração duplicada

- **WHEN** duas políticas declaram o mesmo método e padrão
- **THEN** a aplicação falha ao iniciar e nomeia a rota duplicada

#### Scenario: Rota não declarada

- **WHEN** uma requisição chega a um caminho sob `/api/` sem política declarada
- **THEN** a cadeia padrão a nega com 403
- **AND** nenhum controller é executado

### Requirement: Cobertura entre mapeamentos e políticas

O build SHALL falhar se um mapeamento HTTP de controller sob `/api/` não tiver
exatamente uma política correspondente, ou se uma política não corresponder a um
mapeamento real nem a uma rota do framework listada explicitamente.

#### Scenario: Mapeamento sem política

- **WHEN** um controller ganha um método mapeado sem política declarada
- **THEN** o teste de cobertura falha e nomeia método e padrão

#### Scenario: Política órfã

- **WHEN** uma política declara uma rota que nenhum controller atende e que não está na
  lista de rotas do framework
- **THEN** o teste de cobertura falha e nomeia a rota

### Requirement: Comportamento de acesso preservado

A consolidação das rotas SHALL preservar, para toda rota existente, status,
`Cache-Control`, `Content-Type`, cabeçalhos CORS e criação de sessão para visitante,
conta, conta com troca de senha pendente e requisição sem CSRF, exceto os códigos de
erro alterados pelo requisito de formato de erro.

#### Scenario: Matriz antes e depois

- **WHEN** a matriz de caracterização roda contra a implementação consolidada
- **THEN** cada linha registrada na `main` de origem tem o mesmo resultado
- **AND** variantes com barra final, método errado e rota vizinha inexistente mantêm o
  resultado de origem

#### Scenario: Questões públicas sem sessão

- **WHEN** um visitante lê o catálogo ou uma questão pública
- **THEN** nenhum cookie `SESSION` é criado e a leitura não consulta o repositório de
  sessão

### Requirement: Formato único de erro HTTP

Toda resposta de erro da API com corpo SHALL ser um objeto JSON com o campo `error` em
`UPPER_SNAKE_CASE`, com `Cache-Control: no-store`, e SHALL NOT conter mensagem de
exceção, SQL, conteúdo de questão ou dado pessoal. Campos adicionais já existentes
(`correlationId`, `available`, `questionIds`, `reason`) MAY permanecer.

#### Scenario: Erro de entrada no estudo

- **WHEN** um cliente envia corpo malformado a `/api/v1/attempts`
- **THEN** a resposta é 400 com `{"error":"INVALID_REQUEST"}` e `no-store`

#### Scenario: Visitante sem autenticação em rota de conta

- **WHEN** um visitante chama `GET /api/v1/me`
- **THEN** a resposta é 401 com `{"error":"UNAUTHENTICATED"}` e `no-store`

#### Scenario: Rejeição da segurança sem corpo preservada

- **WHEN** a segurança nega com 403 uma requisição fora das áreas declaradas com falhas
  JSON (CSRF ausente no logout, origem CORS desconhecida, rota pública negada)
- **THEN** a resposta continua 403 sem corpo, como antes da spec

#### Scenario: Backend fora atrás do nginx

- **WHEN** o nginx local ou da VPS não alcança o backend
- **THEN** a resposta é 503 com `{"error":"UNAVAILABLE"}`

### Requirement: Exceções de aplicação nomeadas como contrato

Status HTTP de erro SHALL ser decididos por exceções nomeadas da aplicação ou do
adaptador web. Advices SHALL NOT mapear `IllegalArgumentException` ou
`IllegalStateException` genéricas para um status de cliente. Falha não reconhecida
SHALL responder 500 `INTERNAL_ERROR`, com log e correlação.

#### Scenario: Geração parametrizada desabilitada

- **WHEN** um cliente cria um simulado com `useParameterizedQuestions=true` e a geração
  está desabilitada
- **THEN** a resposta é 503 com `{"error":"PARAMETERIZED_GENERATION_UNAVAILABLE"}`
- **AND** nenhuma tentativa ou comando é gravado

#### Scenario: Dado persistido inconsistente

- **WHEN** a leitura de uma tentativa encontra um invariante violado no dado gravado
- **THEN** a resposta é 500 `INTERNAL_ERROR`, não 400

#### Scenario: Parâmetro inválido do cliente

- **WHEN** um cliente envia um identificador de assunto inexistente na consulta
- **THEN** o adaptador web traduz a recusa do objeto de valor em 400 `INVALID_REQUEST`

### Requirement: 503 somente para indisponibilidade transitória

Respostas 503 SHALL ser usadas apenas para falha transitória de banco, transação ou
recurso, projeção pública insegura recusada e geração parametrizada desabilitada.
Violação de integridade não reconhecida SHALL responder 500 e SHALL NOT ser tratada
como conflito conhecido nem como indisponibilidade.

#### Scenario: Banco indisponível

- **WHEN** o pool de conexões não obtém conexão durante uma leitura de tentativa
- **THEN** a resposta é 503 `UNAVAILABLE`

#### Scenario: Violação de integridade não reconhecida

- **WHEN** uma gravação falha por constraint que o contrato não reconhece
- **THEN** a transação é revertida e a resposta é 500 `INTERNAL_ERROR`

### Requirement: Infraestrutura independente dos adaptadores web

Classes de `..infrastructure..` SHALL NOT depender de `..web..`. Identidade
verificada (`UserPrincipal`) e portas implementadas pela infraestrutura SHALL residir
em `application`; handlers do mecanismo de autenticação SHALL residir na
infraestrutura. Testes de arquitetura SHALL proteger essa direção por regra genérica.

#### Scenario: Infraestrutura importa tipo web

- **WHEN** uma classe de infraestrutura passa a importar uma classe de `..web..`
- **THEN** o teste de arquitetura falha

#### Scenario: Controller fora do pacote web

- **WHEN** uma classe anotada com `@RestController` ou `@RestControllerAdvice` fica
  fora de `..web..`
- **THEN** o teste de arquitetura falha

### Requirement: Controllers recebem identidade verificada e respondem DTO concreto

Todo `@RestController` SHALL receber identidade já verificada, SHALL NOT depender de
mecanismos de sessão, contexto de segurança ou servlet, e todo método mapeado SHALL
retornar `ResponseEntity` de um record do próprio pacote web ou de `shared.web`. A
regra SHALL valer para todos os controllers, sem lista nominal.

#### Scenario: Controller novo sem DTO concreto

- **WHEN** um controller novo retorna `Map`, `Object` ou curinga
- **THEN** o teste de arquitetura falha e nomeia o método

### Requirement: Sessões anteriores à mudança não produzem 500

Uma sessão persistida antes da mudança de pacote do `UserPrincipal` SHALL ser
tratada como sem autenticação, SHALL NOT restaurar a identidade anterior e SHALL NOT
produzir 500.

#### Scenario: Conta conectada antes da atualização

- **WHEN** uma requisição usa o cookie de uma sessão gravada com o `UserPrincipal`
  antigo
- **THEN** `GET /api/v1/me` responde 401
- **AND** o estudo continua disponível como visitante e nenhuma conta é criada

### Requirement: DTOs web próprios na administração de questões

Requisições e respostas de importação, consulta/exportação e decisões editoriais em
lote SHALL usar records do pacote web, sem componentes de `domain` ou `application`,
com mapeamento explícito, e SHALL preservar o JSON atual.

#### Scenario: Contrato preservado

- **WHEN** a mesma requisição de prévia de importação é enviada antes e depois da
  mudança
- **THEN** a resposta tem os mesmos campos e valores

#### Scenario: Renomeação no núcleo

- **WHEN** um enum ou componente do núcleo usado pela administração é renomeado
- **THEN** o JSON público não muda sem alteração explícita do mapeador web
