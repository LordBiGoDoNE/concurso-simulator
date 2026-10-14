# Épica geral: reformulação da aplicação

## Objetivo

Entregar a reformulação completa de uma vez, mantendo `main` e o site atual utilizáveis durante o desenvolvimento. A épica geral é **`epic/application-reformulation`**. As specs permanecem pequenas e revisáveis, mas não são publicadas individualmente em main.

```text
main (site atual)
└── epic/application-reformulation
    ├── epic/bootstrap-application-stack
    │   └── task/bootstrap-application-stack/<assunto>
    ├── epic/<spec-identidade>
    ├── epic/<spec-banco-questoes>
    └── epic/<demais-specs>
```

Tarefa → branch da spec → épica geral → main apenas na entrega final. Nomes entre `<...>` são exemplos, não specs já criadas. PRs exigem aprovação em cada nível; não há merge automático.

PRs de integração de spec usam `[SPEC] <Título>` e passam por revisão independente do conjunto antes da aprovação de merge. Processo, relatório rastreável e gate estão em [Revisão de spec](architecture/spec-review.md). #26 instalou o processo; #25 foi aprovado e integrado após revisão cumulativa independente, correções #27/#28/#29, seis achados revalidados como corrigidos e gate aprovado. Modelo escolhido: `openai/gpt-6-astra#medium`, sem fallback. Revisão/gate não autorizam merge sem decisão explícita do usuário.

## Roadmap acordado

1. **Base da aplicação**: Java 25, Gradle, Spring Boot, PostgreSQL 18, Flyway, segurança inicial, React/TypeScript/Vite, ambiente local e testes. Implementada e integrada na épica geral pelo PR #14.
2. **Identidade e acesso**: login opcional Google/OIDC/PKCE e sessão JDBC integrados na épica geral pelo #25, merge `f72fcc5d9c92c326ad789e5cf72eef54665913cf`, após aprovações de tarefas e revisão independente. Sem publicação ou arquivamento. Smoke Google real depende de credenciais e continua obrigatório antes do deployment público; histórico/sincronização ficam para specs seguintes.
3. **Banco de questões**: proposta `question-bank` aprovada, issue #30. Grupos 1–5 integrados somente em `epic/question-bank` (`dcbe086`): **16/44 tarefas integradas**. Grupos 6/7 implementados/verificados no draft #62: 25 lotes, 250 revisões aprovadas em PostgreSQL local isolado (50 por disciplina), consulta/projeções e navegador conferidos; parecer independente final do lote em `openspec/changes/question-bank/reviews/round3/final-review.md`. As **28 tarefas novas não estão integradas/aprovadas como PR**. Catálogo, versões/editorial explícito, importação controlada e consulta pública sem gabaritos; sem integração na épica geral. Meta aprovada: **50–100 questões por disciplina**, distribuídas entre assuntos; não limita armazenamento nem apaga o material. Escopo inicial: **Limeira 01/2026, Assistente Administrativo e Secretário de Escola**, sem novos cargos/concursos. Evidência local não publica nem popula automaticamente outros bancos; gate cumulativo final Astra medium continua separado.
4. **Simulados e tentativas**: seleção aleatória, critérios de composição, realização, correção e histórico com versões, parâmetros e ordem das alternativas preservados; não expor gabaritos durante o simulado.
5. **Questões parametrizadas**: variações controladas, especialmente em Matemática, com validação de resultados e resoluções didáticas. Não gerar questões por IA ao vivo sem validação.
6. **Revisão por conceito**: desempenho, dificuldades e prática orientada ao aprendizado, não à memorização.
7. **Preparação da entrega**: definir hospedagem Java/PostgreSQL, configuração segura, integração das experiências de estudo, regressão, migrações e plano de publicação/rollback. Esta etapa inclui a spec própria de deployment prevista no plano.

Cada etapa requer seus artefatos OpenSpec e aprovação antes da implementação. Não preparar de uma vez todos os detalhes futuros; dependências e novos requisitos devem ser discutidos por spec.

Acervo original: 51 módulos/510 questões (Português100, Matemática/Raciocínio120, Informática100, Administrativo80, Escolar110), sujeito a revisão. Importação não aprova automaticamente conteúdo. A proposta do banco separa inventário, rascunhos, revisões e versões elegíveis; dificuldades de atingir a meta devem ser registradas, sem aprovação fictícia.

GitHub Stacks: #32 e #36 aprovados/integrados somente na épica de question-bank (head `d04c6d7`), v0.2.1 fixada. Piloto remoto #33/#34 (stack #35, drafts) confirmou criação/diffs/rebase/CI; troca de base de membro foi bloqueada e `edited` foi testado por título. O usuário aprovou a abordagem como **padrão também para próximas specs**, registrada por PR de processo próprio. Depois de integrar esse registro, não repetir autorização do método por spec: dependências reais usam stack da mesma spec/repositório, trunk `epic/<change-name>`; independentes e PR `[SPEC]` separados. Propostas/pedidos de implementação e merges mantêm suas aprovações próprias, sem início automático de tarefas. Conferir refs/checks de cada stack, com fallback sequencial seguro; sem retargeting manual. Merge/fila/sync pós-merge não ensaiados exigem decisão explícita do caminho. Sintéticos não entram no produto nem têm encerramento autorizado. [Procedimento e limites](stacks-pilot.md). Stacks não substitui revisão cumulativa nem garante redução de tokens.

Arquitetura/persistência revisadas pelo usuário em 2026-10-08: monólito modular, UseCases/domínio puro e JPA/Hibernate na infraestrutura, mantendo Flyway e Spring Session JDBC. Regras persistentes em `AGENTS.md` e [ADR 0001](architecture/adr/0001-domain-and-application-boundaries.md); PR #24 registra a decisão e precede a revisão dos PRs #19–#23. Implementação não significa integração aprovada.

## Decisões abertas

- Sincronização do progresso em specs futuras (login opcional Google já decidido).
- Expansão além dos dois cargos atuais, fora da spec inicial do banco (quantidade por disciplina e escopo inicial já decididos).
- Hospedagem, disponibilidade, backups e estratégia de acesso à nova aplicação.

Essas decisões não serão presumidas apenas por constarem no roadmap.

## Critérios da entrega única

- Todas as specs do roadmap aceitas, implementadas, revisadas e integradas na épica geral; mudanças de escopo explicitamente aprovadas.
- Testes de integração, fluxos ponta a ponta e regressão das aulas aprovados na combinação final.
- Infraestrutura de execução acordada e validada; GitHub Pages não executa Java/PostgreSQL.
- Material atual preservado até a transição, com URLs e acessibilidade verificadas.
- Dados privados, senhas e gabaritos protegidos; migrações e rollback documentados.
- PR final da épica geral para main aprovado expressamente antes do merge/publicação.

## Preview e manutenção

CI somente verifica. Preview estático é manual no Actions, executando o workflow de main e informando a branch como input. Não interrompe o site oficial e não executa a aplicação completa. Preview funcional da aplicação dependerá da spec de deployment.

PRs #6 e #8 já foram integrados em main como preparação do processo, sem substituir as aulas. Não serão revertidos. Daqui em diante, o código da reformulação será integrado exclusivamente na épica geral até a entrega final. Correções independentes do material podem seguir em main por revisão normal.
