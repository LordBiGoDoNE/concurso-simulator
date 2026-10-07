# Tasks

Implementação já verificada na branch preservada `feat/application-foundation`, agora repartida em PRs de tarefas. Nesta épica, as caixas representam integração após aprovação dos PRs, não apenas implementação em branches externas. Java 25 LTS e PostgreSQL 18 foram escolhidos pelo usuário.

## 1. Estrutura e builds

- [ ] 1.1 Criar `backend/` com Java 25, Spring Boot e Gradle Wrapper, registrar versões estáveis compatíveis e verificar um build limpo pelo wrapper.
- [ ] 1.2 Criar `frontend/` com React, TypeScript, Vite e lockfile; verificar instalação reproduzível e build, mantendo os arquivos estáticos da raiz intactos.
- [ ] 1.3 Documentar pré-requisitos, versões, diretórios e comandos de build; verificar os comandos em um clone limpo.

## 2. Banco e configuração

- [ ] 2.1 Criar Compose em `infra/` para PostgreSQL 18 com volume persistente, healthcheck e bind em loopback; verificar conexão e persistência após reinicialização sem remover o volume.
- [ ] 2.2 Adicionar exemplos de configuração sem segredos e ignorar configurações locais; verificar que credenciais reais não aparecem no Git nem nos artefatos públicos.
- [ ] 2.3 Integrar Flyway e migração inicial do marcador técnico; testar banco vazio, reinicialização idempotente e falha de migração em bancos Testcontainers isolados.
- [ ] 2.4 Documentar inicialização, parada e preservação do volume; verificar a sequência completa sem comandos destrutivos implícitos.

## 3. API de plataforma

- [ ] 3.1 Implementar `GET /api/v1/status` com checagem de banco e timeout finito; testar HTTP 200/UP e HTTP 503/DOWN após indisponibilidade, sem revelar informações internas.
- [ ] 3.2 Configurar Spring Security e CORS com origens explícitas; testar origem permitida, origem desconhecida e bloqueio de rotas não públicas.
- [ ] 3.3 Documentar o endpoint em OpenAPI e exemplos de chamada; verificar automaticamente schema JSON e códigos HTTP contra o contrato.

## 4. Entrada do frontend

- [ ] 4.1 Criar página inicial com status, nova tentativa e link configurável para as aulas; testar API disponível, resposta 503, erro de rede e timeout.
- [ ] 4.2 Configurar URLs públicas por ambiente sem segredos; verificar o build e documentar execução local e o endereço do material estático.
- [ ] 4.3 Verificar navegação por teclado e apresentação em tela estreita e larga, mantendo o acesso às aulas mesmo com backend parado.

## 5. Integração e regressão

- [ ] 5.1 Adicionar CI da aplicação com builds e testes isolados; verificar sua execução no PR sem alterar o workflow de publicação atual nem usar credenciais de produção.
- [ ] 5.2 Executar montagem do site com `scripts/build_pages.py`, conferência de links e `check_math.py`; verificar os nove módulos, 90 resoluções, fontes nomeadas e âncoras.
- [ ] 5.3 Abrir o pacote montado offline e verificar fórmulas, resolução recolhida e gabarito separado, com API e banco desligados.
- [ ] 5.4 Executar o fluxo completo local de clone limpo até a página inicial, registrar evidências e confirmar que nenhuma funcionalidade de questões, login ou simulados entrou neste incremento.

## Workflow follow-up

- Solicitar aprovação dos artefatos antes de executar estas tarefas.
- Submeter a implementação por PR, sem merge automático.
- Arquivar a mudança somente após implementação e verificação, consolidando as especificações aceitas.
