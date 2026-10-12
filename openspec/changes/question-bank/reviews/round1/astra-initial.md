## Resultado

Revisão estática inicial concluída para os sete diffs e a combinação de produto, limitada às tarefas **1.1–1.3 e 2.1–2.3**, mais processo #39.

**Nenhum achado blocking identificado.** Há uma recomendação de cobertura de testes e uma melhoria opcional de persistência. Isso não aprova merge nem conclui o gate cumulativo da spec.

### Identificação

- Modelo confirmado pelo coordenador: `openai/gpt-6-astra#medium`.
- Base cumulativa: `600cf6a59a608cec37c3cc22e4902de46ef5a46d`.
- Head cumulativo: `2e38370cfc76fd6d0651967686e60569bee87765`.
- Digest informado: `9f35d0c79cc7b0326c4b368e2ea5880bf4414bf252334e21fb062eb4bd214cf5`.
- Bases, heads e digests individuais: manifesto fornecido, incluindo #39 independente no head `ce624fcca484222330351cdf715973859888ab87`.
- ID da sessão: não disponibilizado ao revisor; registrar pelo coordenador.

## Achados

### QB-ASTRA-01 — recommended — Teste de substituição não varia a classificação

**PR:** #44; interação com #38/#40/#42.

**Localização:**
- `backend/src/test/java/br/com/concursosimulator/questionbank/QuestionStorageTests.java:30–39`
- `backend/src/test/java/br/com/concursosimulator/questionbank/QuestionFixtures.java:14–22`

**Evidência:** o teste denominado `roundTripAndAtomicReplacementPreserveExactContentHistoryAndEligibleClassification` cria ambas as revisões com `content(...)`, alterando somente o enunciado. A fixture mantém Português/P01 e os mesmos dois cargos. Portanto, não distingue a classificação da revisão elegível daquela do rascunho mais recente.

**Referência:** `design.md:21` exige classificação da revisão elegível; tarefas 1.2 e 2.2 exigem testes de versões/classificação e substituição atômica.

**Impacto:** lacuna verificável de regressão na integração entre classificação, versionamento e elegibilidade. Não constatei erro funcional nesse comportamento; o teste atual não demonstraria uma troca indevida de classificação.

**Sugestão:** criar segunda revisão com disciplina/assunto/cargos diferentes; recarregar do PostgreSQL e verificar:
1. classificação antiga enquanto a nova revisão está pendente;
2. nova classificação somente após aprovação;
3. preservação integral da classificação histórica.

Não é necessário antecipar endpoints ou filtros da tarefa 4.

### QB-ASTRA-02 — optional — Validação de elegibilidade consulta o acervo inteiro por evento

**PR:** #43; interação com escritas de #44/#45.

**Localização:** `backend/src/main/resources/db/migration/V4__question_bank.sql:71–81`.

**Evidência:** `qb_validate_eligible()` executa uma consulta global entre `qb_question` e `qb_revision`, sem restringir à questão/revisão afetada. A função é chamada por triggers diferidas **por linha**, tanto para alterações de questões quanto de revisões.

**Referência:** persistência e concorrência em `design.md:27–33`; manutenção e responsabilidade da infraestrutura no ADR 0001.

**Impacto:** trabalho repetido proporcional ao acervo nas transações de escrita, inclusive em lotes. A estrutura da consulta é verificável; **não medi degradação nem identifiquei violação de um limite de desempenho aprovado**.

**Sugestão:** considerar validação restrita ao agregado afetado, mantendo cobertura para mudanças de ponteiro e estado. Solicitar medição com lotes representativos antes de tratar isso como problema operacional. Qualquer ajuste deve respeitar a política de migrações versionadas.

## Cobertura

| Área | Análise realizada |
|---|---|
| Requisitos | Catálogo de cinco disciplinas/51 assuntos/dois cargos; matriz confrontada com manifesto; identidade, alternativas, conteúdo imutável, evidência exata, retirada e idempotência. |
| Arquitetura | Núcleo Java puro; invariantes no domínio; UseCases coordenando porta; JPA, JSON e transações na infraestrutura; ArchUnit sem relaxamento de fronteiras. |
| Integração | Cadeia catálogo → conteúdo → editorial → schema → persistência → importação; reconstrução do histórico, comparação de espelhos e preservação do ponteiro na reaplicação. |
| Segurança e privacidade | Nenhum endpoint/editorial web ou vínculo com autorização Google adicionado. Segurança existente permanece com negação padrão; payload corretivo continua interno. Recursos/SVG não foram considerados seguros apenas por validação estrutural. |
| Persistência | V4 aditiva, FKs compostas/diferidas, constraints de origem/revisão, imutabilidade, bloqueio pessimista, retorno após transação e tradução específica de conflito fora do callback transacional. |
| Testes | Leitura dos testes puros, schema/upgrade, rollback, concorrência editorial, corrida de origem ausente e falhas não traduzidas. Ressalva QB-ASTRA-01. |
| Manutenção/processo | Documentação, limites operacionais, dependências reais e #39: revisão dupla separada do gate final, sem autorização implícita de merge/publicação. Ressalva opcional QB-ASTRA-02. |

Não identifiquei outros achados acionáveis em #38, #40, #42 ou #45. No #39, não identifiquei relaxamento das restrições de integração ou do gate cumulativo.

## Limitações e cenários sem evidência

- **Nenhum teste, shell, build, acesso remoto ou alteração de arquivo foi executado.**
- SHAs, limpeza do snapshot, igualdade das cópias e digests são identificações fornecidas pelo coordenador; não os recalculei.
- O diff obrigatório de #39 contém, em `docs/implementation-batches.md`, um resumo de revisões anteriores de #38. Esse conteúdo apareceu durante a leitura integral; não foi buscado separadamente nem usado como evidência de conformidade. Portanto, não posso declarar ausência absoluta de exposição a resultados anteriores.
- Não li `verification.md` nem os arquivos de evidência `spec-review.json/md`.
- Não há demonstração dinâmica nesta revisão de round-trip com recursos não vazios e fundamento legal, mudança de classificação, concorrência de pacotes multiorigem ou custo dos triggers.
- Extração/sanitização, CLI, consulta pública, interface e curadoria real permanecem fora deste lote. Sua ausência não foi tratada como falha.
- Testes sintéticos não demonstram aprovação editorial, atendimento da meta ou preservação operacional completa do legado.

## Verificações solicitadas ao coordenador

1. Executar os testes `Question*Tests` e `ArchitectureTests`, além do build completo do backend, em PostgreSQL 18.6 isolado.
2. Acrescentar/executar o cenário de QB-ASTRA-01 e round-trip com recursos e fundamento legal.
3. Verificar pacotes concorrentes com múltiplas origens, confirmando rollback integral e ausência de duplicação.
4. Registrar comandos, executor, resultados e limitações nos SHAs efetivamente verificados; conferir refs/checks atuais antes de solicitar decisão ao usuário.

**Triagem e decisão final permanecem pendentes com o usuário.**
