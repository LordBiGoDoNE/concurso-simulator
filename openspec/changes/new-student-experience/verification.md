# Verificação local —2026-11-05

Implementação local no worktree `concurso-new-student-experience`, branch
`task/new-student-experience/student-journey`, baseada na main `f61d57d`.
Estado dessa verificação em 2026-11-05: alterações sem commit. Não há revisão
independente ou integração certificada. Em 2026-11-08 o usuário autorizou o envio
ao GitHub para continuar no notebook; essa autorização não equivale a aceite visual.
Acompanhamento: [issue #158](https://github.com/LordBiGoDoNE/concurso-simulator/issues/158).

## Resultados

- `npm run build`: TypeScript e bundle Vite aprovados.
- `npm test -- --reporter=dot`:23 arquivos,206 testes aprovados.
- Pacote estático montado com `scripts/build_pages.py`:108 páginas e1757
  referências locais verificadas;9 módulos/90 respostas conferidos.
- `npm run test:e2e -- --output=test-results/chromium --reporter=line`, com
  `STUDY_ROOT` no pacote isolado:21 testes Chromium aprovados, nenhum ignorado.
- `npx playwright test --config playwright.edge.config.ts --output=test-results/edge --reporter=line`:
  7 testes Microsoft Edge aprovados.
- OpenSpec1.14.1, com `OPENSPEC_TELEMETRY=0`: change criada por `new change`,
  instruções de artefatos consultadas, status reconhecido e `validate new-student-experience --strict` aprovado.
- `git diff --check`: aprovado.

## Cobertura e conferência visual

Visitante, seleção dos dois cargos,40/43 aulas por cargo e51 na exploração livre;
busca/disciplinas, restauração de aula e filtros, atualização, Voltar/Avançar,
teclado/foco e ausência de corte horizontal em390/1440px. Endereços inválidos,
parâmetros duplicados ou privados e associação incompatível são rejeitados.

Disponibilidade cobre carga, vazio confirmado, erro, retry, contrato inválido,
resposta tardia de outro cargo e saída/retorno da área. Contagem de comandos da
aplicação confirma que navegação não cria atividades; apenas o clique explícito
inicia a prática. A requisição conserva o mecanismo existente de comando/CSRF.

Regressões existentes de identidade, logout, filas e repetição idempotente passaram.
Leitor integral testado com arquivos reais locais, CSS e figura da aula; as novas
fixtures de jornada não substituem essa verificação. O protótipo documental foi
percorrido em dois tamanhos, nos dois navegadores. Capturas conferidas visualmente.

Evidências locais ignoradas pelo Git: `backend/build/new-student-evidence/` e logs
`backend/build/new-student-chromium.log`, `new-student-edge.log` e `test-results-unit.log`.

## Preview para o usuário

Servidor local em `http://127.0.0.1:5186/`. Navegação cargo→biblioteca conferida
no navegador interno, seguida de Voltar e atualização com cargo preservado.
O serviço só escuta em loopback. Processo/PID e logs ficam em
`backend/build/new-student-preview/`.

Não há API local ativa nas portas8080/8088 nesta validação. O preview mostra essa
indisponibilidade e conserva a entrada e as aulas; não oferece prática funcional
contra dados reais. A criação e os demais estados foram exercitados com fixtures
de transporte nas suites locais, sem acessar a VPS ou o banco do operador.

## Limites e próximos gates

Não foi executada suite backend/PostgreSQL: backend e contratos HTTP não mudaram.
Não constitui auditoria WCAG completa nem aceite de estudantes reais. Nenhum
acervo foi importado; Google, HTTPS, produção, main e stable foram preservados.

O usuário testa e valida antes de revisão por modelo, conforme AGENTS.md. O envio
ao GitHub foi autorizado separadamente em 2026-11-08 para a troca de máquina.
Revisão SOL6.1xHigh não executada; cota/tokens dessa revisão não medidos, sem
estimar consumo zero. Revisão de lote, gate cumulativo e merge permanecem pendentes.
Não arquivar automaticamente.

## Retomada no notebook — 2026-11-08

Branch `task/new-student-experience/student-journey`, a partir do head publicado
`50edf8f7b7d05c17b50728f2f18abf5455035d5e`, com correção local da expectativa de
retorno do login em `frontend/e2e/identity.real.spec.ts`: a URL exata passa de `/`
para `/#/inicio`, conforme a canonicalização de rotas da spec. Os demais contratos
de identidade, sessão, CSRF e isolamento continuam sendo conferidos pela suíte.
Esta correção ainda não foi enviada ao GitHub; não declarar o CI remoto corrigido.

Ambiente desta execução: Windows, Node 24.22.1, npm 11.20.0, Temurin 25.0.5,
Docker Engine 29.9.2 com daemon Linux e Compose 5.6.1. O Node 26 instalado no
sistema não foi usado. Configuração de sessão, caches, navegadores, scripts de
preview e temporários ficam nas pastas ignoradas do projeto, sem mudar o PATH
global ou versionar configurações privadas.

Resultados executados nesta máquina:

- `npm run build`: aprovado.
- `npm test -- --reporter=dot --maxWorkers=2`: 23 arquivos, 206 testes aprovados.
- Pacote estático: 108 páginas, 1757 referências e 9 módulos/90 respostas conferidos.
- Chromium com `STUDY_ROOT`: 21 cenários aprovados, nenhum ignorado.
- Edge: 7 cenários aprovados.
- `testClasses`: compilação do harness integrado aprovada.
- `npm run test:identity`: 24 cenários aprovados com PostgreSQL e OIDC sintéticos
  isolados no Docker local, incluindo login, expiração, logout, dispositivos,
  tentativas e revisão por conceito. Não usa banco ou credenciais da VPS.
- OpenSpec 1.14.1, telemetria desativada: validação strict aprovada.

Preview da implementação em `http://127.0.0.1:18088/`, restrito ao loopback,
com API e material existentes da VPS acessados por túnel SSH local. Nenhum código,
imagem, configuração ou conteúdo foi publicado ou importado no servidor. O preview
depende de a máquina e os processos locais continuarem ligados; seus PIDs e logs
estão em `backend/build/new-student-notebook/`, fora do Git.

Conferência do preview em Chromium: visitante, cargo Assistente Administrativo,
40 aulas, abertura de P01, material HTTP 200 e restauração do contexto após atualizar.
A navegação não enviou POST/PUT/PATCH/DELETE à API. Captura local em
`backend/build/new-student-notebook/preview-desktop.png`.

Limite observado na API existente da VPS: a consulta pública retornou zero questões
elegíveis; a disponibilidade de prática retornou zero para P01 e M01 no cargo
Assistente Administrativo. O preview apresenta a ausência confirmada e mantém
essas práticas desabilitadas. Isso não demonstra ausência de rascunhos ou de outros
dados privados no banco, nem autoriza carga editorial. Criação/resposta/correção
foram exercitadas com fixtures isoladas nas suítes, não com o acervo da VPS.

Aceite visual do usuário, revisão independente do lote e gate cumulativo continuam
pendentes. Nenhuma revisão por modelo, commit, push, merge ou publicação foi
executada nesta retomada.

## Acervo de desenvolvimento local — 2026-11-08

Depois da conferência acima, o usuário solicitou explicitamente carga somente local
para desenvolvimento e backlog de importação via API. A API HTTP atual não tem
importação administrativa; foi criada a issue
[#160](https://github.com/LordBiGoDoNE/concurso-simulator/issues/160), sem implementar
endpoints ou iniciar uma nova spec/lote.

O preview `http://127.0.0.1:18088/` agora usa exclusivamente recursos desta máquina:
API Java em loopback 18082, material local em 18090 e PostgreSQL 18.6 em container
Docker com porta loopback 15432, banco `concurso_dev158` e volume persistente próprio
`concurso-dev158-postgres`. O túnel criado nesta retomada foi encerrado. O estado
vazio observado na VPS permanece como histórico da conferência anterior, não como
limite do preview local atual. Nenhuma carga ou alteração foi feita na VPS.

Seleção efetivamente importada e habilitada:

| Pacote | Quantidade | Digest da seleção de chaves/fingerprints |
| --- | ---: | --- |
| Simulados curados | 250 | `409be988103beaa590d7625730f21e72e3e82ad36617af0837de453d75e261fb` |
| Base adicional M04-Q05 | 1 | `c818af98a8763eeaea569869124000e5ee08f8129070f302829d537bbe49ba6c` |
| Prática das aulas | 255 | `1c7ff1ad0a916d5edbfb9a58f1a6302484667ca86cc0748a8ec66cbce7125ae1` |

Fonte: snapshot `50edf8f7b7d05c17b50728f2f18abf5455035d5e`, exportado com
`git -c core.autocrlf=false archive` para preservar os bytes LF dos manifests.
A conversão CRLF do checkout Windows muda hashes de fonte; a exportação isolada
evitou alterar manifests, fingerprints aprovados ou arquivos do workspace.
SHA-256 do job local com destino, arquivos, seleções e hashes exatos:
`e48d63cd80847ccc8eed68b286975e4185d860dad3da5a9f281f664f2666b366`.

Flyway migrou o banco novo e Hibernate validou o schema. A operação deliberada
validou os pacotes pelos leitores existentes e usou `ApplyQuestionCandidatesUseCase`
e `DecideQuestionUseCase` pelos adaptadores JPA existentes, em JVM nonweb separada.
O helper operacional permanece fora dos source sets/JAR/startup. Foram criadas
506 identidades/revisões próprias deste destino e 1012 decisões reais SUBMIT/APPROVE,
com fingerprints, instantes e sequências conferidos. As aprovações da outra máquina
não foram reaplicadas por UUID. A habilitação local se baseia no conteúdo já
preparado/conferido e na autorização de desenvolvimento; não afirma nova revisão
humana ou aprovação de publicação.

Evidências e configuração privadas/locais, ignoradas pelo Git:
`backend/build/new-student-notebook/local/`, incluindo `content-job.json`,
`content-preview.json`, `content-report.json`, `content-load.log`, estado Compose,
segredo de banco, PIDs, logs e `browser-check.json`. O relatório contém as novas
identidades/revisões e decisões; senhas não foram impressas ou versionadas.

Verificação real desta carga:

- API pública: 251 questões elegíveis de simulado.
- Inventário editorial: 255 práticas elegíveis, cinco por cada um dos 51 assuntos.
- 83 consultas de disponibilidade assunto/cargo válidas: HTTP 200 e quantidade 5.
- Chromium com dados reais locais: entrada, biblioteca, aula/material, atualização
  da rota e início explícito sem POST de criação durante navegação.
- Uma prática de cinco itens: criação, resposta confirmada, encerramento e resultado
  do servidor, com quatro itens em branco. Um simulado de um item: criação, resposta,
  conclusão e resultado. As duas realizações são somente registros locais de smoke.
- IDs capturados nas duas realizações pertencem aos acervos corretos, sem misturar
  práticas e simulados; a prática não expôs campos corretivos antes do encerramento.

O código do produto não foi alterado por essa carga. Permanecem pendentes o teste e
aceite visual do usuário e as revisões/gates de integração da #158. Este registro
não autoriza publicar a aplicação, migrar dados à VPS ou implementar a issue #160.

## Painel de prática integrado à aula — 2026-11-08

Refinamento solicitado e autorizado pelo usuário após experimentar o exemplo
interativo: painel recolhível no final da aula, com questões carregadas abaixo do
botão somente após Começar prática. Implementado na mesma branch de trabalho,
sem commit/push. As evidências anteriores permanecem históricas; os resultados
abaixo referem-se ao workspace com este refinamento.

O botão permanece acima da realização. O início mostra carregamento, usa o comando
idempotente existente e preserva a URL da aula. Recolher usa a apresentação nativa
de details sem desmontar o executor. As questões de outro assunto/cargo ficam
ocultas, preservando a fila da atividade anterior. O resultado aparece no mesmo
painel. A navegação entre aulas fica depois do painel; Práticas continua disponível
para o contexto atual e o histórico de conta. Login/identidade mantêm a limpeza do
contexto privado, inclusive quando a aula pública está aberta.

Resultados desta execução:

- Build TypeScript/Vite aprovado.
- 23 arquivos/209 testes unitários aprovados. Novas regressões cobrem painel
  inicialmente recolhido, criação somente por clique, ordem abaixo do botão,
  recolhimento/troca de aula com gravação pendente e retry do mesmo comando.
- Chromium: 23 cenários aprovados, com o pacote offline incluído.
- Edge: 9 cenários aprovados, incluindo os dois novos cenários de gravação pendente.
- Backend/PostgreSQL/OIDC sintéticos isolados: 24 cenários integrados aprovados,
  preservando as regressões de identidade, sessão, tentativas e revisão por conceito.
- Após o ajuste final de texto, build e os 11 testes de LessonPracticeEntry
  revalidados. OpenSpec 1.14.1 strict e git diff --check aprovados.
- Seletores da suíte do pacote Docker atualizados para o fluxo inline; 9 cenários
  coletados com sucesso. Essa suíte de imagens empacotadas não foi executada nesta
  rodada e sua coleta não é evidência de execução. Nenhuma imagem foi publicada.

No preview com o acervo real local, Chromium em 390/1440px confirmou: painel fechado
ao entrar, ausência de questões/criação antes do clique, início com cinco questões,
botão acima da atividade, resposta confirmada preservada ao recolher/reabrir,
encerramento e resultado na mesma URL. Trocar aula/cargo não criou outra prática
nem exibiu o resultado do contexto anterior. Não houve corte horizontal ou erros
de execução. Os dois fluxos criaram registros de smoke somente no banco local.

O material local de mesma origem foi medido integralmente, com altura de conteúdo
igual à do iframe: 14873px em 390px e 10185px em 1440px na conferência de P01.
O ajuste não acessa documento de outra origem, não habilita scripts no sandbox e
preserva o fallback/link original. ResizeObserver agenda a medição no próximo
frame para evitar ciclos de atualização de layout. A rolagem e as capturas desktop
e de viewport móvel foram conferidas com a aplicação real.

Evidências ignoradas pelo Git: `frontend/test-results/inline-*` e
`backend/build/new-student-notebook/local/inline-browser-check.json`,
`inline-practice-1440.png` e `inline-mobile-viewport.png`. Backend e conteúdo do
banco local não tiveram mudanças de regra/schema; a VPS permanece fora do preview.

Preview para teste do usuário: `http://127.0.0.1:18088/`. Aceite local do usuário,
revisão independente e gates continuam pendentes. Não houve novo envio ao GitHub,
merge, arquivamento ou publicação externa.

## Índice acima da aula e leitor ampliado — 2026-11-08

Por solicitação do usuário, Nesta disciplina foi movido da lateral para antes do
documento. A área de leitura usa toda a largura do layout, com índice em grade
responsiva e indicação da aula atual. Navegação, seleção por cargo e prática
integrada mantêm os mecanismos existentes. Alteração local de estrutura/CSS,
sem mudanças de dados ou backend.

Verificação desta alteração: build aprovado; 8 testes existentes de biblioteca e
jornada aprovados; 9 cenários da jornada Chromium e 9 Edge aprovados. Os totais
209 unitários e 24 integrados da seção anterior são evidências daquela rodada,
não novas execuções neste ajuste de layout.

No preview real, 390/1024/1440px: índice inteiramente acima do documento, largura
do leitor igual à área disponível (350/786/1120px, respectivamente), sem corte
horizontal. Seleção de P02 por teclado, foco do título, aula atual e retorno a P01
pelo histórico conferidos. A navegação não realizou operações de escrita e não
apresentou erros de execução. Capturas desktop/celular foram inspecionadas.
Evidências ignoradas: `backend/build/new-student-notebook/local/top-index-check.json`
e `top-index-390.png`, `top-index-1024.png`, `top-index-1440.png`; regressões em
`frontend/test-results/top-index-*`. OpenSpec strict e diff --check aprovados.

Pronto para nova validação do usuário no preview local, sem envio, revisão por
modelo, integração ou publicação externa.

Refinamento seguinte solicitado pelo usuário: índice antes do título da aula e
cabeçalho dinâmico, por exemplo Nesta Disciplina - Português. Build e os 3 testes
existentes de LessonLibrary revalidados. No preview em 390/1024/1440px, índice
antes do título, largura integral preservada, rótulos de Português e Matemática,
teclado/histórico e ausência de escrita conferidos. Evidência local atual em
`backend/build/new-student-notebook/local/discipline-index-check.json` e capturas
`discipline-index-390.png`, `discipline-index-1024.png`, `discipline-index-1440.png`.


## Aulas nativas no layout atual — 2026-11-08

Escopo solicitado pelo usuário: converter as 51 aulas; teoria, exemplos resolvidos,
esquemas, tabelas, fórmulas e fontes na aula, com exercícios somente pelo painel
de prática. A decisão substitui o iframe/autoaltura das verificações históricas
acima. Não é apenas ocultação do cabeçalho do HTML antigo.

`NativeLesson` renderiza dados públicos tipados com elementos React conhecidos.
Cada aula tem seu próprio JSON carregado sob demanda, sem HTML bruto, scripts,
CSS legado ou acesso ao site estático. Há um título visível, índice da disciplina
antes dele e sumário interno que foca a seção sem mudar a rota. Atalhos de revisão
abrem a aula nativa em nova aba com cargo compatível; assuntos de outro cargo na
revisão dos 51 abrem em exploração livre, sem alterar a tentativa atual.
Falha de leitura oferece retry; seleção nova cancela/ignora a leitura anterior.

A compilação usa as fontes públicas preservadas e o enriquecimento didático de
Matemática já existente. Mantém 50 esquemas vetoriais e as duas representações
percentuais nativas de M04, além de MathML e tabelas. Os esquemas largos têm
rolagem horizontal com teclado e explicação textual. Não foram alterados os
HTMLs/SVGs de origem, o backend, questões, fingerprints editoriais ou sua elegibilidade.
A seleção exclui exercícios, resoluções do banco, navegação e marcas de edições
anteriores. Exemplos didáticos resolvidos permanecem.

Seleção habilitada para teste local: `frontend/src/lesson-documents/`, 51 JSONs e
um manifesto gerado; 744702 bytes ao todo. SHA-256 LF do manifesto:
`d4f0b4cd4990c7297868521bbcafb4b7f7b353d8aa3759495152b0c814b6ad08`.
O manifesto registra hashes de cada documento e dos insumos, inclusive os oito
arquivos públicos de gabarito lidos pelo enriquecimento existente; seus blocos de
exercício/resolução não entram na seleção nativa. Os 52 arquivos JSON são saída
gerada, identificada separadamente do código autoral. Python é necessário para
autoria/verificação; não é dependência de runtime ou do build Node preparado.

Verificações desta rodada:

- Build TypeScript/Vite aprovado, com 51 assets separados para as aulas.
- Vitest: 24 arquivos/212 testes aprovados com `--maxWorkers=2`.
  Uma execução com concorrência padrão teve timeout na espera inicial de um
  teste de contexto; o arquivo isolado passou seus 12 testes e o conjunto passou
  com dois workers. Asserções de privacidade/filas foram mantidas.
- `compile_native_lessons.py --check` e três regressões Python aprovados:
  seleção integral reproduzível, preservação de conteúdo e rejeição de marcação/links inesperados.
- Chromium: 26 cenários aprovados, incluindo o pacote estático preservado offline.
- Edge: 12 cenários aprovados. As 51 aulas foram percorridas em 390/1440px nos
  dois navegadores, sem API, corte horizontal da página, iframe ou pedidos de
  HTML/assets legados. Índice antes do título, largura integral, fontes e
  ausência de exercícios antigos conferidos.
- Após acrescentar a verificação de rolagem do esquema por teclado, os três
  cenários nativos foram revalidados em Chromium e em Edge (51 aulas nas duas
  larguras, retry de leitura e nenhuma criação automática).
- Regressão integrada: primeira execução teve 18 aprovações e seis falhas de
  seletores ainda voltados ao link estático/H1 do documento antigo. Após atualizar
  essas asserções para o artigo nativo e sua rota, os dez cenários de revisão
  passaram em nova execução com backend/PostgreSQL/OIDC sintéticos isolados.
  Os 24 cenários ficaram cobertos pelas duas execuções; não se declara uma única
  execução completa aprovada nesta rodada.
- Suíte de imagens Docker atualizada para o leitor nativo, mas não executada
  nesta rodada; não há certificação das imagens empacotadas ou publicação.

No preview real `http://127.0.0.1:18088/`, capturas de P01, M04 e I05 em
390/1440px inspecionadas. Um título visível, nenhum iframe, página sem corte ou
erros de execução. Prática real de P01: painel inicialmente fechado, uma criação
explícita por fluxo, cinco questões, resposta confirmada preservada ao recolher,
encerramento e resultado na mesma rota. Dois registros de smoke foram criados
somente no banco local autorizado, preservando os dados anteriores.

Evidências locais ignoradas pelo Git:
`backend/build/new-student-notebook/local/native-preview-check.json`,
`native-practice-check.json`, capturas `native-*.png` e
`frontend/test-results/native-final-{chromium,edge}/`.
OpenSpec strict e `git diff --check` aprovados. Risco conservador permanece
crítico pela convivência com realizações privadas; as verificações locais não
substituem revisão independente ou gate cumulativo.

Entrega local para teste do usuário. Sem novo commit/envio ao GitHub, merge,
revisão por modelo, arquivamento, alteração na VPS ou publicação externa.


## Retorno ao topo ao trocar aula — 2026-11-08

Solicitação adicional do usuário: Próxima aula deve abrir no topo. O leitor agora
foca o título sem rolagem automática e reposiciona a página em scrollY=0 na troca
de assunto, antes da pintura. Aplica-se também à aula anterior e à seleção pelo
índice; o carregamento assíncrono do documento não recupera a posição inferior.

Build aprovado e três testes existentes de LessonLibrary aprovados (jsdom não
implementa rolagem; a posição foi verificada no navegador real). Novas regressões
em Chromium e Edge: dois cenários por navegador, em 390/1440px, partem do final de
P01, avançam para P02 e retornam para P01; confirmam topo antes/depois do documento,
rota e foco do título. Sem operações de escrita ou mudanças na prática/backend.
Evidências ignoradas: `frontend/test-results/lesson-scroll-{chromium,edge}/`.


## Envio para continuar na máquina de casa — 2026-11-08

O usuário solicitou parar e enviar todas as alterações ao remoto para retomada.
Os refinamentos de prática inline, índice, 51 aulas nativas e retorno ao topo
seguem na branch task/new-student-experience/student-journey/PR #159. Inclui fontes,
JSONs gerados, testes, docs e a regra de confirmação de implementação. O handoff
atualizado está em docs/handoffs/new-student-experience.md.

A spec lesson-practice-by-topic está publicada separadamente na branch documental
task/lesson-practice-by-topic/proposal, commit 069b027, issue #161/PR #162.
Prática sem cargo ainda não foi implementada e seus grupos não estão autorizados.
A regra espelhada neste workspace não representa merge do PR documental.

O envio autorizado não inclui banco, senhas, chaves SSH, arquivos .env privados,
volumes ou caches locais e não migra dados à VPS. Não há novo lote/revisão por
modelo, migração, carga, merge, arquivamento ou publicação de aplicação nesta etapa.
As verificações anteriores mantêm seus escopos; a conferência pré-envio atual
valida OpenSpec, seleção gerada e diff, sem alegar nova execução de todas as suítes.

## Correções locais após retomada — 2026-11-09

Escopo explicitamente autorizado nesta conversa: corrigir Python/CI e ajustar o
texto legado das aulas. Branch local nova
`task/new-student-experience/native-lessons-corrections`, a partir do head publicado
`37ccccf1563955d431591629f66c15bb7810a31a`. Alterações sem commit; não atribuir os
resultados abaixo ao commit publicado ou ao CI remoto, que não foi executado
novamente. A branch anterior, o PR #159 e as stacks não foram alterados.

Causa da falha: o teste chama `compile_all` diretamente, mas somente a CLI criava
`backend/build`. Num checkout novo, o diretório temporário não podia ser criado.
Agora a função que usa o workspace garante sua existência. Regra positiva:
entradas reutilizáveis cuidam dos pré-requisitos que possuem, sem depender de uma
execução anterior de outra entrada. A regressão compila as fontes reais copiadas
para uma pasta isolada sem backend/build, compara os 52 documentos e independe
do estado de build do workspace do desenvolvedor. Não houve mudança de workflow.

O texto legado estava nas 51 aulas, em duas variantes. O compilador nativo agora
orienta abrir Pratique esta aula ao final da leitura e explica que a correção
aparece após encerrar a prática. M04 também deixa de remeter à atividade curta e
às dez questões do arquivo antigo. HTMLs/SVGs estáticos, exemplos, fontes, backend,
questões e elegibilidade não foram alterados. A regressão Python cobre as duas
variantes; a verificação do catálogo no frontend exige a orientação atual nas
51 aulas e rejeita a referência antiga. Não equivale a auditoria editorial integral.

Seleção regenerada e habilitada para teste somente neste worktree:
`frontend/src/lesson-documents/`, 51 aulas mais manifesto, 748225 bytes LF.
SHA-256 LF do manifesto:
`dd06193b42c3ad3a52882cac694eec2b80f5546a961e7b3d5acccf5d733fbb6a`.
Os JSONs foram produzidos pelo gerador, sem edição manual. O manifesto identifica
os insumos e o novo gerador; nenhuma carga de banco foi feita.

Verificações confirmadas desta correção:

- Antes da correção, a regressão de cópia limpa falhou por diretório ausente e
  as duas variantes de orientação falharam por manter o texto antigo.
- Windows/Python 3.14: quatro testes de aulas aprovados; geração e --check aprovados.
- Linux/Python 3.14 da imagem fixada em infra/local-app/images.lock.json:
  quatro testes aprovados e --check aprovado, numa cópia das fontes atuais,
  sem rede, portas ou volume persistente. Não é a suíte Python ampla completa.
- Node 24: build TypeScript/Vite e 24 arquivos/212 testes aprovados, dois workers.
- Chromium: 28 cenários aprovados, inclusive pacote estático offline, prática
  recolhível/fila e retorno ao topo. Edge: 14 cenários aprovados. As 51 aulas
  foram percorridas nas duas larguras com o conteúdo regenerado.
- Captura de M04 em 1440px inspecionada: orientação atual presente no leitor.
  Comparação estrutural dos 51 JSONs com HEAD confirmou somente um trecho textual
  alterado por aula; demais conteúdos e estrutura permaneceram idênticos. Entre
  os insumos do manifesto, mudou apenas o hash do compilador.
- OpenSpec 1.14.1 strict, --check e git diff --check aprovados. Backend, infra,
  workflows, AGENTS.md, openspec/config.yaml e material estático sem alterações.

Logs locais ignorados: `backend/build/issue158-corrections-{red,python,linux}.log`,
`frontend/test-results/issue158-corrections-{build,unit}.log`.
Navegadores em `backend/build/issue158-corrections-{chromium,edge}.log`; capturas
em `frontend/test-results/issue158-corrections-{chromium,edge}/`.

Antes destas edições, o head publicado 37ccccf foi revalidado neste PC com
24 cenários integrados em uma execução, cinco parametrizados e nove de navegador
contra o pacote Docker; construção/isolamento/runtime/backup/restauração passaram.
Esses resultados são do head publicado, não novas execuções das imagens com as
correções sem commit. O construtor de imagens usa somente commits; não foi
contornado para atribuir código local ao SHA anterior.

Aceite visual de leitura/texto recebido em 2026-11-09, conforme registro abaixo;
prática com API e revisão independente continuam pendentes. Não houve
commit, push, comentário público, revisão por modelo, gate semântico, merge,
arquivamento, alteração de acervo real ou deploy nesta rodada. A falha remota só
poderá ser declarada resolvida no GitHub após envio autorizado e CI correspondente.

## Aceite visual do usuário — 2026-11-09

Após abrir o preview local, o usuário confirmou explicitamente: Aceito. O preview
em `http://127.0.0.1:5186/` serve as 51 aulas nativas e as correções locais da branch
`task/new-student-experience/native-lessons-corrections`, ainda sem commit, baseada
em 37ccccf. Seleção aceita para leitura: manifesto SHA-256 LF
`dd06193b42c3ad3a52882cac694eec2b80f5546a961e7b3d5acccf5d733fbb6a`.

O aceite se refere ao preview visual de leitura, navegação/índice e orientação
textual corrigida. Não se afirma que o usuário inspecionou individualmente todas
as aulas. A API do preview está indisponível por configuração deliberada; a
confirmação não é aceite de início/resposta/correção de prática funcional, acervo
real, login Google ou backend conectado. As regressões automatizadas anteriores
mantêm seus próprios escopos e não são substituídas por esse aceite.

Revisão independente, autorização de commit/envio das correções, CI remoto,
integração e publicação continuam pendentes. Não foi disparado revisor nem
declarado gate aprovado por esta confirmação.
