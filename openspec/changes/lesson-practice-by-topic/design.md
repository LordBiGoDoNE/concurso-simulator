# Design

## Context

O backend atual exige TargetRole em createPractice/practiceAvailability e cria uma
Composition por cargo. A prática usa snapshot formato 3; codec, histórico e DTOs
também pressupõem cargo. No frontend, seleção, correspondência da realização e
painel são identificados por assunto/cargo. Esconder o select manteria o vínculo.

A classificação das questões continua relevante ao banco e aos simulados. Não
será removida nem regravada para viabilizar a prática por assunto.

## Goals / Non-Goals

Praticar a aula sem cargo explícito ou implícito, preservando capturas históricas,
filas, correção no servidor e limites de segurança/modos. Não inclui níveis de
aprofundamento, dificuldade adaptativa, conteúdo novo, curadoria, cargas, revisão
de dificuldades baseada em práticas ou mudanças nos simulados.

## Decisions

### Seleção e invariantes

- Seleção parte do pool aprovado PRATICA_AULA e filtra assunto/fonte fixa antes
  de contar ou sortear. Não usa roleIds como critério; a disciplina deriva do assunto.
- Escolher min(5, elegíveis), sem repetir questão-base. Zero produz insuficiência
  sem persistir realização parcial; não há fallback de assunto ou destinação.
- Composition continua exigindo TargetRole. Novas práticas não criam Composition
  nem usam o primeiro cargo do catálogo como substituto.
- Snapshot formato 4: purpose PRATICA_AULA, practiceTopic obrigatório e composition
  null. Validar 1–5 itens fixos distintos daquele assunto, opções/respostas,
  propriedade, datas e versão. Formatos anteriores mantêm suas invariantes.
- O domínio/aplicação permanecem Java puro. Codec, JPA, SQL e transações ficam na
  infraestrutura; não criar Services de repasse nem trocar o mecanismo de correção.

### HTTP e tipos

| Operação | Novo contrato de prática |
| --- | --- |
| POST /api/v1/lesson-practices | `{commandId, topicId}` |
| GET /api/v1/lesson-practices/availability | Query `topicId`; retorno `{topicId, available, maximum: 5}` |
| Leitura/criação/resposta | `{id, topicId, createdAt, finishedAt, version, items}` |
| Encerramento | `{attempt: PracticeView, result}` |
| Histórico próprio | Paginação atual; itens mantêm id, topicId, datas, total e percentage, sem roleId |

- Salvar/encerrar/resultados mantêm rotas e comandos existentes. A estrutura dos
  itens/correções permanece, inclusive a classificação editorial com roleIds;
  esses metadados não constituem um cargo escolhido para a realização.
- DTOs de prática são concretos e distintos dos DTOs de simulado. O contrato de
  simulado continua exigindo roleId e seus tipos/validadores não serão relaxados.
- O novo contrato recusa roleId no início e na query de disponibilidade com 400;
  não há conversão silenciosa. Validação dessa alteração fica restrita às rotas
  de prática, sem mudar a política de JSON das demais rotas.
- Preservar 201 na criação, 200 no replay, 409 para intenção/estado conflitante,
  insuficiência tipada, 404 por posse/modalidade e respostas privadas no-store.
- Sessão, autenticação técnica e CSRF continuam na infraestrutura de segurança.
  Controllers traduzem DTOs; regras de seleção/posse permanecem no núcleo.

### Capturas, comandos e dados anteriores

- Criar V12 para permitir formato 4, mantendo as condições anteriores e as travas
  de modalidade, assunto, composição, itens e origem. Não editar V1–V11, alterar
  schema via Hibernate ou regravar snapshots, revisões, itens e comandos existentes.
- Codec lê formatos 1/2/3 e 4; práticas formato 3 continuam respondíveis, encerráveis,
  consultáveis e retomáveis com suas questões/correção capturadas originalmente.
