# Verificação — lote 1, grupos completos 1 e 2

Proposta #105 aprovada/integrada na épica da spec em 0bd6652. Primeiro lote escolhido
explicitamente pelo usuário: 1.1–1.4 e 2.1–2.4. Grupo implementado/verificado não
significa revisão ou integração aprovada. Grupos 3–6 fora do escopo; gate cumulativo
final ainda pendente. Nenhum endpoint, interface ou dado editorial real alterado.

## Matriz requisito/tarefa → evidência

| Tarefa | Código | Testes efetivamente executados pelo coordenador |
|---|---|---|
|1.1|CompletedAttempt, Attempt.evidence|CompletedAttemptTests: V5/fixa/gerada, dono/conclusão/expiração, mesmo desfecho do resultado|
|1.2|ReviewSummary|51 assuntos, soma/null, brancos, HALF_UP e exposições fora do denominador|
|1.3|ReviewSummary.priorities|Mínimo 3, todos certos, fração exata com percentuais arredondados iguais, desempates|
|1.4|ReviewUseCases, CompletedAttemptReader|Porta substituível, conta/cargo/ID/ordem/hasOlder, visitante individual, falhas sem falso vazio|
|2.1|JpaCompletedAttempts|Filtro antes de 100, cabeçalho 101 inválido não materializado, outras contas/cargos/abertas excluídas|
|2.2|AttemptSnapshotReader e reutilização por AttemptStore|100×100 itens, quatro SELECTs, vínculos/capturas e corrupção integral, nenhuma escrita|
|2.3|Transação read-only/RR, índice V10|Conclusão concorrente, SHOW de isolamento/read-only, planos antes/depois sem forçar planner, upgrade sem regravação|
|2.4|Codecs e leitura compatível|Fixture V5 real, fixa/gerada, retirada/reclassificação, reabertura da aplicação duas vezes com geração desligada e tabelas preservadas|

## Resultados

Testes direcionados de domínio, UseCases e ArchUnit passaram. PostgreSQL 18.6 isolado:
leitura básica/volume e regressões de persistência/idempotência/concorrência/V5 passaram;
consistência/índice e capturas passaram. Um setup concorrente foi corrigido para criar
a tentativa pelo UseCase real, conservando a serialização protegida por triggers;
nenhum trigger de produto foi relaxado. Adulterações usam somente fixtures descartáveis.

Build backend completo (`gradlew --offline build`): sucesso em 2m54, 233 casos,
232 executados sem falhas/erros e um teste de symlink omitido por permissão no Windows.
A primeira execução detectou somente a expectativa antiga da contagem de migrações;
o teste de schema foi atualizado para V10, preservando as verificações de dados/JPA
e acrescentando a existência do índice nas instalações nova e atualizada.

OpenSpec 1.14.1 offline strict: seis changes válidas; git diff --check sem erros.
Checks remotos serão consolidados antes da aprovação dos PRs.
Revisor independente não executa testes/build/rede/DB; esta matriz é do coordenador.

## Limites e operação

Planos de 2.400 tentativas sintéticas: Sort substituído por Index Scan após V10.
As medidas locais não são SLA nem prova de desempenho para todo volume/conteúdo.
Payloads/comandos/hash e registros de negócio preservados; só índice é adicionado.
Leitura não consulta banco atual nem gerador. O produtor ainda retorna owner interno;
nenhum endpoint serializa essa projeção. Privacidade HTTP e interface pertencem aos
grupos seguintes e não são certificados por este lote.

Reinícios do contexto completo Spring e fábrica JPA no mesmo JVM, não processos
separados. Nenhum JAR V5 foi iniciado após novas escritas. Recuperação conserva
schema/dados e leitores compatíveis; geração pode ficar desligada. Não há progresso
persistente derivado, eventos editoriais ou backfill. Sem main/deploy/arquivamento.

Cota/tokens e observações mínimas ficam fora do Git; indisponibilidade não é zero.
Revisão do lote e gate cumulativo final continuam separados.
