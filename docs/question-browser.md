# Consulta acessível de questões

Tela React pública: catálogo, filtros disciplina/assunto/cargo, lista paginada e detalhe
com enunciado, alternativas não selecionáveis e referências nomeadas. Visitante e usuário
conectado consultam o mesmo acervo com GET/credentials=omit/no-store; login opcional
continua independente e o link das aulas permanece disponível em todas as falhas.
Não salvar tentativas/histórico nem copiar acervo/gabaritos para storage/bundle.

Estados carregando/vazio/erro são distintos: indisponibilidade não aparece como zero
questões. Retry preserva filtros. Mudança de disciplina limpa assunto; qualquer filtro
reseta página e fecha/cancela detalhe. Consultas vencidas não atualizam tela, mesmo
se o transporte ignorar abort. Cada operação usa prazo único (default 5s, configuração
positiva VITE_QUESTION_TIMEOUT_MS) de início até fim do JSON, sem reinício em headers;
Promise.race limita JSON pendente mesmo quando ignora cancelamento. Cleanup libera
timers/listeners e invalida respostas após unmount/filtro/fechamento. Esse prazo cobre
consultas de questões; não muda comportamentos/proteções já corrigidos de identidade.

Contratos de entrada são verificados por whitelist recursiva, sem campos corretivos extras
ou links javascript/data. React recebe texto, não dangerouslySetInnerHTML; ênfase
textual explícita `[destaque: ...]`/`[ênfase: ...]`, listas e fórmulas textuais preservadas. Recursos exibem
descrição acessível: nenhum arquivo legado é carregado indiscriminadamente nem URL
remota de pacote é executada. Referências aceitam http/https, sem credenciais. Essa
interface não é um visualizador arbitrário de SVG/HTML nem publica imagens automaticamente.

Rótulos associados a selects; botões nativos/teclado, status/alert e foco no título do
detalhe ao abrir e no botão de origem ao fechar. Textos/fontes longos quebram linha.
Paginar direciona foco ao título dos resultados atualizados, não deixa foco em botão removido.
Testes determinísticos exercitam prazo/abort/retry, respostas antigas/filtro/detalhe,
vazio/erro/URLs/contrato e preservação das regressões de login. Fixtures ficam fora
dos imports de produção; nenhuma aprovação editorial do acervo real.

Verificação Playwright real usa o harness exclusivo de testes da identidade, com PG18.6
isolado e provedor OIDC local: 22 identidades elegíveis sintéticas, um rascunho e uma
retirada; nenhuma questão real/curadoria. Seis cenários passaram (três identidade +
três consulta), incluindo 390/1280px, teclado/foco de abertura/fechamento/paginação,
mesmos filtros após login, falha de transporte e inspeção de JSON/DOM/bundle. Nenhum
skip dessa suíte. A indisponibilidade no navegador é falha de rede injetada; queda real
de PG é verificada separadamente no teste HTTP, não alegada como browser.

Primeira execução identificou que labels envolvendo selects incluíam opções no texto
de label consultado pelo browser; regra positiva: label separado com htmlFor/id explícitos.
Regressão exige localizar rótulos exatos e navegar por teclado/foco, além de testar DOM.
Na instrumentação, resposta abortada durante troca de filtro não tem corpo disponível;
inspeção aguarda requestfinished/Promise.all, sem ignorar falhas de respostas concluídas.

Revisões independentes identificaram colisão Markdown/índices: o cliente agora exibe
**o texto integral**, sem split/slice ou interpretação de underscores/asteriscos. Até
pacotes antigos com marcadores ambíguos permanecem literais, nunca são degradados pelo
renderer. Nova extração usa ênfase por extenso, não uma linguagem a interpretar.
Regressões antes da correção reproduziram índices removidos e `_abc` → `ab`;
após correção exigem textContent exato em unit e browser real (390/1280), junto de
expoentes/índices/desigualdades e preservação das negações de markup no HTTP.
