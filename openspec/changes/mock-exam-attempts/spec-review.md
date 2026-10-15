# Revisão cumulativa — mock-exam-attempts, PR #84

**Revisão independente concluída no head conjunto `6727055d0568a13c8b2dab8e61dfe7a7b153afd6`.**
SOL-CUM-R1 corrigido; nenhum novo achado ou bloqueador remanescente confirmado.
Autorização de merge do **#84 ainda pendente**. O usuário autorizou apenas as tarefas
#83/#85; o gate automatizado do head real deve ser conferido após integrar #83.

## Escopo e cobertura

Modelo permanente `openai/gpt-6.1-sol#xhigh`, contexto inicialmente novo
`/root/spec_reviewer_sol_global`, mesmo revisor nas revalidações. Seleção aceita pelo
Codex como gpt-6.1-sol +xhigh; telemetria independente da variante do provedor não exposta.
Risco **crítico**: posse/sessão/CSRF, snapshot e correção privada, idempotência/concorrência,
schema/transações/rollback/expiração e política de seleção do revisor.

Diff completo cumulativo de 94 arquivos relevantes, incluindo código, requisitos,
testes, documentação e política permanente SOL. Os dois arquivos de evidência não
foram lidos pelo revisor e são os únicos excluídos do digest. A matriz foi examinada
depois da análise semântica independente inicial e conferida novamente no conjunto.

| Área | Cobertura |
| --- | --- |
| requirements | Composição de 1–100/filtros/insuficiência/sem reposição, versões/ordens/respostas, conclusão/correção, visitante/histórico e 24 itens da matriz |
| architecture | ADR 0001/checklist HTTP, núcleo puro, agregado/UseCases/portas, transações/JPA infra, ator verificado e DTOs concretos |
| integration | Porta questionbank, HTTP/OpenAPI, fila/retry/resultado/histórico/identidade e coexistência real dos PRs #83/#85 |
| security | Posse antes de replay/exposição, principal/sessão/CSRF/CORS/404/no-store, projeções ativas e respostas tardias |
| persistence | V5 aditiva, captura histórica, constraints/triggers/FKs, versão/CAS/comandos/rollback/expiração/cascata restrita |
| tests | Testes novos de todas as camadas e browser/artefatos/operação lidos; regressão R1 e matriz completas revalidadas |
| maintenance | Rollback sem apagar dados, fixtures só de teste, conteúdo acessível e política SOL sem relaxar gate/workflow/aprovações |

## Achados e triagem

**SOL-CUM-R1 — recommended → fixed.** A tela de resultado mostrava apenas texto das
alternativas escolhida/correta, omitindo descrições de figuras presentes no snapshot.
O #85, aprovado e integrado em `e1c03ec66921c5dca70099f889d175d09d9999b1`, passou a usar Content/AttemptText
completo em blocos HTML válidos. A regressão distingue textos/descrições das duas
alternativas e exige foco/nota; o renderer anterior falha no controle negativo.
Resposta em branco, resolução, fontes e nota do servidor foram preservadas.

O revisor confirmou a correção novamente no head conjunto, sem novos achados
blocking/recommended/optional. Triagem encerrada. Os achados dos lotes anteriores
permanecem históricos separados, sem serem apresentados como resultados desta revisão.

## Evidências e limitações

Coordenador/GitHub Actions: **149 backend, 108 frontend, 14 cenários reais de navegador**, builds,
study-material/aulas/offline e guard de revisão PASS no head conjunto. Oito testes de
cota Windows e 54 scripts Linux passaram na atualização da política. Quatro mudanças
OpenSpec estritas e inspeção de artefatos PASS; rollback operacional adicional já
verificado e não repetido para alterações somente frontend/política. URLs por tarefa
e verificações/executores estão no JSON; matriz em [verification.md](verification.md).

Revisor não executou testes/build/aplicação/browser/rede/DB, não inspecionou artefatos,
logs privados ou CI remoto, nem fez edições/merges. Após comunicar a conclusão inicial
independente e o achado, uma consulta list_agents expôs incidentalmente resumo de lote
anterior; essa exposição posterior limita qualquer alegação de zero exposição. As fases
complementares não repetiram a consulta. Contexto novo reduz viés, não garante ausência.
Testes e inspeção estática não demonstram toda falha de transporte, escalonamento,
carga, ausência de dica semântica ou decisão arquitetural. Cota/tokens indisponíveis;
observações privadas ficam fora do Git e são apresentadas ao usuário separadamente.
Uma tentativa intermediária interrompida não produziu parecer e não foi contada como revisão.

## Revalidação

