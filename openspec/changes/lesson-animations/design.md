# Design

## Context

- As aulas nativas são documentos JSON inertes (`frontend/src/lesson-documents/`)
  gerados por `scripts/compile_native_lessons.py` a partir dos HTML legados. O
  compilador aceita só tags e atributos de listas brancas (`HTML_TAGS`, `SVG_TAGS`,
  `ATTRS`), prefixa IDs por aula e recusa referências externas. O frontend repete a
  validação em `frontend/src/native-lesson.ts` antes de renderizar com
  `createElement` em `NativeLesson.tsx`. Não há HTML nem CSS embutidos em tempo de
  execução.
- Os 51 esquemas-resumo (`assets/esquemas/*.svg`, 220 KB no total) são SVG estáticos
  com `title`/`desc`, cores fixas sobre fundo claro, exibidos inline com rolagem
  horizontal no celular.
- O tema claro/escuro do app vem de `frontend/src/tokens.css` (sistema ou
  `data-theme`), com fonte empacotada (`focus-visual-refresh`).
- O material estático `/material/` e a branch `stable` usam os HTML legados.

## Goals / Non-Goals

Goals:

- Mostrar movimento e sequência onde o texto descreve mal, sem pesar a página, sem
  abrir brecha de segurança no leitor e respeitando acessibilidade.
- Estilo de traço à mão, consistente entre as animações e reprodutível.
- Piloto pequeno (9 animações) para o usuário avaliar antes de expandir.

Non-Goals:

- Animar todas as aulas; vídeo; interação além de reproduzir, pausar e navegar etapas.
- Alterar o material estático, os esquemas existentes ou o texto das aulas.

## Decisions

### D1 — SVG com animação declarativa (SMIL), não GIF

GIF pesa centenas de KB por figura, fica serrilhado, não acompanha o tema, não pausa
(WCAG 2.2.2 exige pausa para movimento automático com mais de 5 s) e transforma
rótulos em pixels. SVG com `animate`, `animateTransform`, `animateMotion`, `mpath` e
`set` cabe em poucos KB, mantém texto real e é controlável pelo leitor com
`SVGSVGElement.pauseAnimations()`, `unpauseAnimations()` e `setCurrentTime()`.

Alternativas descartadas: CSS embutido (o leitor não aceita CSS em tempo de
execução e o compilador remove `class`); JavaScript por animação (abre execução
de código vinda de conteúdo); Lottie (biblioteca e formato novos, payload maior).

### D2 — Lista branca de animação, com bloqueio de alvos perigosos

- Novos elementos aceitos somente dentro de animações: `animate`,
  `animateTransform`, `animateMotion`, `mpath`, `set`.
- Atributos de tempo aceitos: `dur`, `begin`, `end`, `repeatCount`, `fill`
  (`freeze`/`remove` em elemento de animação), `values`, `keyTimes`, `keySplines`,
  `calcMode`, `from`, `to`, `by`, `rotate`, `path`, `type` (de `animateTransform`).
- `attributeName` restrito a: `opacity`, `fill-opacity`, `stroke-opacity`,
  `transform`, `x`, `y`, `cx`, `cy`, `r`, `width`, `height`,
  `stroke-dashoffset`, `visibility`. Qualquer outro, inclusive `href`,
  `xlink:href`, `style` e `on*`, é recusado (vetor conhecido de XSS por SMIL).
- `begin`/`end` aceitam só tempos (`2s`, `1.5s`) e referências a IDs da mesma figura
  (`passo2.end`), que o compilador prefixa como os demais IDs; eventos de usuário
  (`click`, `mouseover`) e `accessKey` são recusados.
- `mpath` só aponta para `#id` da mesma figura, com prefixo.
- A validação existe no compilador **e** no validador do frontend, com testes de
  recusa para cada vetor.

### D3 — Inventário por manifesto; HTML legado intocado

