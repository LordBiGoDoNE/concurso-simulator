# Verificação do estudo visual com aulas

Observação: 2026-10-23T14:46:17.703649+00:00.

- HTML SHA-256: `12b73816bb9cc5efc14b9ab77d26eee0b9b73b10c85b8ae34a2bb7a147e42c23`.
- OpenDesign 0.21.1, direção `modern-minimal`, projeto gerenciado
  “Concurso Simulator — nova experiência de estudo”. Nenhum outro modelo foi acionado.
- Quatro áreas e cinco estados:início,composição,realização,biblioteca e leitura.
- Dez PNGs exportados pelo Chromium oficial em1440×1000 e390×844 CSS px;
  DPI do Windows e altura completa explicam diferenças no raster.
- Conferência visual dos novos catálogo/leitor desktop/celular e amostras de
  regressão:início/composição mobile e realização desktop. As outras capturas
  foram regeneradas para refletir a navegação com Aulas.
- Linter OpenDesign:p0=0, p1=0, p2=0. `node --check` passou após a ampliação.
- OpenSpec1.14.1 estrito válido. Foi corrigido um separador ausente entre a
  entrelinha e um token CSS; lint sozinho não havia detectado essa declaração.
- 51 IDs e caminhos cruzados com `frontend/src/lesson-paths.json`; hashes das51
  fontes conferidos. Todos os 51 links de teoria retornaram HTTP 200 na aplicação8088.
- `lessons.json`: saída gerada das51 fontes públicas, com 153 seções textuais para
  prévia. Textos são escapados,sem injetar HTML do material. Não equivale à aula
  completa:figuras,tabelas,fórmulas e exercícios permanecem no original vinculado.
- GET `http://127.0.0.1:18889/?tela=aulas`:HTTP200 e bytes iguais ao HTML.

As três telas iniciais foram aprovadas visualmente pelo usuário no feedback que
solicitou aulas. A biblioteca/leitura aguardam nova avaliação. Não se declara aceite
React,merge,aprovação editorial,teste integrado ou revisão independente SOL.

Não houve smoke interativo por teclado nem auditoria WCAG nesta rodada. Capturas e
checagens estáticas não demonstram todos os estados de interação. Exportação de
página longa pode repetir elementos fixos e barras de rolagem entre trechos;
confira o comportamento real no HTML local. Não houve medição de cota de revisor,
pois nenhum revisor foi executado nesta fase visual.

## Capturas geradas

| Estado | Desktop | Celular |
| --- | --- | --- |
| Início | [PNG](previews/inicio-desktop.png) | [PNG](previews/inicio-mobile.png) |
| Composição | [PNG](previews/configuracao-desktop.png) | [PNG](previews/configuracao-mobile.png) |
| Realização | [PNG](previews/realizacao-desktop.png) | [PNG](previews/realizacao-mobile.png) |
| Aulas | [PNG](previews/aulas-desktop.png) | [PNG](previews/aulas-mobile.png) |
| Leitura | [PNG](previews/leitura-desktop.png) | [PNG](previews/leitura-mobile.png) |

## Histórico

A direção inicial,fonte anterior e seis prévias estão preservadas no commit
`e21c013e4c151a7f5f8611b8cca22ec2d038fa08`. A ampliação é commit adicional no PR140,
sem rebase,force-push,merge ou alteração da aplicação/Pages/acervo.

A formatação CSS anterior foi preservada para evitar alterações de milhares de linhas sem mudança visual. O snapshot embutido gerado é conferido contra lessons.json, que permanece legível.
