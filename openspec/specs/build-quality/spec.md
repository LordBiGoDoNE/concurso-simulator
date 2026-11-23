# build-quality Specification

## Purpose
Manter o build e os testes do backend, do frontend e do CI verificáveis e baratos de revisar: declarações de build sem duplicação, verificação estática e formatação automáticas, um relógio único da aplicação, PostgreSQL de teste compartilhado com banco isolado por classe, relatórios de cobertura sem gate, um único transporte HTTP no frontend e cobertura do CI sem jobs duplicados.

## Requirements

### Requirement: Declarações de build sem duplicação

O build do backend SHALL declarar a versão do Spring Boot uma única vez, SHALL NOT
declarar dependência já fornecida com a mesma versão por outra declarada, e SHALL
preservar nomes, grupos, descrições, dependências e argumentos das tarefas
operacionais ao reorganizá-las.

#### Scenario: Versão única do Spring Boot

- **WHEN** o build resolve o BOM do Spring Boot
- **THEN** a versão vem do plugin declarado em `plugins`
- **AND** nenhuma outra linha do build repete essa versão

#### Scenario: Tarefas operacionais preservadas

- **WHEN** `./gradlew tasks --all` e `./gradlew help --task <nome>` são executados para
  cada tarefa operacional antes e depois da reorganização
- **THEN** nomes, grupos, descrições e dependências são os mesmos
- **AND** os argumentos por propriedade (`localArgs`, `templateAction`, `rollbackJar`)
  produzem a mesma linha de comando

### Requirement: Verificação estática do backend

O build do backend SHALL falhar quando o código principal gerar advertência do
compilador com `-Xlint:all` (exceto exclusões nominais documentadas no build) ou
quando o código Java não seguir o formatador configurado, e SHALL executar essas
verificações em `./gradlew build`.

#### Scenario: Advertência do compilador

- **WHEN** uma mudança introduz no código principal uma advertência não excluída
  (por exemplo, uso de tipo bruto)
- **THEN** `./gradlew build` falha e aponta o arquivo e a linha

#### Scenario: Formatação divergente

- **WHEN** um arquivo Java não está formatado conforme o Spotless
- **THEN** `./gradlew build` falha em `spotlessCheck`
- **AND** `./gradlew spotlessApply` corrige o arquivo sem mudar comportamento

### Requirement: Relógio único da aplicação

A aplicação SHALL obter o relógio de um único bean `Clock`, e o código principal SHALL
NOT chamar as fábricas `Clock.system*` fora da configuração desse bean.

#### Scenario: Casos de uso com o relógio do contexto

- **WHEN** a aplicação monta os casos de uso de simulados, revisão, curadoria,
  importação e acesso administrativo
- **THEN** todos recebem o mesmo bean `Clock`

#### Scenario: Nova fábrica de relógio

- **WHEN** uma classe do código principal fora da configuração do relógio chama
  `Clock.systemUTC()`
- **THEN** o teste de arquitetura falha nomeando a classe

### Requirement: PostgreSQL de teste compartilhado e isolado por classe

Os testes do backend que usam PostgreSQL SHALL declarar a imagem em um único lugar,
SHALL compartilhar um servidor por execução quando não alteram o estado do servidor e
SHALL receber cada classe um banco vazio e exclusivo, migrado pelo Flyway como hoje.
Testes que param, pausam ou reconfiguram o servidor SHALL manter servidor próprio.

#### Scenario: Banco vazio por classe

- **WHEN** duas classes de teste usam o servidor compartilhado na mesma execução
- **THEN** cada uma vê só os dados que ela própria criou
- **AND** a suíte passa com qualquer ordem de execução das classes

#### Scenario: Teste de indisponibilidade

- **WHEN** um teste para o servidor para verificar falha de banco
- **THEN** ele usa um servidor próprio, com a mesma imagem declarada no suporte
- **AND** as demais classes da execução não são afetadas

### Requirement: Relatórios de cobertura sem gate

