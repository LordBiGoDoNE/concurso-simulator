# Backend

Base inicial: **Java 25 LTS**, **Spring Boot 4.1.1**, **Gradle 9.8.0** via Wrapper, com Kotlin DSL (`build.gradle.kts`).

Requer JDK 25 e acesso à internet no primeiro build. Não requer Gradle global. O Wrapper fixa a versão e valida o SHA-256 da distribuição oficial.

```sh
cd backend
./gradlew clean build
```

No Windows, use `gradlew.bat clean build`. Configure `JAVA_HOME` para o JDK 25 caso o Java padrão seja outro. A toolchain também exige JDK 25 completo.

Os testes requerem Docker disponível: Testcontainers cria bancos PostgreSQL **18.6** isolados e os encerra ao terminar, sem acessar o volume local.

Identidade opcional Google, sem histórico sincronizado nesta etapa. Modelo e privacidade: `docs/identity-storage.md`; sessões/CSRF/contratos: `docs/session-access.md`; configuração e smoke Google real: `docs/google-login.md`. Login desativado por padrão, sem credenciais nem chamadas externas no startup. Testes de login usam provedor OIDC local, nunca Google real.

## Executar localmente

Inicie o banco conforme `../infra/README.md`. Depois, neste diretório:

```sh
export DB_PASSWORD='sua-senha-local'
export SPRING_PROFILES_ACTIVE=local # HTTP local explícito; cookie Secure fora deste perfil
./gradlew bootRun
```

Use a mesma senha definida em `infra/.env`; não copie senhas para arquivos versionados. A API escuta em loopback por padrão. Flyway aplica migrações ao iniciar; uma falha impede a inicialização. Não há criação automática de schema por ORM.

```sh
curl -i http://127.0.0.1:8080/api/v1/status
```

Resposta: HTTP 200 `{"status":"UP"}`. Se o banco ficar indisponível após o startup: HTTP 503 `{"status":"DOWN"}`. O contrato está em `openapi.yaml`, não em uma rota pública de documentação. Auth/config, csrf e entrada/callback Google são exceções explícitas; me exige sessão, logout exige CSRF e demais rotas são negadas.

Configurações externas: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_CONNECTION_TIMEOUT_MS`, `DB_QUERY_TIMEOUT_SECONDS`, `CORS_ORIGINS`, `SERVER_PORT`, `SERVER_ADDRESS` e `SESSION_TIMEOUT` (30m por padrão). Login usa as variáveis documentadas em `docs/google-login.md`. Os defaults estão em `src/main/resources/application.yaml`. `DB_PASSWORD` é obrigatório. Ao personalizar `DB_URL`, mantenha `connectTimeout=2&socketTimeout=3` para limitar falhas de conexão/socket. CORS usa origens explícitas, sem wildcard; não substitui autenticação.

Na máquina atual, o JDK completo está em `/usr/lib/jvm/java-25-temurin-jdk`; o runtime Red Hat não contém `javac`. Se necessário:

```sh
JAVA_HOME=/usr/lib/jvm/java-25-temurin-jdk ./gradlew clean build
```
