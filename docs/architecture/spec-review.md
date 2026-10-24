# Revisão independente de integração de spec

## Integração após a entrega da reformulação

A entrega inicial foi integrada à `main` pelo #133 em 2026-10-23. Novas specs
partem de `main`; tarefas continuam em sua própria `epic/<spec>`, e o PR final
`[SPEC]` segue diretamente para `main`. Não reutilizar a épica geral encerrada.
As etapas e exigências semânticas abaixo continuam: revisão cumulativa própria,
base/head imutáveis, digest, ancestralidade, triagem, CI e autorização de merge.
Mudança do destino do PR exige revalidação, mesmo com árvores idênticas.

A automação instalada abaixo continua limitada a `epic/application-reformulation`: em
PRs para `main` publica `spec-review-tests` e informa não aplicabilidade do guard.
Esse resultado não certifica a revisão cumulativa. Até uma alteração de automação
aprovada em PR próprio, o coordenador executa `validate_report(report, explanation,
base, head, spec, pr)` de `scripts/check_spec_review.py` nos commits reais, além de
registrar revisão independente e conferir todos os checks antes do merge. A API
consultada em 2026-10-24 não apresenta ruleset/proteção para `main`; não se declara
proteção obrigatória inexistente. Este ajuste não altera workflows/rulesets/deploy
nem dispensa o gate semântico. A regra estrita/ruleset documentada abaixo descreve
a antiga épica geral e permanece intacta.

O procedimento abaixo registra o fluxo usado na reformulação; para novas specs,
substituir seu destino pela `main`, respeitando a limitação de automação acima.

## Quando e por quê

### Rodadas de implementação — antes da aprovação de merge

O usuário define o lote por **grupos completos**, sem limite fixo de tarefas: “faça
3, 4 e 5” inclui 3.1–3.4, 4.1–4.3 e 5.1–5.3 na spec question-bank. Se o pedido não
informar escopo, listar os grupos/tarefas pendentes e perguntar quais entram antes de
implementar. Não encerrar num prefixo de grupo; PRs pequenos/coerentes não autorizam
deixar sua última subtarefa para outro lote. Comunicar bloqueios reais sem fingir
conclusão nem incluir dependências fora do escopo por iniciativa própria. Implementação
sem pausas por subtarefa, PRs em rascunho e nenhuma integração presumida permanecem.
Ao fim da implementação,
**antes de solicitar aprovação de merge**, um agente novo com
`openai/gpt-6.1-sol#xhigh` revisa o conjunto como **único revisor do lote**, inclusive
mudanças críticas. Em 2026-10-13, o usuário retirou a verificação Astra dos lotes,
substituindo a política baseada em risco de 2026-10-12. Não disparar Astra nem outro
modelo automaticamente por risco, dúvida, revalidação ou medição. Confirmar
disponibilidade e modelo/variante efetivos; indisponibilidade bloqueia a revisão,
sem fallback silencioso. Por decisão do usuário de **2026-10-15**, SOL 6.1 xhigh é
também o padrão dos gates cumulativos finais, inclusive o da épica geral. A revisão
do lote não substitui esse gate, que continua exigindo contexto independente próprio.
Revisões Astra já executadas permanecem histórico verdadeiro, não são apagadas nem
reclassificadas como execuções SOL.

#### Classificação de risco — registrar antes de disparar revisores

Registrar na issue/PR a classificação do lote, arquivos/regras afetadas e justificativa:

- **Crítico:** alteração de autenticação/autorização, sessão/CSRF/OAuth/segredos;
  persistência, schema/migrações, transações, travas, concorrência, rollback ou
  idempotência; invariantes de integridade/versionamento/elegibilidade editorial ou
  proteção contra exposição indevida de respostas; controles executáveis de CI/gate,
  permissões ou deploy que protegem essas fronteiras. Exige cobertura explícita dessas
  fronteiras pelo SOL xhigh e regressões/verificações adequadas, não segundo modelo.
- **Padrão:** documentação/processo, evidências históricas ou alterações sem impacto
  crítico demonstrado. Exige SOL xhigh. Classificar
  pelo efeito real: extensão `.md`, tamanho do diff e CI verde não garantem baixo risco.
- Se qualquer PR ou interação do lote for crítica, registrar o lote como crítico e
  examinar **todo o conjunto**, incluindo diffs completos por PR e combinação. Dúvida
  na classificação adota crítico; se SOL identificar impacto crítico inicialmente
  não reconhecido, registrar a reclassificação e ampliar a cobertura/verificação
  necessária antes de solicitar aprovação, sem disparar outro modelo.

