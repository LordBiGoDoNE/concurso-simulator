# Revisão cumulativa — mock-exam-attempts

**Gate final pendente.** A revisão abaixo cobre somente o lote solicitado: grupos
1 e 2 completos, 8/24 tarefas implementadas/verificadas, 0/8 integradas/aprovadas.
Os grupos 3–6 aguardam pedido próprio. Não é revisão final da spec nem autorização de merge.

## Lote 1 — grupos 1 e 2

Revisão independente em contexto novo `/root/mock_round1_sol_initial`, somente leitura,
com seleção `openai/gpt-6.1-sol#xhigh` aceita pelo orquestrador Codex. A sessão não expôs
telemetria independente de modelo/variante do provedor. Risco **crítico**: posse/conteúdo
privado, snapshots, schema/FKs, comandos concorrentes/idempotentes e exclusão temporária.

**Parecer:** nenhum achado bloqueador ou defeito funcional confirmado em #66–#70
ou na combinação. Triagem completa; nenhuma correção ou revalidação adicional indicada.
O revisor leu AGENTS, ADR 0001, checklist HTTP, política de revisão e todos os artefatos
aprovados. Não recebeu conversa, justificativas de implementação ou pareceres anteriores.

| PR | Head revisado | Cobertura |
| --- | --- | --- |
| [#66](https://github.com/LordBiGoDoNE/concurso-simulator/pull/66) | `e1a398e97d2a26f040c5d6e17abc117571db266d` | Composição, agregado, snapshots, projeções, correção e testes de domínio |
| [#67](https://github.com/LordBiGoDoNE/concurso-simulator/pull/67) | `5eef51942372c27b2a14ecb1f6fe850693870fd5` | UseCases/portas, posse, hash/replay/retries, histórico e testes substituíveis |
| [#68](https://github.com/LordBiGoDoNE/concurso-simulator/pull/68) | `029eecbbf9b0d07b35cf7c2aef35eef724a0fe44` | V5, JPA, constraints/FKs, captura interna e schema/upgrade |
| [#69](https://github.com/LordBiGoDoNE/concurso-simulator/pull/69) | `a08906b9ca4f13f869bca5e2a85a49adb27090e5` | Transação/CAS/rollback, precisão temporal, captura e corridas reais |
| [#70](https://github.com/LordBiGoDoNE/concurso-simulator/pull/70) | `6612007093be210936489597dbd12a6b67612d93` | Histórico privado, expiração, startup, scheduler e cascata |

Stack #71; trunk `epic/mock-exam-attempts`, base
`54896dae37a65dd0cd5b1cbc9f03fe387eb595a8`. Cada camada superior depende diretamente
na anterior. Nenhum rebase/sync pós-merge ou troca manual de base executado.
Digest canônico da combinação:
`c09a2e0cbc2298d08f3fb6ae96c82306b8adc8bfba57db8c39b96a91caceb7e8`.
Base/heads/digests individuais e URLs dos checks estão no JSON. O registro posterior
altera somente estes dois arquivos de evidência, excluídos pelo algoritmo canônico.

## Fundamentação e limites da análise

O revisor conferiu composição limitada, seleção sem reposição e conteúdo/ordens
imutáveis; a correção usa a versão capturada. Projeções em andamento omitem correção,
proveniência e nota parcial. Posse/expiração antecedem replay ou estado; o histórico
restringe owner conta. Captura, tentativa, itens e comando ficam na mesma transação;
question-bank não depende de mockexam. V5 preserva V1–V4 e complementa invariantes com
constraints/triggers/FKs. @Version e hash canônico impedem sobrescrita/reuso divergente;
conflitos conhecidos são tratados após rollback, sem absorver FK/PK/check/falhas desconhecidas.
Clock substituível, precisão de microssegundos, bloqueio exato em 24h e limpeza restrita
foram conferidos junto aos testes/documentação. Parents, HEAD limpo e digest foram
recalculados pelo revisor. Foram 28 arquivos de implementação/docs/testes, sem binários/lockfiles.

A análise estática não demonstra execução de corridas nem operação sob carga/falhas.
O revisor não executou testes, builds, rede, DB ou merges nem escreveu arquivos.
A fronteira técnica de sessão/CSRF/HTTP e a interface pertencem aos próximos grupos;
esses fluxos não estão entregues neste lote. A referência final a Astra na política
não dispara outro modelo nem amplia a exceção de question-bank.

## Verificação do coordenador

- Backend combinado: **138 testes, 0 falhas/erros/skips**, `test bootJar` passou no Windows/Java25 com PostgreSQL18.6 isolado.
- OpenSpec: `validate mock-exam-attempts --strict` passou; apply confirma 8/24 tarefas.
- Todos os checks dos cinco heads revisados passaram: backend, frontend, study-material,
  identity-browser e spec-review-tests. São verificações de tarefa, sem certificar gate final.
- Runs de Application checks (pull_request):
  [#66](https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/38669731905),
  [#67](https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/38669738861),
  [#68](https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/38669744484),
  [#69](https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/38669749396),
  [#70](https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/38669757339).

## Gate cumulativo final e integração

O status principal do JSON permanece pending, com modelo/sessão/base/head/digest
finais nulos. Selecionar o modelo/variante do gate final antes de contexto novo;
a exceção temporária SOL do #63 não se amplia automaticamente. Implementação ou
aprovação destes cinco PRs não autoriza integração da spec na épica geral/main.
Cada PR precisa de aprovação própria. Antes do primeiro merge real da stack,
conferir novamente composição/SHAs/checks e apresentar os limites do caminho ainda
não ensaiado; sync pós-merge exige autorização específica. Sem deploy/arquivamento.
