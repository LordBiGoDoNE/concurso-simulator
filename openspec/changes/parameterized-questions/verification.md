# Verificação integrada — questões parametrizadas

Escopo autorizado: grupos completos 5 e 6. Grupos 1–4 integrados em `542f9ac`.
Implementação/verificação, revisão de lote, integração de tarefas e gate cumulativo
são estados distintos. Este documento registra execuções do coordenador; não é
parecer do revisor, aprovação editorial, autorização de merge ou gate concluído.

## Requisito → implementação e regressão

| Requisito aprovado | Implementação | Evidência automatizada |
|---|---|---|
| Três famílias e domínios finitos | Family, Parameters, NumericGenerator | NumericGenerationTests/VariantManifestTests: 64/20/27, aritmética e oráculo independente |
| Todas as combinações | VariantManifest, CapturedOutputDigest | VariantManifestTests, CapturedOutputIntegrityTests: 111 saídas/digests em Java e PostgreSQL |
| Aprovação editorial exata | TemplateFamily, TemplateDecision, TemplateUseCases | TemplateEditorialTests/TemplatePersistenceTests, auditor Python; decisões reais separadas em editorial/parameterized-questions/2026-10 |
| Operação local atômica | TemplateLocal, ControlledInput, JpaTemplateStore | TemplateLocalTests/TemplatePersistenceTests: tipos/limites/confirmar/rollback/CI sem aprovação |
| Elegibilidade conjunta | JpaEligibleTemplates, AttemptMaterializer | ParameterizedPersistenceTests, mudança editorial no navegador, retirada sem afetar capturas |
| Sucessão explícita | Ponteiro da família/decisões | TemplateEditorialTests/TemplatePersistenceTests: v2/retirada/concorrência/replay sem reativar |
| Opção compatível/mistura | CreateRequest.NumericOption, Composition, AttemptComposer | AttemptHttpTests: ausente/false/true/null/coerção/extra/CSRF; cinco cenários reais de variações |
| Comandos V5 | AttemptUseCases, AttemptCodec | V5AttemptCompatibilityTests e ParameterizedOperationalFixture com JAR V5 real |
| Captura reproduzível | AttemptQuestion, V7/V8/V9, JpaAttemptStore | Troca válida de tupla/digest e metadados adulterados; restauração/rollback/111 casos |
| Persistência/histórico | Codec de origem e cópia imutável | V5/PG tests, ensaio operacional; conta retoma após restart/alteração da base, visitante expira |
| HTTP/privacidade | Security/CurrentActor/DTOs/advice/validadores | SessionAccessTests/AttemptHttpTests; payload/DOM/storage/artefatos com controles negativos |
| Interface/material | Checkbox nativo/descrição, conteúdo do servidor, link de estudo | 110 testes frontend; teclado/foco em 390/1280px, rede/duas abas/expiração; regressão offline separada |

As classes com nomes resumidos acima estão sob backend/src/test; o nome efetivo de
cada teste/execução está nos logs de CI. A matriz não substitui inspeção semântica
nem garante cobertura de todos os erros possíveis. Escopo não inclui IA ao vivo,
novo conteúdo/família, sincronização offline ou deployment.
O teste antigo da consulta proibia qualquer checkbox na página. A regressão agora
mantém zero controles no detalhe e zero radios, exigindo globalmente o único checkbox
permitido, no compositor e desligado. Os controles negativos de gabarito continuam
globais; não se ocultou o novo controle nem se removeu a proteção da consulta.

## Execuções do lote final

- HTTP real e PostgreSQL: 10 testes passaram, incluindo três cenários novos.
- Frontend unitário: 110 testes passaram (12 arquivos).
- `npm run test:parameterized`: cinco cenários reais passaram com Testcontainers/OIDC
  local, inclusive confirmação perdida da criação, mesma conta após restart/base
  substituída, conflito de duas abas e expiração de visitante; screenshots 390/1280px
  inspecionados, sem overflow horizontal. Fixtures sintéticas, não aprovação real.
- Controles negativos de artefatos: cinco testes passaram. Scanner do JAR/SPA passou;
  metadados/gerador/sentinelas não podem entrar em nenhum chunk, inclusive resultado.
- Build backend completo: 207 casos, 206 executados sem falhas/erros; um symlink
  omitido por permissão do Windows. Inclui domínio, ArchUnit, PostgreSQL e HTTP.
- Regressão existente de identidade/banco/simulados no navegador: 14/14 cenários
  aprovados. Aplicação/material offline: 6/6, incluindo nove módulos e 90 resoluções
  sem JavaScript e sem rede.
