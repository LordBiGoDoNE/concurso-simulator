# Design

## Context

As aulas têm uma fonte única na `main`: os HTML de conteúdo nas cinco pastas de
disciplina. `scripts/compile_native_lessons.py` os compila em
`frontend/src/lesson-documents/<ID>.json` (formato `native-lesson/v1`), retirando
navegação e banco de exercícios, e grava em `manifest.json` o SHA-256 de cada
documento e de cada fonte. `--check` falha se a saída versionada não corresponder
às fontes. O leitor React só consome esses JSON.

As questões do acervo principal estão em
`editorial/question-bank-expansion/2026-11/packages/{SIMULADO,PRATICA_AULA}/<TÓPICO>/preview-request.json`,
com `content.statement`, `content.resolution`, `content.classification.topicId` e
`content.references[].legalBasis.provision`. O vínculo com a aula é só o `topicId`.

## Goals / Non-Goals

**Goals:**

- Quem lê a aula de um assunto encontra nela a explicação de cada conceito que as
  questões desse assunto cobram.
- A cobertura é verificável por um comando, antes e depois.
- A leitura rápida que existe hoje continua disponível.

**Non-Goals:**

- Mexer em questões, no vínculo por `topicId`, no leitor, na API ou no banco.
- Tocar `stable`, Pages ou previews.
- Garantir aderência ao edital, que não está no repositório.

## Decisions

### D1. Editar os HTML de conteúdo e recompilar

A fonte continua sendo o HTML; o JSON nativo continua derivado. Editar o JSON à
mão quebraria `--check` e a reprodutibilidade. A cópia desses HTML em `stable`
não é alterada: o site estático publicado fica como está e passa a divergir das
aulas da aplicação, o que já é a direção registrada no `AGENTS.md`.

Alternativa descartada: tornar o JSON nativo a fonte. Exigiria reescrever o
compilador e seus testes, sem ganho para o estudante.

### D2. Matriz de cobertura em duas partes

1. **Lista editorial de conceitos** em
   `editorial/lesson-coverage/2026-11/concepts.jsonl`: uma linha por conceito, com
   `topicId`, `concept` (nome curto), `kind` (`termo`, `dispositivo`, `comando`,
   `procedimento`, `calculo`), `patterns` (expressões que indicam a presença na
   aula), `questionKeys` (chaves de origem das questões que o cobram) e, quando
   couber, `outOfScope` com justificativa. Os dispositivos legais partem de
   `legalBasis.provision`, que é estruturado; os demais conceitos são levantados
   pela leitura das questões de cada assunto.
2. **Script somente leitura** `scripts/lesson_coverage.py`: lê a lista e os
   documentos nativos, marca cada conceito como coberto, ausente ou fora de escopo
   e emite o relatório `docs/lesson-coverage.md` e um resumo por assunto. `--check`
   falha se houver conceito ausente sem justificativa ou se alguma `questionKey`
   não existir nos pacotes.

Extrair conceitos automaticamente do enunciado foi descartado: produziria ruído e
uma falsa sensação de cobertura. A lista é curada e revisável; o script só confere.

Limite assumido: presença de um padrão no texto não prova que a explicação é boa.
A qualidade é conferida na revisão por grupo e na amostragem do teste de ponta a
ponta.

### D3. Estrutura da aula aprofundada

Cada aula mantém Objetivos, esquema, Mapa, seções numeradas, Exemplos, Pegadinhas,
Resumo, Checklist e Referências. O aprofundamento entra de duas formas:

- ampliando a seção numerada a que o conceito pertence, quando ele é parte do
  núcleo do assunto;
- em seções novas, com as mesmas classes já mapeadas pelo compilador (`card`,
  `callout--law`, `example`, `table-scroll`), quando o conceito é um bloco próprio
  (por exemplo, VBA em I08).

Só são usadas tags, atributos e classes que o compilador já aceita; se uma aula
precisar de algo novo, a mudança no compilador entra no mesmo grupo, com teste.
O Resumo e o Checklist são atualizados para citar o que foi acrescentado.

### D4. Texto de lei

Dispositivo cobrado por questão é citado pelo número (lei, artigo, parágrafo,
inciso) e explicado. Transcrição literal só do trecho curto que a questão cobra,
em bloco `callout--law`, conferida na fonte oficial (Planalto, Inep, órgão
emissor) no dia da edição. A fonte e a data de conferência entram nas Referências
da aula, no mesmo espírito de `legalBasis.checkedOn` das questões. Dispositivo
que não puder ser conferido na fonte oficial não é transcrito: fica como conceito
ausente justificado na matriz.

