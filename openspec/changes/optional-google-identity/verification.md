# Verification

## Padronização após o achado de sessão — #20

Pedido do usuário para tornar o aprendizado persistente: AGENTS.md agora exige revisão de responsabilidades HTTP; ADR 0001 registra o refinamento; `docs/architecture/http-endpoint-review.md` define proprietários, exemplos, checklist e limites; template de PR solicita evidências e exceções justificadas. Sem impor UseCases artificiais ou mover regras de negócio para filtros.

Dois testes em ArchitectureTests protegem o controller de sessão: dependências de autenticação/contexto/sessão e mecanismos CSRF; contratos HTTP com DTOs records concretos na web. Escopo específico, sem proibir Authentication nos callbacks OIDC ou afirmar que análise estrutural prova toda decisão semântica.

Verificação negativa: controller antigo reintroduzido temporariamente; os dois novos testes falharam, os cinco anteriores passaram. Controller corrigido restaurado e confirmado sem diff de produção. Gradle clean build/JDK 25: **36 testes aprovados na branch #20**, incluindo sete arquiteturais, HTTP real e PostgreSQL 18.6. CI executa os novos testes pelo build já existente. Padronização aplicada em PR de trabalho, não integrada: **3/16 tarefas integradas**, sem merge/publicação.

Propagado às branches dependentes preservando o histórico: #20 `ab289e7`, #21 `c6cdccb`, #22 `68ddfc6`. Cadeia final revalidada com Gradle clean build: **48 testes backend aprovados**, incluindo sete arquiteturais e OIDC. Código de produção, frontend e workflows idênticos à rodada `44b578d`; OpenSpec estrito aprovado. Evidência de CI dos heads atualizados fica nos PRs/issue #17; nenhum merge de PR presumido.

## Integração aprovada — estado atual

- #24 aprovado pelo usuário e integrado na épica da spec: `326db4435ad4b4215187bcc4cc9a99cfa50032a8`.
- #19 aprovado na sequência e integrado: `a9edfbf4f7b3681bfaf6e074f9a401dca83f21aa`; CI backend/frontend/material aprovado para o head revisado `3077b85` (28 testes backend).
- Tarefas **1.1–1.3 integradas, 3/16**. #20 é o próximo PR; #20–#23 permanecem sem aprovação/merge. A aprovação destes dois PRs não autoriza integrar a spec na épica geral ou publicar em main.
- Registros abaixo descrevem rodadas históricas e branches de implementação; os números históricos de integração não substituem este estado. Nenhuma alteração de código faz parte deste registro de progresso.

As seções datadas abaixo registram evidências históricas da implementação. A revisão JPA/arquitetural de 2026-10-08 é registrada nas seções finais; a ordem de revisão inclui o PR #24. Somente o estado de integração acima representa os merges já aprovados.

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

O usuário autorizou grupos encadeados sem pausas, com revisão/merges no GitHub apenas após aprovação ao final. Resultados de CI ficam nos PRs; implementação na branch não é integração na épica.

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

