# Revalidação — question-bank, rodada 1

**Resultado:** SOL-QB-001 e SOL-QB-002 corrigidos; SOL-QB-003 permanece opcional, com proposta fundamentada de `deferred`. **Nenhum achado novo verificável.**

Não há achados `blocking` ou `recommended` pendentes deste parecer. Isso **não aprova merge nem conclui o gate cumulativo da spec**.

## Escopo e identificação

Modelo selecionado e confirmado no pedido: `openai/gpt-6.1-sol#xhigh`. Sessão e confirmação operacional da variante devem ser registradas pelo coordenador.

Complementei a revisão inicial dos sete PRs com leitura integral de **#46 — 387 linhas**, do **novo combinado — 2.148 linhas**, conferência da composição e investigação da árvore atual em `source/`. Reexaminei requisitos, ADR 0001, padrões e processo #39.

- **Produto:** `600cf6a59a608cec37c3cc22e4902de46ef5a46d` → `05518635fe160b92e8798dd78c03eff96f6234e5`
- **Digest informado:** `6dea8ab7292bbec0e7c19fad9398db3a0a452c5b587d9b3c820210068a92df45`
- **#46:** `2e38370cfc76fd6d0651967686e60569bee87765` → `05518635fe160b92e8798dd78c03eff96f6234e5`
- **Digest informado de #46:** `10c881fdeeda7484ebfed5709d1894ad63adef1c0d411831f654d0909a2d7353`

Segundo o manifesto, #38/#39/#40/#42/#43/#44/#45 preservam suas identidades. Não recalculei hashes nem consultei refs remotas.

## Achados e resolução

Os caminhos abaixo são relativos a `backend/build/review-round1-final/source/`. Arquivos Java de produção citados pertencem a `backend/src/main/java/br/com/concursosimulator/questionbank/`.

### SOL-QB-001 — recommended → **fixed**

**Origem:** #42/#44; correção em #46.

**Evidência:**
- `application/DecideQuestionUseCase.java:13–22` exige sequência editorial e ponteiro esperados, comparando ambos **após carregar sob trava e antes de decidir/salvar**. Não permanece o overload antigo.
- `domain/Question.java:51–60,89` recompõe a sequência pelo histórico e acrescenta cada decisão.
- `infrastructure/persistence/JpaQuestionStore.java:63–85` verifica sequência persistida e preserva o prefixo histórico.

Os dois cenários ABA estão reproduzidos deterministicamente em `backend/src/test/java/br/com/concursosimulator/questionbank/QuestionStorageTests.java:104–123`: aprovação/retirada com retorno a `null` e retorno a rascunho/nova submissão sem mudança de conteúdo.

**Referência:** `design.md:25–31`, tarefa 2.2 e `docs/question-editorial.md:16–22`.

**Conclusão:** a correção impede os comandos antigos identificados, sem expor versão JPA ao núcleo. A orientação para reler/revisar, sem renovar silenciosamente o token, está documentada. Resolução confirmada por análise estática; testes não executados por mim.

### SOL-QB-002 — recommended → **fixed**

**Origem:** #40, com efeitos sobre #42/#45; correção em #46.

**Evidência:**
- `domain/ContentRules.java:10–20` rejeita surrogates isolados ou pares inválidos, preservando pares válidos.
- `domain/StudyReference.java:14` aplica a validação também à URL.
- `backend/src/test/java/br/com/concursosimulator/questionbank/QuestionContentTests.java:13–20` cobre entradas malformadas, caractere suplementar válido, distinção de fingerprint e URL malformada.

**Referência:** `design.md:33`, tarefa 1.2 e `docs/question-content.md:9–25`.

**Conclusão:** o domínio deixa de aceitar as entradas que seriam substituídas silenciosamente durante a codificação UTF-8. O formato canônico v1 permanece inalterado para conteúdo válido; não foi introduzida normalização ou promessa de sanitização.

### SOL-QB-003 — optional → **deferred proposto**

**Origem:** #43.