Revalidação permanece somente SOL xhigh, com novo snapshot e conferência real do conjunto.
Não rebaixar risco, omitir cobertura ou dispensar correção de bloqueador para economizar
cota. Somente SOL não significa garantia de corretude nem flexibilização de CI/gates.

Cada revisor recebe requisitos/padrões versionados, composição e bases/heads imutáveis,
diffs completos por PR e combinação do lote; não recebe conversa/defesas do implementador
ou resultado do outro revisor. Reportar achados por PR e interações entre camadas,
cobertura/limites e testes não executados. Não exigir tarefas fora do lote antecipadamente.
Aplicar as mesmas regras de triagem, correção e revalidação descritas abaixo.

Em PR cujo objeto é registrar evidências históricas, o perfil pode ler os relatórios e
registros do próprio diff para revisão **documental**, não para atestar a corretude do
produto. Registrar a exposição incidental a achados históricos; não compartilhar
resultados dos revisores atuais. Essa exceção não relaxa o isolamento do gate final.

Registrar evidência por lote em `openspec/changes/<spec>/reviews/<lote>/`, separando
relatórios/modelos/sessões, composição/digests, triagem e verificação do coordenador.
Não preencher `spec-review.json/md` como completed por uma revisão parcial de lote:
esses dois arquivos continuam exclusivos do gate cumulativo final. Alterações de base
ou diff relevante invalidam a revisão do lote; relatórios isolados de evidência não
autorizam alterar código sem revalidação. O check de tarefa continua `spec-review-tests`,
não há novo gate automático nem mudança de workflow/ruleset nesta decisão.

Apresentar relatório único com risco/justificativa, PRs/SHAs/checks, achados/triagem e
consumo separado por modelo/coordenador: cota 5h/semanal antes/depois, com limites de
atribuição, e tokens (entrada nova, cache read/write, saída, raciocínio reportado).
Comparações históricas de achados comuns/exclusivos e consumo só são válidas quando
ambos realmente executaram. Nos lotes SOL-only, registrar que comparação não foi
realizada, não que outro modelo teve zero achados/consumo; não dispará-lo para comparar.
Seguir [protocolo de cota](../review-quota.md); não somar raciocínio à
saída nem converter tokens em consumo da assinatura. Extrapolações exigem amostra e
hipóteses explícitas. Zero achados não prova equivalência nem garante economia futura.

Aprovação do conjunto exige a lista explícita dos PRs/heads; não implica merges automáticos,
aprovação editorial de conteúdo real, publicação ou arquivamento. Não iniciar outra rodada
automaticamente. A primeira rodada autorizada é question-bank 1.2, 1.3, 2.1–2.3, sobre #38.

### Integração final da spec

**Padrão aprovado em 2026-10-15:** `openai/gpt-6.1-sol#xhigh` para todas as revisões,
revalidações e gates cumulativos, inclusive da épica geral. Não há exigência ativa
de Astra. Cobertura, independência, triagem, atualidade e autorização de merge permanecem.
A exceção SOL de question-bank #63 em 2026-10-14 é histórico anterior a este padrão;
a tentativa Astra com 401 permanece falha sem parecer, não revisão executada.

PRs de tarefas são revisados individualmente e integrados na épica da spec. Depois,
`[SPEC] <Título>` propõe `epic/<spec>` → `epic/application-reformulation`. Este gate
examina o conjunto, não uma nova implementação. CI verde e revisão de tarefas não
provam que as responsabilidades e a integração estão corretas.

1. Abrir PR `[SPEC]` em rascunho com escopo, requisitos, evidências e pendências.
2. Modelo/variante escolhido pelo usuário: `openai/gpt-6.1-sol#xhigh`, fixado no
   perfil. Confirmar disponibilidade e ID efetivo antes do disparo, sem presumir
   capacidade/custo. Sem disponibilidade, parar e manter relatório pendente. Outra
   escolha exige aprovação e atualização do perfil; não há fallback silencioso.
3. Capturar base/head imutáveis e snapshot. Disparar `spec-reviewer` em sessão nova,
   sem reutilizar sessão do implementador. O perfil OpenCode V2 é somente leitura e
   fixa o modelo aprovado: o coordenador confere o ID efetivo ao disparar.
