# Evidência e políticas de revisão por assunto

`subject-review/v1` usa no máximo 100 conclusões da conta/cargo, ordenadas por
finishedAt/UUID decrescentes. Comparação de UUID é por bytes sem sinal como no
PostgreSQL, inclusive na fronteira 7fff/8000; não usar UUID.compareTo.

`Attempt` é dono do desfecho correct/wrong/blank. Resultado e projeção mínima
`CompletedAttempt` compartilham esse cálculo; projeção não carrega enunciado,
alternativas, resolução, parâmetros ou digests. Posse/conclusão/expiração antecedem
a evidência. A projeção ainda é interna e contém proprietário; futuros DTOs HTTP
devem selecionar somente métricas, nunca serializá-la como contrato público.

O cálculo Java puro retém a última observação por questionId antes de agrupar pela
classificação capturada. Exposições brutas usam cada classificação original; não
entram no denominador. Todos os 51 assuntos aparecem; percentual/data são null sem
bases retidas. Percentual tem duas casas HALF_UP. Prioridade exige três bases e
erro/branco, ordena pela fração exata, depois quantidade de erros/brancos e topicId.
Essas heurísticas não afirmam domínio ou novidade. Nenhum acesso ao banco atual,
geração, framework, persistência derivada ou interface faz parte do cálculo.

`CompletedAttemptReader` é o contrato de aplicação de mockexam. ReviewUseCases o
consome diretamente, sem interface/Service intermediário redundante. Leitura e
validação pertencem ao produtor; o consumidor ainda recusa mistura de proprietário,
cargo, IDs, ordem e hasOlder sem janela cheia. Falha não vira resumo parcial/vazio.
Painel só aceita conta; revisão individual aceita o proprietário visitante ou conta.
O agregado Attempt protege conclusão/posse/expiração antes de produzir evidência.
O UseCase recebe Clock e a porta, testáveis sem Spring. Não há endpoint neste lote.

O produtor JPA usa transação própria read-only/REPEATABLE_READ com timeout 15s.
Consulta primeiro até 101 UUIDs de conclusões por conta/cargo, retém até 100 e só então
carrega payloads, itens e instâncias em lote: quatro SELECTs para formato 2, independentemente
de 1 ou 10.000 itens. O cabeçalho extra nunca é decodificado. Revisão individual usa
os mesmos codecs/vínculos, com consulta limitada ao ator e vigência, seguida da regra
do agregado. AttemptStore também usa o leitor comum; não mantém validação paralela.
Nenhum JSON/hash, progresso, decisão editorial ou comando é gravado pela consulta.

PostgreSQL 18.6 isolado: 100 tentativas/10.000 itens em quatro SELECTs, nenhuma escrita;
101º cabeçalho adulterado não é decodificado, corrupção dentro da janela falha
integralmente. Regressões de concorrência/idempotência e fixture V5 real passaram
com o leitor comum. Planos, índice e revalidação de capturas também passaram nesta rodada.

## Consistência, índice e recuperação

V10 cria somente me_completed_subject_window_idx: proprietário, cargo capturado,
finished_at/UUID decrescentes, parcial para contas concluídas. Com 2.400 tentativas
sintéticas, EXPLAIN ANALYZE mostrou Sort antes e Index Scan depois, sem desabilitar
seqscan/forçar planner. A consulta existente por createdAt não atende a ordenação
por conclusão/cargo. Planos brutos ficam ignorados em build/subject-review-plans;
números locais não são promessa de latência. Nenhuma migração V1–V9 foi alterada.

Um ensaio pausa após os cabeçalhos e conclui outra tentativa por comando real em
outra transação: a leitura atual mantém sua janela e a seguinte inclui a conclusão.
SHOW confirma repeatable read e read-only on. Para testar UPDATE, o setup cria a
tentativa pelo UseCase, preservando a serialização de origem protegida pelos triggers;
fixtures de volume congeladas são usadas somente em leituras.

Fixture V5 serializada pelo código antigo, novas fixas e geradas passam pelo mesmo
produtor, sem reescrever payloads/hashes. A base foi reclassificada/retirada e a família
retirada em fixtures sintéticas; a janela usa a última classificação capturada,
mas o resultado individual mantém a anterior. Após migração V9→V10 e reabertura da
EntityManagerFactory, dois ciclos reais de fechamento/abertura do contexto da aplicação
com geração false preservam resumo e correção. São reinícios de contexto no mesmo
JVM, não reinícios de processo. Não foi executado JAR V5 após formato 2.

Recuperação mantém dados/schema/codecs e usa leitor compatível com geração desligada.
O índice é aditivo e não exige perfil/progresso persistente ou backfill. A comparação
integral das tabelas antes/depois verifica que as consultas e a migração não regravam
contas, banco, decisões, snapshots, itens, comandos ou instâncias. Nenhum banco editorial
aprovado do operador foi acessado; todos os ensaios usam PG 18.6 descartável.
