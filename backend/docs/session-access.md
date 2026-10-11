# Sessões e acesso

Revisão arquitetural de 2026-10-08: configuração em `identity.infrastructure`, contratos HTTP/principal em `identity.web`. Sessões continuam infraestrutura técnica Spring Session JDBC; não são entidades de negócio nem precisam de um UseCase que apenas repasse chamadas. Persistência de identidade JPA usa o JpaTransactionManager compartilhado com a integração JDBC, validada nos testes HTTP reais. Nenhum controller acessa o banco diretamente.

Spring Session JDBC usa a migração V3 (schema PostgreSQL oficial da versão 4.1.1); Flyway é o único inicializador. Cookie SESSION host-only, Path=/, HttpOnly, SameSite=Lax, Secure por padrão. Para HTTP local, use explicitamente `SPRING_PROFILES_ACTIVE=local`. Use o mesmo hostname para frontend/API, por exemplo 127.0.0.1 nas duas portas.

Sessões expiram após 30 minutos sem atividade (`SESSION_TIMEOUT`, exemplo `15m`). Spring Session remove expiradas periodicamente. O UUID é o único dado do principal; nenhum token Google fica nele. O repositório pode recuperar a sessão após restart; mudanças incompatíveis de serialização exigem expirar sessões antigas no deployment.

Contratos em `openapi.yaml`: GET me → UUID ou 401 JSON; GET csrf → token/headerName e no-store; POST auth/logout → 204 com CSRF válido, 403 sem ele. Logout revoga só a sessão atual; obter novo token após login/expiração. Não há login real neste grupo e nenhum endpoint de autenticação simulada é exposto.

`/api/v1/me` permite ao frontend consultar a identidade da sessão sem ler o cookie HttpOnly. A regra de acesso do Spring Security exige autenticação não anônima e `UserPrincipal`; `ApiAccessFailureHandler` responde 401 JSON/no-store para sessões ausentes, expiradas ou com principal incompatível, antes do controller. Outras rotas negadas continuam retornando 403. O controller recebe `@AuthenticationPrincipal` tipado e apenas monta `MeResponse`, sem repetir a rejeição de autenticação.

`/api/v1/csrf` fornece o token e o nome do header necessários ao POST de logout. Spring Security gera e valida o token; o controller apenas monta `CsrfResponse`. O endpoint atende também visitantes: materializar o token pode criar uma sessão anônima, mas não autentica ninguém. Ambos os DTOs são records web, não entidades nem casos de uso artificiais; JSON/OpenAPI e no-store permanecem inalterados.

Exemplo visitante (com API local em execução):

```sh
curl -i http://127.0.0.1:8080/api/v1/me
curl -c /tmp/concurso-cookies -s http://127.0.0.1:8080/api/v1/csrf
# Copie token do JSON anterior para TOKEN; headerName informa X-CSRF-TOKEN.
curl -i -b /tmp/concurso-cookies -X POST -H "X-CSRF-TOKEN: $TOKEN" http://127.0.0.1:8080/api/v1/auth/logout
```

Resultados esperados: 401, token JSON com cookie, 204. Testes executam a mesma sequência HTTP com cookies reais e PostgreSQL isolado. CORS permite credenciais somente nas rotas de sessão para origens explícitas; `/api/v1/status` continua público e não cria sessão por consulta. Qualquer futura rota privada requer autorização própria; `/api/**` não foi liberado genericamente.

Prontidão não deve depender da leitura **nem do commit** da sessão: `permitAll`/cadeia stateless, sozinhos, não impedem o filtro Spring Session externo de acessar JDBC quando há cookie. `SessionIndependentRoutesFilter` desvia `/api/v1/status` antes de envolver a requisição; a cadeia stateless preserva CORS/headers, permite somente GET e mantém a consulta SQL real UP/DOWN. Cookies ausentes, autenticados e desconhecidos não alteram o contrato nem renovam/invalidam a sessão.

Na API Spring Session 4.1.1, `doFilter` é final e não há `shouldNotFilter`: a extensão usa `doFilterInternal`. `SessionConfiguration` substitui somente o alvo `springSessionRepositoryFilter` após inicialização, com o mesmo repository/resolver de cookie; o proxy único e a ordem do Boot ficam intactos. Testes verificam registro/ordem, ausência de atualização/cookie e HTTP JSON 503 em prazo finito após parar PostgreSQL 18.6. O desvio é restrito às rotas técnicas explícitas; não torna endpoints privados tolerantes à indisponibilidade de banco nem elimina a necessidade de revisar mudanças de versão do framework.

Valide com `./gradlew clean build`. Inclui rejeição na segurança de contexto ausente, principal incompatível, autenticação não concluída e principal anônimo; sessão CSRF de visitante não autentica. Também cobre logout CSRF entre sessões, recuperação por novo repositório JDBC, timeout/limpeza, CORS/preflight e flags de cookie em perfis local/default/produção.