- Exportação limpa dos 739 arquivos/entradas versionados em `7a55651`: sem .git,
  .env ou artefatos/dependências locais. Compilação Java/bootJar offline, npm ci
  offline/build, scanner e auditor 111/111 aprovados. Ajustes posteriores neste lote
  consolidam documentos/checklist e delimitam a expectativa do teste antigo de
  consulta; não alteram fontes da aplicação, HTML/assets ou as dependências compiladas.
- Material da exportação limpa: 108 páginas/1661 referências originais e 108/1757
  no pacote enriquecido, sem erros. HTML/assets originais idênticos à base `542f9ac`.
- Checks remotos e revisão independente do lote ainda pendentes ao registrar estes
  resultados locais; não são substituídos pela contagem de testes.

## Ensaio de artefatos/upgrade/rollback

Backend V5 extraído do commit aprovado `0d0fc4848f55fc93c736aa76dcfbb73a7269eb39`,
recompilado sem alterações por `gradlew --offline bootJar`. SHA-256 do JAR executado:
`a6e8e90b4a433269909deb5f309b4ae14906e1aed357cb8bb815640f107e422d`.

`gradlew parameterizedOperationalCheck -ProllbackJar=<JAR-V5-aprovado>` passou em
PostgreSQL 18.6 descartável, por HTTP/CSRF reais. Criou/respondeu tentativa usando o
JAR V5 e preservou contas, identidade, sessões, banco e JSONs no upgrade V5→V9.
Reiniciou o JAR V5 sobre schema V9 **antes** de qualquer escrita no formato novo,
conferindo leitura/replay. O artefato atual preservou comandos/hashes V5.

A primeira criação nova fixa gravou formato 2; o guard operacional recusou iniciar
o JAR V5 a partir daí, antes de lançar processo. Esse limite não depende de haver
geração. Uma tentativa gerada foi capturada, lida/respondida/concluída com o artefato
atual e `--parameterization.generation-enabled=false`; replay confirmado funcionou,
nova true retornou erro genérico sem gravar, nova fixa continuou disponível. Outro
restart preservou resultado/resolução concretos. Item e hashes/outcomes V5 conservaram
o formato de origem. Nenhum downgrade, apagamento ou JAR V5 pós-formato novo.

O guard pertence ao ensaio; não impede um operador de lançar um JAR por fora dele.
Procedimento de recuperação: manter artefato com leitores compatíveis e geração
desabilitada, preservar schema/dados/comandos, conferir prontidão/leitura/replay,
somente reabilitar novas gerações depois de corrigir/verificar a causa. Não executar
downmigration, reserializar histórico ou prometer rollback para o artefato V5.

O ensaio também encontrou a tradução de IllegalStateException pelo repository para
InvalidDataAccessApiUsageException; o advice trata uso/estado inválido como 500
genérico e mantém indisponibilidade de banco como 503, com regressão HTTP/rollback.

## Conteúdo real e limites

Consulta **somente leitura** pela porta real sobre a cópia editorial aprovada confirmou
três versões elegíveis/111 variantes. Nova prévia offline e auditor racional independente
conferiram todos os conteúdos/digests e três manifestos, sem alteração. A seleção
editorial continua SHA-256 `879e9fa5d43d885a6125c9cbccc8590b373abc8d2321929d370159426ef1dba3`.
As decisões reais da base/templates e retirada/replay em clone são o histórico da
execução autorizada do grupo 4; esta conferência não as reaplica nem aprova conteúdo.

Observações de conexão/logs, screenshots e cotas ficam em diretórios ignorados.
Relatório de lote e gate terão SHAs/digests e modelos/sessões próprios. Cota/tokens
não disponíveis não são consumo zero. Nada foi publicado em main ou implantado.

## Gate cumulativo e integração

PRs #99/#100/#101 aprovados individualmente e integrados atomicamente pela stack
#102 em `4434f7a`. Árvore final e ancestralidade dos heads conferidas, branches
preservadas; nenhum sync/prune pós-merge. `[SPEC]` #103 destina a spec à épica geral.

Revisão cumulativa independente SOL 6.1 xhigh executada em contexto novo, somente
leitura, sobre base `0d0fc48` e head `a8aa71e`, sem achados verificáveis. O revisor
não executou testes/build/rede/DB nem leu este registro ou relatórios anteriores.
Os metadados finais de conclusão exigem revalidação real; fontes/SHAs/digest,
cobertura, limitações, triagem e resultado dessa revalidação ficam em
`spec-review.json/md`. Check estrutural e análise semântica são evidências distintas.

6.4 executada; integração da spec na épica geral aguarda relatório atual, checks
verdes e aprovação própria do usuário. A regra active/strict da épica geral exige
spec-review do app15368 e não tem bypass. Sem autorização de main/deploy/arquivamento.
