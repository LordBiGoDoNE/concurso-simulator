# Sessões e acesso

Spring Session JDBC usa a migração V3 (schema PostgreSQL oficial da versão 4.1.1); Flyway é o único inicializador. Cookie SESSION host-only, Path=/, HttpOnly, SameSite=Lax, Secure por padrão. Para HTTP local, use explicitamente `SPRING_PROFILES_ACTIVE=local`. Use o mesmo hostname para frontend/API, por exemplo 127.0.0.1 nas duas portas.

Sessões expiram após 30 minutos sem atividade (`SESSION_TIMEOUT`, exemplo `15m`). Spring Session remove expiradas periodicamente. O UUID é o único dado do principal; nenhum token Google fica nele. O repositório pode recuperar a sessão após restart; mudanças incompatíveis de serialização exigem expirar sessões antigas no deployment.

Contratos em `openapi.yaml`: GET me → UUID ou 401 JSON; GET csrf → token/headerName e no-store; POST auth/logout → 204 com CSRF válido, 403 sem ele. Logout revoga só a sessão atual; obter novo token após login/expiração. Não há login real neste grupo e nenhum endpoint de autenticação simulada é exposto.

Exemplo visitante (com API local em execução):

```sh
curl -i http://127.0.0.1:8080/api/v1/me
curl -c /tmp/concurso-cookies -s http://127.0.0.1:8080/api/v1/csrf
# Copie token do JSON anterior para TOKEN; headerName informa X-CSRF-TOKEN.
curl -i -b /tmp/concurso-cookies -X POST -H "X-CSRF-TOKEN: $TOKEN" http://127.0.0.1:8080/api/v1/auth/logout
```

Resultados esperados: 401, token JSON com cookie, 204. Testes executam a mesma sequência HTTP com cookies reais e PostgreSQL isolado. CORS permite credenciais somente nas rotas de sessão para origens explícitas; `/api/v1/status` continua público e não cria sessão por consulta. Qualquer futura rota privada requer autorização própria; `/api/**` não foi liberado genericamente.

Valide com `./gradlew clean build`. Inclui logout CSRF entre sessões, recuperação por novo repositório JDBC, timeout/limpeza, CORS/preflight e flags de cookie em perfis local/default/produção.
