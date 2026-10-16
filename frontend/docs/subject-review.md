# Revisão por assunto — interface

Painel somente no contexto conectado, com cargo escolhido explicitamente. Sem escolha,
não há consulta privada; a opção não cria simulados. Os51 assuntos aparecem com contagens,
percentual ausente sem dados, amostra pequena, prioridades e janela/corte/datas explícitos.
Nenhuma nota é calculada no cliente; ele valida o contrato e apresenta métricas do servidor.
A prioridade vem ordenada do servidor; heurística não é selo de domínio nem previsão.

Resultado concluído de visitante/conta carrega apenas a revisão daquela tentativa,
sem carregar painel/histórico implicitamente. Tentativa em andamento não monta a consulta.
Visitante continua temporário e não é adotado pela conta. O painel da conta atualiza após
conclusão confirmada; atualização explícita mantém o cargo e distingue erro de ausência.

DTOs têm chaves exatas,51 IDs públicos distintos, métricas/contagens limitadas, datas,
contexto/cargo/ID e corte consistentes. Campo extra/captura/owner em qualquer nível,
JSON parcial, enum ou contexto incorretos não entram na interface. Requisição usa GET,
credenciais e no-store, sem tokenCSRF artificial; erro401/403/404 não espera corpo.
Timeout finito5s cobre fetch e JSON, mesmo quando o transporte ignora abort.

useSubjectReview limpa o estado e aborta ao trocar cargo, verificar sessão, desmontar
ou atualizar. Cada geração confere sua vigência antes de apresentar resposta. App
suspende somente a revisão durante verificação de sessão, preservando respostas locais
do simulado; logout/troca de conta desmonta o contexto privado. Não há storage, cache
persistente ou dados de desempenho em URL. O cliente não promete revogar resposta já
em voo no servidor; cancela e ignora seu resultado ao mudar o contexto.

131 testes frontend passaram: contratos negativos, erro sem corpo, timeout, cancelamento,
cargo/conta/logout/resposta tardia, retry e conclusão confirmada para visitante/conta,
além das regressões existentes. Acessibilidade usa tabela com caption/cabeçalhos,
estados em texto, seletor rotulado, botões por teclado e foco no painel após ação
explícita. Atualização automática não desloca foco do resultado. Browser real390/1280px
e inspeção dos artefatos pertencem à camada final do mesmo lote; ainda pendentes aqui.
Links de aula/prática dirigida são grupo5, fora deste lote. Sem deploy/main.
