# Proposal

## Why

O site atual atende à leitura de aulas, mas não possui backend nem persistência de tentativas. Precisamos de uma base executável para evoluir para banco de questões e simulados sem interromper o material já publicado.

## What Changes

- Adicionar, em uma implementação posterior à aprovação, backend Java/Spring Boot e frontend React/TypeScript/Vite em diretórios separados.
- Adicionar PostgreSQL local, migrações Flyway, verificação de disponibilidade e contrato REST inicial.
- Preparar builds reproduzíveis, testes com PostgreSQL real via Testcontainers e CI independente da publicação estática.
- Preservar arquivos, URLs e comportamento das aulas atuais; não há alteração incompatível nesta etapa.
- Fora de escopo: login, usuários, cadastro/importação de questões, simulados, geração parametrizada, progresso, painel administrativo e hospedagem do backend. Cada funcionalidade terá sua própria mudança.

## Capabilities

### New Capabilities

- `application-runtime`: execução local, prontidão da API, configuração segura, contrato inicial e verificação automatizada da aplicação.
- `study-material-access`: coexistência do novo frontend com as aulas estáticas publicadas e disponíveis offline.

### Modified Capabilities

Nenhuma. Ainda não há especificações consolidadas em `openspec/specs/`.

## Impact

Diretórios propostos: `backend/`, `frontend/` e `infra/`. A raiz e os scripts Python continuam sendo a fonte do material estático. Será necessária infraestrutura fora do GitHub Pages para executar Java e PostgreSQL, mas nenhum serviço remoto será provisionado nesta mudança. Esta proposta é planejamento, não implementação.
