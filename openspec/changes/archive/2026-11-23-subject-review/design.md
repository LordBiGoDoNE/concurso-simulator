# Design

## Context

`Topic` contém os 51 IDs dos módulos; cargos pertencem às classificações das questões,
não há uma matriz fixa de assuntos por cargo a inventar. `Attempt` conserva cargo,
classificação, respostas e correção, inclusive fontes parametrizadas. `AttemptCodec`
lê V5 e formato 2; `JpaAttemptStore` valida vínculos e usa REPEATABLE_READ. O histórico
atual ordena por criação e não fornece a janela por conclusão/cargo desta proposta.
O catálogo público não fornece links de aulas; `manifesto_v3.json` relaciona IDs aos
arquivos de conteúdo. A composição existente aceita assuntos, limite 1–100 e opção
numérica, com idempotência e insuficiência explícitas.

## Goals / Non-Goals

Usar o acervo e as tentativas existentes para indicar onde revisar e facilitar
estudo/prática. Preservar classificação histórica, posse, limites de consulta e
resultados reproduzíveis. Não criar uma nova taxonomia, afirmar domínio cognitivo,
agendar revisões, gravar um perfil derivado ou alterar a correção dos simulados.

## Decisions

### 1. Amostra e observação por base

Política proposta `subject-review/v1`: selecionar primeiro conta + cargo + estado
concluído, então ordenar por `finishedAt DESC, attemptId DESC` e limitar a 100.
Buscar um 101º cabeçalho apenas para `hasOlder`; não carregar seus itens. Janela,
contagens e itens vêm da mesma visão transacional; conclusão concorrente aparece
integralmente numa consulta posterior. Cada tentativa tem até 100 itens: limite
de materialização de 10.000 itens, sem varrer páginas de histórico pela aplicação.

Dentro da janela, escolher para cada `questionId` o item de maior ordem
`(finishedAt, attemptId)`; uma tentativa já impede bases repetidas internamente.
Fixa, revisão nova e variantes da mesma base compartilham essa identidade. Somente
depois agrupar pela classificação capturada da observação escolhida. Mudança do banco
atual não reclassifica resultados; se uma nova tentativa usa nova classificação,
essa nova evidência substitui a anterior quando for a mais recente da base.

O painel contém os 51 assuntos catalogados, separados por disciplina, e considera
apenas o cargo selecionado. Não mistura os dois cargos nem infere aplicabilidade por
prefixo do ID. Assunto sem evidência continua visível com estado sem dados.
Exposições brutas contam todos os itens da janela por sua classificação capturada;
não entram no denominador nem na prioridade. Informar que o resumo usa a última
observação por base, pode diminuir e não representa o histórico completo quando cortado.

### 2. Métricas e prioridade

Por assunto, `assessed = correct + wrong + blank` sobre bases retidas; percentual
`100 * correct / assessed`, duas casas, HALF_UP; `null` se assessed=0. Registrar
também exposições brutas e última conclusão das observações retidas (`null` sem elas).
No resumo de uma tentativa, todos os itens são distintos, sem janela histórica.

Priorizar somente assuntos com assessed>=3 e wrong+blank>0. Ordenar pela fração exata
`(wrong+blank)/assessed` decrescente, depois wrong+blank decrescente, depois topicId
ascendente. Não ordenar pelo percentual já arredondado. Exibir a justificativa e a
amostra, sem selo de domínio, previsão de aprovação ou pesos oficiais. Menos de três
bases mostra poucos dados e ainda permite estudar/praticar. Todos certos ou ausência
de candidatos gera lista de prioridades vazia, sem inventar uma dificuldade.

### 3. Fronteiras e persistência

Módulo `subjectreview`: objetos de valor/cálculo Java puros e UseCases relevantes de
consulta. Uma porta de aplicação recebe evidência mínima concluída: proprietário
verificado, cargo, IDs de tentativa/base, datas, classificação capturada e desfecho
correct/wrong/blank. Nunca recebe Spring/JPA/HTTP, gerador ou entidade persistente.

`mockexam` oferece contrato de aplicação para ler evidência concluída e verificar
posse/estado/expiração. Seu adaptador reutiliza codecs e critérios de integridade de
snapshots/itens/instâncias, retornando somente a projeção necessária. Adaptador de
revisão consome esse contrato; não chama repository concreto nem reproduz correção.
Sem dependência reversa de questionbank/parameterization no módulo novo e sem Service
que apenas repasse um endpoint técnico. Wiring e transação ficam na infraestrutura.

Consulta por conta/cargo/estado e ordenação ocorre antes do limite. SQL nativo é
permitido nesse adaptador quando justificado. Leitura de itens/instâncias é em lote,
sem consulta por item; testar limites de linhas e número de consultas. Não reutilizar
paginação por createdAt como se fosse a janela proposta. Falha de formato/vínculo
interrompe o resumo inteiro; nunca ignorar registro ou devolver um falso vazio.

