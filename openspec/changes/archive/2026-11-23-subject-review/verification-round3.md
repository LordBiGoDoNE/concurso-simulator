# Verificação cumulativa do coordenador — lote3, grupos5/6

Usuário escolheu explicitamente os grupos completos5 e6 em2026-10-16. Base b2355f8
integrou grupos1–4 (16/24); nova stack parte de epic/subject-review, sem reaproveitar
ou sincronizar stacks110/115. Implementação/verificação e integração/aprovação são
estados distintos. Este registro não atesta revisão independente nem merge.

## Matriz requisito → código/teste

| Requisito/tarefas | Implementação | Evidência executável |
|---|---|---|
|1.1–1.4: evidência/última base/captura/51 assuntos/prioridade e posse|Attempt,CompletedAttempt,ReviewSummary,ReviewUseCases|CompletedAttemptTests,ReviewSummaryTests,ReviewUseCasesTests: Java puro, UUID sem sinal, fração exata, arredondamento, branco/null e falha integral|
|2.1–2.2: conta/cargo/limite e integridade|JpaCompletedAttempts,AttemptSnapshotReader|CompletedAttemptPersistenceTests: filtros antes de101 cabeçalhos,100×100/quatroSELECTs, 101º não decodificado, nenhuma escrita, falha integral|
|2.3–2.4: isolamento/índice/compatibilidade|V10 aditiva, codecs de captura existentes|CompletedEvidenceConsistencyTests: REPEATABLE_READ/read-only, concorrência, plano2.400 Sort→Index Scan; V5AttemptCompatibilityTests;SubjectReviewCaptureReadTests: gerada/fixa/reclassificada/retirada/restart sem regravar|
|3.1–3.4: HTTP e privacidade|ReviewController/Contracts/Advice,Security/rotas técnicas|ReviewHttpTests: cadeia real,401/403/404/409/500/503,no-store,CORS,UUID/parâmetros estritos,corpos mínimos,GET sem escrita,expiração/logout/conclusão/nova sessão|
|4.1–4.4: painel/individual/ciclo/acessibilidade|SubjectReviewPanel/Summary/Individual,useSubjectReview/API,App|SubjectReview*.test,subject-review-api.test:51 contratos fechados,erro/retry,foco explícito,limpeza/contextos/respostas tardias;browser real390/1280|
|5.1: aulas públicas/encoding|lesson-paths.json,lesson-links.ts|lesson-links.test: igualdade de51 conteudo,existênciaHTML,subpath,acentos/espaços,traversal/esquemas/chaves inválidas/gabarito;browser abreHTML real em nova aba|
|5.2: composição explícita|Workspace/Composer,directed-practice.ts|DirectedPractice/Composer/SubjectReviewContext: cargo/disciplina/assunto,1,demais0,variaçõesfalse,semPOST,alterações,foco,catálogo inválido/tardio e limpeza por cargo/conta/logout/expiração|
|5.3: realização/comandos/insuficiência|UseAttemptRun e POST existentes;nova seleção no workspace|Cancelamento mantém resposta local e commandId;retry idêntico,edição muda operação;browser real perda de confirmação,CSRF403,duas abas/recarga,retirada409/zero sem fallback/parcial|
|5.4: fluxo por ator/última base|Aula→compositor→resultado/revisão|subject-review.real: conta390/1280,visitante sem histórico,outra sessão da conta,mesma base muda branco→acerto com bases1/exposições2 e resultado reproduzível|
|6.1: regressões/fronteiras|Módulos existentes e nova integração|Build backend/ArchUnit/PG/HTTP,frontend/build,browser identidade/parametrizados/público/offline;resultados abaixo|
|6.2: operação/recuperação|SubjectReviewOperationalFixture/taskGradle deliberada|V9→V10 com V5/formato2,dois processos do JAR atual,geraçãofalse,cincoGETs idênticos e12 tabelas de negócio inalteradas|
|6.3: exportação/artefatos/acervo|Builds em exportação Git limpa e auditorias existentes|JAR/SPA semfixtures/.env/metadados numéricos privados/solver;material108HTML/1.757links;offline9 módulos/90resoluções;mapa51;auditoria111(64/20/27),sem novo ato editorial|
|6.4: revisão/integração/gate|Relatório de lote e gate cumulativo separados|Revisão independente do lote ainda pendente;gate novo somente após integrar tarefas,com próprio [SPEC] para épica geral|