4. Fornecer spec aprovada, AGENTS.md, ADRs/padrões, commits e diff completo; permitir
   explorar código relacionado. Não fornecer conversa/defesas da implementação,
   relatórios de findings ou verification.md na primeira rodada. Decisões versionadas
   são a fonte de requisitos e não devem ser ocultadas.
5. Revisor analisa responsabilidades, invariantes, dependências, integração, contratos,
   segurança, privacidade, persistência/concorrência/migrações, testes e regressões.
   Ele reporta, não edita/aprova merge. O coordenador executa testes necessários e
   registra quem executou. OpenCode usa read/glob/grep, sem shell; no Codex são
   permitidos comandos estritamente de leitura de código/commits/hashes. Sem testes,
   build, aplicação, rede/DB ou escritas pelo revisor; orientação não é sandbox absoluto.
6. Triar achados com o usuário: corrigir, refutar com evidência ou adiar melhoria não
   bloqueadora. Mudança arquitetural exige decisão, não alteração silenciosa de escopo.
   Ausência fundamentada de achados é válida. Não inventar complexidade/findings.
7. Corrigir por commits/PRs revisáveis, adicionar regressões/regras quando viável e
   revalidar com o revisor. Qualquer mudança relevante requer novo snapshot; confirmar
   findings resolvidos não dispensa conferir o novo conjunto. Não copiar o digest atual
   para uma revisão antiga sem realmente revalidar.
8. Com relatório atual, triagem encerrada, sem bloqueadores pendentes e CI/gate
   aprovados, solicitar aprovação explícita do merge. Aprovação da spec não autoriza
   main, publicação ou arquivamento.

## Pedido inicial ao revisor

> Modelo/variante explicitamente aprovado: `<provider/model#variant>`.
> Spec: `<nome>`. PR: `<número>`. Base: `<sha>`. Head: `<sha>`.
> Revise o diff completo contra essa base e a combinação resultante. Confronte a spec,
> AGENTS.md e ADRs. Investigue o código necessário. Não modificar nem fazer merges.
> Não assumir conformidade por CI/aprovações anteriores. Retorne achados verificáveis,
> distinguindo bloqueador, recomendado e opcional. Inclua localização, evidência,
> requisito/ADR, impacto e sugestão; cobertura/limitações e testes não executados.

O coordenador prepara o diff desses commits (não só a lista de arquivos) e informa
se há arquivos binários/gerados ou limitações de contexto. Revisão parcial não pode
ser declarada revisão completa. A sessão/ID efetivo do modelo fica no relatório.

## Evidências e atualidade

Arquivos por spec: `openspec/changes/<spec>/spec-review.json` (gate) e
`spec-review.md` (explicação humana). Ambos começam **pending**, não executado.
Para gerar a identificação, na raiz do repo:

```sh
python3 scripts/check_spec_review.py --snapshot --base <base-sha> --head <head-sha> --spec <nome>
```

O snapshot contém base/head e SHA-256 do diff completo `base...head`, incluindo docs,
requisitos, CI e testes, excluindo **somente** esses dois arquivos de relatório.
Renderização fixada: três linhas de contexto, zero contexto entre hunks, sem heurística
de indentação/renames, ordem de paths padrão, indicadores/prefixos/quoting explícitos,
sem cor, diff externo ou textconv. Preferências locais de Git não devem mudar o digest;
há regressão para essas opções. `GIT_DIFF_OPTS` é removida apenas do ambiente do
subprocesso Git, sem alterar o ambiente do chamador; há regressão específica para ela.
Use a raiz do repo e versões compatíveis do Git:
não se promete identidade entre implementações diferentes de Git ou atributos de diff
divergentes fora do snapshot versionado.
A base deve ser a base atual do PR, não um merge-base antigo escolhido pelo autor.
O head analisado deve ser ancestral do head atual e conter o mesmo diff relevante.
Mudança de base exige revalidação mesmo quando o diff aparenta não mudar. Registrar
relatório e explicação pode gerar commits posteriores sem invalidar a revisão;
mudar qualquer outro arquivo, incluindo os guardas, exige nova revisão.

JSON versão 1, quando concluído:
- `status: completed`, `spec`, `pr`, `base_sha`, `reviewed_head_sha`, `diff_sha256`;
- `reviewer.model` (provider/model com variante se escolhida), `reviewer.session`,
  `reviewer.independent_context: true`, `reviewed_at` ISO-8601 com timezone;
