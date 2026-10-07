# Base local da aplicação

Requer JDK 25 completo, Node 24 LTS, Docker/Compose v2 e Python 3. Backend Spring Boot 4.1.1; banco PostgreSQL 18.6; frontend React/TypeScript/Vite.

Após integrar os PRs de backend, frontend e banco na épica:

1. Siga `infra/README.md` para definir uma senha local e iniciar PostgreSQL.
2. Em `backend/`, defina `DB_PASSWORD` com a mesma senha e execute `./mvnw spring-boot:run`.
3. Em `frontend/`, execute `npm ci` e `npm run dev`.
4. Abra http://127.0.0.1:5173. A API em http://127.0.0.1:8080/api/v1/status deve informar UP.

Testes: `./mvnw clean verify` no backend; `npm test`, `npm run build` e `npm run test:e2e` no frontend. Defina `STUDY_ROOT` com o pacote montado para incluir a regressão offline. Comandos completos estão nos READMEs de cada diretório.

O material estático e suas URLs permanecem intactos. Não há login, banco de questões ou simulados neste incremento. Preview estático é manual, conforme `AGENTS.md`; Java/PostgreSQL e React não são publicados no Pages.
