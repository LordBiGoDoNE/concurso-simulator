# Animações das aulas

Guia de estilo e de autoria das animações didáticas das aulas nativas (spec
`lesson-animations`, issue #245). A animação reforça o que o texto da aula já
explica; ela nunca ensina sozinha nem traz conceito ausente do texto.

## Quando usar

Use animação só quando o assunto for **movimento ou sequência**: algo que vai de um
lugar a outro, uma ordem de passos, uma regra aplicada repetidas vezes. Texto de lei,
definições e listas continuam em texto, tabela ou esquema estático.

- **Etapas** (`steps`): processo de vários passos. O estudante avança com
  Anterior/Próxima; cada etapa tem uma legenda curta.
- **Ciclo** (`cycle`): fluxo curto repetido, como pacotes indo e voltando.

## Arquivos

| Caminho | Papel |
| --- | --- |
| `assets/animacoes/fontes/<id>.json` | fonte editável: metadados e desenho |
| `assets/animacoes/<id>.svg` | SVG gerado (não editar à mão) |
| `assets/animacoes/manifest.json` | inventário gerado, lido pelo compilador das aulas |

Fluxo:

```sh
python scripts/sketch_svg.py                 # gera SVG e manifesto a partir das fontes
python scripts/sketch_svg.py --check         # confere que os gerados estão atualizados e dentro dos limites
python scripts/compile_native_lessons.py     # insere as animações nas aulas nativas
python scripts/compile_native_lessons.py --check
```

O teste `scripts/tests/test_sketch_svg.py` falha se o SVG ou o manifesto
versionados diferirem do que as fontes geram.

## Fonte de uma animação

Campos de metadados (vão para o manifesto):

- `id` (igual ao nome do arquivo), `topic` (ex.: `I05`), `anchor` (ID da seção da
  aula onde a animação entra, ao final dela, ex.: `s2`);
- `title` e `description` (equivalente textual completo: tudo o que a animação mostra);
- `mode` (`steps` ou `cycle`), `duration` (segundos), `poster` (tempo do quadro
  estático mostrado com "reduzir movimento" e sem suporte a animação);
- `steps` (modo etapas): lista de `{time, caption}` em ordem crescente; `time` é o
  instante em que a etapa está completa.

Campos de desenho: `seed` (semente do traço), `size` (`[largura, altura]`) e
`elements`.

Elementos:

| `type` | Campos |
| --- | --- |
| `box` | `x`, `y`, `w`, `h`, `fill`, `stroke`, `width`, `label`, `label_size`, `label_color`, `weight` |
| `text` | `x`, `y`, `text` ou `spans` (`[[texto, cor ou null], …]`), `size`, `color`, `weight`, `anchor` |
| `line`, `arrow` | `points` (`[[x, y], …]`), `color`, `width`, `head`, `draw` (`[início, fim]`: o traço se desenha) |
| `circle`, `ellipse` | `cx`, `cy`, `r` ou `rx`/`ry`, `fill`, `fill_opacity`, `stroke`, `width` |
| `group` | `children` |

Todo elemento aceita:

- `show`: `[entrada]` ou `[entrada, saída]`, com esmaecimento de 0,3 s;
- `move`: lista de `{to: [dx, dy], at: [início, fim]}` (deslocamento acumulado a
  partir da posição original).

Cada propriedade animada vira um único elemento SMIL que começa em 0 s e dura a
animação inteira. Assim o leitor pode parar em qualquer instante (etapas, pausa,
movimento reduzido) e ver o estado certo.

## Estilo

- **Traço à mão**: linhas, caixas e círculos têm leve irregularidade e traço duplo,
  gerados com semente fixa. A mesma fonte gera sempre o mesmo SVG.
- **Rótulos em texto real**, com a fonte do app (o SVG fica embutido na página e
  herda a fonte). O traço à mão vale só para formas e setas.
- **Paleta** (fundo "papel" claro; no tema escuro a figura continua num cartão claro,
  como os esquemas):

| Nome | Cor | Uso |
| --- | --- | --- |
| `paper` | `#fbf8f1` | fundo |
| `ink` | `#2b2b2b` | texto e traço principal |
| `muted` | `#5f6368` | cabeçalhos e traço secundário |
| `accent` / `accent-soft` | `#0f766e` / `#d6eee8` | o que muda ou anda |
| `warm` / `warm-soft` | `#b45309` / `#fbe5cc` | o que fica fixo, alerta ou erro |
| `soft`, `white` | `#efeae0`, `#ffffff` | preenchimentos neutros |

  Contraste sobre `paper`: `ink` 13,6:1, `muted` 5,9:1, `accent` 5,2:1 e `warm`
  4,8:1. Texto colorido só nessas quatro cores.

## Limites (conferidos pela ferramenta)

- Até 30 KB por SVG; largura do desenho até 480 (legível em 360 px de tela).
- Texto com pelo menos 18 unidades.
- Ciclo de até 12 s; até 8 etapas; no máximo 2 animações por aula.
- Só cores da paleta e só os elementos da tabela acima.

O compilador das aulas aplica ainda a lista branca de segurança (somente animação de
opacidade, posição, tamanho, traço e visibilidade; nada de links, estilos, eventos ou
referências fora da figura).

## Conferência técnica

Cada animação entregue registra no PR, etapa por etapa, a afirmação mostrada e o
trecho da aula que a sustenta. Em Informática vale a versão de referência das aulas
(Windows 11 e Microsoft 365 em pt-BR), sem capturas de tela reais.