As animações não entram nos HTML das aulas. `assets/animacoes/manifest.json` lista,
por animação: `id`, `topic`, `anchor` (ID de seção existente na aula), `file`,
`title`, `description`, `mode` (`cycle`/`steps`), `poster` (tempo do quadro
estático) e, no modo etapas, `steps` com `time` e `caption`. O compilador valida o
manifesto (arquivo em `assets/animacoes/`, âncora existente uma única vez, tempos
crescentes dentro da duração, legendas não vazias) e insere um nó nativo
`animation` ao final do bloco âncora (a seção da aula com aquele ID), depois do
conteúdo dela.

Consequência: `/material/`, `stable` e previews ficam sem animação, sem risco de
movimento automático sem controle nesses canais.

### D4 — Componente de animação no leitor

`LessonAnimation` renderiza a figura dentro de `<figure>` com legenda e controles:

- **Ciclo**: reproduz quando a figura entra na tela (IntersectionObserver) e para ao
  sair; botão Pausar/Reproduzir sempre visível.
- **Etapas**: começa na etapa 1, parado; botões Anterior/Próxima e "Reproduzir
  tudo"; a legenda da etapa atual fica numa região `aria-live="polite"`;
  indicador "Etapa n de N".
- **Reduzir movimento** (`prefers-reduced-motion: reduce`): nunca reproduz
  sozinho; mostra o quadro `poster` (ciclo) ou a etapa atual parada (etapas); o
  estudante ainda pode navegar as etapas.
- A descrição textual completa fica abaixo da figura (equivalente textual), e o SVG
  tem `role="img"` com `aria-labelledby` para `title`/`desc`.
- Navegação por teclado nos controles e foco visível pelos tokens existentes.

### D5 — Traço à mão determinístico

Uma ferramenta de autoria em Python (`scripts/sketch_svg.py`) gera, a partir de uma
descrição simples (retângulos, linhas, setas, círculos, rótulos e etapas), caminhos
com leve irregularidade e traço duplo, usando semente fixa: a mesma entrada produz
o mesmo SVG, revisável no diff. Sem `filter`/`feTurbulence` (custo de renderização
e lista branca maior).

Guia de estilo (`docs/lesson-animations.md`):

- Fundo "papel" claro, tinta escura e duas cores de destaque; texto com contraste
  de pelo menos 4,5:1 e elementos gráficos com pelo menos 3:1. No tema escuro, a
  figura fica num cartão claro, como os esquemas atuais.
- Rótulos em texto real com a fonte do app (SVG inline herda a fonte empacotada);
  o traço à mão vale só para formas e setas, para não prejudicar a leitura.
- Limites: até 30 KB por arquivo, ciclo de até 12 s, até 8 etapas, no máximo 2
  animações por aula; largura mínima legível em 360 px.

### D6 — A animação reforça, não ensina sozinha

Cada animação mostra só o que o texto da aula já explica, na mesma terminologia e
na versão de referência de Informática (Windows 11 e Microsoft 365 em pt-BR, fixada
em `lesson-depth-alignment`). Capturas reais de interface são substituídas por
recriações simplificadas. Cada animação tem conferência técnica registrada no PR do
grupo (o que a animação afirma × trecho da aula).

### D7 — Piloto e avaliação

Os 9 itens da proposta são o piloto. Ao final, o pacote traz a lista das animações
com links locais para o usuário avaliar. A expansão para outras disciplinas é outra
spec; esta não muda nada fora de Informática e Raciocínio Lógico.

## Risks / Trade-offs

- **Suporte SMIL**: Chrome, Edge, Firefox e Safari atuais suportam; navegador sem
  suporte mostra o quadro inicial, e a descrição textual garante o conteúdo.
- **Animação distrai**: limites de duração e quantidade, início parado no modo
  etapas e respeito a "reduzir movimento".
- **Lista branca maior**: mitigada por D2 com testes de recusa nos dois validadores
  e revisão de segurança no grupo de infraestrutura.
- **Traço à mão e legibilidade**: rótulos em fonte normal; conferência em 360 px.
- **Manutenção**: SVG gerado por ferramenta determinística; ajuste é editar a
  descrição e regenerar.

## Migration Plan

Sem migração de dados. Aulas sem entrada no manifesto não mudam. Reverter é remover
entradas do manifesto e recompilar.

## Open Questions

Nenhuma bloqueante. Estilo e escopo do piloto foram decididos pelo usuário em
2026-11-18.