## Resultados conferidos

- Frontend163 testes/17 arquivos passaram; TypeScript/Vite passou.
- Browser real completo24/24 passou1,5m;10 de revisão/prática passaram após ajuste
  visual. Conta em390/1280 abre aula/preenche/cria/conclui/revisa;outra sessão mantém
  resultado. Visitante fica individual. Cinco casos novos de produto,sem mocks da API.
- Material servido no browser pelo HTML/assets públicos reais com hosting substituído,
  sem depender do CDN externo. Servidor de API/OIDC/PG18.6 reais e descartáveis.
- Browser público/offline6/6 passou9,6s;9 módulos/90 resoluções semJS.
- Links estáticos108HTML/1.757 referências locais passaram;arquivos originais preservados.
- Auditoria das definições/previews/proveniências/digests versionados:111/111,64 direta,
  20 porcentagem,27 juros. Não é nova consulta de elegibilidade em DB nem ato editorial.
- Ensaio de processoJAR passou26s,V9→V10,dois processos,12 tabelas inalteradas e geraçãofalse.
- Backend build passou3m13:243 casos,zero falhas/erros,um symlink omitido no Windows.
  Inclui planos2.400 tentativas/Sort→IndexScan,100×100/quatroSELECTs,integridade e cadeiaHTTP.
- Browser parametrizado5/5 passou49,8s;inclui expiração e novas sessões/restart.
- Exportação Git limpa c3f74961db7f57460b11fb6993588da466655016: npmci offline,
  TypeScript/Vite e bootJar offline passaram.263 classes de produção/quatro arquivosSPA
  inspecionados por check_attempt_artifacts.py,sem fixtures/.env/solver/metadados privados.
  JAR SHA256715636d21f0c2db4d1205efbc5c5f1e3d627bb4fe5c1948a934aed762572c9ad,
  idêntico ao usado no ensaio de processo. Atualização posterior deste registro não muda
  código de produção,dependências ou configurações de build.
- OpenSpec6/6 strict e git diff --check passaram antes do fechamento do lote.

Quatro capturas sintéticas de tabela/ações390/1280 foram inspecionadas localmente.
ColunaEstudar tem largura mínima,botão legível e rolagem contida;sem overflow da página.
Não há auditoria integral WCAG/leitor de tela,prova de todo scheduling ouSLA.
Testes não substituem análise semântica;revisor somente lê,não executa os comandos.

## Estado e dependências do gate

Grupo5 implementado/verificado;6.1–6.3 preparados neste lote e conferidos antes da
revisão. A tarefa6.4 exige integração das tarefas anteriores **e depois** contexto novo
SOL6.1 xhigh do diff completo. Sem autorização dos merges ainda não existe esse estado;
não marcar6.4 concluída nem preencher spec-review como completed com parecer parcial.
A pendência é a dependência explícita do AGENTS.md e de tasks6.4, não outro escopo.
Depois de aprovação/integração das tarefas,continuar o gate já autorizado deste grupo;
merge do futuro [SPEC] exige aprovação própria. Sem main/deploy/arquivamento.

## Preparação da revisão

Risco crítico: preservação de contexto/privacidade,CSRF/idempotência de criação,
concorrência/abort e compatibilidade operacional. A revisão SOL6.1 xhigh deve cobrir
cada camada e combinação,sem outro modelo. Camadas:lesson-links→directed-practice→
guided-browser→operational-regression, trunk epic/subject-review em b2355f8.
Nenhum lockfile/artefato gerado alterado;mapaJSON público controlado contém51 pares.

Push/submit foi recusado pela revisão automática de permissões por ausência alegada
de autorização explícita para publicar branches/PRs. Nenhuma parte do comando foi
executada; quatro camadas locais aguardam autorização específica para publicação.
PRs/CI remotos e atualização da issue serão registrados depois,sem inventar números/checks.
Isso não impede revisão independente do conjunto local nem altera o requisito de merge.
