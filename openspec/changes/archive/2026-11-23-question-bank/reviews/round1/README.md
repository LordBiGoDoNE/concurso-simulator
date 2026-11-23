# Rodada 1 — registro de revisão e verificação

Escopo: cinco tarefas novas **1.2, 1.3, 2.1, 2.2, 2.3**, sobre 1.1 (#38).
Processo #39 independente; produto na stack #41, trunk `epic/question-bank`.
Implementação/verificação não significa aprovação/integração. Revisões de lote não
concluem o gate cumulativo final: `spec-review.json/md` continuam pendentes.

## Snapshots

`initial-manifest.json` identifica as bases/heads/digests por PR e combinação examinados
pelos dois revisores. Metadados da primeira camada estavam com trunk local antigo;
recuperação por `gh stack rebase` foi especificamente autorizada, com worktrees limpos
e backups. SHAs/bases de #38/#40 foram preservados; nenhum retargeting/merge/sync pós-merge.
As demais camadas foram acrescentadas depois de conferir a referência correta.

## Independência e limites

Sessões novas `ses_ed38086e0ffeKDMJMRIGZPndCj` (Astra medium) e
`ses_ed38086d7ffeiEB1AKGwTI4uFH` (SOL 6.1 xhigh), modelos disponíveis confirmados antes
do disparo. Revisores não receberam conversa/defesas do implementador ou resultados um
do outro. Ambos inicialmente tiveram acesso aos diffs externos negado; os mesmos bytes
foram copiados para diretório ignorado dentro do workspace, com igualdade SHA-256 conferida.
Bloqueio de acesso não foi contado como revisão sem findings; chamadas entram nos tokens.

O diff de processo contém registro histórico dos resultados de #38. Essa exposição a
evidência anterior é uma limitação explícita; não prometer ausência absoluta de viés.
Os revisores não executam shell/testes, conferem os diffs fornecidos e podem ler código
relacionado. Identificação Git/digests/limpeza/CI e execução dinâmica são do coordenador.

Nenhum teste aprova conteúdo real. Extração/sanitização/CLI/HTTP/UI/curadoria permanecem
pendentes, assim como meta editorial, deployment e smoke Google com credenciais reais.
Preservar gabaritos estáticos públicos não é promessa de sigilo global das respostas.

## Verificação executada pelo coordenador no topo inicial

- JDK25 / Gradle `clean build`: 86 testes backend, zero falhas/erros/skips, PostgreSQL18.6 real.
- `npm ci`, `npm test`, `npm run build`: 47 testes frontend e build passaram.
- `STUDY_ROOT` construído / Playwright: seis e2e, incluindo nove módulos/90 resoluções offline.
- `npm run test:identity`: três browser reais com backend/PG/OIDC local isolados, não Google externo.
- Guards Python: 18; OpenSpec `validate --all --strict`: três mudanças válidas.
- Material: 108 páginas/1661 links na fonte; 108/1757 no pacote gerado, sem alterar a fonte.
- `legacy-inventory.json` mantém hashes dos inventários/math_content e lista paths alterados;
  identidade, workflows, frontend, aulas e migrações V1–V3 não foram modificados.

## Medição

Metadados da API de mensagens, por chamada, sem prompts/segredos; paginação `limit=1`
seguindo `cursor.next`, IDs deduplicados. Entrada nova/cache read/cache write/saída e
raciocínio reportado separados; não somar raciocínio à saída nem inferir gasto por quota.
Preço não disponível no catálogo de modelos: nenhum custo financeiro inventado.
Coordenador é consumo parcial desde a autorização da rodada, com intervalo registrado;
não incluir chamadas posteriores nem confundir com consumo integral da implementação.

## Verificação do conjunto corrigido

Head `05518635fe160b92e8798dd78c03eff96f6234e5`, camada corretiva #46 sobre #45;
nenhum SHA/base de #38–#45 ou #39 foi reescrito. `final-manifest.json` registra o novo
digest combinado e individual; relatórios iniciais permanecem ligados ao snapshot inicial.
Não atualizar o digest do relatório anterior como se já revisasse este head.

Coordenador executou novamente no topo final: Gradle clean build (91 testes, zero
falhas/erros/skips), frontend 47 testes/build, seis browser/offline, três identity-browser,
18 guards e três OpenSpec strict. Três controles negativos antes da correção demonstraram
as rejeições ausentes (ABA do ponteiro, ciclo editorial e UTF-16), sem relaxar expectativas.
Round-trip enriquecido e pacotes multiorigem passaram; três repetições anteriores dos 11
testes de importação/storage não demonstram todos os interleavings. CI dos oito PRs
de produto/processo foi conferida verde no head final; dados/URLs/horário em checks.json.

A causa/regra positiva/regressões/limites estão em `docs/question-bank-round1-regressions.md`
no PR #46. O custo global dos triggers permanece, sem medição de degradação; adiamento
opcional é proposto ao usuário, não apresentado como correção.

Esta evidência não autoriza merge/publicação/arquivamento. Antes de qualquer primeiro
merge da stack, apresentar composição/SHAs/checks atuais e caminhos ainda não ensaiados
(merge/prefixo/fila/sync pós-merge) para decisão explícita. Aprovar topo não aprova inferiores.
