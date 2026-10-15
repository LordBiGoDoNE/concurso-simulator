# Lote 1 — grupos completos 1 e 2

**Revisão independente concluída/revalidada, nenhum achado aberto.** Oito tarefas
implementadas/verificadas; **0/24 integradas/aprovadas**. Risco crítico: geração,
manifesto/elegibilidade, schema/transações, conflitos e captura privada. PRs em rascunho,
sem autorização de merge. Modelo SOL 6.1 xhigh em contexto novo, sessão
`/root/parameterized_round1_sol`; mesma sessão retomada na revalidação.

## Composição e hashes

Stack #91, v0.2.1, trunk `epic/parameterized-questions`.
#88 geração (1.1/1.2) → #89 manifesto (1.3) → #90 editorial (1.4) →
#92 persistência/captura (2.1–2.3) → #93 local (2.4). Dependências reais.

| PR | Base analisada | Fonte analisado | Digest canônico |
| --- | --- | --- | --- |
| #88 | `05b9cb956c78105793d2956c90df81b34d69f4fd` | `a110c2b42d2ba1623991fc9c91e06df648246426` | `98a41593f30910fae2f6540fad30d6572480022dbfcef27c891698cfc2273192` |
| #89 | `a110c2b42d2ba1623991fc9c91e06df648246426` | `40c701aea401db3e32a6e9b332e1cf07c771a107` | `aa633f1cca59f61696be723884148c017c7c18afc45442e303a7f65b4abfcc7b` |
| #90 | `40c701aea401db3e32a6e9b332e1cf07c771a107` | `261ac824ab945bacaedfa4ad7cec7b7e0c10c6d2` | `2739f54c3a58ecf26ac21c4e1295dce149f451cc785469f05060a54a3b167dce` |
| #92 | `261ac824ab945bacaedfa4ad7cec7b7e0c10c6d2` | `89206044b6656041643e5d3e223f57839ec4fa16` | `118edd284088f0848f52b736924b7a5f2aa7897c82ab684a6fb2be21c4941141` |
| #93 | `89206044b6656041643e5d3e223f57839ec4fa16` | `14081b9630488f159ba19b45d70b5c2c7e17853e` | `d6c7693ceef941adc92be36546dc3245dcc80f705334cc12a88f0a2b7e80e507` |

Combinado final: `05b9cb956c78105793d2956c90df81b34d69f4fd` →
`14081b9630488f159ba19b45d70b5c2c7e17853e`, digest
`0b306fd518bec56e3d83c53b22ccaf8556f49a59b12f5ccad0ecea3aa263010a`.
Inicial no topo `ea851aa7273906e4f99de91413a01af94dd4e153`, digest
`90ca1822c63b0f0a4f5cd700bb51852381af0eb81050795f88a43a645f56c697`.

Digests excluem somente `spec-review.json/md`; estes registros do lote são posteriores
ao head de fontes analisado e não se declaram parte desse digest. Alteração posterior
de código/requisitos/docs relevantes exige revalidação, não atualização cosmética de hash.

## Achados e triagem

Inicial: nenhum bloqueador e **P5-R1-R1 recomendado**, confirmado. Defaults do mapper
local Jackson permitiam versões textuais, float truncado para inteiro e ordinais de
ação/checklist. O mapper próprio não herdava regras da configuração web. Confirmação
e controle de CI permaneciam presentes; não foi encontrado bypass dessas proteções.

**Fixed em 14081b9**: tipos numéricos e enums exatos, sem scalar coercion/float→int,
ordinais/nulos primitivos ou número/booleano→texto. Regressão invoca o launcher com
ambiente vazio e exige JacksonException, de modo que erro posterior de conexão não
satisfaz o teste. Documentação registra causa, regra positiva e limites. Revalidação
reexaminou correção e novo conjunto completo: nenhum novo achado.

## Cobertura e verificação

Revisor leu todos os cinco diffs/cumulativo (38 arquivos relevantes, 1.570 inserções e
17 remoções), requisitos/ADRs/padrões, domínio, oráculo/manifesto, editorial, JPA/V6,
captura e launcher/testes/docs. As sete áreas estão detalhadas em `review.json`.
Não exigiu antecipar grupos 3–6 ou aprovar conteúdo real. Não executou testes/build/
aplicação/rede/DB nem escreveu/delegou; somente leitura e hashes diretos dos diffs.

Coordenador: build Windows PASS antes da correção, 177 casos (176 executados, um
symlink omitido por permissão); regressão local posterior do parser PASS (7 executados,
mesma omissão). PostgreSQL18.6 isolado. OpenSpec offline estrito 5/5 e diffcheck PASS.
CI Linux no source head 14081b9: **178 PASSED, zero SKIPPED**, inclusive symlink e
parsing estrito, conferido no [job backend](https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/38791654981/job/116617464041).
Todos os checks dos cinco source heads estavam verdes; head posterior só registra esta
evidência e terá seus próprios checks conferidos. `spec-review-tests` não é gate final.

## Limites e próxima decisão

Modelo selecionado explicitamente `gpt-6.1-sol`, reasoning_effort=xhigh; sem introspecção
adicional do provedor. Contexto inicialmente novo e sem conversas, revisões anteriores,
outros agentes ou Graphify; isso reduz viés, sem garantia de ausência. Cotas/tokens e
observações privadas são apresentados ao usuário fora do Git, conforme protocolo.

Sem famílias reais aprovadas, integração de tentativa/interface, main, deployment ou
arquivamento. Grupos 3–6 pendentes/não autorizados. Gate cumulativo permanece pending.
Aprovação futura deve listar **#88, #89, #90, #92 e #93**, com heads/checks atuais; topo
não aprova inferiores. Primeiro merge/sync pós-merge da stack não ensaiado, sem
retargeting manual/fila/auto-merge/bypass ou deleção de branches presumidos.
