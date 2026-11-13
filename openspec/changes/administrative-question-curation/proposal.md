# Proposal

## Why

A [issue #177](https://github.com/LordBiGoDoNE/concurso-simulator/issues/177) nasceu da [#173](https://github.com/LordBiGoDoNE/concurso-simulator/issues/173): o acervo preparado tem 2.587 questões revisadas, e a habilitação delas exige 5.174 decisões pela CLI local `questionBankLocal decide`, uma por execução, com confirmação digitada. Isso inviabiliza curadoria em escala ou obriga a automatizar uma trava pensada para decisões individuais. Também não há como o ADM consultar o acervo da plataforma de forma prática para conferir o que vai importar ou aprovar.

## What Changes

- **Princípio de produto (decisão do usuário, 2026-11-12):** o concurso-simulator é a plataforma — formato de questão genérico, aberto e documentado, e funcionalidades para usar o acervo importado. Quais questões entram e são aprovadas fica a critério do ADM, não do sistema.
- **Consulta administrativa somente leitura:** busca paginada do acervo com filtros (finalidade, origem/prefixo, assunto, estado, origem da revisão, data), detalhe com histórico e conteúdo integral, consulta em lote por chaves/fingerprints de um pacote local e exportação no formato aberto do pacote de importação.
- **Decisões editoriais em lote pela API:** prévia sem escrita e aplicação confirmada de SUBMIT e APPROVE (avaliar RETURN_TO_DRAFT e WITHDRAW) sobre seleção explícita ou recibo de importação, com recibo das decisões.
- **Origem da revisão:** cada aprovação registra se a revisão foi feita por par humano ou somente por IA (`HUMAN_PEER` | `AI_ONLY`), consultável pelo ADM, para identificar questões aprovadas apenas por IA.
- **Conferência de concorrência pelo operador (decisão do usuário):** verificar se a revisão é a esperada e se não houve mudança concorrente é responsabilidade do ADM ou da IA que o auxilia, apoiados pela consulta administrativa. A plataforma mantém só a integridade técnica: aplicar exatamente o estado mostrado na prévia confirmada, sem efeito parcial silencioso.
- **Decisão anterior alterada:** os documentos da #160 registram "sem decisões editoriais HTTP". Esta proposta substitui essa restrição para decisões em lote do ADM autenticado; a CLI local continua disponível.

Fora do escopo: verificação automática de questões praticamente iguais (futuro, decisão 4 da #177), edição de conteúdo ou upload pela interface, publicação de recursos, gestão de vários administradores, deploy/VPS, carga em produção.

## Capabilities

### New Capabilities

- `administrative-question-curation`: consulta administrativa do acervo, decisões editoriais em lote confirmadas e origem da revisão (humana/somente IA).

### Modified Capabilities

Nenhuma capacidade consolidada em `openspec/specs/` (inventário vazio na base). `administrative-question-import` e `question-bank` permanecem referências: a importação continua criando somente rascunhos; a novidade é a decisão em lote como operação separada.

## Impact

- Base: `main` `226d2e5b07208887941811beff10ac29295b04b3`.
- Áreas: `questionbank` (domínio de decisão, casos de uso de consulta/lote), infraestrutura JPA e nova migração (versão posterior à V15 da #173, a coordenar), segurança/rotas administrativas, OpenAPI, CLI local (origem da revisão), documentação de operação; interface administrativa mínima opcional.
- Risco previsto: **crítico** (elegibilidade em lote, autorização, replay). Revisão: por decisão do usuário de 2026-11-12, somente a revisão final de código antes do merge em `main`, por agente autônomo novo com Claude Opus 5.5 (esforço medium), em contexto independente; a regra SOL anterior não se aplica.
- Dependência operacional: a habilitação local do acervo da #173 usa esta capacidade depois de implementada e aprovada.
- Esta entrega é documental (proposta). Implementação, commits de código, revisões, merges e publicação dependem de aprovação própria dos grupos em `tasks.md`.