O backend e o frontend SHALL gerar relatório de cobertura sob comando local e SHALL
NOT reprovar build ou CI por percentual de cobertura.

#### Scenario: Relatório do backend

- **WHEN** o desenvolvedor executa `./gradlew test jacocoTestReport`
- **THEN** relatórios HTML e XML ficam em `backend/build/reports/jacoco/`

#### Scenario: Relatório do frontend

- **WHEN** o desenvolvedor executa `npm run test:coverage`
- **THEN** o resumo aparece no terminal e os relatórios ficam em `frontend/coverage/`,
  fora do versionamento

### Requirement: Verificação estática do frontend

O CI SHALL reprovar o frontend quando o ESLint encontrar erro, quando o TypeScript
encontrar erro em `src`, `test`, `e2e` ou nos configs do Playwright e do Vite, ou
quando a formatação divergir do Prettier.

#### Scenario: Erro de tipo em teste de navegador

- **WHEN** um arquivo de `e2e/` ou um `playwright.*.config.ts` tem erro de tipo
- **THEN** `npm run typecheck` falha e o job `frontend` do CI falha

#### Scenario: Violação de regra de hooks

- **WHEN** um componente chama um hook condicionalmente
- **THEN** `npm run lint` falha nomeando o arquivo e a regra

#### Scenario: Supressão de regra

- **WHEN** uma violação existente não tem correção inequívoca coberta por teste
- **THEN** ela é suprimida só na linha, com justificativa
- **AND** a regra continua ativa para o restante do código

### Requirement: Transporte HTTP único no frontend

O frontend SHALL resolver a URL base da API em um único lugar e SHALL executar todas
as requisições à API pelo mesmo cliente, que aplica timeout, abort externo e
interrupção depois do corpo, preservando para cada módulo as credenciais, o timeout,
as mensagens e o mapeamento de erros atuais. O status da API SHALL ser validado antes
de ser exibido.

#### Scenario: Timeout

- **WHEN** a API não responde dentro do timeout do módulo
- **THEN** a requisição rejeita com `AbortError`
- **AND** timer e ouvintes do sinal são liberados

#### Scenario: Abort externo depois do corpo

- **WHEN** o chamador aborta depois que o corpo chegou e antes da validação
- **THEN** a requisição rejeita com `AbortError` e nenhum dado é entregue

#### Scenario: Políticas preservadas

- **WHEN** a consulta pública de questões é feita
- **THEN** ela não envia credenciais, como hoje
- **AND** as requisições de identidade e de tentativas continuam enviando credenciais
  e mapeando 401, 403 e 404 como hoje

#### Scenario: Status inválido

- **WHEN** `/api/v1/status` responde com corpo fora do contrato
- **THEN** a interface mostra a API como indisponível

#### Scenario: Requisição fora do cliente

- **WHEN** um arquivo de produção de `src` diferente do cliente HTTP chama `fetch`
- **THEN** `npm run lint` falha
- **AND** a única exceção nominal é `src/native-lesson.ts`, que carrega documentos
  estáticos de aula e não a API; testes (`*.test.*`) e `src/test-setup.ts` ficam fora
  da regra

### Requirement: Cobertura do CI sem duplicação

O CI da `main` SHALL executar uma única vez cada verificação do material estático e
SHALL executar as suítes Playwright reais administrativa e de prática por assunto em
Chromium. Os workflows de `stable` SHALL NOT mudar.

#### Scenario: Verificação de links

- **WHEN** um PR para a `main` é aberto
- **THEN** `check_links.py` roda uma vez, no job `study-material`
- **AND** `test_build_pages.py` continua rodando em `application.yml`

#### Scenario: Suítes reais no CI

- **WHEN** o workflow de aplicação roda
- **THEN** `administrative.real.spec.ts` e `topic-practice.real.spec.ts` executam em
  Chromium com PostgreSQL isolado e OIDC local
- **AND** localmente os mesmos configs continuam iniciando as fixtures com `--offline`
