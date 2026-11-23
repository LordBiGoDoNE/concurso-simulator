# Revisão inicial — question-bank, rodada 1

**Resultado: 0 blocking, 2 recommended e 1 optional.** Triagem pendente. Este relatório não aprova merge nem conclui o gate cumulativo da spec.

## Escopo e identificação

Modelo selecionado e confirmado pelo coordenador: `openai/gpt-6.1-sol#xhigh`. O ID da sessão não está exposto nesta interface; deve ser registrado pelo coordenador.

Li integralmente os sete diffs e as 1.959 linhas de `combined.diff`, além da spec, AGENTS.md, ADR 0001, padrões e código relacionado.

| PR | Tarefa | Base → head |
|---|---|---|
| #38 | 1.1 | `600cf6a` → `cf857f1` |
| #40 | 1.2 | `cf857f1` → `447dec1` |
| #42 | 1.3 | `447dec1` → `efb5c57` |
| #43 | 2.1 | `efb5c57` → `a195035` |
| #44 | 2.2 | `a195035` → `35a3606` |
| #45 | 2.3 | `35a3606` → `2e38370` |
| #39 | Processo, independente | `600cf6a` → `ce624fc` |

SHAs completos e digests individuais: `backend/build/review-round1-initial/manifest.json`.

Conjunto produto:
- **Base:** `600cf6a59a608cec37c3cc22e4902de46ef5a46d`
- **Head:** `2e38370cfc76fd6d0651967686e60569bee87765`
- **Digest informado:** `9f35d0c79cc7b0326c4b368e2ea5880bf4414bf252334e21fb062eb4bd214cf5`

Não exigi implementação das tarefas 3+.

## Achados

### Recommended — SOL-QB-001: ponteiro esperado não detecta mudanças editoriais com retorno ao mesmo valor

**PR:** #42, em interação com #44.

**Localização:**
- `backend/src/main/java/br/com/concursosimulator/questionbank/application/DecideQuestionUseCase.java:13–19`
- `backend/src/main/java/br/com/concursosimulator/questionbank/domain/Question.java:65–84`

**Evidência:** a única comparação da base editorial é `actual == expectedEligible`. Isso não detecta o problema conhecido como ABA:

1. Duas revisões estão `IN_REVIEW`, sem revisão elegível.
2. É preparado um comando de aprovação da segunda, esperando `null`.
3. Outro operador aprova a primeira e depois a retira; o ponteiro volta a `null`.
4. O comando preparado anteriormente aprova a segunda sem detectar essas decisões intermediárias.

Analogamente, `RETURN_TO_DRAFT → SUBMIT` pode manter ponteiro e fingerprint inalterados, permitindo um comando preparado no ciclo anterior. A trava serializa a execução, mas não identifica a antiguidade do comando; a versão JPA é recarregada dentro da transação.

**Referência:** `design.md:25–31`; tarefa 2.2, controle concorrente e base desatualizada.

**Impacto:** uma operação preparada antes de uma mudança editorial pode alterar elegibilidade sem perceber a mudança. Não é reativação automática da própria revisão retirada, nem aprovação implícita. O comportamento é dedutível do código; a exigência de rejeitar toda mudança de ciclo não está explicitada, por isso não classifico como bloqueador.

**Sugestão:** usar também um token editorial monotônico — sequência de decisões ou versão abstrata do agregado — conferido dentro da trava. Adicionar regressões determinísticas dos dois cenários.

### Recommended — SOL-QB-002: canonização aceita texto malformado e o substitui silenciosamente

**PR:** #40, com efeito sobre #42/#45.

**Localização:**
- `backend/src/main/java/br/com/concursosimulator/questionbank/domain/ContentRules.java:5–10`
- `backend/src/main/java/br/com/concursosimulator/questionbank/domain/QuestionContent.java:63–65`

**Evidência:** `ContentRules.text` aceita um surrogate UTF-16 isolado, como `"\uD800"`. `String.getBytes(UTF_8)` substitui sequências malformadas em vez de rejeitá-las. Assim, mantendo os demais campos iguais, textos distintos como `"?"` e `"\uD800"` produzem a mesma representação canônica e fingerprint.

Isso é colisão **antes** do SHA-256, não quebra criptográfica do algoritmo.

**Referência:** `design.md:33`, canonização UTF-8 sem perda; tarefa 1.2 e `docs/question-content.md:9–23`.

**Impacto:** o fingerprint não representa fielmente todo conteúdo aceito pelo domínio. A comparação adicional de conteúdo em #45 impede reutilização silenciosa, mas pode apresentar uma entrada distinta como “Fingerprint colidente”. Não demonstrei exploração persistida no PostgreSQL.

