# Interface de simulados

Composição aceita 1–100 questões inteiras, cargo e assuntos opcionais por disciplina.
O formulário explica que distribuição/pontuação são de prática, sem formato oficial.
Catálogo usa a consulta pública existente; a nova API usa credentials include e no-store.
Operações têm prazo único de cinco segundos para CSRF, headers e JSON, inclusive quando
o leitor ignora abort. Dados ficam só em memória; nenhum owner é enviado pelo cliente.

Criação impede duplo clique sincronamente e mantém commandId/critérios após timeout ou
falha. Retry recupera a tentativa gravada; mudar critérios cria nova operação. A resposta
só é entregue enquanto o componente estiver ativo. Insuficiência mostra disponíveis por
disciplina e não completa o acervo artificialmente. Formulário usa labels/fieldsets,
quantidades limitadas e assuntos por checkbox; testes verificam validação/retry/abort.

O cliente valida objetos, IDs, referências HTTP(S), alternativas e associação de seleção;
rejeita propriedades extras/corretivas na View e nota parcial no histórico. Resultado tem
contrato separado e só é aceito junto de tentativa concluída no finish. Isso complementa
o servidor; não promete detectar toda dica semântica em texto editorial. Não calcula nota.

A realização navega por IDs/ordens capturados. Fila única envia respostas/limpeza em
ordem, atribuindo expectedVersion somente quando chega sua vez. Escolha local aparece
como pendente; resposta de uma gravação anterior não apaga uma escolha posterior.
Falha mantém commandId/payload para retry e bloqueia o restante da fila. Conflito exige
recarregar por ação explícita, mantendo escolhas não confirmadas em memória. O usuário
confere a versão atual e escolhe salvar essas escolhas com novos comandos/versões.
Nenhum conflito vira sucesso fictício nem descarta a escolha local silenciosamente.
Domínio impede alterações quando a tentativa já está fechada; a UI acompanha esse estado.

Concluir exige confirmação em dialog nativo, com foco inicial na ação de continuar e
Escape/cancelamento. Não há conclusão com fila/escolha pendente. Falha de confirmação
mantém o mesmo comando/versão e bloqueia novas respostas até retry ou recarga; não presume
que timeout significa rollback. Resultado só aparece após resposta fechada do servidor
ou consulta de tentativa já fechada, e nunca é calculado na SPA. Resoluções e fontes
são da versão capturada; tentativas ativas continuam sem esses campos. Testes cobrem
cancelamento/foco, retry da conclusão e retry de resultado sem uma segunda conclusão.
