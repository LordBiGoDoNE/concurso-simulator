# Revisão independente — subject-review, grupos 1 e 2

Revisão inicial concluída, **sem achados confirmados**: zero bloqueadores, recomendados
ou opcionais. Escopo completo 1.1–1.4 e 2.1–2.4; 8/24 tarefas implementadas/verificadas,
0/24 integradas. Proposta #105 aprovada, issue #104. Risco **crítico** por persistência,
migração V10, transações/concorrência, posse e integridade de snapshots/capturas.

Modelo `openai/gpt-6.1-sol#xhigh`, sessão independente `/root/subject_round1_sol`.
O harness disponibilizou e aceitou `model=gpt-6.1-sol`, `reasoning_effort=xhigh`,
`fork_turns=none`, sem erro/fallback; perfil versionado coincide. Não expôs ID adicional
do provedor. Seleção observável registrada sem alegar telemetria inexistente.

## Composição e identidade

Stack #110; trunk `epic/subject-review`, base `0bd66526eb93fd8bc51efbea50da4eba4061df09`.
Quatro PRs em rascunho, dependentes na ordem abaixo. Cada diff e combinação foram lidos
e tiveram seus digests recalculados pelo revisor sem executar scripts do projeto.

| PR | Base | Head de fonte revisado | SHA-256 |
|---|---|---|---|
| #106 | `0bd66526eb93fd8bc51efbea50da4eba4061df09` | `3c5e366c72b817e14a92e0af15596849c7da5d84` | `94af173bed3a0f7abf675caef0a58e859c317826860378d9d7703a7e36ddb105` |
| #107 | `3c5e366c72b817e14a92e0af15596849c7da5d84` | `906d54a17bb583faca980be65c5c2b2f2d06091b` | `c5e41b08620546dae75a1fc6ad007b4ce8b68eb54e0bb6b9a5a064d5fe319ab4` |
| #108 | `906d54a17bb583faca980be65c5c2b2f2d06091b` | `8944e3e959d842b6911be88521b7623ef02267c7` | `44ee6a7737fd7a7ac5217b774dff11d13a07e908d9afebe2056c8344a0be31e4` |
| #109 | `8944e3e959d842b6911be88521b7623ef02267c7` | `d7e056f680b9f04cc0d790541e670aa084b250ab` | `f5f6f55e9e7fcb6619d4080448c233d969802de95f67a3115de05ca1a19152c2` |

Combinado `0bd66526eb93fd8bc51efbea50da4eba4061df09` → `d7e056f680b9f04cc0d790541e670aa084b250ab`:
`1cdbfbb3a7f325e7f5409eb8b361497d12ba34654127a4019d894b118de0b094`. Somente spec-review.json/md excluídos
do digest. Estes dois arquivos reviews/round1 registram o parecer posteriormente ao
source head; não alegam revisão de si próprios nem incluem mudanças de produto.

## Cobertura

- **requirements:** Quatro diffs e combinação; 25 arquivos permitidos lidos. 51 assuntos, última base antes do agrupamento, classificação histórica, exposições, HALF_UP, amostra mínima e fração exata; UUID unsigned compatível com PostgreSQL.

- **architecture:** Núcleo Java puro, desfecho no agregado Attempt, contrato CompletedAttemptReader consumido diretamente e wiring/transação na infraestrutura; sem dependência reversa ou camada artificial.

- **integration:** Projeção comum para painel/individual e leitor compartilhado por JpaAttemptStore; codecs V5/formato 2, cabeçalhos, itens, posições, identidade e captura conferidos.

- **security:** Painel só conta; individual por ator/vigência e regras do agregado. Projeção mínima sem conteúdo corretivo/params/digests; owner interno não é contrato público.

- **persistence:** REQUIRES_NEW, REPEATABLE_READ, read-only/timeout; filtros antes do limite, 101 cabeçalhos/100 payloads/10.000 itens; consultas em lote e índice parcial V10 aditivo.

- **concurrency:** Inspeção de teste de conclusão concorrente após cabeçalhos, SHOW de isolamento/read-only, ordem PostgreSQL e snapshot coerente.

