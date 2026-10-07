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

## Grupo 4 — login-interface — 2026-10-07

Tarefas 4.1–4.3 implementadas sobre google-oidc: estados visitante/conectado/indisponível, Google desativado, navegação explícita, saída com CSRF fresco e credentials include, expiração reconhecida ao atualizar/retomar foco, falha genérica removida da URL e aulas sempre acessíveis. Nenhum storage de tokens nem promessa de histórico pronto.

Vitest **14 testes aprovados** (9 novos casos); TypeScript/Vite build aprovado. Playwright **5 testes aprovados**: teclado e ausência de overflow em 390/1280px incluindo consulta indisponível, login opcional, falha e logout; o sexto caso offline não foi executado nesta rodada sem STUDY_ROOT, será obrigatório no clone limpo do grupo 5. **14/16 verificadas, 0/16 integradas**. Documentação local atualizada no README frontend.

## Grupo 5 — identity-verification — 2026-10-07

Tarefas 5.1–5.2 implementadas sobre login-interface. Launcher exclusivamente de teste inicia PostgreSQL 18.6 isolado/API real/OIDC local; Playwright inicia frontend e navega por redirects/cookies reais. Nenhuma credencial Google real ou banco compartilhado. Job separado `identity-browser` no CI, sem publicação; limites de duração impedem jobs presos indefinidamente em downloads.

Verificação repetida no clone limpo `/tmp/opencode/concurso-identity-clean`, commit de código `5678168`:

| Verificação | Resultado |
| --- | --- |
| Gradle clean build, JDK 25 | 29 testes aprovados |
| npm ci / audit | 0 vulnerabilidades reportadas |
| Vitest / TypeScript / Vite build | 14 testes aprovados / build aprovado |
| Playwright interface + offline | 6 testes aprovados, nenhum ignorado |
| Playwright integrado real | 3 testes aprovados, nenhum ignorado |
| Material estático / links | 108 páginas, 1757 referências locais |
| Matemática offline, JS desativado | 9 módulos, 90 resoluções abre/fecha, gabaritos separados e fórmulas |
| OpenSpec validate --strict | aprovado |
| JAR de produção | launcher/provedor/registration de teste ausentes |
| Workflow pages.yml | sem alterações em relação a main; preview manual preservado |

O navegador integrado verificou visitante sem login, sessão após login, **recuperação da mesma sessão após reiniciar o contexto Spring real com PostgreSQL/cookie preservados**, novo login com mesmo UUID, duas identidades e sessões/dispositivos independentes, cookie HttpOnly/SameSite, CSRF 403, logout, inatividade real de 20s e cancelamento genérico com marcador removido da URL. Testes unitários/integrados do backend cobrem cookie Secure fora do perfil local, persistência mínima e negativos criptográficos.

**16/16 tarefas implementadas e verificadas localmente; 0/16 integradas/aprovadas.** As caixas de `tasks.md` permanecem desmarcadas até os merges autorizados. CI dos PRs #19–#22 aprovado; uma execução duplicada do #19 ficou presa na instalação do Chromium, foi cancelada e reexecutada com sucesso. Resultado do CI do grupo 5 será registrado no PR/issue após execução remota.

### Ordem de revisão e integração (não executar merges automaticamente)

1. [#19 — identidade persistida](https://github.com/LordBiGoDoNE/concurso-simulator/pull/19) → épica da spec.
2. [#20 — sessões/contratos](https://github.com/LordBiGoDoNE/concurso-simulator/pull/20) → branch de #19.
3. [#21 — Google OIDC](https://github.com/LordBiGoDoNE/concurso-simulator/pull/21) → branch de #20.
4. [#22 — interface](https://github.com/LordBiGoDoNE/concurso-simulator/pull/22) → branch de #21.
5. Grupo 5 — verificação integrada → branch de #22.

Após revisar cada PR, retargetar e integrar na ordem em `epic/optional-google-identity`, marcando somente tarefas efetivamente integradas. Abrir o PR da spec para `epic/application-reformulation` somente após autorização e integração; não arquivar nem publicar em main. Main permanece no commit `f5f30d64e0c968c4530504929c6d0d413f13eefc`.

Pendência externa: smoke Google real antes da publicação pública na etapa de deployment, seguindo `backend/docs/google-login.md`. Precisa de client/segredo do usuário, não afeta a conclusão dos testes locais/CI e não foi realizado nesta etapa.
