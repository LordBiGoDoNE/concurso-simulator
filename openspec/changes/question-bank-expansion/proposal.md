# Proposal

Issue: [#173 — Acervo e práticas com 25 questões](https://github.com/LordBiGoDoNE/concurso-simulator/issues/173).

Planejamento aprovado em 2026-11-10. Esta entrega publica somente documentação; nenhum grupo de implementação está iniciado. O próximo início exige confirmação explícita dos grupos completos, conforme o plano e o AGENTS.md.

## Why

O estudante encontra pouca variedade: as evidências versionadas registram 255 questões de prática e 251 bases de simulado nos 51 assuntos. Ampliar os dois acervos e permitir práticas de até 25 itens dará mais oportunidades de treino sem substituir as aulas ou apagar realizações existentes.

## What Changes

- Garantir pelo menos 25 bases elegíveis por assunto em cada modalidade: 1.275 de SIMULADO e 1.275 de PRATICA_AULA, contadas sem duplicação por cargo, revisão ou variante.
- Exigir pelo menos cinco originais de provas anteriores em cada combinação assunto/modalidade, totalizando no mínimo 510 distintas. Priorizar Avança SP e complementar com bancas compatíveis; manter autoria, gabarito definitivo e evidência de reutilização verificáveis.
- Completar a cobertura com autorais identificadas, resolução explicada, fontes conferidas e revisão por item. Adaptações substanciais não contam como originais; pendências não são supridas por paráfrases ou variantes numéricas.
- Acrescentar edição editorial nova, inventário, compilação determinística, triagem de duplicatas e relatórios de cobertura. Preservar fontes históricas e bases existentes válidas; os aproximadamente 2.044 acréscimos são estimativa, sujeita à reconciliação do destino.
- **BREAKING**: a disponibilidade passa de `maximum: 5` para `maximum: 25`; clientes estritos devem acompanhar a atualização. Novas práticas selecionam `min(25, elegíveis)`, mantendo replay, respostas e histórico anteriores.
- Reutilizar a importação #160 com pacotes separados por assunto/modalidade e entrada DRAFT. Conferência editorial e habilitação local permanecem operações deliberadas separadas.

## Capabilities

### New Capabilities

- `question-bank-coverage`: cobertura mínima por assunto/modalidade, diversidade de origens, evidência editorial e preparação/importação rastreável dos incrementos.
- `lesson-practice-capacity`: limite 25 na composição, disponibilidade, leitura e histórico, com preservação das práticas anteriores.

### Modified Capabilities

Nenhuma entrada canônica existe em `openspec/specs/`, conforme `openspec list --specs`. Os deltas ADDED acima registram comportamentos desta evolução; não significam recriar os acervos, a prática ou a importação já implementados nas mudanças históricas.

## Impact

Base: `main` `16ca8aef382d64e7652b280b413122c357775ead`; épica `epic/question-bank-expansion`; proposta em `task/question-bank-expansion/proposal`. [Design](design.md), [tarefas](tasks.md), [cobertura](specs/question-bank-coverage/spec.md) e [capacidade](specs/lesson-practice-capacity/spec.md).

Afeta ferramentas/conteúdo editorial, núcleo de práticas, contratos HTTP/OpenAPI, cliente tipado, interface e regressões. Sem novos endpoints, dependências de runtime, migrações ou formato de snapshot; frontend/API devem ser atualizados juntos. As realizações antigas não serão regravadas.

#158 e #160 estão integradas pelos PRs #163 e #171. #161 permanece proposta independente: remoção de cargo não entra aqui e deverá considerar o teto 25 ao ser retomada. Manter composição de simulados e cargo atual, fontes fora dos builds públicos, proteções de identidade/fila e gabarito somente após encerramento. Entrega operacional na VPS, publicação, merge e arquivamento ficam fora desta autorização documental.
