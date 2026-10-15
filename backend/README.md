# Backend

Base inicial: **Java 25 LTS**, **Spring Boot 4.1.1**, **Gradle 9.8.0** via Wrapper, com Kotlin DSL (`build.gradle.kts`).

Persistência de domínio: **JPA/Hibernate**, por contratos de repository e modelos na infraestrutura. Domínio/UseCases são Java puro, conforme [ADR 0001](../docs/architecture/adr/0001-domain-and-application-boundaries.md). Flyway cria/migra schema; Hibernate somente valida, com OSIV desativado. ArchUnit protege as fronteiras. JDBC permanece na prontidão técnica e nas sessões Spring Session, nunca no UseCase de identidade.

O módulo `questionbank` oferece catálogo de cinco disciplinas/51 assuntos/dois cargos de Limeira 01/2026, revisões imutáveis, persistência JPA, importação/editorial local deliberado e consulta pública sem gabaritos. [Catálogo](../docs/question-bank-catalog.md), [API](../docs/question-public-api.md) e [operações locais](../docs/question-local-operations.md). A curadoria inicial tem 250 revisões elegíveis verificadas em banco local isolado, 50 por disciplina; essa evidência não popula outros bancos nem publica a aplicação.

Requer JDK 25 e acesso à internet no primeiro build. Não requer Gradle global. O Wrapper fixa a versão e valida o SHA-256 da distribuição oficial.

```sh
cd backend
./gradlew clean build
```

No Windows, use `gradlew.bat clean build`. Configure `JAVA_HOME` para o JDK 25 caso o Java padrão seja outro. A toolchain também exige JDK 25 completo.

Os testes requerem Docker disponível: Testcontainers cria bancos PostgreSQL **18.6** isolados e os encerra ao terminar, sem acessar o volume local.

Identidade opcional Google e tentativas privadas com histórico/retomada pela conta. Modelo e privacidade: `docs/identity-storage.md`; sessões/CSRF/contratos: `docs/session-access.md`; configuração e smoke Google real: `docs/google-login.md`. Login desativado por padrão, sem credenciais nem chamadas externas no startup. Testes de login usam provedor OIDC local, nunca Google real.

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

Resposta: HTTP 200 `{"status":"UP"}`. Se o banco ficar indisponível após o startup: HTTP 503 `{"status":"DOWN"}`. O contrato está em `openapi.yaml`, não em uma rota pública de documentação. Auth/config, csrf e entrada/callback Google são exceções explícitas. Me/histórico exigem conta; tentativas aceitam o ator verificado da conta ou visitante e verificam posse no núcleo. Escritas/logout exigem CSRF; consulta pública de questões continua stateless. Rotas/métodos restantes são negados.

Configurações externas: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_CONNECTION_TIMEOUT_MS`, `DB_QUERY_TIMEOUT_SECONDS`, `CORS_ORIGINS`, `SERVER_PORT`, `SERVER_ADDRESS` e `SESSION_TIMEOUT` (30m por padrão). Login usa as variáveis documentadas em `docs/google-login.md`. Os defaults estão em `src/main/resources/application.yaml`. `DB_PASSWORD` é obrigatório. Ao personalizar `DB_URL`, mantenha `connectTimeout=2&socketTimeout=3` para limitar falhas de conexão/socket. CORS usa origens explícitas, sem wildcard; não substitui autenticação.

## Simulados e tentativas

O módulo `mockexam` compõe de 1 a 100 questões elegíveis, preserva versões/ordens e
corrige no servidor após conclusão irreversível. Conta salva/retoma e lista histórico;
visitante é temporário por 24h e depende da sessão válida, sem transferência no login.
Dados privados são projetados por DTOs próprios; criação/respostas/conclusão usam
comandos idempotentes/versão esperada. Flyway V5 é aditiva, com JPA validate.

[Domínio](../docs/mock-exam-domain.md), [aplicação](../docs/mock-exam-application.md),
[persistência](../docs/mock-exam-persistence.md), [HTTP/OpenAPI](../docs/mock-exam-http.md)
e [rollback/reinício isolado](../docs/mock-exam-operational-verification.md).
O verificador operacional deliberado usa somente fixtures/testcontainers e JAR anterior
aprovado; nenhum seed/endpoint de fixture entra no bootJar. Não é deployment público.

Para conservar artefatos locais ignorados em build, a verificação incremental pode usar
`gradlew.bat test bootJar` (ou `./gradlew test bootJar`). Configure JAVA_HOME para um
JDK25 completo compatível com sua máquina.