Base da épica geral: `d65006eb29a733c13c4f2a9f3741147d235dcd5b`.
Head conjunto examinado: `6727055d0568a13c8b2dab8e61dfe7a7b153afd6`.
SHA-256 cumulativo recalculado pelo revisor: `d1f7bd2706356ad74d6303426fc8e2ed25edb4e28267cf5bf02eca860dc5fa08`.

O head conjunto tem pais `ee6366403deadb07871311f2de4c98f4b9c4ec57` e `e1c03ec66921c5dca70099f889d175d09d9999b1`;
preserva os commits aprovados/revisados 4ac43c4,4320caa,ee63664 e e1c03ec. A árvore do
merge do #85 é idêntica à do head do #85 aprovado. Contra o candidato inicial 4ac, o delta relevante
é exatamente renderer, regressão, documentação e matriz da correção #85.

O #83 foi revalidado contra sua **nova base real** `e1c03ec66921c5dca70099f889d175d09d9999b1`, não apenas por
igualdade visual/digest anterior. Digest por tarefa
`b4b2181fc89458f6032ea45e3965e8f7e607882de7fad46465560c309359c5d5` recalculado.
A política mantém SOL permanente, independência, risco, triagem, atualidade e aprovação;
sem novo bypass ou exigência de outro modelo.

No momento do parecer, #83 ainda estava aberto e o head real de #84 era e1c03ec.
Após o merge autorizado de #83, o coordenador deve confirmar a mesma base de #84,
ancestralidade deste head revisado, digest idêntico e o check real `spec-review`.
Somente estes dois arquivos de evidência são atualizados depois do head examinado.
Isso não autoriza merge de #84, épica geral→main, deployment ou arquivamento.

## Histórico — revisões dos lotes anteriores à integração

Os registros abaixo preservam os estados no momento das análises. Não certificam
a revisão cumulativa atual nem substituem as integrações registradas na issue #64.

## Lote 2 — grupos 3–6 completos

Revisor independente em contexto novo `openai/gpt-6.1-sol#xhigh`, sessão
`/root/mock_round2_sol_retry`, seguido de quatro revalidações na mesma sessão/modelo.
Seleção aceita pelo Codex; telemetria efetiva de variante do provedor não exposta.
Risco **crítico**: sessão/CSRF, privacidade/correção, concorrência, histórico/expiração
e operação/rollback. Nenhum segundo modelo foi disparado para o lote.

**Parecer final: R1–R5 corrigidos; nenhum novo achado nem bloqueador remanescente.**
Inicial em `27f6d730`: dois bloqueadores e um recomendado, confirmados pelo coordenador.
O #82 contém código, regressões e documentação para os três. A primeira revalidação
aprovou `69855a5`; depois, CI de push registrou falha de foco (o CI do PR passou).
O foco passou de efeito passivo para `useLayoutEffect`, antes da pintura, sem relaxar
asserções. Revalidacoes posteriores confirmaram a correcao da primeira escolha antes dos efeitos
passivos (R4, coordenador/CI) e retomada StrictMode (R5, revisor). Ambas possuem
regressoes com controles negativos que falham na implementacao anterior. Parecer final
confere o novo diff e a combinacao no snapshot abaixo.

| Achado | Causa e correção | Triagem |
| --- | --- | --- |
| R1, bloqueador, #75 | Corpo JSON falho ocultava rejeição conhecida. 401/403/404 agora invalidam pelos headers; regressões de corpo truncado/pendente e limpeza no App. | fixed, confirmado pelo revisor |
| R2, bloqueador, #79 | Renderer lazy sem contenção podia desmontar a raiz. Renderer inicial e projeção tipada no validador tardio eliminam download pós-confirmação; E2E exige resultado/aulas/identidade e única conclusão. | fixed, confirmado pelo revisor |
| R3, recomendado, #79 | Abrir ID atual descartava drafts/comando. Abertura sem efeito preserva estado e mesmo payload/commandId no retry. | fixed, confirmado pelo revisor |
| R4, bloqueador, #82 | Primeira escolha podia ocorrer sem controller. Instalacao em layout e regressao do primeiro commit impedem fila sem envio. | fixed, confirmado pelo revisor |
| R5, recomendado, #82 | StrictMode podia deixar fila parada. Nova geracao viva retoma o mesmo comando depois do abort; unmount definitivo nao repete. | fixed, confirmado pelo revisor |

## Snapshot, composição e checks do lote 2

Stack #76; trunk `epic/mock-exam-attempts`. Base `ec4f85ec133991c6e791914da54bd56ab088906a`.
Head de código revisado `c09c7a0c15913acda42bc530ff7352eab482531c`.
Digest canônico **recalculado pelo revisor**: `cd042b7e6ab417b85fdf157832bac2da986ab4102d4543f995f5ce90aeef1f40`.
Dez camadas em sequência; base de cada superior é o head anterior. Nenhum dos nove
heads originais foi reescrito. Bases/digests e URLs individuais dos checks estão no JSON.

