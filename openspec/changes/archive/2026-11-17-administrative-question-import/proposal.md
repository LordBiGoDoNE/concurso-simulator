# Proposal

## Why

A [issue #160](https://github.com/LordBiGoDoNE/concurso-simulator/issues/160) solicita importação administrativa pela API. Os grupos 1–3 entregaram essa capacidade localmente. A extensão agora proposta elimina a configuração manual do UUID administrativo, criando um ADM persistente com senha e vínculo Google, sem reinício para conceder a permissão.

## What Changes

- Oferecer prévia sem escrita e aplicação síncrona de um JSON versionado, com seleção explícita, fontes e recursos incorporados, limites finitos e diagnósticos estáveis.
- Criar o primeiro ADM uma única vez, com senha configurada ou senha temporária gerada na inicialização, exigindo sua troca. Manter login por senha mesmo depois de vincular Google.
- Autorizar pela permissão administrativa persistida e pelo estado vigente da conta, substituindo a lista externa de UUIDs. Login Google comum isoladamente continua sem conceder privilégios.
- Vincular Google após comprovar as duas identidades. Se a conta Google já existir, preservar seu UUID/histórico e associar a ela credencial local/permissão somente se o ADM local estiver sem histórico; bloquear mesclagem automática.
- Vincular confirmação a ator, seleção, digest, fingerprints, bases esperadas e destino, revalidando tudo na aplicação atômica e preservando reaplicação idempotente.
- Criar somente questões/revisões em rascunho; preservar conteúdo, identidades, histórico e elegibilidade existentes. Importação não submete, aprova, retira ou publica conteúdo.
- Registrar resultado real e metadados de auditoria sem payloads, gabaritos ou credenciais nos logs HTTP.

Decisões de 2026-11-09: SIMULADO e PRATICA_AULA ampliáveis, JSON síncrono autossuficiente, apenas rascunhos e templates excluídos. A decisão inicial de concessão externa por UUID foi substituída, no planejamento, por ADM persistente com senha permanente e Google opcional. O usuário escolheu preservar a conta Google existente e bloquear vínculo se o ADM local também tiver histórico. Os grupos 4–6 foram autorizados em seguida e foram implementados/verificados localmente. A entrega atual substitui o mecanismo R1; seu relatório permanece histórico.

Fora do escopo: formatos legados pela API, painel editorial/upload, gestão de outros administradores, recuperação pública de senha, mesclagem de históricos, decisões editoriais HTTP, templates, jobs, carga real, deploy e publicação de recursos. A interface adicional limita-se a login ADM, senha e vínculo Google. Aprovação/publicação de questões continuam separadas.

## Capabilities

### New Capabilities

- `administrative-question-import`: acesso administrativo, pacote genérico verificável, prévia confirmável, aplicação atômica e rastreabilidade de importação de rascunhos.
- `administrative-access`: primeiro ADM, credencial local, vínculo Google seguro e permissão persistente para administrar a importação.

### Modified Capabilities

Nenhuma capacidade consolidada em `openspec/specs/` (inventário vazio na base). As mudanças históricas `question-bank` e `optional-google-identity` são referências de compatibilidade, não serão editadas ou arquivadas. A nova capacidade acrescenta escrita administrativa sob prefixo próprio; preserva a proibição de escrita nas rotas públicas históricas.

## Impact

- Base examinada: `main`/HEAD `f61d57d5d55d6d61609815a2faa4b415ed8dca4b`, também conferida remotamente; planejamento independente das stacks #158/#161 e relacionado ao futuro acervo #156.
- Extensão: `identity`, segurança/sessão, autorização da importação, novos contratos HTTP e interface mínima React. Exige coordenação central com frontend #158 e arquivos compartilhados; nenhuma stack é importada.
- V12 e evidência dos grupos 1–3 preservadas. Grupos 4–6 propõem migrações adicionais e decisão arquitetural específica para hash de senha ADM, vínculo e proteção do histórico, sem reescrever ADR/migrações históricos ou os contratos públicos existentes.
- `tasks.md`: 1–3 implementados/verificados localmente; **4–6 implementados/verificados localmente**, aguardando teste do usuário e revisão/integração posteriores. Cada grupo inclui testes/docs. Commit, envio, revisão independente, merge, carga e deploy continuam sem autorização; nenhum artefato é anunciado como publicado.
