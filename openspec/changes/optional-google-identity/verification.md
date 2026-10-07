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

Grupo 2 (sessões/contratos) começa após integração aprovada do grupo 1, conforme o fluxo de PRs da spec. Resultados de CI serão registrados na discussão do PR; não confundir implementação na branch com integração na épica.
