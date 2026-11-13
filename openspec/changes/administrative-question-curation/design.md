# Design

## Context

Planejamento da [issue #177](https://github.com/LordBiGoDoNE/concurso-simulator/issues/177) sobre `main` `226d2e5b07208887941811beff10ac29295b04b3`. Motivação e decisões do usuário: [proposal.md](proposal.md). Requisitos: [spec](specs/administrative-question-curation/spec.md).

| Área | Estado observado na base | Consequência |
| --- | --- | --- |
| `EditorialDecision`, `DecideQuestionUseCase` | Decisão por questão com revisão, fingerprint, responsável, checklist, justificativa; confere ponteiro elegível e `editorialSequence` esperados sob trava | Reutilizar o domínio por item; o lote coordena várias decisões, não reimplementa transições |
| `qb_decision` (V4) | Histórico imutável (trigger), sem campo de origem da revisão | Nova coluna anulável; linhas históricas ficam "não informado" |
| CLI `questionBankLocal decide` | Uma decisão por execução, confirmação `DECIDE <DB_URL> <SHA-256>`, recusa APPROVE com CI=true | Preservada; passa a aceitar a origem da revisão |
| `POST /api/v1/admin/question-imports/{preview,apply}` (#160) | Prévia sem escrita, token HMAC (ator, destino, digests, tempo), aplicação atômica, recibo, replay por nonce | Mesmo modelo para o lote de decisões |
| `GET /api/v1/question-bank/questions[/{id}]` | Pública, somente elegível, sem origem/fingerprint/estado/gabarito | Não alterar; consulta administrativa usa prefixo próprio |
| Segurança ADM (V13) | Sessão ADM persistente, troca inicial obrigatória, versão da credencial, CSRF | Reutilizar a autorização administrativa existente |

Referências obrigatórias: ADR 0001, `docs/architecture/http-endpoint-review.md`, `docs/question-administrative-import.md`, `docs/question-local-operations.md`.

## Goals / Non-Goals

**Goals:** permitir ao ADM consultar o acervo e decidir em lote com prévia e confirmação; registrar a origem da revisão; manter domínio Java puro e autorização na infraestrutura; preservar histórico, rotas de estudante e CLI.

**Non-Goals:** curadoria automática pela plataforma, detecção de quase-duplicatas, edição de conteúdo, upload pela interface, gestão de vários ADMs, publicação de recursos.

## Decisions

### 1. Plataforma, não curadoria

A plataforma valida estrutura, integridade e transições; não decide quais questões são boas. Sem regras editoriais novas impostas pelo sistema além das já existentes (checklist completo para APPROVE, transições válidas do domínio).

### 2. Origem da revisão

`EditorialDecision` ganha `reviewKind` (`HUMAN_PEER` | `AI_ONLY`), obrigatório em APPROVE criado depois desta mudança e opcional nas demais ações. Persistido em `qb_decision.review_kind` (anulável, CHECK nos valores; nulo = histórico não informado). A consulta administrativa expõe a origem da decisão que aprovou a revisão elegível. A CLI aceita o campo no arquivo de decisão (formato compatível: ausência só é aceita em ações que não sejam APPROVE).

Alternativa descartada: marcar a revisão de conteúdo — a origem é da avaliação, não do texto; a mesma revisão pode ser reavaliada.

### 3. Consulta administrativa (somente leitura)

Sob `/api/v1/admin/questions`, cadeia de segurança administrativa (sessão ADM, permissão persistida, no-store):

- `GET /api/v1/admin/questions`: paginação (até 100), filtros AND por finalidade/namespace, prefixo de chave de origem, assunto, estado da última revisão, presença de elegível, origem da revisão e intervalo de atualização. Item: questionId, origem, última revisão (id, estado, fingerprint), elegível (id, fingerprint, origem da revisão), `editorialSequence`.
- `GET /api/v1/admin/questions/{id}`: todas as revisões com conteúdo integral (inclusive gabarito e resolução), decisões (com origem da revisão) e recibos de importação relacionados.
- `POST /api/v1/admin/questions/lookup`: até 1.000 pares (namespace, chave, fingerprint opcional) → por par: ABSENT, SAME_AS_LATEST, SAME_AS_ELIGIBLE, DIFFERENT, com os ids/fingerprints atuais. POST por tamanho do corpo; sem efeitos.
- `POST /api/v1/admin/questions/export`: seleção explícita (ids ou recibo) → JSON no formato `administrative-question-package/v1` reimportável, limites iguais aos da importação.

Casos de uso de leitura no núcleo com porta de consulta; projeções JPA/SQL na infraestrutura; DTOs tipados.

### 4. Decisões em lote

`POST /api/v1/admin/editorial-decisions/preview` e `/apply`:

- Envelope: `action`, seleção (lista de `{questionId, revisionId, fingerprint}` ou `importReceiptId`), `reviewKind`, `checklist`, `rationale`. Até 100 itens por lote (mesmo teto da importação).
- Prévia sem escrita: por item, estado atual, transição resultante ou motivo de recusa; digest da seleção com o estado observado (ponteiro elegível e sequência de cada item); token HMAC vinculando ator, destino, digest e tempo (15 min), como na importação.
- Aplicação: revalida o token e o estado de cada item igual ao da prévia, sob trava ordenada, e aplica `DecideQuestionUseCase` por item na mesma transação. Qualquer divergência recusa o lote inteiro (sem efeito parcial), devolvendo os itens divergentes para nova prévia.
- Recibo de decisões: ator, horário do servidor, destino, ação, origem da revisão e decisões criadas; replay do mesmo token devolve o mesmo recibo.
- `responsible` da decisão = identificador do ADM autenticado; `rationale` do envelope (com referência à prévia).

Conferência de concorrência como decisão do operador (decisão 3 da #177): a plataforma não procura conflitos editoriais; a única recusa automática é a divergência entre prévia confirmada e estado atual, que é integridade técnica do consentimento.

### 5. Interface administrativa mínima (grupo opcional)

Tela para filtrar, revisar a prévia e confirmar um lote; sem edição de conteúdo. Pode ficar para depois sem bloquear a API.

## Risks / Trade-offs

- [Aprovação em massa mal selecionada] → prévia obrigatória, limite por lote, recibo e origem da revisão visíveis.
- [Exposição de gabaritos] → somente rotas ADM no-store; rotas de estudante inalteradas; testes de cadeia de segurança real.
- [Replay/resposta perdida] → nonce e recibo idempotente, como na importação.
- [Lote tudo-ou-nada pode frustrar lotes grandes] → limite de 100 e prévia reexecutável (decisão 1).
- [Histórico sem origem da revisão] → nulo explícito "não informado", nunca inferido.

## Migration Plan

Migrações V16 e V17 após a V15 da #173 (número final reconferido na integração): coluna `qb_decision.review_kind`, tabela de recibos de decisões em lote. Sem alterar migrações versionadas nem dados históricos. Rollback operacional: revogar permissão ADM; sem down migration.

## Decisões das questões em aberto (usuário, 2026-11-12)

1. **Lote tudo-ou-nada.** Qualquer divergência entre prévia e aplicação recusa o lote inteiro e devolve os itens divergentes para nova prévia; sem aplicação parcial.
2. **Limite de 100 itens por lote.** Um recibo de importação tem no máximo 100 questões (teto do pacote), então um recibo inteiro sempre cabe no limite.
3. **Somente SUBMIT e APPROVE na primeira entrega.** RETURN_TO_DRAFT e WITHDRAW continuam disponíveis pela CLI local e podem entrar no lote depois, por proposta própria.
4. **Acervo da #173 marcado `AI_ONLY`** na aprovação.

Numeração das migrações: V16 (`qb_decision.review_kind`) e V17 (recibos de decisões em lote), após a V15 da #173; o número final é reconferido na integração com a `main`.