Sem nova tabela/cache persistente, evento de conclusão ou regravação de JSON/hash.
Conferir plano sobre volume representativo. Se necessário, novo índice aditivo por
Flyway após V9 para proprietário/cargo/conclusão; justificar expressão/custo e testar
upgrade. Não alterar V1–V9, triggers, codecs de origem ou dados para facilitar consulta.

### 4. HTTP e proteção dos dados

Proposta de rotas:
- `GET /api/v1/me/subject-review?roleId=...`: conta autenticada, cargo obrigatório.
- `GET /api/v1/attempts/{id}/subject-review`: resultado de tentativa própria concluída,
  visitante ou conta, com as mesmas regras de posse e expiração dos resultados atuais.

Segurança verifica principal/rota/sessão antes do controller; posse/estado ficam no
núcleo. Entrada estrita: rejeitar parâmetros extras, repetidos, UUID/cargo inválidos
com 400; não aceitar owner/accountId, limites arbitrários ou cargo no resultado
individual. Conta ausente/inválida no painel recebe 401 JSON; tentativa inacessível
404 indistinguível; tentativa própria em andamento 409 sem dados corretivos.
Integridade interna retorna 500 genérico; indisponibilidade 503, sem payload privado.
O checklist HTTP deve conferir a tradução de exceções, não somente o status final.

DTOs concretos contêm política, cargo, metadados da janela (limite, quantidade,
hasOlder, datas extremas quando presentes) e métricas por assunto; sem IDs de contas,
itens/alternativas, gabaritos, resoluções, parâmetros ou digests. Resultado individual
identifica a tentativa já solicitada e não expõe a janela da conta. `no-store` em todos
os resultados/erros. GETs não criam progresso/tentativas nem exigem CSRF artificial;
novas criações seguem POST existente com CSRF. Sem dados privados em storage/cache/URL.

### 5. Interface e estudo orientado

Conta: seção Revisão por assunto, escolha explícita de cargo, dados da janela,
tabela acessível e prioridades explicadas. Visitante: decomposição somente dentro
do resultado concluído, sem painel agregado ou histórico permanente. Resultado da
conta também pode exibir essa decomposição sem carregar implicitamente seu histórico.
Estado sem dados, poucos dados, falha e carregamento são distintos. Atualização é
explícita e após conclusão confirmada; dados são coerentes com a janela da resposta,
não uma promessa de atualização em tempo real. Troca de cargo/conta, logout ou 401
cancela requisições e impede reaparecimento tardio de dados; novo login não adota visitante.

Rever aula resolve topicId por mapa versionado dos 51 campos `conteudo` do manifesto,
contra a base de material configurada; percent-encoding correto, sem inferir nomes,
traversal, esquema arbitrário ou campo gabarito. O mapa só contém IDs/paths públicos;
nenhum resultado entra no bundle. Conferir aulas/links/offline e preservar arquivos.

Praticar este assunto preenche compositor existente com cargo e um assunto de sua
disciplina, quantidade inicial 1 e variações false; demais disciplinas zeradas. Não
cria tentativa automaticamente. Usuário pode mudar critérios/quantidade/opção e
confirmar; geração só usa versões elegíveis e pode repetir bases/valores entre
tentativas. Se não houver acervo, preservar 409/contagens reais e escolhas, sem
trocar assunto/reduzir quantidade/fallback silencioso. Retry usa mesmo commandId para
os mesmos critérios e contexto; mudança de critérios inicia outro comando.
Não substituir realização ativa nem abandonar respostas locais sem a confirmação
existente; trocar contexto limpa preenchimento e comando. Teclado/foco/390px/1280px.

## Risks / Trade-offs

Janela de 100 e última base limitam custo e repetição, mas descartam observações
anteriores; UI deve explicar ambos. Três bases é heurística inicial, não evidência de
domínio. Um assunto pode ter pouca diversidade elegível: estudar continua disponível
e a prática não promete novidade. Snapshot preserva versões antigas mesmo retiradas.
Sem nova persistência, rollback conserva dados; eventual índice não autoriza remover
schema anterior nem voltar ao JAR V5 após escritas no formato 2.

## Open Questions

Nenhuma decisão de produto aberta. Proposta e políticas aprovadas pelo usuário em
2026-10-16. Grupos1–5 e6.1–6.3 integrados na épica da spec; correção do gate#122
integrada com autorização em b811fcb629169bf0f949df21af0f45a400ab5501.
Grupo6.4 consolida os registros do gate cumulativo no [SPEC] #121. SOL6.1xHigh
confirmou a resolução de SR-FINAL-SOL-001 na candidata; os registros finais também
exigem revalidação cumulativa. Evidências atuais em spec-review.json/md.
24/24 tarefas executadas/verificadas não autorizam integrar a spec na épica geral;
essa aprovação continua própria e pendente. Sem main,deploy ou arquivamento.
