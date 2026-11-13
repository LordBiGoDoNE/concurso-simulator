# Design

## Context

O frontend React usa um único `frontend/src/style.css`. Ele declara oito variáveis
em `:root` (`--ink`, `--muted`, `--blue`, `--blue-soft`, `--teal`, `--line`,
`--paper`, `--canvas`) e mais de vinte cores literais espalhadas. Entre elas estão
as cores dos blocos das aulas nativas (`#dba94f`, `#b8d2ce`, `#e5f2ef`). A fonte é a
do sistema e não há tema escuro. O design system extraído desse código registra
inventário, valores e pares com contraste insuficiente.

As aulas são documentos nativos (`native-lesson/v1`) renderizados pelo `NativeLesson`.
Os tons de bloco aceitos são: section, diagram, note, example, solution, formula,
attention, summary, reference, label, sources, description, table, steps e contents.
O documento também pode trazer SVG, MathML, tabelas e as grades e barras de percentual.

A direção Foco foi desenhada em sete telas: início, questão no modo escuro, questão
no celular, montar simulado, resultado, biblioteca e leitor.

## Goals / Non-Goals

**Goals**

- Uma única fonte de valores visuais (tokens CSS), com tema claro e escuro.
- Contraste conforme WCAG 2 AA nos dois temas, sem depender só da cor para estados.
- Aplicar a direção Foco às telas existentes sem mudar comportamento, contratos
  ou textos de estado já aprovados.
- Resultado mais legível e com retorno à aula do assunto.

**Non-Goals**

- Alterar documentos das aulas, o gerador `compile_native_lessons.py` ou o
  vocabulário aceito pelo `parseNativeLesson`.
- Aplicar tema ao site estático `stable` ou ao HTML das aulas originais.
- Progresso por aula, metas ou gamificação (direção Trilha, não escolhida).
- Biblioteca de componentes separada, Storybook ou CSS-in-JS.

## Decisions

### Tokens em CSS custom properties

Criar `frontend/src/tokens.css`, importado antes de `style.css`, no formato:

```css
:root { /* tema claro */ }
@media (prefers-color-scheme: dark) { :root:not([data-theme="light"]) { /* escuro */ } }
:root[data-theme="dark"] { /* escuro */ }
```

`style.css` passa a referenciar somente tokens. Uma verificação automatizada
falha se surgir cor literal fora de `tokens.css`. A verificação aceita uma lista
curta e documentada de exceções, como `transparent` e `currentColor`.

*Alternativa descartada:* CSS-in-JS ou Tailwind. Mudaria o build e a forma de
escrever componentes sem ganho para uma interface deste tamanho.

### Paleta Foco

Os tokens preservam os nomes semânticos atuais e ajustam os valores:

| Token | Claro | Escuro | Uso |
| --- | --- | --- | --- |
| `canvas` | `#f4f6f9` | `#0f1724` | fundo geral |
| `paper` | `#ffffff` | `#172233` | cartões |
| `ink` | `#14243a` | `#e8eef7` | texto principal |
| `muted` | `#56657a` | `#9fb0c6` | texto secundário |
| `line` | `#e1e6ee` | `#2a3a52` | divisórias decorativas |
| `control-border` | `#7d8a9e` | `#6b7d97` | bordas de campos (≥3:1) |
| `blue` | `#2457d6` | `#8fb0ff` | ação principal |
| `on-blue` | `#ffffff` | `#0f1724` | texto sobre `blue` |
| `blue-soft` | `#e9f0ff` | `#1d3157` | seleção e item ativo |
| `teal` | `#0f6f68` | `#5fd0c3` | eyebrow, salvo, acerto |
| `warning` | `#9a3f07` | `#f0a050` | erro de resposta, falha ao salvar |
| `focus` | `#b35c00` | `#f0a050` | anel de foco (≥3:1 em `canvas` e `paper`) |

Os valores finais são confirmados pelo teste de contraste do grupo 1. Se um par
falhar, o ajuste é de luminosidade, mantendo o matiz.

### Tipografia