**Sugestão:** rejeitar UTF-16 malformado na validação, ou usar codificação configurada para reportar erro, sem substituir caracteres. Testar surrogates isolados e pares válidos de caracteres suplementares.

### Optional — SOL-QB-003: validação de elegibilidade consulta o acervo inteiro por evento

**PR:** #43.

**Localização:** `backend/src/main/resources/db/migration/V4__question_bank.sql:71–81`.

**Evidência:** ambos os constraint triggers executam uma consulta global de `qb_question`/`qb_revision`, sem limitar a questão afetada. A consulta é repetida para cada evento de linha, inclusive em operações com vários agregados.

**Referência:** tarefa 2.1; desenho de persistência e manutenção. Não identifiquei requisito de desempenho violado.

**Impacto:** o trabalho de validação pode crescer com o acervo e com o tamanho do pacote. Não medi planos, latência ou timeouts.

**Sugestão:** avaliar validação restrita à questão afetada, consultando seu estado corrente no momento da verificação adiada. Preservar testes de substituição atômica e rejeição de ponteiro não aprovado.

## Cobertura

| Área | Verificação estática |
|---|---|
| **Requisitos** | Catálogo de cinco disciplinas/51 assuntos/dois cargos; classificação compartilhada; conteúdo imutável; alternativa correta pertencente à revisão; evidência exata, checklist, retirada e preservação histórica. |
| **Arquitetura** | Núcleo Java puro; invariantes no domínio; UseCases coordenando portas; JPA, Jackson e transações na infraestrutura. Não encontrei camadas artificiais ou acesso direto à persistência pelo núcleo. |
| **Integração** | Bases encadeadas consistentes no manifesto; #39 independente; importação preserva decisões/ponteiro ao reutilizar revisão antiga; novos conteúdos entram em rascunho. |
| **Segurança e privacidade** | Nenhum endpoint/CLI editorial ou autorização Google acrescentado. Não encontrei chamada de aplicação dos candidatos no startup normal. Resposta, resolução e proveniência permanecem internas; projeções públicas ainda são tarefa futura. |
| **Persistência** | V4 aditiva, sem alterações de V1–V3; catálogo sem questões aprovadas; FKs compostas e adiadas; bloqueio por agregado; espelhos/fingerprint conferidos na leitura; tradução restrita à constraint de origem após encerramento transacional. |
| **Testes e contratos** | Inspecionei testes puros, migração/upgrade, rollback, concorrência, idempotência e ArchUnit. Sua existência não comprova execução ou sucesso. |
| **Manutenção/processo** | #39 preserva modelo padrão, revisão dupla, autorização separada de merge e gate cumulativo final. Não altera workflows/proteções. Não há achado funcional confirmado em #38 ou adicional sobre essas decisões normativas de #39. |

## Limitações e cenários sem evidência

- **Não executei testes, shell, escrita ou operações Git/GitHub.**
- SHAs/digests foram lidos no manifesto, não recalculados por mim. A identidade das cópias foi informada pelo coordenador.
- Não consultei `verification.md`, arquivos de evidência da spec, conversas ou relatórios de outros revisores.
- **Limitação de cegamento:** o diff obrigatório de #39 contém, em `docs/implementation-batches.md:21–35`, um resumo de revisões anteriores de #38. A exposição foi incidental; não o usei para orientar os achados. Portanto, não certifico ausência total de exposição a resultados anteriores.
- As fixtures de round-trip não exercitam recursos não vazios, fundamento legal nem mudança de classificação entre revisão elegível e rascunho.
- Não há evidência executada aqui dos cenários ABA/UTF-16 apontados, nem de desempenho dos triggers.
- Sanitização/extrator, CLI, contratos públicos, browser e curadoria real permanecem fora deste lote.
- Não conferi checks remotos, ruleset atual, autenticidade dos números históricos de processo ou segurança operacional de merge/sync de Stacks.

## Verificações solicitadas ao coordenador

1. Executar regressões para SOL-QB-001 e SOL-QB-002 e fornecer resultados com executor identificado.
2. Ampliar round-trip com recursos, fundamento legal e classificação diferente entre revisões.
3. Executar o build completo com PostgreSQL 18.6 isolado, incluindo regressões existentes de identidade/sessão/prontidão.
4. Conferir refs e checks de cada head atual; qualquer alteração relevante exige novo snapshot e revalidação.

**Decisão de merge permanece exclusivamente com o usuário.**
