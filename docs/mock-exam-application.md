# Aplicação de tentativas

`AttemptUseCases` coordena criar, responder, concluir, ler resultado e histórico.
`AttemptStore` é a unidade transacional: a infraestrutura retorna somente depois do
commit ou rollback. O núcleo recebe Clock, gerador de IDs e sorteio substituíveis.
Nenhum contrato depende de sessão, HTTP, JPA ou Spring.

Posse e expiração são verificadas antes de consultar comandos de uma tentativa.
Comandos são identificados por owner (tipo + UUID) e operationId. SHA-256 do payload
canônico inclui ação, tentativa, versão esperada, item e alternativa; a composição
ordena disciplinas/assuntos. A mesma chave com payload diferente é conflito. A chave
de criação não depende de um ID de tentativa ainda inexistente.

O registro guarda o snapshot do resultado confirmado, permitindo repetir respostas
antigas sem aplicar novamente nem devolver o estado de uma operação posterior.
Conclusão é irreversível; seu resultado deriva do snapshot fechado. Conflitos conhecidos
são recuperados somente após rollback e apenas se o comando vencedor estiver gravado.
Outras falhas propagam sem sucesso fictício. A camada de persistência deve proteger
versão e unicidade; o contrato puro sozinho não garante concorrência entre processos.

Histórico aceita apenas conta, page 0–100000, size 1–100, filtro opcional de conclusão,
ordem createdAt/id decrescente. Só tentativas concluídas incluem percentual. Offset
não promete estabilidade se novas tentativas forem inseridas entre páginas.
`AttemptUseCasesTests` usa uma porta substituta com commit/rollback para testar retries,
posse, expiração, insuficiência, falhas e projeções. PostgreSQL e corridas reais pertencem
à camada seguinte; autenticação técnica e contratos HTTP ficam no grupo 3.
