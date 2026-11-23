# Comparação e triagem — rodada 1

## Resultado e composição

Cinco tarefas novas concluídas/verificadas: **1.2, 1.3, 2.1, 2.2, 2.3**; 1.1 é dependência.
Progresso OpenSpec: **6/44 implementadas/verificadas, 0/44 aprovadas/integradas**.
Nenhum merge, publicação, arquivamento ou aprovação editorial real efetuado.

| PR | Escopo | Base | Head |
| --- | --- | --- | --- |
| #38 | 1.1 catálogo, dependência existente | épica `600cf6a` | `cf857f1` |
| #40 | 1.2 conteúdo/revisões | #38 `cf857f1` | `447dec1` |
| #42 | 1.3 ciclo editorial | #40 `447dec1` | `efb5c57` |
| #43 | 2.1 schema/JPA | #42 `efb5c57` | `a195035` |
| #44 | 2.2 persistência/concorrência | #43 `a195035` | `35a3606` |
| #45 | 2.3 idempotência/recovery | #44 `35a3606` | `2e38370` |
| #46 | correções/regressões da revisão | #45 `2e38370` | `0551863` |
| #39 | processo independente, fora da stack | épica `600cf6a` | `ce624fc` |

Stack de produto #41, trunk `epic/question-bank`. Bases/heads inferiores preservados.
SHAs completos e digests estão em initial-manifest.json/final-manifest.json; checks
verdes com URLs/horário em checks.json. Não confundir CI `spec-review-tests` com aprovação
semântica ou gate final da spec. As correções de #46 são necessárias ao conjunto recomendado;
não apresentar o prefixo inferior sozinho como se já incluísse essas correções.

Produto final revisado: base `600cf6a59a608cec37c3cc22e4902de46ef5a46d`, head
`05518635fe160b92e8798dd78c03eff96f6234e5`, digest
`6dea8ab7292bbec0e7c19fad9398db3a0a452c5b587d9b3c820210068a92df45`.
Os relatórios iniciais conservam seus digests; ambos reexaminaram o novo conjunto/combinação.

## Achados comuns e contribuições adicionais

Inicialmente, Astra reportou uma recomendação e um opcional; SOL, duas recomendações
e um opcional. São **quatro achados distintos**, sendo o opcional comum aos dois.
Não houve bloqueadores. Pedidos adicionais de cobertura não foram inflados como findings.

| Achado | Revisor | Origem | Triagem e evidência |
| --- | --- | --- | --- |
| Classificação não variava no teste de substituição | Astra QB-ASTRA-01 | #44, interação catálogo/conteúdo/editorial | **fixed em #46**, classificação distinta conferida no PostgreSQL durante draft/revisão/aprovação e histórica; ambos revalidaram a cobertura. |
| Ponteiro esperado não detectava ciclo editorial antigo/ABA | SOL SOL-QB-001 | #42/#44 | **fixed em #46**, sequência monotônica de decisões conferida sob trava; duas regressões determinísticas falharam antes e passaram depois. |
| UTF-16 malformado era substituído na canonização UTF-8 | SOL SOL-QB-002 | #40 com efeito em #42/#45 | **fixed em #46**, rejeitar surrogates inválidos incluindo URL; pares suplementares válidos preservados. Controle negativo reproduziu rejeição ausente. |
| Consulta global por trigger diferida por linha | Ambos QB-ASTRA-02 / SOL-QB-003 | #43 | **deferred proposto ao usuário**, não corrigido. Sem limite de desempenho violado/degradação medida; manter V4 e medir antes de crescimento operacional, eventual migração aditiva. |

Contribuição adicional demonstrada do SOL: dois problemas reproduzidos por controles
negativos, não apenas inspeção de mais arquivos. Contribuição específica do Astra:
lacuna de regressão de classificação. Sobreposição de conclusões arquiteturais/integracionais,
ressalva de round-trip e mesmo opcional; não demonstram equivalência geral dos modelos.

Round-trip enriquecido com recurso/fundamento legal/Unicode e corrida multiorigem com
entrada invertida foram acrescentados. Ambos confirmaram pedidos atendidos na leitura
dos testes e **nenhum achado novo** na revalidação. Nenhum revisor executou testes.

## Consumo real

Inclui tentativa inicialmente bloqueada por acesso, revisão inicial e revalidação na
mesma sessão de cada revisor. Modelos/variantes efetivos conferidos em todas as mensagens.
Tokens de raciocínio são o campo reportado pela API, não soma adicional à saída.

| Modelo | Chamadas | Entrada nova | Cache read | Saída | Raciocínio reportado | Cache write |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Astra medium | 20 | 208.649 | 1.838.592 | 4.743 | 975 | 0 |
| SOL 6.1 xhigh | 23 | 532.369 | 1.953.408 | 8.179 | 18.810 | 0 |

| Fase | Astra: chamadas / entrada nova | SOL: chamadas / entrada nova |
| --- | ---: | ---: |
| Inicial, incluindo bloqueio de acesso | 13 / 146.345 | 11 / 158.300 |
| Revalidação | 7 / 62.304 | 12 / 374.069 |

Nesta amostra, SOL consumiu aproximadamente **2,55× a entrada nova** do Astra no total;
a maior diferença ocorreu na revalidação. Não atribuir causalidade, preço ou economia
geral apenas por isso. Os contextos examinados, uso de cache e variantes diferem.
Sem preço confiável disponível, nenhum gasto financeiro ou projeção de próximas rodadas
foi calculado. Uma estimativa futura deve ser explicitamente extrapolação desta amostra,
com hipóteses, não promessa de qualidade/economia.

Coordenador, parcial de **2026-10-11T19:08:34.193Z a 22:42:17.396Z**: 114 chamadas,
404.046 entrada nova, 16.923.264 cache read, 53.597 saída, 18.620 raciocínio reportado,
cache write zero. Exclui revisores e todas as mensagens posteriores à coleta, inclusive
consolidação final. Não somar este parcial como gasto completo do projeto.

JSONs resumidos e JSONL por chamada preservam IDs/tempos/modelos/tokens; sem prompts,
segredos ou quota. Totais por revisor já incluem suas fases, não somar inicial + total.

## Decisões e limitações pendentes

- Usuário decide o adiamento opcional e a aprovação dos PRs explicitamente enumerados;
  nenhum relatório presume essa decisão. Triagem automática do gate final não foi concluída.
- Revisão parcial de lote não cobre tarefas 3+ nem substitui revisão cumulativa `[SPEC]`.
- Diffs de processo/correção expõem resumos/IDs anteriores; cegamento não é absoluto.
  Relatórios detalhados e resultados entre revisores não foram compartilhados na análise inicial.
- Primeiro merge/prefixo/fila e sync pós-merge de Stacks seguem não ensaiados. Antes de
  operação real, apresentar composição/SHAs/checks/caminho/limites para autorização específica;
  não retargetar/unstack ou sincronizar silenciosamente. Aprovação do topo não aprova inferiores.
- PR independente de evidência registra relatórios/metadados; não altera os diffs semânticos
  analisados nem transforma uma revisão parcial em gate completo.
