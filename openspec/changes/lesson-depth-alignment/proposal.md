# Proposal

Issue: [#230](https://github.com/LordBiGoDoNE/concurso-simulator/issues/230).
Épica: `epic/lesson-depth-alignment`, criada da `main` `fc3b9e4` em 2026-11-17.

## Why

O usuário pediu, em 2026-11-17, a comparação entre as matérias de estudo e as
questões. A análise mostrou que o nível técnico não bate: as 51 aulas são uma
apostila de revisão (470 a 1.650 palavras, um a três parágrafos por subtópico),
enquanto o acervo de expansão de 2026-11 (2.587 questões, 530 originais de prova)
cobra detalhes que o texto das aulas não traz. O estudante lê a aula, abre a
prática do mesmo assunto e encontra perguntas que a leitura não o preparou para
responder.

Diferença observada, por disciplina:

| Disciplina | Aulas hoje | Questões cobram | Diferença |
| --- | --- | --- | --- |
| Informática I01–I10 | uma ou duas frases por conceito | atalho e caminho de menu exatos por versão, resultado de fórmula, leitura de código VBA | grande |
| Conhecimentos Administrativos A01–A08 | conceitual, sem citar artigos | caso prático preso a dispositivo legal (303 de 406 itens com base legal) | grande |
| Conhecimentos Escolares S01–S11 | a mais técnica, mas parafraseia a lei | literalidade de artigo e inciso, rotinas do Censo | grande em S04, S09, S10 e S11; média nas demais |
| Raciocínio Lógico M10–M12 | só o básico | equivalências, negação de quantificador, três conjuntos, casa dos pombos | média |
| Português P01–P10 | regra central e uma ou duas exceções | coesão, casos especiais de concordância e regência, crase facultativa | média |
| Matemática M01–M09 | passo a passo, didática | contas de um a três passos; originais com problemas longos | pequena |

Exemplos confirmados por busca literal nas aulas compiladas (termo cobrado em
questões, nenhuma ocorrência na aula do assunto): em I08, 44 de 50 questões de
simulado são de VBA e a aula não traz `ClearContents`, `PasteSpecial` nem `Range`;
I07 não traz Análise Rápida, caixa e bigodes nem tabela dinâmica; A08 não traz
crédito extraordinário nem balanço; A04 não traz a IN SEDAP 205/1988; S04 não traz
o art. 60-A nem reclassificação; S11 não traz superusuário; M12 não traz três
conjuntos; P01 não traz anáfora nem catáfora.

Limites da análise: foram lidas 14 das 51 aulas e cerca de 180 questões de
simulado; o banco não tem campo de dificuldade; termo ausente pode estar coberto
com outra redação. Por isso o primeiro grupo desta spec produz o inventário
completo antes de qualquer reescrita.

## What Changes

- Criar uma matriz de cobertura reprodutível: por assunto, os conceitos e
  dispositivos legais cobrados pelas questões e se a aula os explica.
- Aprofundar as aulas até cobrir os conceitos da matriz, na ordem do tamanho da
  diferença: Informática, Administrativos, Escolares, Raciocínio Lógico e Português.
  Matemática M01–M09 recebe só os ajustes pontuais que a matriz apontar.
- Citar e explicar os dispositivos legais cobrados, conferidos na fonte oficial,
  com data de conferência.
- Fixar em Informática a versão de referência (Windows 11 e Microsoft 365 em
  pt-BR) e os comandos, atalhos e caminhos exatos.
- Manter a leitura rápida atual: o aprofundamento entra em seções próprias, sem
  remover objetivos, resumo, checklist e exemplos existentes.
- Recompilar os documentos nativos das aulas alteradas.

Fora do escopo:

- Alterar, reclassificar, remover ou aprovar questões; corrigir distratores fracos
  das questões autorais; criar campo de dificuldade.
- Mudar o vínculo aula–questão (continua por `topicId`), o leitor React, a API, o
  banco ou migrações.
- Alterar a branch `stable`, o site estático publicado, `pages-store` ou previews.
- Auditar o edital: não há cópia dele no repositório; a referência do
  aprofundamento é o que o banco cobra.
- Deploy ou carga em VPS.

## Capabilities

### New Capabilities

- `lesson-content-depth`: profundidade mínima do conteúdo das aulas em relação ao
  que o banco de questões cobra, e a matriz que a verifica.

### Modified Capabilities

Nenhuma. `study-experience`, `lesson-practice-capacity` e `question-bank-coverage`
permanecem como estão.

## Impact

- Fontes das aulas na `main`: `01_Portugues/`, `02_Matematica_Logica/`,
  `03_Informatica/`, `04_Assistente_Administrativo/`, `05_Secretario_Escola/`
  (arquivos de conteúdo; os de "Gabarito e Revisão" só mudam se a matriz exigir).
- Saída derivada: `frontend/src/lesson-documents/{P01..S11}.json` e `manifest.json`,
  gerados por `scripts/compile_native_lessons.py`.
- Novo script somente leitura e seus testes em `scripts/`; lista editorial de
  conceitos em `editorial/lesson-coverage/2026-11/`; relatório em `docs/`.
- Sem mudança em backend, schema, API, frontend de execução, CI ou deploy.
- As aulas ficam mais longas; o tempo de leitura por assunto aumenta.
