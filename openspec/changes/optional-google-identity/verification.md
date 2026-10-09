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

O usuário autorizou grupos encadeados sem pausas, com revisão/merges no GitHub apenas após aprovação ao final. Resultados de CI ficam nos PRs; implementação na branch não é integração na épica.

## Revisão arquitetural/JPA do grupo 1 — 2026-10-08

ADR 0001/regras e desenho aprovados no planejamento do usuário, registrados no PR #24 (ainda não integrado); #19 agora aponta para sua branch. Commits originais preservados; nenhuma tarefa integrada.

GoogleIdentityService removido: VO ExternalIdentity + ResolveExternalIdentityUseCase Java puro, portas IdentityRepository/UnitOfWork, JpaIdentityRepository e execução transacional na infraestrutura. Principal fora do domínio. Prontidão SQL extraída do controller para porta/adaptador técnico, sem inventar uma cadeia de Services. Nenhuma migração alterada.

Gradle clean build: **28 testes aprovados**, incluindo 5 ArchUnit, 2 de domínio, 5 de aplicação e 11 de storage (4 novos casos JPA); checks existentes de API/migração preservados. Cobertos namespaces/subject opaco, restart EntityManagerFactory, 12 logins concorrentes sem órfãos, rollback antes de retry, conflito de chave do usuário não convertido em identidade, transação chamadora e schema incompatível recusado. Configuração efetiva valida Hibernate/OSIV; logServerErrorDetail=false e logs de bind/extract OFF evitam exposição de valores pessoais.

**3/16 verificadas com a arquitetura revisada; 0/16 integradas.** Grupos seguintes serão propagados/revalidados. Não confundir evidências históricas JDBC de 2026-10-07 com a versão atual JPA. CI remoto e cadeia completa serão registrados nos PRs/issue #17.
