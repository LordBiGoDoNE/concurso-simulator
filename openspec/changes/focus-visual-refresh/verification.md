# Verificação local — 2026-11-13

Grupos 4 e 6.1–6.2 da `focus-visual-refresh`, na branch
`task/focus-visual-refresh/result-history`, commit `7249c6d`, sobre a épica em
`cb16697` (grupos 1, 2, 3 e 5 integrados pelos PRs #194, #196, #197 e #198).
Estado: commit local, não enviado ao GitHub. Não há revisão final de código nem
integração na épica. Acompanhamento: [issue #193](https://github.com/LordBiGoDoNE/concurso-simulator/issues/193).

## Resultados

- `npx tsc --noEmit` e `npm run build`: aprovados.
- Vitest: 29 arquivos, 299 testes. Com a máquina sob carga alta (CPU em 96% por
  outros programas), testes que renderizam o `App` estouraram o tempo na execução
  completa, inclusive na épica sem as mudanças (8 falhas na base contra 4 a 6 na
  branch, variando por execução). Cada arquivo afetado passou isolado: AttemptContext,
  NewStudentJourney, PracticeTransition, DirectedPractice, AttemptComposer,
  QuestionBrowser e SubjectReviewContext. A execução completa fica para o CI.
- Playwright com API simulada: 33 aprovados, 1 ignorado por design (`study.spec.ts`
  exige `STUDY_ROOT`). Inclui o novo `e2e/result-history.spec.ts`.

## 6.1 — Docker local

Imagens construídas por `scripts/local_app_images.py` a partir de `7249c6d`. O
operador não tem comando para trocar imagens de um estado existente: o bloco
`images` do `state.json` local (ignorado) foi substituído pelo relatório do build,
com cópia do anterior (`5312a32`) guardada fora do repositório. Dono, segredos e
volume foram preservados (`stop`/`start`). Web, API e PostgreSQL saudáveis em
`127.0.0.1:8088`.

Fluxos como visitante, sem simulação de API, nos temas claro e escuro, em 390px e
1440px: início, aula P07, prática da aula até o resultado, simulado de 3 questões
de Português, resultado com barra e filtro "Erros e em branco", "Rever a aula" e
volta ao resultado, e "Praticar este assunto" com o painel aberto. Em todas as
combinações: nenhuma requisição fora de `127.0.0.1`, nenhum erro de JavaScript e
nenhum estouro horizontal. Em claro/390px a disponibilidade da prática ainda
carregava quando o roteiro conferiu o botão; a prática foi exercitada nas outras
três combinações.

O histórico de simulados exige conta; o login Google está desligado no ambiente
local. O histórico foi verificado com API simulada (e2e e capturas), não no Docker.

## 6.2 — Capturas antes e depois

Roteiro único com API simulada (conta conectada, simulado de 3 questões com uma
correta, uma errada e uma em branco), em 390px e 1440px:

- antes: `main` em `bfcda06`, só tema claro (não havia tema escuro);
- depois: `7249c6d`, temas claro e escuro.

Telas: início, biblioteca, aula P07, montagem, realização e resultado. São 12
capturas de antes, 24 de depois e 24 reais do Docker. Foram conferidas
visualmente e não entram no repositório: as do Docker mostram questões do banco
local, e as simuladas são reproduzidas pelo e2e (`test-results/`).

## Limites

- Figuras SVG das aulas ficam sobre placa clara (`--lesson-figure-plate`) também no
  tema escuro, porque os diagramas trazem cores próprias no documento.
- A barra do resultado usa as cores apenas como reforço; o estado de cada questão
  está em texto e a legenda traz os números.
- "Praticar este assunto" só abre o painel da aula. Começar ou trocar a prática
  continua exigindo o botão e a confirmação existentes.
- Na prática da própria aula, os atalhos apontam para a mesma aula aberta.
