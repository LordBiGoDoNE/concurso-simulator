# Proposal

Issue: [#173 — Acervo e práticas com 25 questões](https://github.com/LordBiGoDoNE/concurso-simulator/issues/173).

Planejamento aprovado em 2026-11-10. Esta entrega publica somente documentação; nenhum grupo de implementação está iniciado. O próximo início exige confirmação explícita dos grupos completos, conforme o plano e o AGENTS.md.

**Atualização de coordenação em 2026-11-10:** usuário autorizou implementação local dos grupos completos 1–8, em conversa própria da #173. A frase anterior descreve a publicação inicial #174, preservada. Grupo 1 primeiro e conteúdo 3–7 em seguida; grupo 2 e validação integrada 8 aguardam #161 integrada à main por merge autorizado. A #161 tem prioridade nos contratos de prática. Depois, conciliar teto 25 com prática por assunto sem cargo, sem restaurar cargo nem empilhar specs. [Estado e limites](tasks.md) e [guia técnico](../../../docs/question-bank-expansion.md); nenhuma publicação de implementação, revisão, integração ou expansão completa presumida.

## Esclarecimento de escopo — 2026-11-11

O usuário esclareceu: cada prática deve conter até cinco questões sorteadas de
um acervo com pelo menos 25 por assunto/modalidade. A interpretação anterior de
25 por prática foi superada. A meta editorial 25/5 originais por célula permanece;
a correção não autoriza reduzir o acervo. Novas práticas usam `min(5,pool)`;
capturas anteriores maiores são preservadas, sem regravar V1–V15.

## Why

**Dependência satisfeita em 2026-11-10:** #161 integrada à main pelo PR #176,
merge `226d2e5b07208887941811beff10ac29295b04b3`, conforme pedido de continuidade
do usuário. Grupo 2 adota início `{commandId,topicId}`, sem cargo, e captura formato
4. Grupo 8 continua dependendo da conclusão dos grupos 2–7. As referências anteriores
a aguardar #161 descrevem a ordem histórica, sem nova pausa de implementação.

O estudante encontra pouca variedade: as evidências versionadas registram 255 questões de prática e 251 bases de simulado nos 51 assuntos. Ampliar os dois acervos e sortear práticas curtas de até cinco itens desse acervo dará mais oportunidades de treino sem substituir as aulas ou apagar realizações existentes.

## What Changes

- Garantir pelo menos 25 bases elegíveis por assunto em cada modalidade: 1.275 de SIMULADO e 1.275 de PRATICA_AULA, contadas sem duplicação por cargo, revisão ou variante.
- Exigir pelo menos cinco originais de provas anteriores em cada combinação assunto/modalidade, totalizando no mínimo 510 distintas. Priorizar Avança SP e complementar com bancas compatíveis; manter autoria, gabarito definitivo e evidência de reutilização verificáveis.
- Completar a cobertura com autorais identificadas, resolução explicada, fontes conferidas e revisão por item. Adaptações substanciais não contam como originais; pendências não são supridas por paráfrases ou variantes numéricas.
- Acrescentar edição editorial nova, inventário, compilação determinística, triagem de duplicatas e relatórios de cobertura. Preservar fontes históricas e bases existentes válidas; os aproximadamente 2.044 acréscimos são estimativa, sujeita à reconciliação do destino.
- Manter `maximum: 5` para novas práticas, selecionando aleatoriamente `min(5, elegíveis)` do acervo completo. Available informa a contagem real, incluindo 25 ou mais; replay, respostas e histórico permanecem intactos.
- Reutilizar a importação #160 com pacotes separados por assunto/modalidade e entrada DRAFT. Conferência editorial e habilitação local permanecem operações deliberadas separadas.

## Capabilities

### New Capabilities

- `question-bank-coverage`: cobertura mínima por assunto/modalidade, diversidade de origens, evidência editorial e preparação/importação rastreável dos incrementos.
- `lesson-practice-capacity`: limite 25 na composição, disponibilidade, leitura e histórico, com preservação das práticas anteriores.

### Modified Capabilities

Nenhuma entrada canônica existe em `openspec/specs/`, conforme `openspec list --specs`. Os deltas ADDED acima registram comportamentos desta evolução; não significam recriar os acervos, a prática ou a importação já implementados nas mudanças históricas.

## Impact

Base: `main` `16ca8aef382d64e7652b280b413122c357775ead`; épica `epic/question-bank-expansion`; proposta em `task/question-bank-expansion/proposal`. [Design](design.md), [tarefas](tasks.md), [cobertura](specs/question-bank-coverage/spec.md) e [capacidade](specs/lesson-practice-capacity/spec.md).

Afeta ferramentas/conteúdo editorial, núcleo de práticas, contratos HTTP/OpenAPI,
cliente tipado, interface e regressões. Sem novos endpoints, dependências de runtime
ou formato de snapshot; frontend/API devem ser atualizados juntos. V15 altera apenas
o CHECK do teto conforme ajuste autorizado. Realizações antigas não serão regravadas.

#158/#160 estão integradas pelos PRs #163/#171, e #161 pelo PR #176. A #173 preserva
o contrato sem cargo e formato 4 dessa base, ampliando apenas o teto. A V15 para o
CHECK de quantidade tornou-se necessária e foi autorizada conforme
[ajuste registrado](capacity-schema-amendment.md). Manter composição de simulados,
fontes fora dos builds públicos, proteções de identidade/fila e gabarito somente
após encerramento. VPS, publicação, merge e arquivamento exigem seus marcos próprios.
