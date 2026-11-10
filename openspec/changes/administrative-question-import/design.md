# Design

## Context

Planejamento local da [issue #160](https://github.com/LordBiGoDoNE/concurso-simulator/issues/160), sobre `main` `f61d57d5d55d6d61609815a2faa4b415ed8dca4b`. Motivação e decisões de produto confirmadas: [proposal.md](proposal.md). Requisitos: [importação](specs/administrative-question-import/spec.md) e [acesso administrativo](specs/administrative-access/spec.md). A etapa inicial foi de planejamento; os grupos 1–3 foram posteriormente autorizados para implementação local. Não houve revisão independente ou envio externo.

> **R1 histórica / R2 autorizada:** as seções 1–7 registram a entrega local dos grupos 1–3. A seção 8 define a extensão solicitada pelo usuário, autorizada nos grupos 4–6 e implementada/verificada localmente, substituindo o acesso por UUID externo. As verificações R1 não demonstram essa nova autenticação.

Evidência lida na base original:

| Área | Estado observado e consequência |
| --- | --- |
| `QuestionPackageReader`, `PracticePackageReader`, `LocalFiles` | Aceitam lotes fechados de 510/255 itens e fontes no disco. Não constituem leitor genérico de upload. |
| `PackageContentSafety`, `QuestionTextSafety` | Validam texto, links e PNG/JPEG/SVG com regras já refinadas. Reutilizar a política, substituindo apenas a obtenção de bytes por fronteira útil. |
| `PreviewQuestionCandidatesUseCase` | Classifica NEW/IDENTICAL/CHANGED/STALE; `QuestionInventory.read()` hoje lê o acervo inteiro. Prévia não reserva IDs. |
| `ApplyQuestionCandidatesUseCase`, `QuestionStore`, `JpaQuestionStore` | Aplicação atômica, travas ordenadas, identidade por origem, reutilização por fingerprint e recuperação de colisão conhecida após rollback. O adaptador abre `REQUIRES_NEW`; um wrapper transacional externo não engloba esse commit. |
| `Question`, `DecideQuestionUseCase` | Conteúdo novo em DRAFT; decisão editorial tem revisão/fingerprint/ponteiro/sequence próprios. Não conectar decisões à importação. |
| `UserPrincipal`, `GoogleLoginHandlers` | UUID interno e ROLE_USER, sem privilégio administrativo. Não armazenar e-mail nem alterar o callback para inferir permissão. |
| `SecurityConfiguration`, `PublicQuestionRoutes`, `SessionIndependentRoutesFilter` | Todo `/api/v1/question-bank/**` pertence à cadeia pública stateless/bypass de sessão. A escrita administrativa precisa de outro prefixo. |
| `V4__question_bank.sql` e migrações posteriores | Revisões imutáveis, unicidade de origem e elegibilidade protegidas; não há armazenamento de bytes de evidência ou recibo de importação. |

Referências: `docs/question-local-operations.md`, `docs/question-extraction.md`, `docs/question-editorial.md`, ADR 0001 e `docs/architecture/http-endpoint-review.md`. `openspec/specs/` está vazio; leram-se também os deltas históricos de banco de questões e identidade. Eles não serão reescritos para acomodar esta evolução.

## Goals / Non-Goals

**Goals:** preservar as invariantes atuais; receber material genérico verificável sem filesystem do cliente; tornar prévia, consentimento técnico e commit observáveis; manter autenticação/CSRF na infraestrutura e regras/coordenação no núcleo Java puro.

**Non-Goals:** converter formatos antigos pela API; criar gestão de outros administradores, autorização por e-mail, editor visual, jobs, scraper, endpoint de download de fontes, publicação de assets ou fluxo editorial HTTP. Não se promete que uma aprovação local futura já distribui os novos recursos: essa evolução precisa de desenho próprio.

## Decisions

### 1. Acesso R1 por UUID autorizado externamente (estado ainda implementado)

Usar sessão Google existente e lista externa `app.question-import.allowed-user-ids`, vazia por padrão. A concessão/revogação é alteração operacional de configuração seguida de reinício; a política consulta a lista ativa por requisição, sem autoridade administrativa embutida na sessão. O operador obtém seu próprio UUID pelo `/me` existente. Configuração com UUID inválido falha explicitamente; não existe fallback por e-mail, domínio Google ou primeira conta cadastrada.

Destino é um identificador lógico externo `app.question-import.destination-id`, diferente por banco/ambiente e trocado ao substituir/restaurar o destino lógico. Não expor URL JDBC, schema privado ou credenciais. Com lista não vazia, destino e chave de confirmação ausentes/inválidos impedem startup; com lista vazia, a funcionalidade fica negada sem exigir segredo administrativo.

Segurança recebe principal verificado e decide acesso antes de ler o pacote. Controllers não repetem autenticação/CSRF, não acessam sessão ou JPA e não aceitam ator/instante autodeclarados. Consomem portas Java puras para leitura e confirmação, implementadas na infraestrutura; indisponibilidade do banco de sessões é tratada antes do MVC por filtro limitado ao prefixo administrativo. Relógio e UUID do principal alimentam a coordenação. Manter `/me`, OIDC, logout e as permissões de estudo intactos. Alternativas descartadas: promover qualquer usuário Google, criar senha administrativa ou tabela de papéis sem necessidade nesta versão.

### 2. Formato e limites propostos

Contrato JSON tipado, a documentar em `backend/openapi.yaml` no grupo HTTP, sem criar DTOs agora:

| Objeto | Campos aceitos |
| --- | --- |
| Prévia | `package`, `keys` |
| Aplicação | `package`, `keys`, `confirmationToken` |
| Pacote | `format=administrative-question-package/v1`, `purpose`, `items`, `files` |
| Item | `origin` (`namespace`, `key`), `content` no formato estrutural de `QuestionContent`, `fingerprint`, `expectedLatestRevision` explícito, UUID ou null |
| Arquivo | `path`, `mediaType`, `sha256`, `base64` dos bytes originais |

Cada pacote declara `purpose=SIMULADO|PRATICA_AULA`. SIMULADO usa `admin-question-v1`; PRATICA_AULA usa `lesson-practice-v1`, com chaves novas obrigatoriamente prefixadas por `admin.`. Chaves ASCII têm até 100 caracteres. Cada origem mantém finalidade imutável, assunto e cargos do catálogo existente; os inventários históricos são preservados. O teto de 100 itens é por pacote, sem limitar o acervo acumulado nem alterar o limite de cinco questões por treino. Esses detalhes e números são propostas técnicas submetidas à aprovação da spec, não respostas atribuídas ao usuário.

| Limite | Valor proposto |
| --- | --- |
| Corpo HTTP integral, inclusive confirmação | 16 MiB = 16.777.216 bytes; limitar stream, não confiar em Content-Length |
| Itens / chaves selecionadas | 1–100 / 1–100; sem repetição, seleção deve ser subconjunto dos itens |
| Arquivos / bytes decodificados | até 100 / 1 MiB por arquivo / 8 MiB somados |
| JSON | profundidade até 32; proibir campos/nomes duplicados, tokens finais, coerções de tipos e Unicode inválido |
| Coleções por item | 2–10 alternativas, 1–20 referências, até 8 recursos por bloco textual; demais limites dos VOs vigentes |
| Confirmação | 15 minutos; token até 4 KiB; sem renovação silenciosa |
| Concorrência operacional | uma requisição de importação/prévia processando por instância; excesso 429 com Retry-After; sem fila de payloads |

São tetos de aceitação, não capacidade ou latência comprovadas. Base64 é padrão com padding, sem whitespace, decodificação estrita e contagem antes de alocar. JSON somente UTF-8, Content-Type application/json, sem Content-Encoding comprimido. Arquivos fonte: `sources/<caminho-relativo>` com `text/plain` ou `application/json` UTF-8; são evidência inerte, não conteúdo executável. Recursos: `assets/<caminho-relativo>` com PNG/JPEG/SVG e mesma política de segurança existente. Não aceitar arquivos não referenciados por algum item.

Proveniência aponta aos dois arquivos fonte (podem coincidir), com hashes reais. Recursos devem existir no manifesto e conferir tipo/hash/descrição. Validar todos os itens/arquivos, mesmo os não selecionados, para não produzir um digest confirmável sobre partes desconhecidas. Não extrair arquivos em diretório temporário nem procurar caminhos/URLs no host. Caminhos são chaves lógicas canônicas; compartilham a validação dos VOs, sem relaxar o leitor local antigo. Fonte é evidência, não prova semântica de correção/autoria.

### 3. Reutilização sem acoplar entrada HTTP ao launcher

Adicionar leitor genérico em adaptador próprio de pacote, sem iniciar `QuestionBankLocal` ou registrar sua configuração nonweb no web startup. Extrair uma interface de resolução de bytes útil à validação comum: adaptador local conserva allowlist/disco/limites antigos; adaptador de pacote resolve exclusivamente bytes incorporados já limitados. Domínio conserva fingerprint e invariantes. A gramática de segurança de recursos não deve ser duplicada entre leitores.

Para a prévia HTTP, acrescentar consulta de inventário restrita às até 100 origens selecionadas, mantendo `read()` para relatórios locais. A regra de classificação existente continua na aplicação. Suspeita de duplicidade nesta versão: igualdade exata de texto de enunciado entre origens diferentes no pacote; retornar pares de chaves como aviso. Não consultar semanticamente todo o acervo nem prometer detecção global/fusão.

### 4. Digest semântico completo e confirmação sem persistência

O digest do pacote é SHA-256 de um enquadramento binário versionado: marcador `administrative-question-package/v1`; finalidade; contagem de itens ordenados por namespace/chave; para cada item, origem, fingerprint recalculado e UUID-base com presença explícita; contagem de arquivos ordenados por caminho; para cada arquivo, caminho, mediaType, SHA-256 recalculado e comprimento dos bytes. Textos são UTF-8 com comprimento de 32 bits big-endian, contagens idem, presença como byte 0/1. UUIDs usam representação textual canônica; não normalizar Unicode/conteúdo. Fingerprint do conteúdo permanece o `question-content/v1` vigente, não JSON serializado. Rejeitar campos desconhecidos impede semântica fora do enquadramento.

Seleção: digest próprio sobre chaves ordenadas, únicas e enquadradas com marcador `question-import-selection/v1`. Arquivos de itens não selecionados também integram o digest do pacote; sua alteração invalida confirmação. Ordem de itens/arquivos/cargos/chaves é semântica de conjunto; ordem de alternativas/referências/recursos é editorial e afeta o fingerprint. Variação de espaços e ordem das propriedades JSON não muda identidade. Hash de cada fonte continua sobre bytes exatos, sem equivalência textual.

`POST /api/v1/admin/question-imports/preview` valida integralmente, consulta inventário sem escrita e retorna relatório tipado, avisos, `packageDigest`, `selectionDigest`, `destinationId`, `confirmable`, `expiresAt` e `confirmationToken` (null para STALE). Não guarda bytes nem linha de prévia; metadados sanitizados de diagnóstico podem ser emitidos em logs, nunca payloads. IDs observados não são reservas.

Confirmação é envelope pequeno assinado por HMAC-SHA-256 em adaptador de infraestrutura, usando chave externa aleatória de pelo menos 256 bits. Contém versão, nonce aleatório, ator UUID, destino, digests, emissão e expiração. Verificar tamanho, formato, assinatura com comparação constante, ator/destino e relógio; chave diferente invalida confirmações pendentes e exige nova prévia. Não há chave no repositório. O envelope vincula os fingerprints/bases por inclusão no digest do pacote; não precisa carregar conteúdo/100 chaves no token. Isso é confirmação de operação, não token de autenticação ou CSRF; ambos continuam responsabilidade do framework. Alternativa de gravar um ticket na prévia foi rejeitada para cumprir ausência de persistência.

### 5. Uma unidade para questões, evidência e recibo

`POST /api/v1/admin/question-imports/apply` revalida pacote/seleção e a confirmação antes da escrita. Reutilizar a lógica de candidatos; o caso de uso coordena também evidência/recibo por uma fronteira transacional específica de importação. Não anotar controller nem envolver o `execute` atual num wrapper que comita questões antes do recibo.

Refinar a coordenação de `ApplyQuestionCandidatesUseCase` para compartilhar a aplicação de candidatos dentro da MESMA sessão transacional usada por essa ação. Preservar a entrada local atual. A porta pode ser estendida por um contrato de sessão de importação com operações necessárias a recibos/evidência; sem expor Spring/JPA/SQL ou criar Service só de repasse. Persistência e wiring permanecem na infraestrutura. Revisão arquitetural deve conferir esse ponto explicitamente.

Dentro da unidade: procurar recibo pelo nonce; se existente, conferir vínculo integral e devolver resultado original; caso ausente, obter travas de origem em ordem estável, reutilizar conteúdo idêntico ou conferir base e criar DRAFT; gravar bytes referenciados pela seleção e manifesto privado; gravar recibo com ator, horário do servidor, destino, seleção, digests e resultados reais; retornar somente depois do commit. Nunca chamar `DecideQuestionUseCase`.

Reaplicação idêntica segue a regra vigente: pode reutilizar revisão histórica mesmo se a última base mudou, sem alterar nenhum estado/ponteiro. Concorrência idêntica pode mudar NEW para reutilização no resultado real; conflito de conteúdo novo com base desatualizada reverte toda a seleção. Repetição de nonce concluído retorna IDs e instante originais, não afirma que ainda sejam a última revisão. Verificar autorização, confirmação e validade também no replay. Após expiração, gerar nova prévia permite idempotência por conteúdo.

Criar migrações novas (número livre conferido na implementação): recibo único por nonce e destino, manifesto vinculado ao recibo/revisões e blobs privados por hash com verificação de igualdade de bytes para colisão. FK e constraints protegem coerência. Evidência e metadados são imutáveis e sem exclusão automática nesta versão; documentar crescimento e backup. Armazenar só arquivos alcançáveis pelos itens selecionados, sem duplicar blobs idênticos. Sem volume público, filesystem ou endpoint que sirva esses bytes. Não armazenar token bruto ou pacote integral contendo itens não selecionados.

Colisão conhecida de origem/nonce/blob: terminar rollback e somente então, em nova unidade, conferir identidade exata e recuperar resultado/repetir uma vez. Corrida de nonce exige resultado consistente após o concorrente completar; falha desconhecida, FK, indisponibilidade ou timeout não são idempotência. Preservar timeout transacional de 15 s, sem retry infinito ou promoção a job. Desconexão HTTP pode ocorrer após commit: retry resolve pelo recibo; não prometer cancelamento de transação já concluída.

### 6. Contrato de erros e responsabilidades

| Resposta | Condição / código proposto |
| --- | --- |
| 200 | Prévia válida, inclusive STALE informativo; aplicação/replay após commit confirmado |
| 400 | JSON malformado, campo de requisição ausente ou tipo inválido (`INVALID_REQUEST`) |
| 401 / 403 | Autenticação / permissão, CSRF ou origem recusados pela cadeia real; preservar convenções de segurança existentes |
| 409 | `CONFIRMATION_MISMATCH`, `CONFIRMATION_EXPIRED` ou `STALE_BASE`; sem confirmação inválida renovada automaticamente |
| 413 / 415 | `REQUEST_TOO_LARGE` / `UNSUPPORTED_MEDIA_TYPE` ou `UNSUPPORTED_CONTENT_ENCODING` |
| 422 | `INVALID_PACKAGE`, com diagnósticos finitos de código/chave validada ou índice; inclui formato, limites internos, hash, proveniência e conteúdo |
| 429 | `IMPORT_BUSY`, capacidade por instância ocupada, sem reservar pacote |
| 503 / 500 | Falha transitória conhecida de banco/timeout / falha inesperada; sem alegar commit nem converter em conflito conhecido |

JSON sintaticamente inválido (inclusive nomes duplicados) e envelope HTTP inválido recebem 400; pacote sintaticamente válido com estrutura/conteúdo não aceitos recebe 422. Não classificar por texto livre de exceção. O identificador de correlação descrito abaixo vale para os novos erros da aplicação; os contratos de segurança existentes permanecem próprios.

Erros de aplicação retornam DTO concreto com código/correlationId; INVALID_PACKAGE é deliberadamente genérico nesta versão e exige conferência privada do pacote original, nunca mensagem livre de parser/exceção. Segurança mantém seus DTOs e 401/403; não alterar contratos globais para uniformizar este recurso. Respostas de prévia/aplicação não incluem enunciado, resposta, resolução ou Base64; mostram metadados suficientes para o operador conferir seu pacote original. `Cache-Control: no-store` também nos erros administrativos.

| Responsabilidade | Proprietário e evidência futura |
| --- | --- |
| Sessão/principal, autorização técnica, CSRF, CORS | Spring Security/infraestrutura; testes pela cadeia real, inclusive Google desativado, principal inválido, expiração e revogação |
| Limite de corpo, tipos, códigos HTTP, no-store | Entrada web/infraestrutura; stream com/sem Content-Length, conteúdo comprimido e logs com sentinelas |
| Parser/assinatura/bytes de recursos | Adaptadores; fixtures positivas e negativas e vetores de digest/assinatura |
| Seleção, vínculo de confirmação, candidatos, recibo e regras de conflito | Aplicação Java pura com portas substituíveis; testes sem framework |
| Conteúdo, revisão e transições | Domínio vigente; regressões de idempotência e nenhuma chamada editorial |
| Travas, rollback, unicidade, evidência e commit | Infraestrutura JPA/Flyway; PostgreSQL 18 isolado com concorrência controlada |

### 7. Dependências, coordenação e integração futura

| Relação | Situação / responsável |
| --- | --- |
| #160 | Única issue desta spec; grupos 1–3 autorizados localmente em 2026-11-09; envio/revisão/integração pendentes. Não criar outra issue. |
| #158 / #161 | Não dependem deste planejamento e suas stacks não são importadas. Nenhum arquivo de produto delas foi alterado. |
| #156 | Relação futura de acervo; não é dependência de implementação nem autorização para ampliar catálogo/editorial. |
| Banco de questões/identidade em main | Pré-requisitos existentes lidos; não presumir que testes históricos passaram nesta máquina. |
| Segurança, OpenAPI, migrações, configuração transversal | Mudanças futuras pertencem à #160 apenas após divisão registrada pela central; coordenar números de migração, arquivos e ordem de integração antes de implementar. |

Grupos completos: **1** pacote/digests/consulta limitada; **2** aplicação atômica/evidência/recibo; **3** acesso/HTTP/prévia-confirmação/aplicação. Dependências reais 1 → 2 → 3; testes/docs acompanham cada grupo. A spec usa futura `epic/administrative-question-import`, tarefas `task/administrative-question-import/<assunto>` e Stacks somente conforme regras atuais, preservando refs e worktrees. Nenhuma branch foi criada: a implementação local sem commits permanece neste worktree do aplicativo, em HEAD destacado. A separação em branches/PRs será realizada apenas quando commits/envio forem autorizados, preservando este estado. Cada grupo pode gerar PRs coesos revisáveis sem deixar subtarefas do grupo para outro lote.

Antes de execução paralela/integração, a central precisa registrar na #160 áreas, contratos compartilhados, responsável, base atual e ordem. O vínculo atual é local por links nesta proposta/tarefas: nenhum comentário, arquivo ou relatório foi enviado ao GitHub nesta rodada. Publicar backlinks para estes artefatos depende de envio autorizado; não usar URLs inexistentes de main.

### 8. R2 — primeiro ADM, senha permanente e vínculo Google (implementada/verificada localmente)

**Decisões confirmadas:** o usuário pediu cadastro ADM na primeira inicialização, senha inicial gerada quando ausente, manutenção de senha e Google como meios de login e preservação da conta Google existente. Se o ADM local já tiver histórico, bloquear a reatribuição para outra conta; nenhuma mesclagem automática é autorizada. O objetivo é eliminar a edição/reinício para configurar UUID. A configuração do cliente OAuth Google continua externa; habilitar ou trocar esse cliente pode exigir reinício, independentemente da permissão ADM.

#### 8.1. Cadastro e credencial

Proposta técnica: cadastro inicial único, marcador de conclusão persistente, credencial local com proprietário `app_user.id`, permissão ADM e versão de credencial. Novas tabelas/migrações aditivas, com número livre conferido pela central; não reescrever V1–V12. A permissão administrativa não remove as regras de posse dos simulados/práticas nem dá acesso ao histórico de terceiros.

Runner de infraestrutura, após Flyway, chama uma ação relevante do núcleo `identity`. O núcleo coordena cadastro/concorrência por porta; a geração criptográfica, `PasswordEncoder`, armazenamento, console e sessão ficam em adaptadores. Nunca inserir senha/usuário por Flyway nem reativar o usuário padrão em memória do Spring. Preservar `UserPrincipal(UUID)` e o JSON do `/me`.

Configuração inicial proposta: `app.admin.bootstrap.username` (padrão `admin`) e `app.admin.bootstrap.password` opcional. Nome local único e estável, sem depender de e-mail Google. Se ausente, gerar 32 bytes aleatórios e codificar Base64 URL-safe, guardar apenas hash e marcar troca obrigatória. A senha temporária só é mostrada pela instância que confirmou o commit, uma vez, no console restrito do operador. Esse console é um canal de credencial: não publicar sua saída em issue/PR/CI. Testes fornecem credenciais sintéticas ou capturam o canal privado; logs HTTP e relatórios nunca recebem a senha.

Usar encoder adaptativo do Spring por porta, com sal aleatório e formato versionado. Proposta: PBKDF2-HMAC-SHA256 com 600.000 iterações, sal de 16 bytes e derivação de 256 bits, implementado pelo encoder do framework, sem algoritmo caseiro ou dependência nova para hashing. Senha escolhida: 15–128 caracteres, sem normalização, truncamento ou composição artificial; corpo dos novos pedidos limitado a 8 KiB. Esses números são parâmetros técnicos propostos, sujeitos aos testes de custo antes da entrega.

A senha fornecida externamente serve só ao cadastro inicial. Após conclusão persistida, valores de bootstrap alterados/ausentes não sobrescrevem nome, senha, permissão ou vínculo. Estado desabilitado não significa cadastro novo. Duas instâncias disputam o mesmo registro/constraint e apenas a vencedora emite uma senha gerada. Queda após commit antes da emissão exige recuperação local, sem regeneração no próximo startup.

Antes da troca inicial, a sessão local só pode trocar a senha ou sair. A troca normal requer a senha atual. Recuperação deliberada pelo operador local emite outra senha temporária, exige confirmação, preserva Google/histórico e invalida acessos administrativos antigos; não criar recuperação pública por e-mail/HTTP nesta extensão. Não existe cadastro público de ADM ou gestão de outros administradores.

#### 8.2. HTTP, sessão e interface mínima

Contratos novos propostos (nenhum endpoint/DTO criado nesta rodada):

| Operação | Entrada e resultado |
| --- | --- |
| `POST /api/v1/admin/auth/login` | Nome/senha por JSON; 204 depois de autenticar/rotacionar a sessão; 401 genérico em falha, 429 por excesso de tentativas. Processamento pelo mecanismo de autenticação do Spring, não pelo controller. CSRF do framework permanece obrigatório. |
| `GET /api/v1/admin/account` | Estado privado tipado: nome local, troca pendente e Google vinculado. Não devolver hash, senha, tokens ou perfil Google. |
| `POST /api/v1/admin/account/password` | Senha atual/nova, autenticação e CSRF; 204 após commit e atualização da sessão. |
| `POST /api/v1/admin/account/google-link` | Confirmar senha local, criar intenção de vínculo por cinco minutos e indicar início Google em destino fixo. 409 se Google estiver desativado ou outra identidade já estiver vinculada. |
| Callback Google existente | Distinguir login comum e vínculo por intenção armazenada no servidor, vinculada ao state/sessão/versão da credencial; não aceitar intenção, UUID alvo ou retorno livre do cliente. |

Spring Security/infraestrutura cuidam de autenticação, CSRF, CORS, rotação e evidência de autenticação recente. Login só por senha do ADM também precisa produzir o principal interno mínimo verificado. Não aceitar `UserDetails` com hash dentro da sessão pública nem criar tokens CSRF manualmente. Ao trocar credencial/vínculo, invalidar acessos administrativos antigos por versão persistente conferida em cada ação, além da invalidação de sessões pelo framework; preservar sessão nova com estado atual. Sessões Google anteriores à concessão precisam de nova autenticação para ganhar acesso ADM, sem perder seus dados de estudo.

Proteção inicial proposta: cinco falhas por credencial em quinze minutos geram bloqueio de quinze minutos, persistente entre instâncias, mais limite técnico por origem de rede. Nome desconhecido e senha inválida recebem a mesma resposta e trabalho de hash comparável. Não confiar em X-Forwarded-For de cliente não reconhecido. Rejeições e falhas de banco/sessão usam JSON/no-store, sem imprimir credenciais ou confundir indisponibilidade com visitante.

Interface: entrada ADM dedicada e acessível, troca inicial/normal e botão de vínculo Google. Reutilizar os mecanismos de sessão/rechecagem de `LoginPanel`, `identity-api` e `App`; o login local respeita o aviso existente sobre atividades de visitante. Reconsultar `/me` e limpar estado privado ao trocar de UUID. Sem formulário de upload, painel editorial, dados pessoais Google ou reformulação da navegação de estudo. A seleção da conta ocorre no Google; a interface explica que uma conta existente será preservada e apresenta conflito sem sugerir apagar dados.

O usuário delimitou explicitamente LoginPanel, identity-api e integração de identidade em App para esta entrega no checkout atual. A conciliação com #158 permanece com a central; não importar sua stack ou modificar seu checkout.

#### 8.3. Vínculo e preservação de contas

Autenticar explicitamente ambas as pontas: sessão ADM, confirmação da senha local e fluxo Google OIDC completo, usando issuer + subject verificados. Nenhuma promoção por e-mail, nome, domínio ou configuração de client-id. Uma identidade Google por cadastro administrativo nesta versão; troca/desvínculo de Google ficam fora do escopo. Senha local permanece utilizável com Google fora do ar.

| Google autenticado | Comportamento atômico |
| --- | --- |
| Sem vínculo na aplicação | Vincular a identidade ao UUID ADM atual. Manter histórico local, mesmo se existente; não criar outra conta. |
| Já vinculado ao próprio UUID ADM | Confirmar estado sem duplicar identidade ou permissão; não reinterpretar replay inválido do callback como novo login. |
| Vinculado a outro UUID, ADM local sem histórico | Preservar a conta Google e seu UUID. Mover somente propriedade da credencial local e permissão ADM; retirar acesso separado da conta local substituída e rotacionar a sessão para o UUID Google. Manter a linha antiga inativa, sem apagar contas. |
| Vinculado a outro UUID, ADM local com histórico | 409 `LOCAL_ACCOUNT_HAS_HISTORY`; não transferir credencial, permissão, propriedade ou dados. Não fundir contas nem escolher silenciosamente qual histórico manter. |

**Histórico que bloqueia:** tentativas de simulado/prática abertas ou encerradas, comandos próprios e recibos de importação do UUID local. Sessão técnica, cadastro/credencial iniciais e eventos de autenticação não contam como histórico de estudo/importação. Recibos têm ator imutável e também precisam ser preservados; por isso, preferir concluir o vínculo antes de importar quando houver conta Google existente.

O caso de uso no núcleo `identity` confere a origem, destino, intenção, versão da credencial e histórico dentro da mesma unidade atômica. O adaptador observa histórico por fronteira de leitura útil, sem o núcleo depender de `mockexam`, `questionbank`, JPA ou HTTP. Não usar o caminho de login comum para criar um terceiro usuário temporário e depois mesclá-lo.

Uma barreira transacional por conta serializa vínculo e novas gravações autenticadas. O adaptador usa explicitamente READ COMMITTED para que a leitura de histórico após esperar a trava veja a gravação vencedora, mesmo se o default do datasource for mais forte. Marcar a fonte substituída como inativa e impedir gravações posteriores para ela em tentativas/recibos, inclusive requisições que autenticaram antes do vínculo. Preservar snapshots e atores antigos: não reescrever `me_attempt`, `me_command` ou recibos para o UUID Google. Se uma gravação vencer, o vínculo encontra histórico e é recusado; se o vínculo vencer, a gravação antiga falha com rollback. Testar ambos os ordenamentos em PostgreSQL real, sem criar dependência reversa entre núcleos.

Concorrência de login comum com vínculo deve respeitar `external_identity_key`. Conflito conhecido só é recuperado depois do rollback, com nova leitura e revalidação integral. Falha técnica, sessão/versão alterada, outra conta Google ou tentativa expirada não são idempotência. Cancelamento não altera vínculo; não restaurar uma sessão cuja credencial/permissão já foi invalidada.

Referências consultadas para esta proposta técnica: [armazenamento de senhas no Spring Security](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html), [parâmetros PBKDF2 da OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#pbkdf2) e [vínculo federado de contas](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html#secure-federated-account-linking). A escolha do encoder não declara certificação FIPS ou implementação já validada.

#### 8.4. Integração, compatibilidade e sequência

A interface técnica da permissão precisa de uma porta de leitura na fronteira útil do consumidor; a implementação consulta o cadastro administrativo vigente. O núcleo de questões continua independente da identidade. A lista R1 de UUIDs deixa de autorizar: não manter dois mecanismos como fallback. Documentar migração de configuração legada e recusar configuração conflitante em vez de conceder privilégios silenciosamente. Clientes/segredos OAuth, destino lógico e chave de confirmação de importação continuam externos; não são senha ADM. Ausência de destino/chave mantém importação recusada com 503 `IMPORT_NOT_CONFIGURED`, sem impedir cadastro/login ADM; configuração parcial/inválida ou legada conflitante é diagnosticada sem expor valores. A disponibilidade de importação aparece somente na descoberta privada administrativa.

O ADR 0001 e a spec histórica de Google não previam senha própria/vínculo. Propor uma decisão arquitetural adicional, limitada à credencial administrativa e ao ciclo de contas, preservando as fronteiras Java puro/JPA e o histórico aprovado. Não editar AGENTS.md ou config.yaml nesta revisão. O usuário aprovou a extensão e autorizou os grupos 4–6 completos em 2026-11-09. A decisão complementar está no ADR 0002.

Dependências reais: grupo 4 usa a identidade existente; grupo 5 depende de 4 e da evidência/histórico do grupo 2; grupo 6 depende de 3, 4 e 5. A ordem operacional proposta é 4 → 5 → 6. Não iniciar lote dependente sem pré-requisitos solicitados/disponíveis, nem fabricar dependência Git entre tarefas independentes. Preservar a entrega R1; nenhuma revisão/teste antigo certifica R2.

## Risks / Trade-offs

- A extensão R2 inclui senha, bootstrap e vínculo de contas, com novas fronteiras de concorrência e de sessão. O console inicial contém credencial temporária; sua saída é privada. O vínculo com conta Google existente será bloqueado se houver uso local anterior, sem migração automática.
- **Risco crítico previsto**, conforme `docs/architecture/spec-review.md`: autorização/CSRF, conteúdo não confiável, schema, transações, idempotência e privacidade interagem. → Revisão futura cobre conjunto e regressões por fronteira; classificação não dispara revisor nesta rodada.
- Bytes e parser podem consumir memória/CPU; Base64 amplia tamanho. → Tetos de stream/coleções/bytes e uma execução por instância, com medição de memória/tempo por fixtures máximas; se inviável, ajustar a proposta antes de ampliar limites ou introduzir jobs.
- Validação integral pode recusar seleção boa por outro item inválido. → Relatório sanitizado e pacote corrigido/menor; não importar prefixo silenciosamente.
- Texto e fontes estruturais não provam correção, licença ou autoria. → DRAFT obrigatório; referência institucional não equivale a autoria; processo editorial permanece separado.
- Evidências privadas crescem e podem conter material sensível. → Não logar nem servir bytes; persistência deduplicada e backup restrito, sem política destrutiva inventada. Operador continua responsável pelo material enviado.
- Atualização da chave, destino ou relógio invalida confirmação pendente. → Erro explícito e nova prévia; nenhuma reconfirmação automática.
- Assets incorporados não passam a existir na distribuição estática. → Este incremento entrega rascunhos/evidência. Não aprovar material real esperando publicação de recursos por esta API.
- CI/gate estrutural não demonstra revisão semântica. → Testar cenários reais e limites; `spec-review-tests` não certifica integração cumulativa.

## Migration Plan

Autorização local recebida para grupos 1–3: base/HEAD conferidos em f61d57d; implementar grupos escolhidos com migrações aditivas, sem seed; verificar compatibilidade sobre dados existentes em PostgreSQL isolado. Nenhuma alteração de V1–V11, `ddl-auto=validate` e `open-in-view=false` preservados. Rollback operacional inicial: esvaziar lista de permissão/reiniciar; manter novas tabelas/recibos/evidências e dados anteriores, sem down migration destrutiva.

Testar localmente com fixtures e recursos exclusivos por conversa; sem conexão à VPS, carga real ou publicação. A instrução desta rodada não autoriza conteúdo real nem aprovação editorial. Revisão por modelo somente após testes/validação do usuário e envio autorizados, com SOL `openai/gpt-6.1-sol#xhigh` confirmado, contexto novo e somente leitura. Medir cota antes/depois conforme `docs/review-quota.md`/`scripts/review_quota.py`, sem geração concorrente do coordenador e sem publicar observações privadas. Revisão de lote não substitui gate cumulativo final, `validate_report` explícito, CI e autorização própria de merge. Nenhuma revisão, cota, token ou aprovação é simulada neste planejamento.

R2 (autorizada): migrações posteriores à V12, cadastro inicial após schema válido e autorização da implementação; comparar usuários/identidades/sessões/históricos existentes antes/depois. Alterações de credencial/vínculo não reescrevem dados. Inativação da fonte precisa impedir gravações tardias. Rollback operacional deve preservar todas as tabelas/identidades; não voltar ao binário R1 como forma de recuperar acesso por senha nem restaurar grants por configuração sem plano e autorização específicos. O relatório verification.md cobre apenas R1 e permanece histórico; verification-r2.md registra as execuções atuais.
