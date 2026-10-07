# Design

## Context

Veja `proposal.md` para motivação. `scripts/build_pages.py` monta uma lista explícita de arquivos públicos, enriquece Matemática e compacta fontes. `.github/workflows/pages.yml` publica a raiz aprovada e guarda previews em `pages-store`. Hoje não há aplicação Java, frontend compilado nem banco; os arquivos-base V3 permanecem na raiz.

## Goals / Non-Goals

**Goals:** criar uma base local testável, com limites claros e migrações, preservando a publicação atual.

**Non-Goals:** não migrar as 510 questões nesta mudança; não modelar antecipadamente todo o domínio; não implementar login nem publicar endpoints de dados pessoais ou gabaritos. Não substituir a página inicial oficial pela nova aplicação ainda.

## Decisions

### Monorepo incremental

Propor `backend/`, `frontend/` e `infra/`, sem mover os arquivos estáticos existentes. Manter scripts Python e publicação atuais. Alternativa rejeitada: substituir a raiz pelo frontend agora, pois quebraria o pacote offline e ampliaria o risco da mudança.

### Java e monólito modular

Usar Java 25 LTS (alteração solicitada pelo usuário), Maven Wrapper 3.3.4 com Maven 3.9.16 e Spring Boot 4.1.1. Usar organização por funcionalidade; criar apenas o módulo de plataforma nesta etapa, sem módulos vazios para capacidades futuras. Alternativa rejeitada: microsserviços, que adicionariam deploys e comunicação distribuída sem necessidade demonstrada.

### PostgreSQL e migrações

Usar PostgreSQL 18 (alteração solicitada pelo usuário) em Docker Compose, volume nomeado montado em `/var/lib/postgresql`, conforme o layout da imagem oficial a partir da versão 18. Credenciais locais via `.env` não versionado e `.env.example` sem segredos. Bind do banco em loopback. Flyway controla o schema; não usar criação automática destrutiva por ORM. A primeira migração cria somente um marcador técnico da aplicação, sem tabelas artificiais de usuários ou questões. Testcontainers usa a mesma versão major. Não incluir comandos de exclusão do volume na inicialização padrão.

### Contrato REST e segurança inicial

`GET /api/v1/status` consulta a disponibilidade do banco e devolve somente UP/DOWN. Usar timeout finito configurável para a consulta e para a chamada do frontend. Falha de conexão no startup pode impedir a API de iniciar; HTTP 503 cobre perda de conexão durante execução. Documentar em OpenAPI e verificar schema/status nos testes. Endpoints internos e detalhes de saúde não serão públicos; configurar Spring Security para permitir apenas o status inicial e negar por padrão demais rotas. CORS local permite exclusivamente a origem configurada, sem wildcard com credenciais. O mecanismo de login será decidido em outra mudança.

### Frontend e coexistência

Propor React, TypeScript e Vite com lockfile. Página inicial mínima: disponibilidade da API, botão de nova tentativa e link para as aulas. Configurar URL da API e URL do material por ambiente; variáveis de frontend são públicas, nunca segredos. Evitar SPA com rotas nesta etapa. Desenvolver localmente sem alterar o workflow de Pages; validar o build do frontend no novo CI. Publicação da aplicação completa será outra mudança, com estratégia própria de previews de backend e banco.

### Ambiente e testes

Docker Compose inicia o banco; Maven Wrapper e comandos npm documentados iniciam API e frontend. Portas propostas: banco 5432 (loopback), API 8080, frontend 5173. Novo CI executa build, testes HTTP, migrações em PostgreSQL Testcontainers, build e testes de estados da página inicial. Manter os testes Python de montagem do site. Nenhum teste usa banco persistente de desenvolvimento.

## Risks / Trade-offs

- [Duas experiências temporárias] → manter o site oficial como entrada e explicar que o frontend novo ainda é local.
- [Confundir build estático com arquivos-base] → documentar como gerar e testar o pacote offline; não exigir backend para as aulas.
- [Banco acessível ou senha versionada] → loopback, configurações externas, exemplos sem segredos e revisão do artefato de publicação.
- [Prontidão lenta quando banco cai] → timeouts finitos e teste de indisponibilidade.
- [Versões e bibliotecas incompatíveis] → fixar versões compatíveis e comprová-las em builds; Spring Boot e integração OpenAPI exatos serão registrados durante a implementação.

## Migration Plan

Implementar a base em branch após aprovação, executar CI e regressão do material, abrir PR. Não provisionar infraestrutura remota nem alterar a entrada oficial. Reverter o commit de implementação remove a stack sem tocar nas aulas; preservar o volume local para evitar perda de dados. Não arquivar a mudança OpenSpec antes de implementar e verificar as tarefas.

## Open Questions

- Hospedagem do backend/banco e estratégia de previews da aplicação completa: decidir na mudança de deployment, antes de colocar a API online.
- Login próprio ou provedor externo: decidir na mudança de identidade, antes de criar dados privados.
- Meta de 50–100 questões por disciplina ou por assunto: confirmar na mudança do banco de questões; não interfere nesta base técnica.
