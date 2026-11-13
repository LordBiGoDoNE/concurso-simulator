# Retomada da issue #173 — checkpoint de 12/11/2026

O usuário solicitou salvar o estado e criar um commit para testar outra IA.
A implementação está **incompleta**. Os agentes foram interrompidos; não houve
push, merge, deploy, importação de conteúdo real ou aprovação no banco.

## Código e comportamento testado

- Branch: `task/question-bank-expansion/practice-capacity`; base
  `226d2e5b07208887941811beff10ac29295b04b3` (#161 integrada pelo PR #176).
- Meta do acervo: 25 bases por assunto/modalidade; cada prática nova sorteia
  até cinco do pool elegível inteiro. Usuário testou e respondeu “Deu certo”.
- Capturas antigas de 25 são preservadas. V15 permanece para compatibilidade;
  não reescrever V1–V15. Codec preserva o JSON imutável ao gravar respostas.
- Evidência anterior: backend 341 testes (340 passaram, um skip histórico de
  symlink), frontend 238, Chromium/Edge 30 e reinício/backup reais passaram.
  Consulte `practice-sampling-five-verification.md` na spec para o recorte exato.
- Em 12/11, a suíte Python de expansão passou 16 testes. O auditor foi corrigido
  para preservar operadores, expoentes e pontuação: a normalização anterior
  confundia operações diferentes. Limiares mantidos; variantes reais continuam
  bloqueadas. A auditoria global v4 foi interrompida a pedido do usuário e deve
  ser executada novamente. Isso não é revisão independente por modelo.

## Acervo externo preservado (somente local)

Diretório relativo à raiz deste worktree:
`acervo-local/question-bank-expansion/2026-11/`.
Após o primeiro checkpoint (`a6b286e`), o usuário pediu incluir também os arquivos
temporários para continuar no Claude; isso ficou só nos commits locais
`eb251ab`…`39e410d` (branch local `task/question-bank-expansion/practice-capacity`,
nunca enviada, em checkout no worktree `7ac1`). **Decisão do usuário de 13/11/2026:**
as branches publicadas não levam o acervo bruto (≈1,5 GB, 711 PDFs de provas de
terceiros) nem `backend/build`; levam código, scripts, docs, os pacotes finais e
manifesto SHA-256 dos arquivos locais. `acervo-local/` passa a ser ignorado pelo Git.
Abra este repositório no Claude e comece pela leitura deste guia e de `AGENTS.md`.
JSONs, provas e gabaritos continuam como arquivos separados dos artefatos da aplicação.

- `edition/` e `packages/`: entrega histórica de 79 candidatas, preservada.
- `work/aggregate-wip/`: última consolidação de **2.342 candidatas**, em 102
  células; 2.043 autorais e 299 originais de provas. São quantidades preparadas,
  não cobertura elegível nem cota certificada. Novos arquivos dos agentes podem
  estar mais adiantados que essa consolidação.
- `work/{portugues,matematica,informatica,administrativo,escolar}/`: entradas,
  geradores, fontes, provas, gabaritos, oráculos, correções e quarentenas.
- `work/reader-batch-2026-11-11-2270/`: snapshot de 102 pacotes/2.270 candidatas
  que passou pelo leitor/prévia Java em PostgreSQL isolado, sem escrita editorial.
  `snapshot-manifest.json` e `reader-preview-report.json` identificam seus bytes.
  Esse resultado não aprova automaticamente versões posteriores.
- `work/aggregate-audit-v3/`: auditoria anterior à correção da normalização.
  Algumas decisões ficaram obsoletas porque prefixar IDs de referências mudou
  os bytes da fonte/proveniência e os fingerprints. Há revalidações individuais
  `pair-decisions-v3.json` em Matemática e Informática, ainda não adotadas pelo
  agregador. Não atualizar hashes para fazer uma decisão passar: reler o conteúdo
  atual e conferir fonte/proveniência. Preservar as decisões anteriores.
- Existem 51 conferências individuais assistidas por IA de originais de
  Administrativo/Escolar em `aggregate-wip/reviews.json`. Isso não comprova
  diversidade global, aprovação institucional, revisão humana ou elegibilidade.
- Cinco equivalências escolares foram substituídas em 12/11 e preservadas em
  `work/escolar/diversity-corrections-2026-11-12.json`. Há outras semelhanças
  semânticas que o auditor textual pode não detectar; a revisão não terminou.

O checkpoint local do acervo tem manifesto próprio de arquivos/hashes. A cota
continua sendo cinco originais distintas por célula: 510 no conjunto. Não somar
excedente de uma célula para cobrir outra, repetir cargos/questões entre modalidades
ou contar anuladas e variantes. Outros bancos compatíveis podem complementar a
Avança SP. A avaliação de reutilização está em `docs/question-source-reuse-assessment.md`;
não se afirmou licença expressa da banca nem se autorizou copiar obras de terceiros.

## Ferramentas de trabalho e dados privados

Os auxiliares locais estão em `backend/build/question-expansion/`. Os arquivos
de trabalho selecionados foram adicionados explicitamente ao Git, embora a pasta
de build continue ignorada para novas saídas. Principais: `consolidate_work_edition.py`,
`export_work_packages.py`, `derive_admin_originals.py`,
`derive_school_originals.py`, `prepare_scanned_additions.py` e
`ocr_public_proof.ps1`. OCR exige confronto visual, inclusive lacunas e símbolos.

Backups, credenciais, logs de execução e caches ficaram fora do commit. Os arquivos
privados continuam apenas neste computador: **não os versionar nem publicar**.
As rotinas de restauração dependem deles e não restauram o banco apenas com um clone.
`local-destination-private/metadata-private.json`
identifica o backup mais recente usado, SHA-256
`4698e618ccca07edc101bb4034b153112429c0bab2752dfbd04d60f91abcf0bb`.
Em 11/11, a restauração conferiu igualdade de 25 tabelas de negócio e preservou
duas tentativas. O destino observado tinha 54 questões sintéticas, 52 elegíveis,
zero reais; esses números não contam no acervo. Banco do operador/VPS não foi
consultado. Histórico offline 251/255 não é carga atual.

O ambiente local pode estar parado. Para retomá-lo com o backup validado, há
`restart_local_environment.ps1` e `verify_current_restore.py`; conferir portas
5393/19370/19371 antes, preservar dados e não usar bancos de outros projetos.
Não presumir que processos anteriores continuem ativos. JDK necessário:
Temurin 25 instalado localmente, indicado em `JAVA_HOME` (o PATH padrão pode ter Java 21).

## Retomada no Claude — 12/11/2026

Base: commit `eb251ab`, mesmo worktree/branch. Sem push, merge, importação ou habilitação.

- Suíte Python de expansão: 16 testes passaram. A reconsolidação a partir das entregas
  individuais reproduziu o agregado **byte a byte** (2.342 candidatas, 299 originais):
  nenhuma entrega posterior estava fora da consolidação.
- Déficit real por célula (prepared, não verificado): 75 de 102 células abaixo de 25,
  faltando 229 itens; 75 células abaixo de cinco originais, faltando **231 originais**.
  As autorais já estão em ~20 por célula; o gargalo é obter originais de provas.
- Auditoria corrigida v4 (`work/aggregate-audit-v4/`, ~3,5 min): 133 pares, 90 bloqueando
  (61 STALE, 29 PENDING). 61 eram cobertos pelas releituras v3 de Matemática/Informática
  com fingerprints atuais. Os outros 29 foram relidos pelo conteúdo atual
  (`aggregate-audit-v4/adjudicate_v4.py` → `work/{matematica,informatica,portugues}/pair-decisions-v4.json`):
  todos DISTINCT — itens diferentes do mesmo texto-base CEBRASPE, com gabaritos
  conferidos, funções/atalhos distintos de Informática e duas regências de Português.
- `consolidate_work_edition.py` agora aplica decisões em camadas (base/additions → v3 → v4).
  As substituídas ficam em `aggregate-wip/pair-decisions-superseded.json` (149: 68 por
  mudança de conteúdo e 81 só relidas; nenhuma inversão DISTINCT/EQUIVALENT).
- Auditoria após adoção (`work/aggregate-audit-v4-adopted/`): 133/133 pares CURRENT,
  zero bloqueios. A cobertura verificada continua baixa: só **51 itens** têm revisão
  individual em `reviews.json`, logo 51 revisados/51 originais verificados. Triagem
  textual não decide equivalência semântica, e as decisões são assistidas por IA
  pelo próprio preparador, sem revisão independente.

## Originais de prova — rodada Claude de 12/11/2026

Download autorizado pelo usuário. Catálogo público Avança SP (179 concursos, 45 com prova
+ gabarito definitivo): `work/shared-sources/avanca-catalog-2026-11-12.json`. Provas e
gabaritos com SHA-256 em `shared-sources/avanca-2026-11-12/` e cópias nos dossiês dos grupos;
datas de aplicação conferidas nos editais de convocação (`convocacoes/`, `check_exam_years.py`).
Regras e formato: `shared-sources/BRIEF-claude-originals.md`. Cinco agentes, um por grupo,
gravaram `work/<grupo>/claude-originals-*` (linhas, fontes, ledger com aceitos/rejeitados/
déficits, downloads e geradores). O consolidador incorpora `claude-originals-*`.

- Aceitas: Português 22, Matemática 37 (+1 em quarentena), Informática 52, Administrativo 34,
  Escolar 41 = **186**. Agregado: **2.528 candidatas, 485 originais**. Cada item teve
  transcrição e gabarito definitivo conferidos visualmente pelo agente; checagem automática
  de texto (`check_transcription.py`): 134 coincidem, 47 de cadernos digitalizados, 5
  sinalizados e relidos como falsos positivos de fonte matemática/colunas.
- Anos: 5 itens de Informática corrigidos de 2025 para 2026 (Americana e Potim PS aplicaram em 25/01/2026).
- Auditoria v5: 8 pares novos. `tai26-alm-q12` (área b·h/2) é EQUIVALENT a `legacy-v3:M09-Q04` e foi
  para `matematica/claude-originals-quarantine.jsonl`; os demais DISTINCT
  (`matematica/pair-decisions-v5.json`). Após adoção (`aggregate-audit-v5-adopted/`): 136/136
  CURRENT, zero bloqueios.
- **Déficit real restante: 45 originais em 20 células** — P01 (3+1); M06p 1, M07 (3+2), M09p 1;
  I08p 1; S02 (3+2), S03 (3+4), S04 (1+1), S05 (4+4), S06 (2+2), S09p 1, S11 (3+3). Motivos no
  ledger de cada grupo: interpretação quase sempre depende de texto de terceiros; gráficos de
  terceiros ou não transcritíveis; macros e escrituração/arquivo/censo escolar raros nas provas.
  Releitura pendente no Escolar: 219 pp. 6–17, 256 pp. 6–16, 259 pp. 6–12.
- Gabaritos oficiais duvidosos foram descartados, não corrigidos (ver `rejected` nos ledgers).
- Continua valendo: só 51 itens têm revisão individual em `reviews.json`; nenhuma revisão
  independente por modelo foi executada; nada importado ou habilitado.

## Rodadas 2 e 3 de originais — 12/11/2026

- Rodada 2 (outras bancas permitidas, briefing atualizado): Português +4 (FGV, P01 fechada),
  Matemática +7 (Avança/FGV; M06, M07, M09 fechadas), Informática +1 (FGV Recife, I08),
  Escolar +17 (Fundatec com gabarito em PDF, Avança 219).
- Rodada 3: os gabaritos definitivos Fundatec dos concursos 679 (Foz do Iguaçu/PR), 704 (Ibarama),
  739, 794 (Sapucaia do Sul) e 799 (Ibiraiaras) são gerados por formulário PHP bloqueado a acesso
  externo direto. Com o link do usuário, o coordenador fez a consulta no navegador embutido e gravou
  o texto integral (respostas e assinatura eletrônica) em `escolar/dossier/fundatec-r3/`; o download
  automatizado dos cadernos é bloqueado por desafio anti-robô e **não foi contornado**: o usuário
  salvou os cinco PDFs. Datas de aplicação pelos editais de data/local (704 aplicada em 08/01/2023).
  Escolar +10; letras conferidas uma a uma contra os gabaritos salvos.
- Correção de transcrição: Redentora q30, alternativa C ("e da filha").
- Tesseract 5.4 + `por.traineddata` (4.1.0) instalados localmente (diretório `tessdata` indicado por `TESSDATA_PREFIX`).
  `ocr_scanned.py` (triagem) e `check_transcription_ocr.py` (segunda checagem): 49 itens de cadernos
  digitalizados, 10 sinalizados e todos relidos sem correção. Checagem por texto: 171 coincidem,
  5 falsos positivos já relidos.
- Agregado: **2.567 candidatas, 524 originais**; auditoria v7: 136/136 CURRENT, zero bloqueios.
- **Déficit real restante: 6 originais em 3 células** — S03 prática 1 e S11 Censo Escolar (2 sim +
  3 prat). Censo não aparece nas provas Fundatec 2022–2026 nem na Avança disponível; Saeb de
  Varginha q48 aguarda conferência da portaria do Inep (in.gov.br 403). Opção para S03: caderno
  de Atendente Escolar do 739 (0739.003), não obtido.

## Rodada 4 de originais — 12/11/2026

- S11 fechada com o PSS 2025 da Prefeitura do Crato/CE (Edital nº 002/2025, Secretário Escolar,
  prova em 05/10/2025 conforme edital de alteração do cronograma): questões 17 e 19 no simulado,
  14, 16 e 18 na prática, em `escolar/dossier/s11-r4/` (caderno com texto, gabarito oficial de
  05/11/2025, cronograma). Letras conferidas automaticamente contra o gabarito por
  `claude-originals-generator/round4.py`. A q15 foi descartada (consequência "repasse cancelado"
  imprecisa); FUNCERN/AMCEVALE q27 ficou sem uso por falta de gabarito definitivo localizado.
- Resoluções apoiadas no Decreto 6.425/2008 (arts. 2º, 4º e 5º) e nas páginas do Inep (Censo
  Escolar e FAQ: etapas Matrícula Inicial/Situação do Aluno, níveis executor/superusuário/leitor).
- Agregado: **2.572 candidatas, 529 originais**; auditoria v8: 136/136 CURRENT, zero bloqueios,
  nenhum par novo com os itens do Crato.
- **Déficit restante: S03 prática 1.** O gabarito definitivo do 739, cargo 3 (Atendente Escolar),
  foi consultado no formulário Fundatec (mesma sequência e assinatura 24385 do cargo 17); falta o
  caderno `0739.003_CE_Atendente_Escolar_NS_DM_POS-PRELO.PDF`, cujo download automatizado é
  bloqueado pelo site e não é contornado — depende de o usuário salvá-lo.
- Rodada 5: o usuário salvou o caderno 0739.003. A q34 (documentação escolar: ficha de matrícula e
  histórico V; folha ponto e guia de prorrogação F; gabarito D) fechou S03/PRÁTICA
  (`claude-originals-generator/round5.py`, evidências `fundatec-r3/739-*-atendente.*`, prova em
  26/03/2023). O gabarito do cargo 3 coincide com o do cargo 17, mas os cadernos diferem; as letras
  conferem com o conteúdo do 0739.003 (ex.: q31 requerimento A, q36 Conselho Tutelar A).
- **Agregado final: 2.573 candidatas, 530 originais; 102/102 células com ≥25 bases e ≥5 originais;
  zero déficit.** Auditoria v8 (refeita após a rodada 5): 136/136 CURRENT, zero bloqueios.
  Validadores sem erros nos cinco grupos; 16 testes de expansão OK.

## Revisões individuais — rodadas r1 e r2, 12/11/2026

- `work/reviews-r1/`: 2.522 candidatas sem revisão exportadas em 20 lotes (`export_review_shards.py`),
  revisadas por subagentes Claude em contexto próprio pelos sete critérios (`BRIEF.md`,
  `verdicts/`): 1.344 PASS, 1.178 FIX, 0 REJECT; nenhum gabarito errado.
- Correções aplicadas pelo consolidador numa camada auditável (`reviews-*/corrections.json`, troca
  do campo inteiro com valor anterior conferido; `source-corrections.json` para espaço em fontes
  compartilhadas; `sources.json` para fontes novas por questão). Fontes e edição histórica não são
  editadas. Pipeline: `reviews-r1/run_pipeline.py` (lê `reviews-r1/base`, cópia pré-correção).
  Descrição da fonte OBMEP nas originais de Matemática corrigida (`source-overrides.json`).
  38 suspeitas de transcrição conferidas no texto da prova (`transcription-confirmed.json`);
  "descreve o o atendimento" consta do original e foi mantido.
- `reviews-r1/BRIEF-r2.md` + `verdicts-r2/`: segunda conferência das 1.067 corrigidas (861 PASS,
  206 FIX); correções em `reviews-r2/corrections.json` (`build_r2.py`, base pós-r1).
- `aggregate-wip/reviews.json`: **2.297 de 2.573 com revisão válida** (fingerprint atual). Situação
  em `reviews-r2/not-yet-reviewed.json` (`status_r2.py`). Restam 276: 146 corrigidas na r2 sem nova
  conferência; ~65 sem fonte pública adequada (P07/P08 concordância nominal e regência verbal sem
  página oficial; 40 de A04 citam manual do Rioprevidência hoje atrás de login — cópia do Internet
  Archive não foi baixada por bloqueio de permissão; 5 com Ajuda do Google recusada pela regra de
  URL "answer"); 14 reclassificações (S04→S01, S09→S10) que deixariam células com 25 abaixo do
  mínimo sem questões de reposição; e casos pontuais.
- Pares: as correções mudaram fingerprints; auditoria v9 com 40 decisões STALE e 2 PENDING
  (bloqueando 42) — readjudicar após estabilizar o conteúdo.

## Revisões individuais concluídas — rodadas r3 a r6, 12/11/2026

- Camadas aplicadas pelo consolidador, em ordem (`work/reviews-r1` … `reviews-r6`, cada uma com
  `base/` = agregado antes dela): r3 fontes (`sources-out-*.json` de subagentes que abriram cada
  documento; `coordinator-corrections.json`/`coordinator-sources.json`), r4 ajustes da conferência
  final, r5 pontuação mecânica (".”." → ".”" em 496 resoluções), r6 ajustes da conferência r4.
  `reviews-r2/source-corrections.json` normaliza espaço em 36 nomes/dispositivos de fontes.
- Fontes: Rioprevidência (atrás de login) substituído pela IN SEDAP 205/1988 (PDF público da UFF),
  apostila Enap "Gestão de materiais" (Fenili, 2013) e manuais TJRJ/UFPA; concordância/regência com
  manuais oficiais públicos (Redação Oficial do Executivo do RS 2024, TCDF 2014, TJSP, STJ, Senado,
  FUNAG) e, onde não havia, dicionários (Aulete/Michaelis) — indicado na nota; busca no Google
  sem a Ajuda bloqueada pela regra "answer" (Pesquisa avançada e Search Central).
- Reclassificação: 12 autorais S04→S01 e 2 S09→S10; reposição com 14 autorais novas
  (`escolar/claude-autoral-*.jsonl`, consolidador `add(...,'claude-autoral',...)`), revisadas na r3.
- Conteúdo corrigido por decisão do coordenador: pt-p08-29 (distrator "Que" virou "Em que", pois
  necessitar também admite objeto direto), novo-ara-q05 (resolução explicita regência tradicional),
  a04-29 (enunciado cita a IN SEDAP). "descreve o o atendimento" consta da prova e foi mantido.
- Revisões renovadas por recompilação com e sem camadas só de normalização
  (`reviews-r2/revalidate_layer.py`); 10 resoluções da r6 e 1 decisão de transcrição registradas
  como leitura do coordenador (não independente), dito no `limits`.
- Pares: `aggregate-audit-v9/adjudicate_v6.py` → `work/<grupo>/pair-decisions-v6.json` (33 mantidas
  por problema inalterado, 9 relidas). **Auditoria v10: 2.587 candidatas, 138/138 CURRENT, zero
  bloqueios; cobertura com 2.587 revisadas, 530 originais verificadas, 102/102 células com ≥25
  revisadas e ≥5 originais.** Validadores sem erros; 16 testes de expansão OK.
- Observação aberta: vários distratores autorais de A04 são fracos (eliminação fácil); os revisores
  não os reprovaram pelo critério "só se grave". Revisão humana e revisão independente
  `openai/gpt-6.1-sol#xhigh` continuam pendentes; nada importado ou habilitado.

## Habilitação local — 12/11/2026

A habilitação das 2.587 questões no ambiente de teste exigiria 5.174 decisões pela CLI
`questionBankLocal decide` (uma por execução, confirmação digitada). O usuário decidiu não
automatizar essa confirmação e criar autorização editorial em lote por rota/API, fora do escopo
da #173: [#177](https://github.com/LordBiGoDoNE/concurso-simulator/issues/177). Nada foi importado
nem alterado no ambiente local (54 sintéticas, 0 recibos); a importação/habilitação aguarda essa
decisão.

## Próximos passos

1. Ler AGENTS, a spec aprovada, tarefas e este checkpoint; não reiniciar do zero.
2. Reconciliar as últimas entregas individuais com o agregado; concluir fontes e
   cotas, conferir cada item e substituir equivalências reais sem perder histórico.
3. Reexecutar a auditoria corrigida, adjudicar os pares atuais e congelar os pacotes
   finais para leitor/prévia. Preparação, importação DRAFT e habilitação são etapas
   distintas; usar os mecanismos existentes, sem aprovação pelo startup ou HTTP.
4. Entregar conteúdo conferido para teste local preservando tentativas e registrando
   seleção/fingerprints/digests/destino. Não declarar os grupos completos antes disso.
5. Depois do teste, cumprir revisões independentes e gate cumulativo com o modelo
   exigido em AGENTS (`openai/gpt-6.1-sol#xhigh`), em contexto novo. Nenhuma revisão
   desse tipo foi executada. Publicação/merge/deploy não estão autorizados pelo commit.
