# Tasks

Spec da [issue #177](https://github.com/LordBiGoDoNE/concurso-simulator/issues/177). Base: `main` `226d2e5b07208887941811beff10ac29295b04b3`; integração em `epic/administrative-question-curation`. Referências: [proposta](proposal.md), [desenho](design.md), [requisitos](specs/administrative-question-curation/spec.md).

Estado: **proposta aprovada pelo usuário em 2026-11-12; grupos 1, 2, 3 e 5 autorizados para implementação** (grupo 4 opcional, não autorizado). Decisões das questões em aberto em [design.md](design.md#decisões-das-questões-em-aberto-usuário-2026-11-12). Autorização de implementação não autoriza merge, arquivamento ou publicação. Implementação: stack #180 ← #181 ← #182 (checklist e evidências na [issue #177](https://github.com/LordBiGoDoNE/concurso-simulator/issues/177)); as caixas abaixo só são marcadas após integração na épica. Cada grupo inclui código, testes e documentação. Risco previsto: crítico.

| Grupo | Dependência real | Áreas previstas | Critério de conclusão |
| --- | --- | --- | --- |
| 1. Origem da revisão | Domínio de decisão existente | `EditorialDecision`, migração, JPA, CLI local, docs | APPROVE exige origem; histórico não informado preservado; CLI compatível |
| 2. Consulta administrativa | Grupo 1 (filtro por origem) | Casos de uso de leitura, projeções, rotas ADM, OpenAPI | Busca/detalhe/lookup/exportação com matriz de acesso e exportação reimportável |
| 3. Decisões em lote | Grupos 1 e 2 | Caso de uso de lote, token, recibo, migração, rotas ADM | Prévia sem escrita, aplicação atômica tudo-ou-nada, replay, rollback |
| 4. Interface administrativa mínima (opcional) | Grupos 2 e 3 | React, acessibilidade, docs | Filtro, prévia e confirmação sem edição de conteúdo |
| 5. Operação e ensaio | Grupos 1–3 | Docs de operação, ensaio local | Guia atualizado e habilitação do acervo da #173 em ambiente local de teste |

## 1. Origem da revisão

- [ ] 1.1 Acrescentar `reviewKind` (`HUMAN_PEER`/`AI_ONLY`) à decisão no domínio, obrigatório em APPROVE novo; testes de domínio sem framework.
- [ ] 1.2 Migração com `qb_decision.review_kind` anulável e CHECK; JPA; histórico preservado como nulo; teste com PostgreSQL isolado.
- [ ] 1.3 CLI `decide` aceita o campo; recusa APPROVE sem origem antes de conectar; docs de operação.

## 2. Consulta administrativa

- [ ] 2.1 Casos de uso de busca/detalhe/lookup/exportação com porta de leitura e projeções na infraestrutura.
- [ ] 2.2 Rotas `/api/v1/admin/questions` com DTOs tipados, no-store, OpenAPI; revisão pelo checklist de endpoints.
- [ ] 2.3 Testes de cadeia de segurança real (visitante, comum, ADM pendente, revogado, válido) e PostgreSQL isolado; rotas públicas inalteradas.
- [ ] 2.4 Exportação validada pelo leitor de importação com fingerprints idênticos.

## 3. Decisões em lote

- [ ] 3.1 Caso de uso de prévia e aplicação em lote reutilizando `DecideQuestionUseCase` por item, transação única, tudo-ou-nada.
- [ ] 3.2 Token de confirmação e recibo idempotente no modelo da importação; migração do recibo.
- [ ] 3.3 Rotas `/api/v1/admin/editorial-decisions/{preview,apply}`, CSRF, OpenAPI, testes de segurança, divergência, replay e rollback.
- [ ] 3.4 Atualizar `docs/question-administrative-import.md` e `docs/question-local-operations.md`, registrando a substituição da restrição "sem decisões editoriais HTTP".

## 4. Interface administrativa mínima (opcional)

- [ ] 4.1 Tela de consulta, prévia e confirmação de lote, acessível por teclado; testes de componente e navegador.

## 5. Operação e ensaio

- [ ] 5.1 Ensaio local: importar e habilitar o acervo da #173 (origem `AI_ONLY`) pelo fluxo novo, registrando seleção, digests, destino e recibos, preservando dados existentes.
