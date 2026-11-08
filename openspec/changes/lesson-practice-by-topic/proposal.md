# Proposal

Acompanhamento: [issue #161](https://github.com/LordBiGoDoNE/concurso-simulator/issues/161).

## Why

O painel de prática solicita um cargo mesmo quando o estudante já escolheu uma
aula. As questões próprias de Português, Matemática e Informática atendem aos
dois cargos; essa escolha não representa dificuldade ou profundidade do conteúdo.
O estudante quer praticar o assunto da aula, independentemente do cargo.

Em 2026-11-08, o usuário escolheu remover o vínculo também do backend, em vez de
somente esconder o seletor. Aprofundamento por matéria terá proposta futura.

## What Changes

- Iniciar e consultar disponibilidade da prática somente pelo assunto, com até
  cinco questões fixas aprovadas e exclusivas de PRATICA_AULA. A classificação
  editorial de cargos permanece, mas não filtra esse treino.
- Remover cargo da entrada, contexto público da realização e histórico de práticas.
  Manter o cargo na organização do edital, biblioteca e simulados.
- Preservar o painel recolhível, questões após clique, correção ao encerrar,
  respostas pendentes, idempotência, posse, expiração e isolamento entre modos.
- Introduzir captura formato 4 sem composição por cargo para novas práticas;
  preservar dados, resultados e comandos anteriores, incluindo práticas formato 3.
- Atualizar API e tipos específicos de prática, sem tornar cargo opcional nos
  contratos de simulados nem inventar um cargo interno para satisfazer o modelo.
- Registrar confirmação explícita de início/grupos após apresentar spec e issue,
  como regra permanente em AGENTS.md e no contexto do OpenSpec.

## Capabilities

### New Capabilities

- `lesson-practice-by-topic`: prática independente de cargo, com compatibilidade
  histórica e nova apresentação/contratos por assunto.

### Modified Capabilities

Nenhuma capability consolidada será reescrita nesta entrega. O novo requisito
substitui, para novas práticas, a seleção por assunto/cargo documentada na change
histórica `lesson-practice`, integrada à main pelo PR #154. Essa change permanece
preservada; não haverá arquivamento automático.

## Impact

Futura implementação: domínio/aplicação de práticas, codec/JPA, nova migração
Flyway, DTOs HTTP, fila/executor React tipados, histórico, navegação e testes.
Preservar ADR 0001 e o checklist de endpoints. Nenhuma mudança editorial, carga,
reclassificação de questões, integração com Google ou publicação está incluída.

Nesta etapa serão publicados somente proposta, design, requisitos, tarefas e
regras do projeto. A autorização documental não autoriza implementar.

## Workflow

Base: main `f61d57d5d55d6d61609815a2faa4b415ed8dca4b`, conferida com origin/main.
Épica: `epic/lesson-practice-by-topic`. Proposta:
`task/lesson-practice-by-topic/proposal`, em worktree isolado.

A interface final depende de `new-student-experience` ([#158](https://github.com/LordBiGoDoNE/concurso-simulator/issues/158),
PR #159 e refinamentos locais ainda não integrados). Não transportar essa stack,
fazer merges ou criar dependência entre specs via Stacks por implicação.

Após entregar os links e grupos 1–4, solicitar confirmação explícita para começar
e quais grupos estão autorizados. Implementação, revisões por modelo, merge,
arquivamento e deploy não foram autorizados nesta entrega documental.
