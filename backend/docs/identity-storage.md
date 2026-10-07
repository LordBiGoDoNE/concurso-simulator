# Identidade persistida — primeiro incremento

Migração `V2__user_identity.sql`: `app_user` guarda somente UUID e criação; `external_identity` guarda o vínculo Google (provider, issuer canônico e subject) com chave única e FK para o UUID. Não há nome, foto, e-mail, senha ou tokens. O subject é opaco: não é e-mail nem deve ser normalizado como um.

`GoogleIdentityService.resolveVerifiedSubject` é uma operação interna de persistência, **não autentica**. Só deverá ser chamada depois da validação OIDC do grupo de login. Não há endpoint público de criação/consulta de identidade neste incremento; todas as regras HTTP existentes permanecem.

Uma transação cria usuário e vínculo. Se dois primeiros logins competirem pela mesma identidade, a restrição única faz rollback da transação perdedora (incluindo o usuário), que lê então o vínculo vencedor. Isso mantém o UUID estável e evita usuários órfãos. Novas instâncias do serviço consultam o mesmo vínculo persistido.

`UserPrincipal` é serializável e contém somente UUID interno, preparando sessões JDBC futuras sem guardar claims ou tokens Google. Não contém permissões de administrador.

O vínculo é dado pessoal pseudonimizado, não anônimo. Deve permanecer privado, sem logs de subject, acesso público ou exposição pelo frontend. Sessões, consentimento OAuth, logout e políticas de operação/deployment serão implementados nos próximos grupos; este incremento não cria histórico nem sincronização.

Verificação, em `backend/`, com JDK 25 e Docker:

```sh
./gradlew --no-daemon --console=plain clean build
```

`IdentityStorageTests` usa PostgreSQL 18.6 isolado, cobrindo banco vazio, upgrade V1 sem perda do marcador, idempotência, UUID estável, 12 chamadas concorrentes, FK/unicidade, ausência de colunas pessoais adicionais, inputs inválidos e serialização mínima. Nenhum teste usa o banco local ou credenciais Google reais.

Flyway aplica V2 automaticamente no startup. Não altere V1 nem habilite baseline automático; schemas existentes sem histórico exigem revisão própria. Rollback da aplicação não apaga as novas tabelas ou identidades.
