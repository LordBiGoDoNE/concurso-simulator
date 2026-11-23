# Verification

## Integração aprovada — 2026-10-07

PRs #6 e #8 integrados em main; PRs #9, #10, #11 e #12 integrados em `epic/bootstrap-application-stack` após aprovação do usuário. CI Gradle integrado aprovado em https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/37665072528 e https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/37665078380 (backend, frontend e study-material).

Repetidos na integração: Gradle `clean build` com 5 testes PostgreSQL; `npm ci`, 5 testes Vitest, build e 4 testes Playwright; montagem das 108 páginas e conferência de 90 resoluções. Repetidos também em clone limpo `/tmp/opencode/concurso-gradle-integrated-clean`, incluindo Wrapper Gradle e regressão offline do novo pacote `/tmp/opencode/concurso-integrated-study`. Todos aprovados. Não houve mudança no conteúdo das aulas. A revisão final épica → main permanece pendente; OpenSpec não arquivado.

Os registros seguintes são históricos, preservados para rastreabilidade.

Este registro descreve a implementação original preservada em `feat/application-foundation`/PR #7. Ela foi repartida em PRs destinados à épica; os resultados abaixo não significam que a épica já integrou esses PRs. Reexecute a validação integrada após suas aprovações. O preview passou a ser manual no PR de organização #8.

## Ambiente local — 2026-10-06

As referências a Maven abaixo são evidências históricas do PR #7, não instruções atuais. O backend do PR #9 foi migrado para Gradle em 2026-10-07, com `./gradlew --no-daemon --console=plain clean build` aprovado e os mesmos 5 testes PostgreSQL aprovados. Gradle 9.8.0, Java 25, Spring Boot 4.1.1; distribuição e JAR do Wrapper conferidos contra SHA-256 oficiais. O CI deste PR #12 foi alinhado ao Gradle; continua aguardando integração dos PRs de implementação antes da validação conjunta.

- Java Temurin 25.0.4.1, Spring Boot 4.1.1, Maven 3.9.16 via Wrapper 3.3.4.
- PostgreSQL 18.6, Flyway 12.4.0 e Testcontainers 2.0.5 (dependências gerenciadas pelo Boot).
- Node 24.21.0, React 19.3.0, TypeScript 5.9.3, Vite 8.3.3, Vitest 5.0.3, Playwright 1.63.0.

## Resultados

- `./mvnw -B -ntp clean verify`: build aprovado, 5 testes, sem falhas ou testes ignorados. Verificados schema inicial, migração idempotente sem perda de marcador, startup interrompido por SQL inválido, contrato OpenAPI/HTTP, CORS, bloqueio de rotas e HTTP 503 após parar PostgreSQL.
- `npm ci`, `npm test`, `npm run build`: instalação reproduzível, 5 testes aprovados, compilação TypeScript e bundle aprovados. Timeout do frontend testado com relógio controlado.
- `STUDY_ROOT=... npm run test:e2e`: 4 testes Chromium aprovados, sem testes ignorados. Navegação por Tab/Enter, ausência de overflow em 390 e 1280px, disponibilidade e indisponibilidade da API, nove módulos e 90 resoluções abrindo/recolhendo offline, sem JavaScript, com fórmulas e gabaritos separados.
- Montagem Python, `check_links.py` e `check_math.py`: 108 páginas, 1757 referências locais, nove módulos e 90 respostas; 1000 fontes compactadas pela montagem existente.
- Artefato público inspecionado: sem backend, frontend, infra, OpenSpec, `.opencode` ou arquivos `.env`. Configurações locais ignoradas pelo Git; exemplos sem senhas.
- Compose em projetos de teste separados: conexão, healthcheck, persistência de linha após reinício e sequência `up --wait`, `stop`, `start --wait` verificadas. Volumes preservados; containers de teste locais parados ao terminar.
- Clone limpo do commit `0bf0770` em `/tmp/opencode/concurso-foundation-clean-copy`: builds, testes e navegador repetidos. Smoke com frontend real, API empacotada e PostgreSQL via Compose: página exibiu `API disponível.` após chamada cross-origin real. Serviços encerrados após o teste.

O primeiro smoke usou um banco de teste contendo uma tabela de persistência criada antes do Flyway; a inicialização foi corretamente recusada por schema não vazio sem histórico. O smoke foi repetido com banco novo, sem habilitar baseline automático ou apagar dados. Não use tabelas manuais no schema gerenciado antes da primeira migração.

## Revisão

PR de implementação: https://github.com/LordBiGoDoNE/concurso-simulator/pull/7, empilhado sobre o PR OpenSpec #6. Nenhum merge realizado. A primeira execução do CI aprovou backend e frontend, mas revelou que o verificador estático tentava validar `/src/main.tsx` do Vite como arquivo do site. O verificador agora exclui os diretórios da aplicação, mantendo os diretórios de aulas e previews; o workflow Pages permanece intacto.

CI corrigido aprovado em push e PR no commit `e9185ff`: backend, frontend (incluindo testes offline) e study-material. Execuções: https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/37529983894 e https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/37529988893.

O workflow Pages foi recusado antes de executar por regras do ambiente: `Branch "feat/application-foundation" is not allowed to deploy to github-pages due to environment protection rules.` Nenhuma regra de deploy foi alterada. Não há preview novo dessa branch; o site oficial permanece intacto. A publicação da aplicação completa continua fora de escopo. A mudança não será arquivada antes da revisão.
