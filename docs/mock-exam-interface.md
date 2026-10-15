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
