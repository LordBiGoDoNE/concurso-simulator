# Backend

Base inicial: **Java 25 LTS**, **Spring Boot 4.1.1**, Maven **3.9.16** via Wrapper **3.3.4**.

Requer JDK 25 e acesso à internet no primeiro build. Não requer Maven global.

```sh
cd backend
./mvnw clean verify
```

No Windows, use `mvnw.cmd clean verify`. Configure `JAVA_HOME` para o JDK 25 caso o Java padrão seja outro.

Os testes requerem Docker disponível: Testcontainers cria bancos PostgreSQL **18.6** isolados e os encerra ao terminar, sem acessar o volume local.

## Executar localmente

Inicie o banco conforme `../infra/README.md`. Depois, neste diretório:

```sh
export DB_PASSWORD='sua-senha-local'
./mvnw spring-boot:run
```

Use a mesma senha definida em `infra/.env`; não copie senhas para arquivos versionados. A API escuta em loopback por padrão. Flyway aplica migrações ao iniciar; uma falha impede a inicialização. Não há criação automática de schema por ORM.

```sh
curl -i http://127.0.0.1:8080/api/v1/status
```

Resposta: HTTP 200 `{"status":"UP"}`. Se o banco ficar indisponível após o startup: HTTP 503 `{"status":"DOWN"}`. O contrato está em `openapi.yaml`, não em uma rota pública de documentação. Demais rotas são negadas; não há login neste incremento.

Configurações externas: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_CONNECTION_TIMEOUT_MS`, `DB_QUERY_TIMEOUT_SECONDS`, `CORS_ORIGINS`, `SERVER_PORT` e `SERVER_ADDRESS`. Os defaults estão em `src/main/resources/application.yaml`. `DB_PASSWORD` é obrigatório. Ao personalizar `DB_URL`, mantenha `connectTimeout=2&socketTimeout=3` para limitar falhas de conexão/socket. CORS usa origens explícitas, sem wildcard; não substitui autenticação futura.

Na máquina atual, o JDK completo está em `/usr/lib/jvm/java-25-temurin-jdk`; o runtime Red Hat não contém `javac`. Se necessário:

```sh
JAVA_HOME=/usr/lib/jvm/java-25-temurin-jdk ./mvnw clean verify
```
