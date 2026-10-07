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

## Roadmap acordado

1. **Base da aplicação**: Java 25, Gradle, Spring Boot, PostgreSQL 18, Flyway, segurança inicial, React/TypeScript/Vite, ambiente local e testes. Implementada e integrada na branch da spec; PR #14 agora aponta para a épica geral.
2. **Identidade e acesso**: decidir login e regras antes de persistir histórico privado; especificar e implementar a solução aprovada.
3. **Banco de questões**: disciplinas/assuntos, revisão editorial, referências reais e importação controlada do material. Definir a quantidade por disciplina ou assunto antes de ampliar o acervo.
4. **Simulados e tentativas**: seleção aleatória, critérios de composição, realização, correção e histórico com versões, parâmetros e ordem das alternativas preservados; não expor gabaritos durante o simulado.
5. **Questões parametrizadas**: variações controladas, especialmente em Matemática, com validação de resultados e resoluções didáticas. Não gerar questões por IA ao vivo sem validação.
6. **Revisão por conceito**: desempenho, dificuldades e prática orientada ao aprendizado, não à memorização.
7. **Preparação da entrega**: definir hospedagem Java/PostgreSQL, configuração segura, integração das experiências de estudo, regressão, migrações e plano de publicação/rollback. Esta etapa inclui a spec própria de deployment prevista no plano.

Cada etapa requer seus artefatos OpenSpec e aprovação antes da implementação. Não preparar de uma vez todos os detalhes futuros; dependências e novos requisitos devem ser discutidos por spec.

## Decisões abertas

- Login obrigatório ou opcional, método de autenticação e sincronização do progresso.
- Meta de 50–100 questões por disciplina ou por assunto.
- Escopo de cargos/concursos além dos dois atuais.
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
