# Verification

## Grupo 1 — identity-storage — 2026-10-07

Tarefas 1.1–1.3 implementadas e verificadas na branch `task/optional-google-identity/identity-storage`, ainda aguardando revisão/merge na épica da spec. **3/16 tarefas verificadas; 0/16 integradas.** As caixas do checklist permanecem pendentes até a aprovação do PR.

- Migração V2 aditiva cria somente usuário mínimo e vínculo Google. V1 não foi alterada; upgrade desde V1 preserva o marcador técnico.
- Serviço transacional mantém UUID entre chamadas/instâncias; corrida pela chave única faz rollback do usuário perdedor antes de consultar o vencedor.
- Principal serializável contém somente UUID. Nenhuma coluna de nome, foto, e-mail, senha ou tokens.
- `JAVA_HOME=/usr/lib/jvm/java-25-temurin-jdk ./gradlew --no-daemon --console=plain clean build`: aprovado; **12 testes**, sem falhas ou testes ignorados, com PostgreSQL 18.6 Testcontainers.
- Sete testes novos: banco vazio/idempotência, upgrade V1, estabilidade/diferenciação de identidades, 12 chamadas concorrentes sem órfãos, FK/unicidade/schema mínimo, inputs inválidos e serialização mínima. Cinco testes existentes de API/migração continuam aprovados.
- Documentação e comando de validação: `backend/docs/identity-storage.md`; descoberta pelo README do backend.

O serviço não valida tokens nem cria sessão: sua entrada deve vir somente do callback OIDC verificado, a implementar no grupo 3. Não há endpoint público de identidade ou login neste incremento. Nenhuma credencial Google foi usada, e main permanece intocada.

## Próxima dependência

O usuário autorizou grupos encadeados sem pausas em 2026-10-07. PRs seguintes têm como base a tarefa anterior; revisão e merges ficam para o final.

## Grupo 2 — session-access — 2026-10-07

Tarefas 2.1–2.4 implementadas na branch encadeada sobre identity-storage. Spring Session JDBC com migração V3, cookie seguro por padrão, perfil local explícito, CSRF e CORS; contratos me/csrf/logout documentados em OpenAPI. Testes HTTP reais exercitam exemplos, independência de sessões, logout, timeout/limpeza, recuperação por novo repositório JDBC, flags de cookie e preflight. Comparação automática das respostas JSON com OpenAPI.

Gradle clean build aprovado: 17 testes (12 anteriores + 5 novos). **7/16 verificadas, 0/16 integradas**. Nenhum endpoint de autenticação mock em produção; o fluxo OIDC é o próximo grupo. Documentação: `backend/docs/session-access.md`.

## Grupo 3 — google-oidc — 2026-10-07

Tarefas 3.1–3.4 implementadas sobre session-access: configuração desativada sem rede/segredos, startup recusado quando incompleta/insegura, fluxo code + PKCE, principal mínimo e redirect fixo. Provedor efêmero de teste exercita discovery/authorize/token/JWK/userinfo via HTTP e cookies reais. Verificados state/nonce/issuer/audience/assinatura/expiração/cancelamento/replay, rotação (cookie antigo inválido), novo login com mesmo UUID, ausência de contas criadas nas falhas e tokens em logs/sessões finais. CORS auth/config permite a consulta com credentials include.

Gradle clean build aprovado: **29 testes**, 12 novos casos neste grupo. **11/16 verificadas, 0/16 integradas**. Roteiro Google real em `backend/docs/google-login.md`, não executado por depender das credenciais do usuário. Recuperação de identidade/sessão após restart é coberta pelos testes de serviço/repositório; o navegador integrado é o grupo 5.
