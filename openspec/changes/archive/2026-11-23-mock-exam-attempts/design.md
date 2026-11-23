# Design

## Context

Ver motivação/escopo em `proposal.md`. Inspeção do código em `28932ed`:

- `Question` preserva revisões e o ponteiro elegível; `QuestionContent` contém resposta,
  resolução, recursos e fingerprint. `EligibleQuestions` projeta apenas conteúdo público;
  não fornece material de correção nem sorteio. `QuestionInventory` não traz o payload.
- `JpaEligibleQuestions` abre transação própria de leitura. Sua API pública não deve ser
  usada para montar uma tentativa em várias consultas independentes.
- `UserPrincipal` contém somente UUID de `app_user`; Spring Session/CSRF já existem.
  `PublicQuestionRoutes` e o bypass de sessão pertencem à consulta stateless do banco.
- React oferece login, consulta de questões e link independente às aulas. Não há fluxo
  de tentativa, persistência de respostas ou histórico. Não há specs consolidadas;
  identidade e question-bank permanecem em changes não arquivadas.

Decisão do usuário: visitante temporário e conta com salvar/retomar. Limites de 100
questões, validade visitante de 24h e pontuação uniforme são propostas desta spec,
ainda sujeitas à sua aprovação, sem alegar formato oficial do concurso.

## Goals / Non-Goals

**Goals:** integridade da tentativa capturada, correção reproduzível, posse no núcleo,
retomada confirmada da conta, visitante temporário e fronteiras HTTP explícitas.

**Non-Goals:** motor genérico de provas, nova identidade visitante de negócio, nova
autenticação, pesos por edital, recomendação por conceito, geração parametrizada,
adoção/mescla de tentativa visitante ou fila de sincronização offline.

## Decisions

### 1. Núcleo por funcionalidade e contratos úteis

Módulo `mockexam` com agregado `Attempt`, itens/respostas imutáveis e transições
`IN_PROGRESS` → `FINISHED`. Objetos de valor validam composição/alternativas/posse;
UseCases coordenam criar, responder, concluir, consultar e listar. Relógio e sorteio
substituíveis permitem testes determinísticos. Sem Spring/JPA/HTTP no núcleo.

Portas de unidade transacional, persistência e seleção/obtenção de versões são úteis;
não criar Service que apenas delegue. O banco expõe contrato interno mínimo para
obter revisões elegíveis completas; adaptador de `mockexam` traduz para seu snapshot.
Domínio do banco não depende do módulo novo, nem se transforma DTO/JPA em entidade
compartilhada. Evita acesso direto de controller/UseCase a SQL ou repository concreto.

### 2. Captura atômica e seleção

Uma unidade transacional de infraestrutura lê o pool elegível com snapshot consistente,
sorteia sem reposição, captura os itens e grava a tentativa/comando de criação.
Critérios: cargo obrigatório; mapa de disciplina/quantidade; assuntos opcionais por
disciplina com união dentro de cada conjunto e AND com disciplina/cargo. Limite total
1–100. Erro de insuficiência retorna a disciplina/quantidade disponível; nada parcial.

O ponto de elegibilidade é a leitura transacional: retirada confirmada antes dela
exclui a revisão; retirada posterior não modifica itens capturados. Não chamar a
projeção pública com `REQUIRES_NEW` por item nem usar `ORDER BY random()` sem preservar
o resultado. Sorteio uniforme sem reposição e Fisher–Yates nas ordens, com política
`uniform-v1`; persistir ordens finais é obrigatório, seed sozinho não reproduz o histórico.

Cada item preserva questionId/revisionId/fingerprint, conteúdo/alternativas/fontes,
resposta/resolução privadas, ordem e parâmetros `{}`. Revisões existentes permanecem
intactas; não construir gerador fictício para preencher parâmetros da futura etapa 5.

### 3. Persistência e concorrência

Flyway aditivo após V4, Hibernate validate/OSIV false. Tabelas para tentativa, itens
capturados e comandos idempotentes. Proprietário é conta (FK `app_user`) ou UUID
temporário atribuído pela infraestrutura de sessão, exclusivos entre si; identidade
de conta nunca vem do JSON. `@Version` permanece infraestrutura, versão esperada é
contrato do comando. Respostas aceitam apenas IDs pertencentes ao item; null limpa.

Operação identifica owner/attempt/commandId e hash do payload canônico. Mesmo comando
recupera resultado; outro payload com mesma chave retorna 409. Criação repetida
recupera a seleção já gravada. Atualizações distintas usam compare-and-set; perdedor
não sobrescreve nem recebe sucesso fictício. Conclusão grava estado/resultado e comando
num único commit, com correção uniforme-v1: acerto=1, demais=0, percentual em duas casas.
Retry idempotente de conclusão recupera o resultado mesmo com versão esperada antiga.
Conflitos conhecidos são traduzidos depois do rollback; falhas de FK/indisponibilidade
não viram conflito esperado. Clock do servidor define criação/conclusão/expiração.

### 4. Posse, visitante e sessão