**Local:** `backend/src/main/resources/db/migration/V4__question_bank.sql:71–81`.

**Evidência:** permanece a consulta global de elegibilidade, executada por constraint triggers diferidos por linha. #46 não modifica V4 e documenta a limitação em `docs/question-bank-round1-regressions.md:31–37`.

**Referência:** tarefa 2.1; ADR 0001, preservação de migrações versionadas.

**Impacto:** custo potencialmente crescente com acervo/eventos; nenhuma degradação foi demonstrada nesta revisão.

**Sugestão:** medir carga representativa antes de uso operacional maior; otimização eventual por migração aditiva, preservando regressões de elegibilidade/substituição.

A proposta de adiamento é compatível com a severidade opcional. **Não considero o achado corrigido nem presumo a decisão final de triagem do usuário.**

## Pedidos adicionais

| Pedido | Evidência acrescentada em #46 |
|---|---|
| Classificação diferente entre revisões | `QuestionStorageTests.java:33–52`: Português/P01/dois cargos → Matemática/M01/um cargo; classificação elegível preservada até aprovação. |
| Recursos e fundamento legal no round-trip | `QuestionStorageTests.java:54–67`: recurso em enunciado/alternativa/resolução, dispositivo/data, igualdade integral e fingerprint após recarga. |
| Pacotes concorrentes multiorigem | `QuestionImportTests.java:99–135`: entradas em ordem oposta, barreira de origem ausente, recuperação em nova unidade e mesmos IDs, sem decisões editoriais. |

Esses pedidos estão atendidos **no código dos testes**. Fixtures não demonstram segurança de arquivos reais, validade de normas ou aprovação editorial.

## Cobertura e efeitos entre PRs

- **Requisitos/#38/#40:** catálogo limitado, classificação compartilhada, revisões/conteúdo imutáveis e alternativa correta pertencente à revisão.
- **Arquitetura/#42/#46:** sequência pertence ao domínio; comparação concorrente à aplicação; trava/transação continuam na infraestrutura. Nenhuma camada artificial acrescentada.
- **Persistência/#43/#44:** migração aditiva, FKs, histórico protegido, rollback e reconstrução de conteúdo/estados. #46 não exige alteração de schema.
- **Idempotência/#45:** ordenação das origens e recuperação somente do conflito conhecido após rollback permanecem; reutilização não altera ponteiro, decisões ou estados.
- **Segurança/privacidade:** nenhum endpoint, CLI editorial ou privilégio Google acrescentado. Não encontrei chamada dos UseCases no startup normal; contratos e configuração de identidade permanecem fora do diff.
- **Processo/#39:** continua independente da stack produto, preservando revisão dupla, modelo padrão e gate cumulativo final. Não identifiquei regressão normativa adicional.
- **Manutenção/testes:** correções acompanhadas de regressões e limites documentados. Não exigi tarefas 3+ antecipadamente.

## Limitações e verificações não executadas

- Não executei shell, testes, escrita, delegação ou operações Git/GitHub.
- O coordenador informou **três controles negativos com falhas esperadas** e **91 testes backend sem falhas/erros/skips no head final**. São resultados atribuídos ao coordenador, não execuções ou logs verificados por mim.
- Não medi desempenho, nem demonstrei todos os interleavings ou combinações de origens novas/existentes.
- HTTP público, sanitização/extrator, CLI, browser e curadoria real continuam fora deste lote.
- Não consultei relatórios detalhados de outros revisores, `verification.md` ou evidências `spec-review.json/md`. Os diffs obrigatórios #39/#46 contêm resumos/referências a outros pareceres: houve exposição incidental, não utilizada como prova de correção.

## Verificações solicitadas ao coordenador

Registrar comando, executor e evidências dos testes informados vinculados ao head final; conferir refs/checks atuais; levar a proposta `deferred` de SOL-QB-003 à decisão do usuário. Qualquer mudança relevante exige novo snapshot e revalidação.

**A decisão de merge permanece exclusivamente com o usuário.**