| PR | Head revisado | Checks |
| --- | --- | --- |
| [#72](https://github.com/LordBiGoDoNE/concurso-simulator/pull/72) | `a550c241458cca6257fe859f31df0bc39080ed23` | SUCCESS |
| [#73](https://github.com/LordBiGoDoNE/concurso-simulator/pull/73) | `89c4525a1e9c17fd7dcf9d9960ca0d6d8d3b561f` | SUCCESS |
| [#74](https://github.com/LordBiGoDoNE/concurso-simulator/pull/74) | `5232393aaf875c26a23f9efece72dea3f4070ea9` | SUCCESS |
| [#75](https://github.com/LordBiGoDoNE/concurso-simulator/pull/75) | `6ac58ec0b0ffd811e3d2d9c2b67c645c790be0b9` | SUCCESS |
| [#77](https://github.com/LordBiGoDoNE/concurso-simulator/pull/77) | `1d03b0bd9055e7281964787d1c59c520c3a0d370` | SUCCESS |
| [#78](https://github.com/LordBiGoDoNE/concurso-simulator/pull/78) | `a93b76f0b5119ebe4d46450b340d2c43e29c90a5` | SUCCESS |
| [#79](https://github.com/LordBiGoDoNE/concurso-simulator/pull/79) | `e3c267003328dc94d86d5d5f3f4d6d7b13438c81` | SUCCESS |
| [#80](https://github.com/LordBiGoDoNE/concurso-simulator/pull/80) | `904aeed1caf17f1a7625e5479a2ac2c65c35b781` | SUCCESS |
| [#81](https://github.com/LordBiGoDoNE/concurso-simulator/pull/81) | `27f6d7308020d8ecc7d6cc371645dfffe4cd384c` | SUCCESS |
| [#82](https://github.com/LordBiGoDoNE/concurso-simulator/pull/82) | `c09c7a0c15913acda42bc530ff7352eab482531c` | SUCCESS |

Todos os checks desses heads passaram: backend, frontend, study-material,
identity-browser e spec-review-tests. Registro posterior altera apenas os dois arquivos
de evidência excluídos do digest. Conferir novamente os heads/checks antes de qualquer merge.

## Cobertura, verificação e limites do lote 2

Revisor leu diffs completos por PR e combinação: ator/segurança, DTOs/OpenAPI estritos,
projeções ativas, conclusão/resultado/histórico e indisponibilidade; cliente/prazo,
composição/fila/comandos, identidade/logout/descarte tardio; browser/fixtures/operação,
artefatos e documentação. R1–R5 e foco foram revalidados com seus consumidores.
Posse/transições continuam no núcleo, autenticação/CSRF na segurança, correção no
servidor e recuperação de conflitos após rollback. A apresentação inicial é código
genérico, sem payload editorial; contrato corretivo continua no módulo tardio.

Coordenador/CI: **149 backend, 107 frontend, 14 browser reais, 54 Python/Linux,
6 browser offline**, builds/artefatos/links/OpenSpec PASS. Operação adicional local
com JAR anterior sobre V5 e retorno à versão atual PASS. Matriz e limites por tarefa
em [verification.md](verification.md). Backend/Python/rollback/offline locais não foram
repetidos por correções somente frontend; checks atuais verificaram a combinação.

Revisor somente leitura: não executou testes, build, browser, rede ou DB nem conferiu
CI, screenshots ou binário histórico. Resultados acima não são execuções do revisor.
Leu verification.md após formar achados independentes para revisão documental do #81.
Regressão de renderer depende do padrão de URL AttemptResult; testes/sentinelas não
demonstram toda falha de rede, ausência de todo segredo ou toda decisão semântica.
Dados privados de medição permanecem fora do Git; disponibilidade/limites são
apresentados ao usuário separadamente. Sem comparação de consumo com outro modelo.

## Integração ainda exige aprovação

Solicitar aprovação explícita de **#72, #73, #74, #75, #77, #78, #79, #80, #81 e #82**.
Topo não aprova inferiores; sem auto-merge/fila/bypass/sync pós-merge presumido.
O status principal permanece pending, com modelo/sessão/base/head/digest finais nulos.
Gate cumulativo final, integração na épica geral/main, deploy e arquivamento separados.

## Histórico — lote 1, grupos 1 e 2

Registro abaixo preserva a revisão anterior à integração autorizada dos #66–#70.
Os estados de pendência daquele momento não substituem a situação atual acima.


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