Infraestrutura resolve ator tipado a partir de `UserPrincipal` verificado ou atributo
aleatório da sessão visitante; não expõe cookie/UUID da sessão no domínio/JSON. Não
cria `app_user` para visitante. Domínio/aplicação verificam owner em todas as ações.
O resolvedor técnico não concede acesso ao histórico privado nem aceita owner do cliente.

Visitante: dados temporários no servidor, expiração fixa de 24h desde a criação,
acesso também dependente da sessão válida (padrão atual 30min de inatividade). Não
prometer retomada após fechar navegador/expirar sessão. Sem lista de histórico
visitante. Limpeza indexada por vencimento, execução no startup e pelo menos horária
enquanto saudável; ao reiniciar, remove vencidos antes de uma hora. Sem apagar contas
ou revisões do banco; comandos/resultados temporários são removidos junto da tentativa.

Conta: tentativas/respostas confirmadas persistem após logout e podem ser retomadas
de outra sessão autenticada. Login durante visitante não transfere tentativa; UI
informa antes de iniciar autenticação e limpa contexto anterior na troca. Sem merge
automático por e-mail, IDs fornecidos ou coincidência de sessão após rotação de cookie.

### 5. Fronteiras HTTP propostas

| Rota | Público e contrato |
| --- | --- |
| `POST /api/v1/attempts` | Visitante/conectado, CSRF, critérios + commandId; 201 na criação, 200 no retry |
| `GET /api/v1/attempts/{id}` | Dono; estado, versão, itens e respostas próprias, sem correção em andamento |
| `PUT /api/v1/attempts/{id}/answers/{itemId}` | Dono, CSRF; alternativeId nullable, commandId, expectedVersion |
| `POST /api/v1/attempts/{id}/finish` | Dono, CSRF; commandId/expectedVersion, resultado após commit |
| `GET /api/v1/attempts/{id}/result` | Dono; 409 em andamento, resultado fechado após conclusão |
| `GET /api/v1/me/attempts` | Conta autenticada; page/size limitados, estado opcional, ordem createdAt DESC/id DESC |

Spring Security decide rota/principal/CSRF antes do controller. Histórico exige
`UserPrincipal`; demais rotas aceitam ator temporário sem autorizar posse. Não colocar
essas rotas no bypass stateless de `/question-bank`, não afrouxar deny-by-default.
Controllers recebem ator tipado e traduzem DTO/status; posse/transições ficam no núcleo.
400 inválido/campo extra; 401 histórico sem sessão; 403 CSRF/rota negada; 404 não dono,
ausente ou expirado; 409 estado/versão/insuficiência; 503 indisponibilidade.

DTOs separados para tentativa e resultado, sem serializar entidades/snapshot privado.
No-store em respostas; CORS origens explícitas. O resultado nunca amplia a consulta
pública do banco. Logs não imprimem payload privado, cookies, tokens ou respostas.

### 6. React, salvamento e recuperação

UI de composição/realização/resultado e lista da conta sem nova biblioteca de roteamento
obrigatória. Radio groups/labels/foco/progresso acessíveis. Uma fila serializa salvamentos
da tentativa e mostra pendente/salvo/falhou. Finalizar aguarda confirmação dos salvamentos;
em conflito 409, recarrega por ação explícita e preserva a escolha não confirmada em
memória, identificada como tal. Não marca escolha local como persistida.

Prazos finitos incluem consumo do JSON e abort. Geração de contexto ignora respostas
tardias após navegação, logout ou troca de conta. Dados privados só em memória; sem
localStorage/sessionStorage/cache de service worker ou URL com respostas/correção.
Browser não calcula nota; link das aulas e consulta do banco continuam independentes.

## Risks / Trade-offs

- [Snapshot privado duplica conteúdo] → tamanho limitado por tentativa e DTOs separados;
  evita dependência da elegibilidade atual e permite manter correção histórica.
- [Abuso de armazenamento visitante] → validade/limpeza, limites de tamanho e validação;
  política de proteção de tráfego público será tratada no deployment, sem prometer escala.
- [Duas abas e rede instável] → versão esperada/idempotência e testes de corridas/timeout;
  retry não sorteia de novo e não confirma gravação perdida.
- [Gabaritos do legado continuam públicos] → proteção dos novos contratos não promete
  sigilo global, conforme decisão já aprovada no banco de questões.

## Migration Plan

Só implementar após aprovação desta proposta e integração do #63. Criar a épica a
partir da épica geral atualizada; tarefas pequenas com código/testes/docs coesos e
dependências reais via Stacks v0.2.1. Não colocar esta proposta na épica question-bank.
Migração aditiva testada sobre V4 com identidades/sessões/acervo preservados. Rollback
desabilita o fluxo novo e preserva dados; sem downmigration/apagamento. CI só verifica;
revisão SOL 6.1 xhigh por lote e gate cumulativo SOL 6.1 xhigh para `[SPEC]`, em contexto
independente próprio, conforme decisão do usuário de 2026-10-15; sem merge/main/deploy presumidos.
