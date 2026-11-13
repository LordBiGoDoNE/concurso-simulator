# Design

## Context

Spec da [issue #173](https://github.com/LordBiGoDoNE/concurso-simulator/issues/173); motivação em [proposal.md](proposal.md). Base examinada: `main` `16ca8aef382d64e7652b280b413122c357775ead`.

**Ordem autorizada em 2026-11-10:** todos os grupos 1–8 autorizados localmente. Esta conversa possui inventário/ferramentas/editorial/fontes/testes/docs e começa pelo grupo 1, seguido de 3–7. A #161 está sendo implementada separadamente e tem prioridade em mockexam/persistência/HTTP/OpenAPI/frontend. Grupo 2 e validação integrada do 8 aguardam sua integração autorizada à main. As decisões abaixo de manter cargo/não depender da #161 documentam a base histórica; foram superadas nesta coordenação: conciliar prática por assunto sem cargo com teto 25 após aquela integração, sem stack/cherry-pick/merge entre specs ou formato/migração adicional por quantidade. Publicação/revisão de implementação aguardam teste local do usuário. [Execução e limites atuais](../../../docs/question-bank-expansion.md).

Base de implementação do grupo 2 atualizada para `226d2e5b07208887941811beff10ac29295b04b3`, merge autorizado da #161/PR #176 conferido na main. A épica local avançou apenas por fast-forward; a branch anterior e todos os arquivos locais foram preservados por hash. Épica/branches remotas não foram movidas.

O catálogo local possui 51 assuntos e dois cargos editoriais. O banco separa SIMULADO de PRATICA_AULA pela origem imutável. Após #161, práticas novas usam somente bases fixas, assunto sem cargo e captura formato 4; limite inicial 5 no núcleo, disponibilidade, OpenAPI e cliente. Formatos 1/2/3 são históricos. Simulados já permitem composição própria; seu tamanho não muda nesta spec.

A #160, integrada pelo #171, oferece prévia/aplicação administrativa para JSON autossuficiente, até 100 itens e uma modalidade por pacote, sempre DRAFT. A UI administrativa atual não tem upload; usaremos os endpoints existentes. Fontes PDF não são um tipo aceito nos arquivos do pacote. A operação local `decide` registra decisões editoriais separadas.

O histórico versionado informa 250 questões curadas de simulado e a aprovação posterior de M04-Q05; prática tem 255. A API local 8088 estava indisponível na investigação. Esses números não certificam o estado do banco do operador ou da VPS.

## Decisão atual — acervo 25, prática até cinco

Em 2026-11-11 o usuário corrigiu a interpretação: 25 é a meta do acervo por
assunto/modalidade; cada nova prática sorteia até cinco do pool completo. Esta
regra supera os trechos históricos de teto25 nesta documentação. V1–V15 e capturas
anteriores, inclusive as de25 já abertas, permanecem legíveis; não há nova migração
ou regravação para corrigir a quantidade de novos sorteios.

## Goals / Non-Goals

**Goals:** entregar cobertura rastreável nas 102 combinações, conteúdo original/autoral conferido, ferramentas incrementais, acervo de 25 por assunto com práticas de até cinco sorteadas (esclarecimento de 2026-11-11; a meta original "práticas de até 25" é histórica) e validação local preservando dados e contratos privados.

**Non-Goals:** remover cargo (#161), alterar composição de simulados, ampliar templates, adicionar editor/upload PDF/aprovação HTTP, executar importação no startup/Flyway, distribuir fontes pelos builds públicos ou publicar na VPS. Esta entrega inicial é documental; conteúdo/código dependem da confirmação dos grupos.

## Decisions

### 1. Medir bases elegíveis por assunto e finalidade

Usar uma matriz de 51 assuntos × duas modalidades. Cada célula registra referências versionadas, estado realmente observado no destino, candidatos, importados, elegíveis, originais verificadas e pendências. Relatórios offline não inventam elegibilidade: quando não consultado, o estado do banco é desconhecido, não zero.

| Disciplina | Assuntos | Simulado de referência | Prática de referência | Meta por modalidade | Acréscimo estimado | Mínimo de provas, duas modalidades |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Português | 10 | 50 | 50 | 250 | 400 | 100 |
| Matemática/Raciocínio | 12 | 51 | 60 | 300 | 489 | 120 |
| Informática | 10 | 50 | 50 | 250 | 400 | 100 |
| Administrativo | 8 | 50 | 40 | 200 | 310 | 80 |
| Escolar | 11 | 50 | 55 | 275 | 445 | 110 |
| Total | 51 | 251 | 255 | 1.275 | 2.044 | 510 |

O déficit é calculado por célula sobre bases elegíveis válidas, preservando excedentes. Não multiplicar conteúdo comum por cargo, revisões por identidade ou variantes por base. Arquivo/questão ausente do registro antigo de elegibilidade não significa rejeição, nem autoriza reutilizar seu problema sem triagem.

Alternativa descartada: multiplicar os totais globais por cinco. Ela esconderia os assuntos de simulado com quatro a sete bases e as diferenças de distribuição entre disciplinas.

### 2. Guardar a expansão em uma edição nova

Criar, quando o grupo 1 for autorizado, uma edição `2026-11` com arquivos e fontes próprios de cada disciplina/finalidade. Não acrescentar linhas às fontes de outubro: seus hashes participam da proveniência/fingerprint e devem permanecer históricos. Os compiladores antigos continuam validando seus lotes fechados, inclusive a regra histórica de cinco práticas por assunto.

Um compilador incremental reutiliza a canonização/fingerprint existentes e produz pacotes administrativos separados por assunto/finalidade, contendo apenas a seleção nova. Saída ordenada e determinística para a mesma entrada; chaves estáveis com prefixo `admin.` e identificador da edição/assunto, nunca derivadas de posição de alternativa ou do gabarito. SIMULADO usa `admin-question-v1`; PRATICA_AULA usa `lesson-practice-v1`, conforme #160. Preservar UUIDs de alternativas por conteúdo/identidade, não pela posição correta.

Não duplicar fontes antigas para gerar novas identidades. Não colocar pacotes, provas, gabaritos ou relatórios privados no JAR, frontend, assets estáticos, contexto Docker público ou migrações. Preferir conteúdo textual acessível; recursos novos só entram pelos formatos e validações já suportados.

### 3. Tornar a mistura de origens auditável

O registro editorial distingue ORIGINAL_DE_PROVA, AUTORAL e ADAPTADA. Esse metadado fica na edição/relatórios; não exige coluna ou novo DTO público. Cada original liga a sua identidade editorial à banca, concurso, ano, cargo, versão/caderno, número, URLs/fontes com hashes, gabarito definitivo, situação da questão e evidência de reutilização permitida. Autoria e referências existentes transportam a identificação compatível com o contrato atual.

Avança SP tem prioridade, conforme a [Prefeitura de Limeira](https://www.limeira.sp.gov.br/imprensa/noticias/geral/inscricoes-para-concursos-da-prefeitura-de-limeira-entram-na-reta-final). Outras bancas complementam a cobertura com aderência documentada. Selecionar ao menos dez originais diferentes por assunto: cinco para cada acervo. A mesma questão oficial, mesmo em outro caderno/numeração, não pode preencher duas células. Adaptações substanciais, questões anuladas ou sem resposta definitiva/evidência suficiente ficam fora da cota original.

A prospecção do grupo 1 registra disponibilidade, não presume encontrar as 510 originais. Cada disciplina só conclui quando comprovar sua própria meta. Escassez de fontes não autoriza substituir a cota por autorais, importar de agregadores sem comprovação ou declarar o grupo pronto.

### 4. Extrair e revisar offline, sem ampliar o importador

Conferir provas/gabaritos em fontes primárias, normalizar o texto e registrar a correspondência. PDFs originais são evidência de trabalho fora do payload HTTP; o pacote contém fontes normalizadas permitidas, hashes e referências, respeitando limites atuais de corpo/arquivos. Não alterar formatos/MIME/limites da #160 para acomodar documentos inteiros.

Cada item recebe resolução explicada e revisão dos sete critérios editoriais sobre seu fingerprint. Cálculos usam verificação independente; legislação e informática indicam dispositivo, recorte/data, versão/idioma quando necessários. Referências oficiais sustentam o estudo e a conferência, sem declarar as questões autorais como publicadas por esses órgãos.

A triagem abrange edição nova, acervos atuais e 509 candidatos válidos do legado 510, sobrepondo as revisões curadas correntes. Combinar comparação textual, estrutura/valores e conferência semântica. A decisão de cada suspeita registra par, hashes e motivo específico; mudança de fingerprint invalida a decisão antiga. Limiar e ausência de alertas são apoio, não prova automática de originalidade.

A prova e o gabarito necessários à auditoria permanecem no dossiê editorial. Não colocar URLs corretivas no campo público de referências de uma questão aberta. Não se promete sigilo das fontes autorizadas publicadas no repositório público.

### 5. Sortear até cinco de um acervo de pelo menos25

A meta editorial de25 bases por assunto/modalidade pertence ao acervo, sem alterar
seu tamanho quando uma prática é iniciada. O núcleo seleciona aleatoriamente
`min(5,pool próprio elegível)` sem repetição interna, cargo atribuído ou complemento
por SIMULADO/template. Cada novo commandId dispara sorteio; coincidências entre
práticas são possíveis. Não limitar a consulta aos primeiros25 ou primeiros5.

Disponibilidade informa available real e `maximum:5`; início mantém
`{commandId,topicId}`, sem quantidade livre. `Attempt.startPractice` recusa nova
captura acima de5; restauração aceita o histórico formato4 até25 já criado na
etapa anterior. Replay recupera o tamanho original e não ressorteia. Formatos1/2/3,
comandos v1/v2 e política de correção são preservados.

Frontend/API acompanham maximum5; textos informam cinco sorteadas. Leitor/histórico/
resultado continuam aceitando as capturas históricas maiores. O codec preserva o
JSON imutável original ao responder/concluir, inclusive entre JVMs. A V15 já
executada permanece intacta para compatibilidade, sem nova migração, truncamento
ou regravação. Os dois limites não devem compartilhar a mesma constante de negócio.

### 6. Importar, conferir e habilitar em passos separados

Após autorização do grupo correspondente, reconciliar o destino local e preservar backup privado. Usar a prévia/aplicação da #160 com sessão ADM/CSRF/configuração existentes, seleção explícita e digest conferido. Manter dados e recibos privados fora do Git. Um pacote contém um assunto/finalidade e até 100 itens; erros/retry/confirmação seguem os contratos atuais.

Importar apenas como DRAFT. Submissão/aprovação usam as operações editoriais locais existentes com revisão, fingerprint, ponteiro e sequência esperados, responsável e destino exatos. A autorização permanente de teste local registrada no AGENTS.md dispensa nova aprovação local genérica para conteúdo conferido do grupo autorizado; não dispensa evidência nem autoriza aprovação pelo startup/CI/HTTP.

Conferir pós-condições no banco e pelos contratos públicos. Versionar apenas evidências públicas apropriadas; não publicar credenciais, atores privados, recibos ou backups. Conteúdo pronto no Git não é cobertura habilitada. Se uma original ficar pendente, mostrar o déficit e manter a combinação incompleta.

### 7. Separar proprietários e integração

A central coordena contratos, bases, ordem e manifestos agregados. Grupo 1 possui contrato/formato editorial, compilador e testes compartilhados. Grupo 2 possui mockexam, HTTP/OpenAPI e frontend. Grupos 3–7 possuem exclusivamente os arquivos/fontes/testes/documentação de sua disciplina; não alteram contratos compartilhados ou evidências de outra disciplina. Grupo 8 consolida o conjunto e o destino local.

Registrar responsável nominal/conversa, branch/worktree, base e grupo na #173 antes de cada início. Um worktree e recursos de teste exclusivos por escritor, sem DB/portas/saídas mutáveis compartilhadas. Autorizar paralelismo dos grupos de conteúdo apenas depois do contrato do grupo 1; cada grupo fecha sua disciplina completa nas duas modalidades.

Usar `epic/question-bank-expansion`, com tarefas em `task/question-bank-expansion/<assunto>`. Grupo 2 pode avançar separado dos conteúdos; 3–7 dependem de 1; 8 depende de 2–7. PRs independentes vão à épica; Stacks somente para dependências reais da mesma spec conforme AGENTS. A publicação desta proposta não integra documentos ou código na épica/main.

## Risks / Trade-offs

- [Faltam originais reutilizáveis para algum assunto] → inventariar fontes cedo, complementar bancas compatíveis e manter o déficit explícito; não relaxar a cota nem inventar origem.
- [Quantidade encobre repetição/erro] → conferir por item e por pares, testar cálculos e revisar fontes atuais; conter a produção em lotes revisáveis, sem completar metas por variação superficial.
- [Contagens do Git divergem do operador] → distinguir inventário offline, estado importado e elegibilidade observada; medir antes/depois sem destruir o banco.
- [25 itens ampliam filas e navegação] → regressões do percurso completo, último item, reinício, identidade, teclado e celular, preservando proteções existentes.
- [Binário anterior não lê novas práticas acima de cinco] → atualização coordenada e recuperação com backup/imagem exatos em destino separado; nunca truncar registros para fazer rollback.
- [Paralelismo altera contratos/manifestos comuns] → proprietários explícitos, worktrees/recursos isolados e consolidação única no grupo 8; nenhuma stack entre specs.

Risco previsto crítico pela captura persistida, replay, filas/identidade, importação e elegibilidade. ADR 0001 e checklist HTTP continuam obrigatórios. Revisão editorial não substitui revisão independente de lote; esta não substitui o gate cumulativo final SOL 6.1 xhigh, em contexto novo e somente leitura. Não foram executadas revisões ou testes de implementação nesta proposta; não coletar cota nem estimar tokens ausentes.

## Migration Plan

1. Publicar esta proposta e a #173, validar OpenSpec e solicitar grupos completos. Fechar #158/#160 somente após conferir seus merges/CI; manter #161 aberta.
2. Implementar e verificar os grupos autorizados em seus worktrees, preservando os formatos. Única migração adicional do teto é V15, conforme ajuste autorizado após #161; não editar V1–V14. Conteúdos continuam incrementais e separados do startup.
3. Disponibilizar API/frontend coordenados para teste local. Importar/habilitar a seleção exata deliberadamente, com backup, resultados e limites registrados, preservando dados existentes.
4. Obter teste do usuário, revisões de lote e autorizações próprias de PRs; depois executar gate cumulativo e solicitar autorização específica da integração `[SPEC]` para main. Sem merge, arquivamento ou publicação presumidos.
5. Recuperação usa backup privado e a imagem exata em destino novo conforme os guias atuais. Não iniciar aplicação anterior sobre práticas novas de 25 ou apagar/regravar capturas para compatibilizá-la. Publicação/carga na VPS exige entrega operacional posterior.