A fonte é Figtree (SIL OFL), variável, pelo pacote `@fontsource-variable/figtree`
5.3.0 fixado. O build copia os WOFF2 para `dist/assets` (latin com cerca de 20 KB;
latin-ext só é baixado se um caractere precisar). Usa `font-display: swap` e
fallback `system-ui`. Não há requisição ao Google Fonts, para manter a execução
local e offline. *Decidido no grupo 1:* Figtree em vez da fonte do sistema.

A escala fica entre 13 e 44px:

- enunciado de 19px no desktop e 18px no celular, com entrelinha de 1,7 a 1,75;
- números tabulares em contadores, mapa e resultado.

### Troca de tema

- Por padrão o tema segue `prefers-color-scheme`.
- Um seletor "Tema" na barra superior alterna automático, claro e escuro.
  *Decidido no grupo 1:* a barra superior aparece em todas as larguras, enquanto o
  rodapé da navegação some abaixo de 800px. No modo prova a barra fica oculta, e
  a escolha anterior continua valendo.
- A escolha é gravada em `localStorage` com chave própria, dentro de `try/catch`, e
  aplicada como `data-theme` no `<html>` antes da primeira pintura.
- A chave guarda só a preferência visual. Isso respeita a regra de não guardar
  respostas no navegador.

### Resultado

- O resumo usa uma barra proporcional com acertos, erros e em branco, sempre com
  legenda numérica.
- Cada item revisado tem rótulo textual e ícone ("Correta", "Errada", "Em branco").
  A cor apoia, nunca é o único sinal.
- O filtro "Erros e em branco" é estado local do componente.
- "Rever a aula" abre o leitor do `classification.topicId` do item.
- "Praticar este assunto" abre a prática da aula do mesmo assunto.
- Nenhum dos dois atalhos é exibido antes do encerramento.

### Leitor de aulas nativas

- Corpo da aula em 18px, entrelinha de 1,8 e largura de leitura de cerca de 70 caracteres.
- Cada tom de bloco ganha tokens próprios nos dois temas (`lesson-attention`,
  `lesson-summary`, `lesson-note` etc.). Os blocos de atenção e resumo mantêm o
  rótulo textual que já trazem.
- O índice das seções, a paginação entre aulas e o painel de prática seguem a
  direção Foco. O carregamento, o erro e a validação do documento não mudam.
- Figuras SVG ficam sobre uma placa clara (`lesson-figure-plate`) também no tema
  escuro. Os diagramas trazem `fill` e `stroke` próprios no documento, e a spec
  não altera o conteúdo.
- MathML, `var` e as grades e barras de percentual passam a usar tokens.

### Revisão

- Cada lote termina com testes, triagem própria e relatório de SHAs e checks antes
  de pedir aprovação de merge na épica.
- A única revisão por modelo é a revisão final de código antes do `[SPEC]` para
  `main`. Ela usa o subagente `spec-reviewer` (Claude Opus 5.5, esforço medium), em
  contexto novo, conforme `AGENTS.md` e `docs/architecture/spec-review.md`.

## Risks / Trade-offs

- **Regressão visual ampla:** a mudança toca quase todo o CSS. *Mitigação:* grupos
  por área, capturas Chromium em claro e escuro, desktop e 390px, e testes de
  comportamento existentes sem alteração.
- **Contraste do tema escuro:** cada par é medido por teste, não por inspeção visual.
- **Peso da fonte:** três pesos em WOFF2 somam cerca de 60 a 90 KB.
  *Alternativa:* manter a fonte do sistema e só ajustar a escala.
- **Figuras das aulas no modo escuro:** os SVGs têm cores próprias e ficam sobre
  placa clara. É aceito nesta spec e registrado como limite. Tematizar os diagramas
  exigiria mudar o gerador das aulas.
- **51 aulas com estruturas variadas:** o grupo 5 verifica todas as aulas nos dois
  temas, sem depender de uma amostra.

## Open Questions

Nenhuma. As duas perguntas originais (fonte e posição do controle de tema) foram
decididas no grupo 1, conforme as seções acima.
