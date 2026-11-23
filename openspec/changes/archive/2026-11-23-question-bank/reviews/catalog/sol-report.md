## 1. Identificação e escopo

- **Modelo/variante pedido:** `openai/gpt-6.1-sol#xhigh`, override autorizado somente nesta revisão comparativa. Modelo informado pelo ambiente: `openai/gpt-6.1-sol`.
- **PR:** #38 — `task/question-bank/catalog` → `epic/question-bank`.
- **Escopo:** somente tarefa 1.1; não é revisão cumulativa da spec.
- **Base:** `600cf6a59a608cec37c3cc22e4902de46ef5a46d`
- **Head:** `cf857f12784824e23dccbf993ca782791ae62026`
- **Digest:** `63a8ffc5d6e09707d81f56f25780b396172ae5212d56e836b2b44d70277dfddf`

SHAs, correspondência do worktree e digest foram fornecidos pelo coordenador; não os recalculei.

## 2. Achados

**Nenhum achado blocking, recommended ou optional identificado.**

A leitura estática não revelou violação verificável dos requisitos aplicáveis à tarefa 1.1. Essa conclusão não certifica execução dos testes nem conclusão da spec inteira.

### Delimitação do compartilhamento sem duplicação

A tarefa 1.1 exige IDs/mapeamento das cinco disciplinas, assuntos e dois cargos; testes de associação assunto–disciplina, compartilhamento sem duplicação e rejeição de cargos externos; matriz rastreável aos módulos.

A implementação permite uma classificação aplicável aos dois cargos e elimina associações repetidas:

- `QuestionClassification.java:8–23`: conjunto de cargos não vazio, validado e defensivamente copiado; `fromIds` reúne os cargos num `HashSet`.
- `QuestionCatalogTests.java:57–69`: entrada com cargo repetido resulta nos dois cargos únicos, ambos reconhecidos pela mesma classificação.

**Isso não demonstra ainda que filtros retornam a mesma identidade/versão de questão sem duplicar contagens**, cenário de `spec.md:12–14`. Questão/revisão e consulta pertencem às tarefas posteriores 1.2 e 4. A documentação distingue esses níveis em `docs/question-bank-catalog.md:17–24`; não considerei esse cenário implementado nem sua ausência um bloqueador antecipado desta tarefa.

## 3. Áreas efetivamente verificadas

| Área | Evidência examinada e conclusão estática |
|---|---|
| Escopo inicial | Cinco disciplinas, dois cargos e concurso `limeira-01-2026`; conversões rejeitam IDs desconhecidos, sem fallback. |
| IDs e rastreabilidade | Os 51 IDs/títulos e vínculos de `Topic` correspondem ao manifesto e à matriz: distribuição 10/12/10/8/11. Os caminhos constam das listagens das cinco pastas legadas. |
| Invariantes | `QuestionClassification.java:9–16` rejeita disciplina/assunto incompatíveis, nulos e cargos ausentes; protege o conjunto contra alteração posterior do chamador. |
| Responsabilidades | Domínio Java puro, sem Spring/JPA/HTTP; invariantes no objeto de valor, sem Service/UseCase artificial. Coerente com ADR 0001. |
| Integração arquitetural | As regras existentes de `ArchitectureTests.java:23–57` abrangem o novo pacote. Não há consumidores de produção do catálogo fora dos quatro tipos adicionados. |
| Segurança e privacidade | Cargos de estudo não foram associados a permissões. Não há novos endpoints, principal, sessão, tokens ou dados pessoais. A configuração de segurança existente não é alterada pelo diff. |
| Persistência e concorrência | Nenhum schema, repository, transação ou mecanismo concorrente foi introduzido; não há alteração das migrações existentes. |
| Testes | Inspecionadas as sete rotinas: IDs/escopo, combinações assunto–disciplina, compartilhamento, cópia imutável, rejeições e rastreabilidade dos 51 módulos. Gradle fixa o diretório esperado; CI está configurada para executar o build completo. |
| Manutenção e regressões | Documentados IDs exatos, proibição de persistir ordinais, estabilidade dos nomes de `Topic` e caráter candidato das associações editoriais. O diff fornecido não altera conteúdo legado, frontend ou contratos existentes. |

Li integralmente os oito requisitos/documentos solicitados, além do roadmap versionado. Investiguei também manifesto completo, diretórios legados, build, ArchUnit, segurança, filtro de sessão, configuração da aplicação e workflow de CI.

### Arquivos do diff lidos integralmente

1. `backend/src/main/java/br/com/concursosimulator/questionbank/domain/Discipline.java`
2. `backend/src/main/java/br/com/concursosimulator/questionbank/domain/QuestionClassification.java`
3. `backend/src/main/java/br/com/concursosimulator/questionbank/domain/TargetRole.java`
4. `backend/src/main/java/br/com/concursosimulator/questionbank/domain/Topic.java`
5. `backend/src/test/java/br/com/concursosimulator/questionbank/QuestionCatalogTests.java`
6. `docs/question-bank-catalog.md`
7. `backend/README.md`
8. `openspec/changes/question-bank/tasks.md`

Para os dois últimos, confrontei o contexto atual com os hunks completos fornecidos.

## 4. Limites e verificações pendentes

- Revisão exclusivamente estática, usando `read/glob/grep`. Não executei shell, builds, testes ou consultas ao GitHub.
- Não li relatórios anteriores, `spec-review.json/md` ou `verification.md`.
- Não há evidência dinâmica nesta revisão de compilação, sucesso dos testes ou regressões de identidade/offline.
- Não foram demonstrados identidade/versão e contagem por filtros, aplicação atômica de pacote inválido, persistência, importação, aprovação editorial ou consulta HTTP. Esses cenários permanecem nas tarefas posteriores.
- Rastreabilidade ao manifesto não comprova correção editorial, vigência normativa ou aplicabilidade de cada questão ao cargo. Não revisei integralmente o conteúdo pedagógico legado.

**Solicito ao coordenador**, no head identificado, executar e registrar autoria/resultados:

```sh
cd backend
./gradlew test --tests '*QuestionCatalogTests' --tests '*ArchitectureTests'
./gradlew clean build
```

Este parecer **não aprova merge** e não substitui a futura revisão cumulativa. A decisão final permanece com o usuário.