- **compatibility:** Fixture V5 real, capturas fixas/geradas, retirada/reclassificação, geração desligada, fábrica JPA e contextos Spring reabertos; nenhuma regravação ou progresso derivado.

- **maintenance:** Documentação da política/limites, distinção entre implementação e integração e reutilização dos critérios de restauração. Grupos futuros não exigidos antecipadamente.

- **tests:** Inspeção de testes puros/portas/ArchUnit/PG/índice/volume/adulteração/compatibilidade; revisor não executou testes.

## Verificação do coordenador

- gradlew --offline build, Windows: Sucesso em 2m54; 233 casos, 232 executados sem falhas/erros, um symlink omitido por permissão. Expectativas de migração atualizadas de V9 para V10 sem relaxar assertions de dados/JPA. Responsável: coordinator.

- Application checks Linux, source d7e056f, run38926586822/job117059565965: 233 PASSED, zero SKIPPED/falhas; BUILD SUCCESSFUL in 2m59. Responsável: github-actions-verified-by-coordinator.

- Testes direcionados domínio/UseCases/ArchUnit e PostgreSQL 18.6 isolado: Passaram políticas, 100×100/quatro SELECTs sem escrita, cabeçalho 101, integridade, concorrência, V5/formato 2, índice, retirada/reclassificação, geração desligada e contextos reiniciados. Responsável: coordinator.

- OpenSpec1.14.1 offline validate --all --strict e git diff --check: 6/6 changes válidas e whitespace limpo. Responsável: coordinator.

- Recalcular diretamente bytes dos quatro diffs e combinação com argumentos canônicos de snapshot: Todos os cinco digests e commits confirmados. Nenhum script do projeto executado pelo revisor. Responsável: reviewer.

Todos os checks dos quatro heads de fonte passaram: backend/frontend/study-material/
identity-browser/spec-review-tests. Os links constam no JSON e nos PRs/issue. A primeira
execução de frontend do #108 falhou no foco da paginação (QuestionBrowser.test.tsx:79);
outra execução do mesmo source já passava. A tentativa 2 do run38926581220, job117062648674,
reexecutou apenas o job falho e passou em npm test/build/e2e, sem mudança/skip/relaxamento.
Intermitência permanece como limite de teste existente; causa temporal é hipótese, não
defeito corrigido. Esse estado não é achado semântico do revisor.

## Triagem e limites

Nenhum achado aberto; não houve correção nem revalidação de código após a revisão inicial.

- Revisão parcial dos grupos completos 1/2, não gate cumulativo final nem aprovação de merge. Grupos 3–6 fora do lote.

- Revisor leu somente código/testes/requisitos/docs permitidos; não leu semanticamente spec-review.json/md, verification.md ou relatórios históricos. verification.md integra o digest criptográfico.

- Revisor não executou testes/build/aplicação/scripts do projeto/rede/DB/escritas/delegação nem conferiu CI/regras/plano bruto; resultados de execução abaixo são do coordenador/CI.

- Restart demonstrado por contextos Spring/fábrica JPA no mesmo JVM, não por processos/JAR separados; não se iniciou JAR V5 após formato 2.

- Não há ensaio específico de limpeza concorrente de visitante no novo leitor; vigência/visão transacional inspecionadas sem defeito concreto identificado.

- Volume sintético/planos locais não provam latência no banco operacional. Nenhum banco editorial aprovado do operador foi usado.

- Contexto novo reduz viés sem garantir ausência de viés. Sem Graphify ou comparação com outro modelo.

- Cota, tokens e observações operacionais privadas não são publicados nestes relatórios/issues/PRs; indisponibilidade não é consumo zero.

- Os dois registros reviews/round1 são posteriores ao source head revisado e não se declaram incluídos no digest. Nenhuma mudança de código/requisito acompanha o registro.

Merges de #106, #107, #108 e #109 exigem aprovação explícita individual no conjunto
e conferência de refs/checks atuais. Não foi executado merge/sync/retargeting, nem
main/deploy/arquivamento. Revisão do lote não conclui o gate cumulativo final da spec.
