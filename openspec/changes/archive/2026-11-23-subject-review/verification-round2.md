# Verificação coordenador — lote2, grupos completos3 e4

Escopo escolhido pelo usuário em2026-10-16:3.1–3.4 e4.1–4.4. Base da épica da spec
a16b69acf3ffcb4f82eac87018fd1270fae4aea8, que integrou o lote1 aprovado. Nova stack,
sem sync/rebase/retarget da stack110. Grupos5/6 não autorizados neste lote.
16/24 implementadas/verificadas,8/24 integradas. Revisão inicial executada com dois achados confirmados e corrigidos, revalidação
e merges do lote2 pendentes na preparação deste registro; nenhuma tarefa nova integrada por CI ou commit local.

## Matriz de cobertura executada

| Tarefa | Código e contratos | Evidências do coordenador |
|---|---|---|
|3.1|ReviewController/ReviewContracts/OpenAPI|Dois GETs,51 métricas,window tipada, chaves fechadas, cargo/UUID, extras/repetidos/vazios/limite rejeitados|
|3.2|SubjectReviewRoutes,SecurityConfiguration,AttemptAccess reutilizado|Servidor/segurança reais: conta/visitante/principal inválido,401,POST403,404,409; sem decisão de autenticação no controller;ArchUnit|
|3.3|ReviewFailureAdvice/filtro técnico/DTOs|500 integridade versus503 recurso/transação/sessão; PG parado real; no-store/CORS; negativos de captura/owner; snapshots de tabelas inalterados; variações após retirada|
|3.4|Leitor concluído e segurança|Consulta em voo, conclusão/logout, visão antiga coerente e seguinte atual; nova sessão da conta; expiração visitante com mesmo cookie e prazo24h; resultado anterior preservado|
|4.1|SubjectReviewPanel/Summary/API|Escolha explícita de cargo,51 assuntos,zero/null/poucos dados/prioridade/corte100/datas, retry e erro sem falso vazio|
|4.2|IndividualSubjectReview/AttemptPractice|Só depois da conclusão, conta/visitante, somenteID pedido sem painel/histórico implícito;401/403/404 limpam contexto,409 distinto, retry|
|4.3|useSubjectReview/App/AttemptWorkspace|Troca de cargo/conta,logout/verificação cancela e ignora resposta tardia,sem storage; resposta real retida no browser; atualização após confirmação|
|4.4|Tabela/controles e testes browser reais|Teclado/foco/caption/cabeçalhos,390/1280px,rolagem horizontal contida,6 capturas inspecionadas, falha/retry e aulas preservadas|

## Resultados conferidos

- Backend `gradlew --offline build`: sucesso3m23;243 casos,242 executados,zero falhas/erros,
  um teste de symlink omitido por permissão do Windows. PostgreSQL18.6 isolado.
- ReviewHttpTests ampliado: nove testes passaram22s. Primeira camada junto com ArchUnit
  passou17s. O teste adicional de expiração com o mesmo cookie também passou isolado.
- Frontend antes da revisão:133 testes passaram. Após correções:137 testes passaram,
 15 arquivos, TypeScript/Vite aprovado.
- Novos fluxos reais:4/4 passaram25,9s; inspeção das capturas de painel/individual/tabela
  em390/1280px. Uma primeira rodada encontrou apenas localizador de screenshot ambíguo;
  foi tornado explícito, sem relaxar assertions de produto/teclado/foco.
- Browser identidade/simulados/questões/revisão:18/18 passaram1,4m; parametrizados5/5
  passaram52s. A captura específica de tabela foi acrescida e os4 fluxos repetidos.
- Browser público/material:6/6 passaram8,8s, incluindo nove módulos/90 resoluções
  offline sem JavaScript. Links:108 páginas/1.757 referências locais válidas.
- SPA: sentinelas privadas sintéticas ausentes dos quatro artefatos; JAR inclui a
  web de revisão e exclui fixtures de teste,arquivos.env e seleção editorial.
- OpenSpec1.14.1 offline strict:6/6 válidas; git diff --check limpo.

Logs,XML,planos/capturas e observações operacionais ficam ignorados em backend/build/.
Checks remotos e SHAs/digests/revisão independente serão consolidados nos PRs e issue104.
Nenhum resultado de execução do coordenador se apresenta como execução pelo revisor.

## Limites

Requisição já autorizada pode terminar após logout/expiração; não se promete revogação
retroativa. Novas requisições são barradas,e o cliente cancela/ignora respostas de
contexto anterior. Testes não demonstram todo scheduling possível nem SLA operacional.
Não houve auditoria WCAG integral/leitor de tela. Reinício de contexto nos testes
anteriores não certifica reinício de processo/JAR; estes registros não ampliam tal prova.
Não houve acesso ao PostgreSQL editorial aprovado do operador, decisão editorial,
nova migração ou alteração de material. Não há implementação de aula/prática dirigida
do grupo5 nem gate cumulativo do grupo6. Sem main/deploy/arquivamento.
Cota/tokens privados não são inventados/publicados. Revisão de lote e gate final separados.

## Correções após revisão inicial

SOL6.1 xhigh apontou SR-R2-SOL-001 recomendado (intenção de foco sobrevivendo a falha)
e SR-R2-SOL-002 opcional (coerção de disciplineId aceitando array). Ambos confirmados.
Os três casos reproduzidos falharam antes do ajuste. Regra de foco vinculada à consulta
explícita e tipo textual verificado antes do enum;137 testes unitários passaram após
a correção, incluindo cancelamento, e cinco fluxos reais passaram26,4s com nova regressão
de foco após falha/conclusão. Nenhuma mudança backend; build completo anterior preservado,
checks do novo source e revalidação independente serão conferidos antes da aprovação.

O CI do registro posterior detectou intermitência no teste positivo de foco em
SubjectReview.test.tsx: a presença do texto não aguardava useEffect. Assertion alterada
para waitFor do mesmo título focado; nenhum comportamento de produção ou destino foi
alterado. Esta mudança de teste/documentação exige nova revalidação do snapshot;
a evidência anterior permanece histórico e será consolidada após ela.
