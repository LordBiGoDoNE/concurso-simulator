## Resultado da revalidação

**Nenhum achado novo blocking, recommended ou optional.** QB-ASTRA-01 está corrigido; QB-ASTRA-02 permanece opcional, com adiamento tecnicamente justificável e documentado, sujeito à decisão do usuário.

Revisei o manifesto atual, o diff integral de #46, todo o diff combinado e código/testes relacionados em `source/`. As camadas anteriores mantêm as identidades registradas no manifesto; esta análise considera sua combinação com #46, não apenas a troca do digest.

### Identificação

- Modelo confirmado: `openai/gpt-6-astra#medium`.
- Escopo: #38/#40/#42/#43/#44/#45/#46 e processo independente #39.
- Base combinada: `600cf6a59a608cec37c3cc22e4902de46ef5a46d`.
- Head: `05518635fe160b92e8798dd78c03eff96f6234e5`.
- Digest informado: `6dea8ab7292bbec0e7c19fad9398db3a0a452c5b587d9b3c820210068a92df45`.
- #46: base `2e38370cfc76fd6d0651967686e60569bee87765`; digest `10c881fdeeda7484ebfed5709d1894ad63adef1c0d411831f654d0909a2d7353`.

## Resolução dos achados

Os caminhos abaixo são relativos à árvore `backend/build/review-round1-final/source/`.

### QB-ASTRA-01 — recommended → fixed

**Evidência:** `backend/src/test/java/br/com/concursosimulator/questionbank/QuestionStorageTests.java:33–52`.

A segunda revisão agora muda Português/P01/dois cargos para Matemática/M01/Secretário de Escola. O teste recarrega o agregado e verifica:
- classificação antiga durante rascunho e revisão pendente;
- classificação nova após aprovação;
- conteúdo histórico preservado;
- retirada posterior sem elegibilidade.

Isso resolve a lacuna apontada na interação entre #38/#40/#42/#44, conforme `design.md:21` e tarefas 1.2/2.2. A conclusão é sobre a adequação da regressão inspecionada; não executei o teste.

### QB-ASTRA-02 — optional → deferred proposto

**Localização:** `backend/src/main/resources/db/migration/V4__question_bank.sql:71–81`.

A consulta global por trigger diferida continua presente. `docs/question-bank-round1-regressions.md:31–37` documenta o custo potencial, ausência de medição e eventual otimização por migração aditiva.

O adiamento é compatível com a severidade original: não foi demonstrada degradação nem violação de limite aprovado. **Não está corrigido**, e não deve ser registrado como tal. Permanece necessária medição representativa antes de crescimento operacional relevante.

## Pedidos adicionais e correções de #46

- **Round-trip enriquecido atendido:** `QuestionStorageTests.java:54–68` inclui recursos em enunciado/alternativa/resolução, fundamento legal, data, Unicode composto/decomposto e igualdade do fingerprint.
- **Pacotes multiorigem atendido:** `QuestionImportTests.java:99–136` força corrida na primeira origem, usa entradas em ordens opostas e verifica reutilização consistente, duas questões/revisões e ausência de decisões. Complementa os testes anteriores de rollback integral.
- **Concorrência editorial:** `DecideQuestionUseCase.java:13–22` confere ponteiro e sequência dentro da unidade bloqueada. `Question.editorialSequence()` deriva do histórico append-only, reconstruído pela persistência. Os testes `QuestionStorageTests.java:104–124` cobrem retorno do ponteiro a `null` e novo ciclo editorial com conteúdo inalterado.
- **Unicode:** `ContentRules.java:5–20` rejeita surrogates isolados ou malformados, preservando pares válidos. `StudyReference` aplica a validação também à URL. A codificação canônica de entradas válidas não mudou.

Não identifiquei regressão introduzida por essas mudanças. O futuro adaptador editorial deverá conservar a sequência do snapshot revisado, sem renová-la automaticamente para repetir comando rejeitado; essa responsabilidade está documentada, sem exigir antecipação da tarefa 3.4.

## Cobertura do conjunto corrente

- **Requisitos:** catálogo, conteúdo imutável, evidência exata, classificação elegível e reaplicação sem reativação.
- **Arquitetura:** núcleo permanece independente de frameworks; concorrência técnica e persistência continuam na infraestrutura.
- **Integração:** assinatura editorial atualizada nos chamadores encontrados; importação mantém identidade, histórico e recuperação pós-rollback.
- **Segurança/privacidade:** nenhuma nova rota, autorização Google ou exposição de payload corretivo.
- **Persistência:** V4 preservada; sequência aproveita histórico existente, sem alteração de schema.
- **Testes/manutenção:** regressões acrescentadas sem relaxar invariantes; limites e causa das correções documentados.

## Limitações e verificações

- Nenhum teste, build, shell, escrita ou operação remota executado pelo revisor.
- Os **91 testes sem falhas/erros/skips e os controles negativos** são resultados informados pelo coordenador, não verificados por execução independente.
- Digests e correspondência Git dos snapshots não foram recalculados.
- A documentação do diff menciona IDs de achados do outro revisor; não consultei seu relatório nem usei suas conclusões como prova.
- Não há comprovação de todos os interleavings concorrentes, desempenho dos triggers, segurança de arquivos de recursos ou funcionalidades das tarefas 3+.
- Solicito ao coordenador vincular comandos/logs ao head final e conferir refs/checks atuais antes de apresentar a decisão ao usuário.

**Esta é revalidação do lote, não revisão cumulativa final da spec nem aprovação de merge.**