- A projeção HTTP por assunto também atende práticas antigas, omitindo o cargo
  público sem apagar sua composição histórica interna ou unir realizações.
- Criação nova usa identidade canônica `lesson-practice/v2` + assunto, junto à
  identidade do ator/comando existente. Mesmo comando/intenção recupera a captura;
  assunto diferente, outro modo ou comando antigo de criação incompatível gera 409.
- Hashes de comandos anteriores não são migrados. Replays de respostas e
  encerramentos históricos mantêm a interpretação original e verificam posse/expiração.
- Práticas continuam fora dos históricos, janelas e métricas dos simulados, com
  filtragem por modalidade antes de paginação/limites.

### Interface e navegação

- Remover seletor, cargo automático e textos de cargo da prática e de seu histórico.
  Seleção local de prática contém apenas assunto; disciplina pode ser derivada.
- Extrair o contrato mínimo de realização para compartilhar fila/executor; manter
  tipos específicos de simulado com roleId e prática com topicId. Não fabricar
  roleId vazio para reutilizar validadores que o exigem.
- Chaves de painel, início pendente e correspondência do treino usam assunto,
  não cargo. Mudar o cargo público mantendo a mesma aula não reinicia/oculta o treino.
- Manter um executor montado, independência do simulado, bloqueios por operação
  pendente, confirmação de substituição e invalidação por mudança de identidade.
- Abrir/recolher consulta disponibilidade sem criar realização. Somente Começar
  prática inicia e carrega questões abaixo do botão; resultado permanece no painel.
- Retorno à aula usa seu assunto e conserva o cargo atual da navegação se
  compatível; caso contrário abre exploração livre. Não usa cargo de captura antiga.

## Alternatives Considered

Esconder o seletor ou herdar um cargo manteria filtro e identidade de criação por
cargo, contrariando a decisão. Tornar cargo opcional em Composition ou nos DTOs de
simulado enfraqueceria contratos alheios à prática. Reescrever formato 3 apagaria
contexto histórico e conflitaria com imutabilidade e replay; usar formato 4 é explícito.

## Risks / Trade-offs

Risco crítico: altera captura persistida, criação idempotente, contratos privados
e uma fila compartilhada. CI verde não certifica revisão semântica. Cobertura exige
PostgreSQL isolado, HTTP com segurança real, formatos históricos e navegador.

O contrato HTTP muda de forma coordenada. Cliente antigo com roleId recebe 400;
frontend/backend devem ser atualizados juntos no teste local. Backend antigo não
lê formato 4; não retornar ao binário anterior sobre esses dados por automatismo.

## Migration Plan

Na futura implementação autorizada: registrar cópia de segurança do banco local,
testar V12 primeiro em PostgreSQL isolado e atualizar API/frontend locais juntos.
Preservar registros existentes; não resetar banco, recarregar questões ou acessar
a VPS. Esta proposta não executa migração, backup ou mudança de runtime.

Grupos 1–2 partem da main com lesson-practice já integrada. Grupo 3 exige conferir
a integração da jornada #158 e seus refinamentos; se a dependência continuar
ausente, comunicar o bloqueio sem merge/sync/retarget ou ampliação de escopo tácitos.
Stacks somente para tarefas dependentes da mesma spec, com autorização do lote.

## Validation / Delivery

Domínio/aplicação sem framework; codec/JPA/Flyway e concorrência em PostgreSQL real
isolado; segurança/DTOs em HTTP; tipos/estado em Vitest; Chromium/Edge em celular e
desktop, incluindo filas, trocas de identidade e legado. Conferir checklist de
endpoints e ArchUnit. Manter seleção editorial e contratos de simulados como controles.

Esta etapa entrega apenas documentos e regra permanente, com OpenSpec strict e
diff documental conferidos. Depois, apresentar issue/spec/grupos e pedir confirmação
explícita para implementar. Usuário testa localmente antes de publicação de código
ou revisões por modelo; revisões SOL e gates previstos no AGENTS continuam separados.
