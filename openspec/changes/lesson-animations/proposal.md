# Proposal

Issue: a criar (link registrado após a criação).
Épica: `epic/lesson-animations`, criada da `main` `85c2d2c` em 2026-11-18.

## Why

O usuário pediu, em 2026-11-18, animações curtas como reforço visual de
aprendizado nas aulas, "quando fossem viáveis ou fizessem sentido", e deu como
referência um GIF de balanceador de carga desenhado com traço à mão.

Hoje cada uma das 51 aulas tem um esquema-resumo estático (`assets/esquemas/<ID>.svg`)
e tabelas, mas nenhuma figura junto do conceito que ela explica. Vários assuntos
cobrados no banco são movimento ou sequência, que o texto descreve com dificuldade:
o que acontece ao arrastar uma fórmula, o caminho de uma mensagem de e-mail, um
laço VBA percorrendo células, o preenchimento de um diagrama de três conjuntos.

Indicação do peso desses assuntos no acervo de expansão 2026-11 (busca por termos
nos enunciados e resoluções; contagem aproximada, a refinar no grupo 1):

| Assunto | Questões que tocam o tema |
| --- | --- |
| I02 Área de Transferência, copiar e recortar | 19 de 50 |
| I09 animação de objeto × transição de slide | 18 de 50 |
| I10 caminho do e-mail, Cc e Cco | 9 de 50 |
| I08 laço e ações sobre intervalos em VBA | 7 de 50 (44 de 50 são de VBA) |
| M11 condicional, tabela-verdade e equivalências | 15 de 54 |
| M12 conjuntos e casa dos pombos | 30 de 54 |

Decisões do usuário em 2026-11-18: estilo **traço à mão** e **piloto** em
Informática e Raciocínio Lógico; a expansão para as demais disciplinas fica para
outra spec, depois da avaliação do piloto.

## What Changes

- Novo tipo de figura nas aulas nativas: **animação** em SVG declarativo (SMIL),
  sem GIF, vídeo, script ou CSS embutido, com dois modos:
  - **ciclo**: fluxo curto repetido (como o exemplo do balanceador);
  - **etapas**: processo de vários passos, avançado pelo estudante.
- Controles no leitor: reproduzir/pausar, etapa anterior/próxima, legenda de cada
  etapa anunciada a leitores de tela; com "reduzir movimento" ativo no sistema, a
  animação começa parada num quadro estático completo.
- Lista versionada das animações (`assets/animacoes/manifest.json`): aula, seção
  onde entra, arquivo, título, descrição, modo, etapas e quadro estático.
  O compilador insere cada animação no documento nativo da aula, depois da âncora.
- Lista branca de animação SVG no compilador e no validador do frontend, com
  bloqueio de alvos perigosos (por exemplo, animar `href`).
- Ferramenta de autoria que gera o traço à mão de forma determinística a partir de
  formas simples, e um guia de estilo (paleta, espessuras, fonte, contraste,
  tamanho, duração).
- Piloto com 9 animações:

| Aula | Animação | Modo |
| --- | --- | --- |
| I02 | copiar, recortar e colar pela Área de Transferência; histórico (Win+V) | etapas |
| I05 | alça de preenchimento com referências relativa, absoluta e mista | etapas |
| I07 | tabela dinâmica: campos arrastados para Linhas, Colunas e Valores | etapas |
| I08 | laço `For Each` percorrendo um intervalo e o efeito de `ClearContents` | etapas |
| I09 | animação de objeto dentro do slide × transição entre slides | ciclo |
| I10 | caminho do e-mail (SMTP, servidor, IMAP/POP) e quem vê Cc e Cco | etapas |
| M11 | tabela-verdade da condicional preenchida linha a linha e contrapositiva | etapas |
| M12 | três conjuntos preenchidos de dentro para fora | etapas |
| M12 | casa dos pombos | ciclo |

Fora do escopo:

- Animações em Português, Matemática M01–M09, Conhecimentos Administrativos e
  Conhecimentos Escolares (spec futura, se o piloto for aprovado).
- GIF, vídeo, imagens geradas por IA e capturas de tela reais do Windows/Office.
- Material estático (`/material/`), branch `stable`, site publicado, `pages-store`
  e previews: os HTML legados das aulas não mudam.
- Mudar o texto das aulas, os esquemas-resumo existentes, questões, backend, API,
  banco ou migrações.
- Deploy ou release na VPS.

## Capabilities

### New Capabilities

- `lesson-animations`: animações didáticas nas aulas nativas (formato seguro,
  controles, acessibilidade, estilo, inventário e piloto).

### Modified Capabilities

Nenhuma. `lesson-content-depth` e `study-experience` permanecem como estão; a
animação reforça o texto e não substitui conteúdo exigido pela matriz de cobertura.

## Impact

- Novos: `assets/animacoes/` (SVG do piloto e `manifest.json`), ferramenta de autoria
  e testes em `scripts/`, guia de estilo em `docs/`.
- `scripts/compile_native_lessons.py`: lista branca SMIL e inserção por âncora.
- Frontend: `frontend/src/native-lesson.ts` (validador), novo componente de
  animação no leitor (`NativeLesson.tsx`), estilos em `frontend/src/styles/lessons.css`,
  testes Vitest e e2e.
- Saída derivada: `frontend/src/lesson-documents/{I02,I05,I07,I08,I09,I10,M11,M12}.json`
  e `manifest.json`.
- Sem mudança em backend, schema, API, CI ou deploy. As imagens Docker passam a
  incluir `assets/animacoes/` pelo mesmo contexto filtrado de `assets/`.
