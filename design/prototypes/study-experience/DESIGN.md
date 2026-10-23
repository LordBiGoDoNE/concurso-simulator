# Concurso Simulator — direção visual proposta

Estudo em OpenDesign 0.21.1 a partir de `modern-minimal`; identidade própria do projeto.
O usuário aprovou o layout completo, incluindo aulas, em 2026-10-23. O primeiro lote inclui
todos os grupos2–6, escolhidos pelo usuário; o aceite visual não autoriza merges.

## Paleta

Tokens autorais em `:root` do HTML são a referência executável.

| Papel | Token | Cor |
| --- | --- | --- |
| Texto principal | `--ink` | `#15283b` |
| Texto secundário | `--muted` | `#5c6b7b` |
| Ação principal | `--blue` | `#255ed9` |
| Estado ativo suave | `--blue-soft` | `#edf3ff` |
| Sinal secundário | `--teal` | `#16746d` |
| Fundo geral | `--canvas` | `#f6f8fb` |
| Superfície | `--paper` | branco |
| Contorno | `--line` | `#e2e7ed` |
| Foco de teclado | `--focus` | `#d57d17` |

Cores de disciplinas e tons intermediários também têm tokens semânticos. Não usar
cores literais dispersas; as cores de estado não indicam acerto durante a realização.

## Tipografia e espaço

- Fonte de sistema, sem downloads; números tabulares para contadores.
- Títulos grandes e curtos; enunciados entre 16 e 18px, com entrelinha confortável.
- Bordas finas, raios de 6–14px e sombras reservadas ao que precisar de sobreposição.
- No desktop, navegação lateral; na realização, cabeçalho reduzido.
- No celular, conteúdo em uma coluna, mapa com cinco colunas e ações de toque maiores.
- Resumo lateral na composição desktop; total e ação principal fixos no celular.
- Biblioteca com cartões de disciplinas, busca e lista de aulas; leitor em superfície
  branca, texto espaçado e índice lateral, passando a uma coluna no celular.
- Trechos da leitura são identificados como prévia; acesso ao original permanece claro.

## Linguagem e confiança

Texto direto em português. Uma ação principal por etapa. Não prometer desempenho,
salvamento, login ou correção que o sistema ainda não confirmou. Separar claramente
dados demonstrativos de dados da conta. Os metadados do protótipo ficam na barra superior.

## Acessibilidade inicial

HTML semântico, labels dos campos, radio buttons reais, foco visível, confirmação
em dialog nativo, legendas textuais dos estados e respeito à preferência de movimento.
Esses recursos precisam de testes de acessibilidade e integração na implementação;
não constituem certificação WCAG nesta fase visual.
