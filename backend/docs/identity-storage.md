# Identidade persistida — primeiro incremento

Migração `V2__user_identity.sql`: `app_user` guarda somente UUID e criação; `external_identity` guarda o vínculo Google (provider, issuer canônico e subject) com chave única e FK para o UUID. Não há nome, foto, e-mail, senha ou tokens. O subject é opaco: não é e-mail nem deve ser normalizado como um.

`ResolveExternalIdentityUseCase.execute` resolve identidade externa em UUID, **não autentica**. Recebe `ExternalIdentity` somente de adaptador OIDC já validado, nunca de input HTTP público. O VO protege invariantes (limites/não vazio) e não normaliza subject. O UseCase depende de `IdentityRepository` e `UnitOfWork`, não de Google, SQL, Spring ou JPA. Não há endpoint público de criação de identidade neste incremento.

`JpaIdentityRepository` mapeia o VO para modelos JPA privados da infraestrutura. Uma transação cria usuário e vínculo; o flush detecta unicidade antes do UUID retornar. Apenas a constraint `external_identity_key` vira `IdentityAlreadyLinkedException`. `SpringUnitOfWork` executa transações independentes: o concorrente perdedor sofre rollback completo (incluindo usuário), depois o UseCase consulta o vencedor em nova transação. Falhas de chave do usuário/FK/banco não são tratadas como sucesso. Não generalizar REQUIRES_NEW a outros casos de uso.

O EmbeddedId usa a chave natural UNIQUE NOT NULL de V2; o ORM não adiciona uma PK nem modifica a migração. UUIDs atribuídos pela aplicação; created_at recebe o default do PostgreSQL. Flyway é o único dono do schema, Hibernate `ddl-auto=validate` e `open-in-view=false`. Reiniciar EntityManagerFactory mantém o vínculo. Referência: [ADR 0001](../../docs/architecture/adr/0001-domain-and-application-boundaries.md).

PostgreSQL `logServerErrorDetail=false` é aplicado pelo datasource para não incluir valores pessoais nos erros de constraints. Logs de bind/extract do Hibernate ficam OFF. A identidade e modelos de persistência não devem ser serializados/registrados. Não ativar logs sensíveis nem imprimir exceções com payloads; o adaptador de login fornece apenas falha genérica.

`UserPrincipal` é serializável e contém somente UUID interno, preparando sessões JDBC futuras sem guardar claims ou tokens Google. Não contém permissões de administrador.

O vínculo é dado pessoal pseudonimizado, não anônimo. Deve permanecer privado, sem logs de subject, acesso público ou exposição pelo frontend. Sessões, consentimento OAuth, logout e políticas de operação/deployment serão implementados nos próximos grupos; este incremento não cria histórico nem sincronização.

Verificação, em `backend/`, com JDK 25 e Docker:

```sh
./gradlew --no-daemon --console=plain clean build
```

`IdentityStorageTests` usa PostgreSQL 18.6 isolado: migrações/upgrade V1, restart JPA, 12 chamadas concorrentes sem órfãos/logs de subject, namespaces provider/issuer, transação chamadora, classificação específica de conflitos e schema incompatível recusado sem reparo automático. Testes Java puro do VO/UseCase e ArchUnit cobrem invariantes, fluxo/rollback e dependências. Nenhum teste usa banco local ou credenciais Google reais. A persistência genérica não habilita um segundo provedor em produção.

Flyway aplica V2 automaticamente no startup. Não altere V1 nem habilite baseline automático; schemas existentes sem histórico exigem revisão própria. Rollback da aplicação não apaga as novas tabelas ou identidades.
