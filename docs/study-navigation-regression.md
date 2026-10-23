# Regressões da nova navegação

Os checks iniciais detectaram dois testes de navegador ainda acoplados à página única: ordem inicial de Tab e presença do checkbox da composição durante consulta pública. Os testes agora percorrem o atalho ao conteúdo e a área correspondente, preservando as verificações de teclado, ausência de dados corretivos e checkbox de variações desligado. A consulta continua montada ao alternar de área.

Uma falha intermitente de foco na paginação expôs uma janela entre publicar o resultado no DOM e executar o efeito de foco. O foco solicitado explicitamente na consulta/revisão passa a ser aplicado no layout da mesma atualização, antes da pintura. Atualizações automáticas continuam sem intenção de foco, protegendo o resultado de conclusão. Os testes mantêm as mesmas expectativas de destino e as regressões da revisão continuam cobrindo atualização automática.

Verificação da camada corrigida: 166 unitários e cinco testes Chromium de entrada/login passaram. Build/integração e checks dos novos SHAs são conferidos no lote completo. Não se reutiliza CI de commits anteriores nem se afirma que esses testes cobrem toda acessibilidade.

LAYOUT-SOL-002: a realização agora propaga seu estado ao shell. Durante o treino, o cabeçalho é compacto e as demais áreas ficam no menu acessível “Ir para outra área”; orientações da tentativa ficam em expansão. A mudança é de apresentação e preserva a montagem, sessão, fila e comandos. Conclusão ou saída do contexto restaura o shell geral. A regressão real de teclado confere abrir/fechar o menu em desktop/celular.