### D5. Informática com versão fixa

Referência: Windows 11 e Microsoft 365 em pt-BR, a mesma das questões. Cada
atalho, caminho de menu e função aparece com o nome exato da interface e, para
funções, com um exemplo de entrada e resultado. As ressalvas genéricas "conforme
a versão" são substituídas pela versão de referência; diferenças entre versões só
são citadas quando alguma questão as cobra.

### D6. Aula não reproduz questão

O aprofundamento explica o conceito com exemplos próprios. Não copia enunciado,
alternativas nem gabarito do banco, para não antecipar respostas da prática e do
simulado nem atribuir questão de prova à aula.

### D7. Ordem e encadeamento dos grupos

Grupo 1 (matriz) primeiro. Os grupos de conteúdo seguem a ordem do tamanho da
diferença. Todos regeneram `manifest.json`, um arquivo único; por isso são PRs
encadeados, cada um partindo do anterior, e não paralelos. O grupo final roda a
matriz completa e o teste de ponta a ponta.

Grupos de conteúdo passam de 500 linhas autorais por natureza (prosa didática);
a divisão por blocos de assuntos mantém cada PR revisável. Os JSON nativos e o
`manifest.json` são identificados no PR como gerados.

### D8. Fontes congeladas da curadoria 2026-10

Motivo: as 250 questões curadas em `editorial/question-bank/2026-10/` são extraídas
dos exercícios dentro dos HTML de conteúdo e gabarito. `provenance.contentSha256` e
`answerSha256` guardam o SHA-256 do arquivo inteiro e entram na fingerprint; o
`manifest.json` registra também o SHA-256 de cada arquivo em `sourceFiles`. Editar as
aulas fora dos blocos de exercício (D1) muda esses hashes, e a curadoria passa a
recusar o snapshot já revisado e carregado no banco local (falhas de CI em
`spec-review-tests` e `CuratedPackageTests`).

Solução (decisão do usuário, "fixar fontes antigas"): versionar em
`editorial/question-bank/2026-10/legacy-sources/<caminho original>` cópias byte a byte
dos 104 arquivos de `sourceFiles` (blobs da base `fc3b9e4`, conferidos pelo SHA-256
registrado), sem conversão de fim de linha (`.gitattributes` com `-text`).
`compile_curation` extrai dessas cópias em vez da raiz viva; com isso também
`verify_curated_bank.py`, `audit_question_expansion.py` (`baseline`) e
`compile_lesson_practice.py` (`comparison_snapshot`) comparam com o snapshot 2026-10.
Divergência entre uma cópia e `sourceFiles` é recusada por `verify_manifest`.
`check_links.py` ignora a pasta, que não é página publicada.

Fica idêntico: o texto de `contentPath`/`answerPath` (caminho original), os hashes de
proveniência, as 250 fingerprints, o `manifest.json`, os 25 pacotes gerados e o banco
já carregado. Não muda: o site estático (`build_pages.py` copia só as raízes `0[1-5]_*`,
`assets` e arquivos de topo), o compilador de aulas nativas (lê as aulas vivas) e a
extração local completa (`extract_questions.py --root .`, usada pela importação local),
que continua lendo as aulas vivas.

Limites: as cópias são dados editoriais, não fonte de estudo; correções futuras nas
aulas não chegam às questões 2026-10 sem nova curadoria e novo manifest explícitos.
Questões com imagem seriam lidas de `assets/` dentro da pasta congelada; o snapshot
2026-10 não tem nenhuma. O repositório cresce cerca de 1,9 MB.

## Risks / Trade-offs

- **Erro de conteúdo jurídico ou técnico** → conferência na fonte oficial com data
  (D4), versão fixa (D5), revisão por grupo e final com as fontes em mãos.
- **Aulas longas demais** → o resumo e o mapa continuam no topo; aprofundamento em
  seções identificáveis (D3). Sem teto de palavras fixo: o limite é a matriz.
- **Lista de conceitos incompleta** → levantada por assunto a partir de todas as
  questões do pacote, não por amostra; revisada no grupo 1.
- **Legislação muda** → data de conferência registrada; atualização futura é
  manutenção editorial, fora desta spec.
- **Divergência entre `stable` e `main`** → aceita e registrada (D1).

## Migration Plan

Sem migração de dados. A entrega é conteúdo versionado; reverter é reverter os
commits. Deploy em VPS, se desejado, é passo separado e não autorizado aqui.

## Open Questions

- Edital: se o usuário fornecer o conteúdo programático, a matriz pode ganhar uma
  segunda coluna de referência em spec futura.
