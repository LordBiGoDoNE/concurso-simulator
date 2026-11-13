# Expansão editorial 2026-11 — operação local #173

Checkpoint de 12/11/2026, solicitado pelo usuário: [estado e retomada](question-bank-expansion-handoff.md).
O conteúdo foi ampliado em arquivos externos; os números de 79 candidatas neste
guia são históricos. Nenhuma cota final ou carga real foi declarada concluída.

Implementação local autorizada em 2026-11-10 para os grupos 1–8. A dependência
#161 foi integrada à main pelo PR #176, merge `226d2e5b07208887941811beff10ac29295b04b3`.
O usuário pediu continuar e o grupo 2 está em implementação sobre essa base:
prática por assunto sem cargo, captura formato4 preservada. O esclarecimento atual
do usuário mantém25 no acervo e até cinco sorteadas por prática. A V14 também
limitava o banco a cinco; o [ajuste V15 autorizado](../openspec/changes/archive/2026-11-13-question-bank-expansion/capacity-schema-amendment.md)
amplia apenas esse CHECK, sem regravar dados. Grupo 8 continua dependendo de 2–7.
Não há stack, cópia de código ou merge entre specs.

Base inicial: `16ca8aef382d64e7652b280b413122c357775ead`; base atual do grupo 2:
`226d2e5b07208887941811beff10ac29295b04b3`. Trabalho exclusivo agora em
`task/question-bank-expansion/practice-capacity`, worktree `7ac1`; branch anterior
`task/question-bank-expansion/inventory-tooling` e ref de preservação mantidas.
Épica local avançada por fast-forward, sem mover referências remotas. Documentos
adotados de `ecb80c1ebd4b011c9f41c38283080ab56b88be21`, preservando o PR #174.
O [registro na issue](https://github.com/LordBiGoDoNE/concurso-simulator/issues/173#issuecomment-6778396819)
separa autorização local de publicação/revisão/integração.

## Regra atual de prática

25 é a meta do acervo por assunto/modalidade; cada novo treino sorteia até cinco
do pool próprio inteiro. `available` informa o acervo real e `maximum:5` o tamanho
do treino. Capturas de25 já criadas na etapa anterior são preservadas; V1–V15 não
são reescritas. Novo commandId sorteia de novo; repetir o mesmo mantém a captura.

## Inventário e fontes

[Edição](../editorial/question-bank-expansion/2026-11/README.md),
[matriz pública](../editorial/question-bank-expansion/2026-11/inventory-baseline.json) e
[prospecção](../editorial/question-bank-expansion/2026-11/source-inventory.json).
As 102 células distinguem histórico, candidatos, revisados, importados com assunto
conhecido e elegíveis observados. Sem observação do destino, elegibilidade e déficit
real são `null`; o déficit de referência de 2.044 não é uma consulta atual.

O inventário recompila o legado e sobrepõe as revisões curadas por namespace/chave.
Preserva M04-Q05 e as 255 práticas de outubro. Não duplica por cargo ou revisão;
templates/variantes não entram como novas bases. Referências históricas permanecem
251/255, com distribuição por assunto. Uma origem não selecionada historicamente
não é uma questão rejeitada nem fica liberada de nova triagem.

Fontes prospectadas priorizam Avança SP: Francisco Morato 01/2025, Potim 01/2025
(dois cargos), Araçariguama 01/2026. A data da prova pode ser 2026 embora o edital
seja 2025. Os metadados registram links oficiais, hashes realmente baixados,
classificação inicial e pendências. Barretos 01/2024 foi excluído por cancelamento.
O aviso de reprodução de um documento antigo CESPE/UnB é prospecto complementar,
sem generalizar sua permissão para outras provas ou afirmar aderência/gabarito.

**Déficit atual de originais verificadas nesta edição: dez por assunto, 510 no
conjunto.** A disponibilidade total ainda é desconhecida; não foi demonstrada
impossibilidade de localizar outras fontes. As 120 posições prospectadas não são
120 questões aprovadas, distintas semanticamente ou aptas à cota. A classificação
dessas posições foi preliminar e está preservada como histórico. A retomada incluiu
Nova Odessa 01/2026 e prepara **79 candidatas originais em 57 células** (43 de simulado,
36 de prática), ainda sem revisão integral ou contagem na cota.

O usuário esclareceu que o acervo será importado por JSON externo e não possui
licença anterior. A [avaliação documentada de reutilização](question-source-reuse-assessment.md)
admite, para questões oficiais autossuficientes, fundamento no art. 8º, IV, da Lei
9.610 e no precedente indicado do TJPR, com seus limites. Isso não é licença
expressa da Avança SP. A separação em JSON e a venda apenas das funcionalidades
não determinam, sozinhas, o enquadramento do conteúdo. Elementos de terceiros
recebem análise própria. Pesquisa não autoriza contatar bancas nem presume
impossibilidade de obter outras fontes aptas.

## Preparar entradas e selecionar

Os dez JSONL separam disciplina e modalidade. A pasta versionável mantém um
esqueleto vazio; o **acervo real está separado**, em diretório explícito ignorado:
`backend/build/question-expansion/external-2026-11`. `--edition-root` aceita outro
diretório externo sem embutir acervo no código/runtime. Fixtures são sintéticas e
permanecem em testes/build. Cada linha real tem `id`, `topicId`, `originKind`, `author`, `statement`,
`alternatives` (`id`/`text`), `correctOption`, `resolution` e `references` (IDs do
registro `sources.json`). `originKind`: AUTORAL, ADAPTADA ou ORIGINAL_DE_PROVA.
Identificar assistência por IA na autoria/revisão quando aplicável, sem alegar
revisão humana independente. A resolução tem etapas explícitas `1.` e `2.`.

Referências contêm `name`, `url`, `checkedOn`, `legalBasis` (null ou dispositivo/data).
URLs são referências de estudo, não gabaritos. Fontes normativas/software exigem o
recorte, dispositivo/versão/idioma que sustentam a resposta. `checkedOn` registra
conferência efetiva; preencher data não faz uma consulta. `checkExpr` opcional usa
o oráculo aritmético restrito existente e confronta a resposta correta; não prova
interpretação, unidade, distratores ou classificação.

Originais exigem `exam`: `board`, `competition`, `year`, `role`, `booklet`, `number`,
`identity`, `status=VALID`, `definitive=true`, `proof`, `answer`, `reuse`. Cada evidência
tem `path` relativo sob `dossier/`, `sha256`, `url`, `note`; os bytes devem existir e
corresponder ao hash. Guardar dossiês não publicáveis fora do Git. `identity` une
republicações/cadernos da mesma questão: ID diferente não transforma uma repetição
em original nova. Adaptações não contam na cota original. Os validadores conferem
estrutura/bytes; a interpretação da permissão e correspondência da prova são tarefas
editoriais, nunca certificadas por um campo booleano.

Após editar entradas/fontes, atualizar deliberadamente seus SHA-256 em `edition.json`.
Não alterar os arquivos históricos. A compilação recusa hash divergente, JSON
ambíguo, catálogo, origem, finalidade, cálculo e seleção inválidos. O manifesto é
integridade de entrada, não aprovação. A fonte normalizada autossuficiente de cada
item é JSON canônico com conteúdo/proveniência; acrescentar outro item não altera
seu fingerprint. IDs de alternativas são estáveis por origem/ID editorial; mudar a
ordem da entrada preserva o pacote, alterar gabarito/conteúdo invalida a seleção.

```powershell
python -X utf8 scripts/question_expansion.py --output backend/build/question-expansion/planned
python -X utf8 scripts/audit_question_expansion.py --output backend/build/question-expansion/audit
```

Para o acervo externo preparado nesta retomada:

```powershell
python -X utf8 scripts/question_expansion.py --edition-root backend/build/question-expansion/external-2026-11 --output backend/build/question-expansion/real-plans
python -X utf8 scripts/audit_question_expansion.py --edition-root backend/build/question-expansion/external-2026-11 --output backend/build/question-expansion/real-audit
```

O acervo contém prova/gabarito com hashes, identidade única, referência de estudo
e resolução nova identificada como assistida por IA. Quatro itens ficaram fora
dos pacotes por inconsistência ou equivalência identificada. Os cálculos de 13
itens foram confrontados com oráculos independentes do compilador. Os sete checks
editoriais por fingerprint e a adjudicação semântica final continuam pendentes.
Não há revisão humana ou aprovação presumida.

Uma cópia local íntegra foi preservada fora de `build`, em
`acervo-local/question-bank-expansion/2026-11/`, ignorada pelo Git. `edition/`
é a entrada do compilador, `packages/` contém as requisições e a leitura editorial,
`audit/` contém os relatórios e `delivery-manifest.json` confere os bytes copiados.
Essa pasta não depende da permanência dos outputs do Gradle e não é carregada
automaticamente pela aplicação. Também pode ser usada com `--edition-root`.

O primeiro comando gera planos `selection.json` e dossiês por assunto/modalidade;
não gera requisições importáveis com seleção implícita. Conferir/copiar o plano,
limitar cada lote a 1–100 itens e preencher `expectedLatestRevision` com null para
origem nova ou UUID corrente conferido para edição. Seleção exige chave/fingerprint
atuais e não pode atravessar assunto/modalidade. Exemplo de comando **após haver
seleção real conferida**, sem chamada à API:

```powershell
python -X utf8 scripts/question_expansion.py --topic P01 --purpose SIMULADO --selection backend/build/question-expansion/selected-P01.json --output backend/build/question-expansion/P01-simulado
```

Ao selecionar o acervo externo, acrescentar o mesmo `--edition-root` usado no plano.
Os pacotes concretos desta retomada ficam em `backend/build/question-expansion/real-packages`;
`manifest.json` registra seleção/fingerprints/digests e `content-review.md` apresenta
o conteúdo para conferência local. O ZIP externo contém esses pacotes, sem os PDFs.
Essas saídas são candidatas DRAFT para preparação: não foram importadas/habilitadas.

A saída é `preview-request.json` no contrato #160, mais `digests.json` com os digests
binários da API. Hash JSON do arquivo não substitui `packageDigest`/`selectionDigest`.
Pacotes usam `admin.2026-11.<assunto>.<s|p>.<id>` e namespaces existentes;
até 100 itens/fontes, 1 MiB por arquivo, 8 MiB decodificados e 16 MiB no envelope.
Não há PDF no payload, recurso novo, mudança de limite ou endpoint novo.

## Triagem e revisão

`pair-decisions.json` registra `pair` ordenado (namespace:chave), `fingerprints` na
mesma ordem, `decision` DISTINCT/EQUIVALENT, `rationale`, `responsible`, `at` com
data/hora. Conferir ambos os problemas, não justificar apenas pelo limiar. Mudança
de qualquer fingerprint torna a decisão STALE. Exatidão textual/estrutura numérica
idêntica continua bloqueando diversidade, mesmo se alguém marcar DISTINCT.

A triagem compara a edição com os dois acervos, os 509 candidatos válidos do legado,
as revisões curadas e M04-Q05. Pares entre disciplinas também passam por comparação
textual; semelhança numérica e intenções diretas conhecidas complementam o sinal.
A igualdade textual compara enunciado, alternativas e recursos, sem IDs/ordem ou
gabarito, para não confundir um comando genérico com o mesmo problema completo.
Só são emitidos pares tocados por candidato novo. Limiares não demonstram ausência
de paráfrase/equivalência semântica. Decisões sem par atual são listadas para conferir.

`reviews.json` contém `key`, `fingerprint`, os sete `checks`, `responsible`, `at`,
`limits`. Checks: CORRECTNESS, DISTRACTORS, CLARITY, STEP_BY_STEP, ACCESSIBILITY,
SOURCES, CLASSIFICATION. Mudança invalida a conferência. Revisados só são contados
quando o fingerprint coincide e não há par bloqueante; isso não habilita o banco.
Originais do dossiê são contadas separadamente; o relatório não certifica a meta.

## Observação privada do destino

O comando de auditoria aceita `--observation <arquivo-privado>`. Nunca conecta banco
automaticamente. Envelope: `destination` lógico sem DSN, `observedAt` ISO com fuso,
`complete=true`, `report` obtido pelo comando `questionBankLocal report` existente,
e `contents`, um mapa fingerprint → conteúdo completo das revisões necessárias.
Remover apenas o cabeçalho de console antes de compor o JSON; não reconstruir números
a partir do histórico. Conferir os conteúdos por fingerprint no snapshot privado.

O contador segue o ponteiro elegível, não a revisão mais nova; recusa bases/origens
repetidas, revisão não aprovada e ausência/divergência do conteúdo elegível. Originais
elegíveis ficam desconhecidas até vincular observação/dossiê: não deduzir sua origem
do namespace ou da quantidade. Não versionar snapshot, recibos, atores privados,
credenciais ou backups. Este estágio não consultou banco do operador ou VPS.

## Ensaiar, aplicar e entregar

Testes Python e Java abaixo usam dados explicitamente sintéticos. O teste Java gera
pacotes completos pelo Python, valida fingerprints/digests pelo leitor real e usa
PostgreSQL 18.6 exclusivo, schemas/portas aleatórios. O teste HTTP usa Spring Security,
sessão/CSRF reais e destino lógico exclusivo, importando 100 itens por modalidade.
Verifica prévia sem escrita, confirmação, DRAFT, ausência de elegibilidade/decisões,
replay, separação e ausência de correção nas respostas. Isso não aprova acervo real.

`QuestionExpansionContentCheck` é uma ferramenta somente de teste: recebe diretório
de requisições já selecionadas e exercita leitor, digests e prévia sobre PostgreSQL
novo exclusivo. Confere zero questões/recibos/decisões persistidas e grava relatório
privado. Não aplica/importa, não aprova conteúdo e não consulta o banco do operador.

```powershell
python -X utf8 -m unittest discover -s scripts/tests -p test_question_expansion.py -v
./backend/gradlew.bat -p backend --offline --no-daemon test --tests '*QuestionExpansion*Tests' --tests '*AdministrativeImportHttpTests' --tests '*AdministrativePackageTests' --tests '*AdministrativeDigestTests' --tests '*AdministrativeImportPersistenceTests'
```

Para carga real futura, seguir [a operação administrativa](question-administrative-import.md)
e [as decisões locais](question-local-operations.md). Backup privado/destino/imagem,
seleção e digests conferidos precedem prévia/aplicação; registrar recibo real. Novos
itens entram DRAFT. Conferência/habilitação usa `decide` separado com fingerprint,
ponteiro/seqüência esperados; não executar aprovação por startup, upload ou CI.
Tarefas e dossiês de disciplina continuam obrigatórios antes do grupo 8.

O contexto Docker permitido, o JAR (somente src/main/resources) e o material público
não incluem `editorial/question-bank-expansion`, scripts/fixtures de teste ou build.
Usar o empacotamento filtrado `local_app_images.py`, não enviar checkout completo ao
Docker. Nenhuma alteração de allowlist/workflow/manifesto de outra issue foi feita.

Risco do conjunto da spec: crítico (capturas, replay, elegibilidade/importação). Este
incremento de ferramentas inicial não alterava runtime; o grupo 2 agora altera o
teto do núcleo/HTTP/cliente e adiciona V15 autorizada. Teste local
do usuário precede publicação de implementação e revisão por modelo. Revisão de lote
e gate cumulativo SOL 6.1 xhigh continuam pendentes e distintos; CI não os substitui.
Nenhum push de implementação, merge, arquivamento, deploy ou carga na VPS realizado.