- `coverage`: explicação não vazia para requirements, architecture, integration,
  security, persistence, tests e maintenance (inclusive não aplicável justificado);
- `limitations`: lista explícita; `verification`: lista de objetos com `command`,
  `result` e `executed_by` (não confundir testes do implementador com os do revisor);
- `findings`: lista, possivelmente vazia. Cada item: `id`, `severity` (blocking,
  recommended, optional), `location`, `evidence`, `reference`, `impact`, `suggestion`,
  `disposition` (fixed, rejected, deferred) e `resolution`. Bloqueador não pode deferred;
  correções/refutações exigem evidência no texto de resolution;
- `triage_complete: true`. Isso atesta triagem, **não a aprovação do usuário**.

O Markdown deve explicar escopo/cobertura, achados/triagem, evidências/limitações e
revalidação, usando os títulos do relatório inicial. Modelo ainda não escolhido:
`reviewer.model: null`, `status: pending`; nunca preencher conclusão fictícia.

## Check e proteção no GitHub

`Spec review gate` executa testes do guard em todos os PRs, inclusive alterações de
título/base. Na épica geral publica `spec-review`; nas tarefas, `spec-review-tests`,
para que sucesso não aplicável de tarefa não certifique revisão de spec no mesmo SHA.
Tarefas são não aplicáveis; na épica geral
exige branch `epic/<spec>` e título `[SPEC]`, evitando bypass só por renomear título/branch.
`main` → épica geral é sincronização do site existente, não integração de spec;
o check registra essa exceção e não autoriza seu merge.

O [padrão de Stacks e a evidência do piloto em question-bank](../stacks-pilot.md) registram
criação/diffs/rebase/CI remotos e a restrição de retargeting: GitHub bloqueia troca de
base de membro. #36 integrou a adoção em question-bank; o usuário aprovou o padrão para
próximas specs, versionado por PR de processo próprio. Não repetir aprovação do método
por spec após esse registro; conferir refs/checks e limitar a dependências reais da
mesma spec/repositório, trunk `epic/<change-name>`. Aprovações de propostas/tarefas/merges
continuam separadas. PR `[SPEC]`
fica fora da stack, sempre da épica da spec para a épica geral; sem isenção nova no
guard. Logs do ensaio confirmaram base/head e `spec-review-tests`; raw payload não
foi exportado. Igualdade visual de diff após rebase não preserva evidência: o gate
exige base atual, ancestralidade do head revisado e digest. O ensaio não demonstrou
merge/fila/proteções de prefixo nem sync pós-merge; não presumir esses caminhos seguros
ou autorizados. Aprovação do topo não aprova camadas inferiores nem a spec cumulativa.

O check exige relatório concluído, snapshot atual, cobertura e triagem registradas.
Uma regra GitHub na **épica geral apenas** exige PR e check `spec-review` da aplicação
GitHub Actions. Não alterar proteção de main/deploy nem regras existentes. A ativação
é registrada no PR; sem workflow integrado, o check fica ausente e bloqueia integração,
inclusive #25, até instalar o processo e executar a revisão.

Obrigatório também exigir branch atualizada antes do merge:
`strict_required_status_checks_policy: true`. O workflow de PR não dispara só porque
a branch-base avançou. A regra estrita bloqueia o head desatualizado; atualizar a
branch dispara nova execução, e o script rejeita relatório com base anterior mesmo
quando o diff relevante continua igual. Não desativar essa política para reaproveitar
check antigo. Regra ativa: `24766642`, limitada a `epic/application-reformulation`,
check `spec-review` vinculado ao app `15368`, sem bypass configurado. O coordenador
confere esses campos via API antes de solicitar integração. O teste Git isolado cobre
avanço/merge da base e exigência de novo snapshot; não substitui ensaio operacional
de avanço da base na integração de specs no GitHub, não coberto pelo piloto de tarefas.

Limites: o check valida evidência estrutural, não prova qualidade, identidade do modelo
ou autenticidade do relatório. Workflow/script são versionados no PR e também precisam
ser revisados: alteração maliciosa pode falsificar sucesso. Sem uma automação confiável
instalada na base/default branch não há attestation forte; não usar pull_request_target
para executar código não confiável. Administradores podem alterar/desativar regras.
Proteção não é garantia absoluta contra dono malicioso ou erro semântico. O mecanismo
reduz omissões normais, e AGENTS/revisão/aprovação continuam obrigatórios.
