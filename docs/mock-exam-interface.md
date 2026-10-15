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

## Conta, visitante e mudança de contexto

Histórico da conta tem páginas de 20 registros, estado/datas/cargo/quantidade e percentual
apenas ao concluir. Abrir usa o ID persistido e nunca sorteia novamente; atualizar retoma
somente respostas confirmadas. Outra tentativa/nova composição exige confirmação de
troca para evitar descarte acidental de escolhas locais. A página cheia permite avançar;
uma última página cheia pode ser seguida de página vazia, sem total inventado.

LoginPanel publica a identidade interna verificada ao App, que mantém a workspace por
ID da conta (ou contexto visitante). Durante revalidação da identidade, o conteúdo fica
oculto até conferência; identidade diferente desmonta/aborta o contexto anterior. Logout
e início de login limpam imediatamente. Rejeições 401/403/404 limpam conteúdo e pedem
nova verificação; não mantêm resposta tardia ou supõem posse. Falha de logout não anuncia
saída: o conteúdo privado já foi limpo e o usuário pode verificar novamente a sessão.

Visitante vê validade de até 24h dependente da sessão, sem histórico/mescla. Login com
tentativa ativa exige confirmação de que ela não será transferida. Voltar como visitante
após logout não restaura a sessão antiga. Conteúdo, respostas e resultados permanecem só
em memória; sem localStorage/sessionStorage, cache público, service worker ou respostas
na URL. Aulas e consulta pública continuam independentes da disponibilidade privada.
Testes exercitam páginas/estado, logout/troca de conta e escrita tardia ignorada; a
identidade só muda após resposta verificada, sem alegar detecção de cookie de outro
contexto antes de qualquer evento/requisição. Browser real/OIDC/PG verifica a combinação.

Ao revalidar a identidade com tentativa ativa, a workspace também confere seu acesso por GET antes de voltar a exibi-la, preservando escolhas em memória enquanto a identidade é a mesma. Isso detecta expiração/troca de sessão visitante, que não pode ser distinguida apenas pelo 401 de /me. Falha mantém conteúdo oculto até retry; 401/403/404 desmontam o contexto.

Retorno à aba visível e pageshow de documento restaurado também revalidam a identidade;
a proteção não depende apenas de focus. O teste de restauração exige que a conta nova
não receba conteúdo da workspace anterior.

## Verificação de assets e módulos corretivos

A entrada inicial da SPA contém a apresentação genérica, sem dados corretivos. O
validador do contrato de resultado é importado dinamicamente após a resposta de
conclusão ou ao consultar tentativa fechada. Todos os chunks continuam sem payloads,
resoluções, credenciais ou seeds de fixtures. O campo de contrato correctAlternativeId
é usado apenas nos módulos corretivos; seu nome no código genérico não é um gabarito.
O teste anterior procurava esse nome em todo JS, antes de existir uma tela de resultado.
A regressão agora verifica ausência no entry inicial, ausência de carregamento corretivo
na consulta pública e ausência de conteúdo privado em todos os chunks, além dos
controles negativos de JSON/DOM ativos. Os endpoints continuam sendo a fronteira de posse.

## Verificação em navegador real

Playwright/Chromium com API Java, PostgreSQL18.6 e OIDC local: 14 cenários,
oito de simulados e seis regressões de identidade/banco. Os novos cobrem teclado,
foco/radios/dialog e largura 390/1280 sem overflow, visitante e resultado, confirmação
perdida depois do commit, retomada em outro navegador após reinício, duas contas/abas,
edição editorial depois do início, login sem transferência e expiração real da sessão.
Um controle negativo acrescenta campo corretivo e exige falha da proteção de JSON.
Screenshots de realização/resultado em ambas as larguras foram inspecionados localmente.
Fixtures/editorial/restart existem somente no classpath de testes; sem Google real ou
acervo aprovado local. Cache/storage/service workers permanecem vazios na origem da SPA.
A verificação de navegador não demonstra comportamento sob toda carga/rede nem o smoke
Google real exigido para deployment; estes limites permanecem explícitos.

## Recuperação verificada na revisão do lote

401/403/404 invalidam acesso assim que os headers chegam, antes de consumir JSON:
corpo truncado ou pendente não pode manter a tentativa privada visível. Testes de
transporte e da aplicação cobrem ambas as falhas; abort continua descartando respostas
tardias. Outros erros mantêm o prazo total e o tratamento de insuficiência por disciplina.

O renderer do resultado faz parte do código inicial. A validação tardia projeta os
campos corretivos em conteúdo para apresentação, sem calcular a nota. Assim, depois
de aceitar o resultado, não há novo download de renderer capaz de desmontar a raiz.
O título recebe foco antes da pintura da tela, sem depender de um efeito posterior.
O controle de vida da fila também é instalado antes de a tela aceitar escolhas:
uma ação no primeiro commit já deve iniciar o salvamento, sem manter o estado salvo
de uma escolha ainda não enviada. Regressão dispara a escolha antes dos efeitos passivos.
Se o StrictMode recriar os efeitos durante um envio, a nova geração retoma a fila após
o abort da anterior com o mesmo comando/versão; desmontagem definitiva não inicia retry.
Regressão exige abort da geração antiga, confirmação pela nova e payload idêntico.
Um teste real bloqueia esse download após iniciar a realização e exige resultado,
aulas e identidade acessíveis com uma única conclusão. O contrato HTTP e os controles
de gabarito no JSON/DOM ativo e de payloads nos assets permanecem os mesmos.

Abrir pelo histórico o ID já ativo não faz nova leitura nem desmonta a realização;
regressão preserva a escolha após falha e o mesmo commandId/payload ao repetir.
Essas proteções não cobrem toda falha de execução do navegador ou de rede.
