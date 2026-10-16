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