1. [#24 — arquitetura/decisões JPA](https://github.com/LordBiGoDoNE/concurso-simulator/pull/24) → épica da spec.
2. [#19 — identidade JPA](https://github.com/LordBiGoDoNE/concurso-simulator/pull/19) → branch de #24.
3. [#20 — sessões/contratos](https://github.com/LordBiGoDoNE/concurso-simulator/pull/20) → branch de #19.
4. [#21 — Google OIDC](https://github.com/LordBiGoDoNE/concurso-simulator/pull/21) → branch de #20.
5. [#22 — interface](https://github.com/LordBiGoDoNE/concurso-simulator/pull/22) → branch de #21.
6. [#23 — verificação integrada](https://github.com/LordBiGoDoNE/concurso-simulator/pull/23) → branch de #22.

Após revisar cada PR, retargetar e integrar na ordem em `epic/optional-google-identity`, marcando somente tarefas efetivamente integradas. Abrir o PR da spec para `epic/application-reformulation` somente após autorização e integração; não arquivar nem publicar em main. Main permanece no commit `f5f30d64e0c968c4530504929c6d0d413f13eefc`.

Pendência externa: smoke Google real antes da publicação pública na etapa de deployment, seguindo `backend/docs/google-login.md`. Precisa de client/segredo do usuário, não afeta a conclusão dos testes locais/CI e não foi realizado nesta etapa.

## Revisão arquitetural/JPA do grupo 1 — 2026-10-08

ADR 0001/regras e desenho aprovados no planejamento do usuário, registrados no PR #24 (ainda não integrado); #19 agora aponta para sua branch. Commits originais preservados; nenhuma tarefa integrada.

GoogleIdentityService removido: VO ExternalIdentity + ResolveExternalIdentityUseCase Java puro, portas IdentityRepository/UnitOfWork, JpaIdentityRepository e execução transacional na infraestrutura. Principal fora do domínio. Prontidão SQL extraída do controller para porta/adaptador técnico, sem inventar uma cadeia de Services. Nenhuma migração alterada.

Gradle clean build: **28 testes aprovados**, incluindo 5 ArchUnit, 2 de domínio, 5 de aplicação e 11 de storage (4 novos casos JPA); checks existentes de API/migração preservados. Cobertos namespaces/subject opaco, restart EntityManagerFactory, 12 logins concorrentes sem órfãos, rollback antes de retry, conflito de chave do usuário não convertido em identidade, transação chamadora e schema incompatível recusado. Configuração efetiva valida Hibernate/OSIV; logServerErrorDetail=false e logs de bind/extract OFF evitam exposição de valores pessoais.

**3/16 verificadas com a arquitetura revisada; 0/16 integradas.** Grupos seguintes serão propagados/revalidados. Não confundir evidências históricas JDBC de 2026-10-07 com a versão atual JPA. CI remoto e cadeia completa serão registrados nos PRs/issue #17.

## Revisão arquitetural/JPA do grupo 2 — 2026-10-08

Dependência #19 atualizada por merge local que preserva commits, não por merge de PR. Configuração de sessões na infraestrutura, endpoints/principal na web; nenhuma entidade artificial para CSRF/cookies e nenhum endpoint/contrato alterado. Migração V3 intacta. Spring Session JDBC permanece compatível com JpaTransactionManager, sem alterar sua recuperação JDBC de sessões.

Gradle clean build **33 testes aprovados** (28 da base revisada + 5 de sessão/cookie). HTTP real verifica me/csrf/logout, timeout/limpeza, recuperação do repositório e isolamento de dispositivos; ArchUnit cobre os novos pacotes. **7/16 revalidadas, 0/16 integradas.** Próximo: adequar o adaptador Google ao UseCase/portas.

## Revisão arquitetural/JPA do grupo 3 — 2026-10-08

Callback Google convertido em adaptador web do UseCase genérico, sem SQL/JPA no handler. Propriedades/wiring do provedor na infraestrutura; LoginOptions é a porta mínima para disponibilidade/destino, sem credenciais na web. Não adicionados Domain Services artificiais nem provedores extras. SessionPrincipal/contratos continuam mínimos; testes também recusam modelos JPA serializados nas sessões.

Gradle clean build **45 testes aprovados**: todos os anteriores e 12 casos de configuração/OIDC. PKCE, rotação, negativos criptográficos/replay, UUID estável e ausência de tokens/logs preservados com persistência JPA real. **11/16 revalidadas, 0/16 integradas.** Teste Google real segue externo para antes do deployment público.

## Revisão arquitetural/JPA do grupo 4 — 2026-10-08

Backend revisado propagado sem reescrever commits. React permanece adaptador de contratos HTTP; nenhum modelo JPA, token ou provider secret no cliente. Não alterar contratos nem criar camadas de negócio artificiais para sessão. Documentação explicita essa fronteira.

Gradle clean build **45 testes aprovados**; Vitest **14**, TypeScript/Vite build aprovado; Playwright teclado/390/1280px **5 aprovados**, offline pendente nesta rodada sem STUDY_ROOT e obrigatório no clone limpo do grupo 5. **14/16 revalidadas, 0/16 integradas.**

## Revisão arquitetural/JPA do grupo 5 — 2026-10-08

Cadeia completa revalidada no clone limpo `/tmp/opencode/concurso-jpa-clean-20261008`, commit de código `f4beef8`. Sem banco local/credenciais Google reais. Launcher OIDC/API/PostgreSQL continua exclusivo de teste, fora do bootJar.

| Verificação atual | Resultado |
| --- | --- |
| Gradle clean build/JDK 25 | **45 testes**, incluindo 5 ArchUnit, aprovados |
| npm ci / audit | 0 vulnerabilidades reportadas |
| Vitest / TypeScript / Vite | **14 testes** / build aprovados |
| Playwright UI + material offline | **6 testes**, nenhum ignorado |
| Playwright API/JPA/PostgreSQL/OIDC reais | **3 testes**, nenhum ignorado |
| Links/pacote de estudo | 108 páginas, 1757 referências locais |
| Matemática offline sem JavaScript | 9 módulos, 90 resoluções e gabaritos separados preservados |
| Flyway V1/V2/V3 / pages.yml | idênticos à implementação anterior / main |
| OpenSpec estrito | aprovado |
| JAR de produção | nenhum launcher/provedor/registration ou teste |

O navegador verifica login opcional e isolamento de identidades/dispositivos com a implementação JPA; reinicia o contexto Spring real e recupera a sessão JDBC/principal mínimo; novo login conserva UUID; CSRF/logout, inatividade real, cancelamento e link de aulas preservados. Nenhum modelo JPA é transportado à web/sessão. SQL de prontidão permanece em adaptador técnico, fora do controller.

**16/16 tarefas revalidadas na arquitetura JPA atual; 0/16 integradas.** 68 casos de teste aprovados no conjunto das suítes, além das verificações estruturais do material. ADR 0001 e AGENTS.md são a memória versionada desta decisão, não apenas o histórico da conversa. Sem skill adicional por não substituir regras permanentes. CI remoto atualizado será registrado nos PRs/issue #17.

Ordem atual de revisão: **#24 → #19 → #20 → #21 → #22 → #23**. Commits anteriores preservados com merges locais de dependências, sem force-push nem merge de PRs; nenhum checklist de integração marcado, nenhum arquivamento/preview/publicação. Main continua `f5f30d64e0c968c4530504929c6d0d413f13eefc`. Smoke Google real permanece pendente para antes do deployment público.

## Revisão do controller de sessão — #20

Correção solicitada pelo usuário durante a revisão, sem autorização de merge do #20. A regra de `/me` exige autenticação não anônima e principal interno; rejeições 401 JSON/no-store ficam em `ApiAccessFailureHandler`, compartilhado pelos pontos de entrada/negação do Spring Security. O controller recebe `@AuthenticationPrincipal UserPrincipal` e retorna `MeResponse`; `/csrf` retorna `CsrfResponse`, sem assumir geração/validação do token. Sem UseCase/Service artificial, mudança de contrato, schema ou endpoint.

`JAVA_HOME=/usr/lib/jvm/java-25-temurin-jdk ./gradlew --no-daemon --console=plain clean build`: **34 testes aprovados**, incluindo ArchUnit e HTTP real/PostgreSQL 18.6. Novo teste cobre contexto sem autenticação, principal incompatível, autenticação não concluída e principal anônimo; todos retornam 401 sem redirect/dados pessoais. O teste de visitante também confirma que obter CSRF não autentica a sessão. Contratos OpenAPI, cookie, logout/CSRF, expiração e isolamento continuam aprovados. Estado de integração permanece **3/16**; #20–#23 aguardam aprovação individual.

### Revalidação da cadeia após a correção

Correção propagada por merges locais preservando os commits: #20 `7f4bfb5`, #21 `fd4f2c8`, #22 `7b8c8bf`, código final #23 `8b44f20`. Nenhum merge de PR no GitHub.

- Gradle clean build/JDK 25: **46 testes backend aprovados**, incluindo ArchUnit, persistência JPA e OIDC.
- Vitest: **14 testes**; TypeScript/Vite build aprovados.
- Playwright integrado com API/PostgreSQL/OIDC isolados: **3 testes**, incluindo restart Spring/recuperação da sessão, duas identidades/dispositivos, logout CSRF, inatividade e cancelamento.
- Playwright UI/material offline: **6 testes**, nenhum ignorado, com `STUDY_ROOT=/tmp/opencode/concurso-session-review-study` montado por `scripts/build_pages.py`. A primeira tentativa apontou incorretamente para os HTML-fonte da raiz, falhando no teste offline; corrigido o ambiente sem mudar código/testes/material.
- Pacote estático: **108 páginas / 1757 referências locais**, nove módulos e 90 resoluções offline sem JavaScript preservados.
- **69 casos aprovados** no conjunto das suítes. Migrações Flyway, OpenAPI, frontend e workflows idênticos ao estado anterior `1ca023f`; nenhuma publicação ou credencial Google real. OpenSpec estrito validado; CI remoto registrado nos PRs/issue #17.
