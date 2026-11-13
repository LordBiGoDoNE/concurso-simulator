# Tasks

Proposta criada e aprovada pelo usuário em 2026-11-13. Direção visual escolhida
pelo usuário: B · Foco. A aprovação da proposta não autoriza implementar, escolher
o lote nem fazer merges. Esses estados ficam separados na issue da spec. As
perguntas em aberto do design (Figtree ou fonte do sistema; posição do controle de
tema) são decididas no grupo 1.

Issue da spec: [#193](https://github.com/LordBiGoDoNE/concurso-simulator/issues/193).

Lotes escolhidos pelo usuário em 2026-11-13: lote 1 (grupo 1), lote 2 (grupos 2,
3 e 5) e lote 3 (grupos 4 e 6). Itens marcados abaixo estão implementados e
verificados. A integração à épica é registrada separadamente na issue
[#193](https://github.com/LordBiGoDoNE/concurso-simulator/issues/193), com os PRs
e SHAs de cada grupo; o merge em `main` exige autorização própria.

## 1. Fundação visual (base dos demais grupos)

- [x] 1.1 Criar `tokens.css` com os temas claro e escuro, incluir Figtree WOFF2 empacotada com licença OFL e migrar `style.css` para usar só tokens.
- [x] 1.2 Aplicar o tema antes da primeira pintura e criar o controle claro/escuro/automático com preferência em `localStorage` protegida por `try/catch`.
- [x] 1.3 Adicionar a verificação de cores literais e o teste de contraste dos pares de tokens nos dois temas, incluindo anel de foco e bordas de controle.
- [x] 1.4 Atualizar o design system publicado para os tokens Foco e documentar os limites.

## 2. Estrutura e início (depende de 1)

- [x] 2.1 Aplicar a direção Foco à navegação lateral, à faixa superior no celular, à topbar e ao painel de identidade e sessão.
- [x] 2.2 Redesenhar o início com boas-vindas, sugestão de primeira aula e cartões das cinco disciplinas com contagens reais.
- [x] 2.3 Concluir as regressões React e Chromium (claro, escuro, desktop e 390px), teclado, foco e documentação.

## 3. Montagem e realização (depende de 1)

- [x] 3.1 Aplicar Foco à montagem do simulado: etapas, campos com borda de 3:1, resumo lateral e barra fixa no celular.
- [x] 3.2 Aplicar Foco à realização: enunciado de 18 a 19px, alternativas em cartão, mapa com estados por forma e legenda, rodapé de navegação no celular.
- [x] 3.3 Concluir as regressões, incluindo a ausência de gabarito durante a realização nos dois temas, e a documentação.

## 4. Resultado e histórico (depende de 1 e 3)

- [x] 4.1 Criar o resumo do resultado com barra proporcional e legenda numérica, e os rótulos "Correta", "Errada" e "Em branco" com ícone.
- [x] 4.2 Adicionar o filtro "Erros e em branco" e os atalhos "Rever a aula" e "Praticar este assunto" pelo `topicId`, respeitando realizações em andamento.
- [x] 4.3 Aplicar Foco ao histórico e à paginação, concluir as regressões e a documentação.

## 5. Aulas e prática da aula (depende de 1)

- [x] 5.1 Aplicar Foco à biblioteca: busca, filtro por disciplina em pílulas com contagem e lista de assuntos.
- [x] 5.2 Aplicar Foco ao leitor nativo: corpo em 18px, tokens por tom de bloco, tabelas, MathML, grades e barras de percentual, placa clara para SVG, índice e paginação. Preservar documentos, validação, carregamento e erro.
- [x] 5.3 Aplicar Foco ao painel, à realização, à correção e ao histórico da prática da aula.
- [x] 5.4 Verificar as 51 aulas nos dois temas (contraste dos blocos e legibilidade das figuras), concluir as regressões e a documentação.
- [x] 5.5 Biblioteca pede a disciplina antes de listar as aulas; cartões de mesma altura; busca atravessa disciplinas (decisão do usuário em 2026-11-13, após teste local).

## 6. Verificação integrada e revisão final (depende de 1 a 5)

- [x] 6.1 Verificar no Docker local os fluxos início, aula, prática, simulado, resultado e histórico, nos dois temas, em desktop e celular, sem requisições externas.
- [x] 6.2 Consolidar as capturas antes e depois e os limites (figuras SVG em placa clara).
- [ ] 6.3 Executar a revisão final de código com o `spec-reviewer` (Claude Opus 5.5, esforço medium) em contexto novo, registrar `spec-review.json` e `spec-review.md` e corrigir os achados.
- [ ] 6.4 Conferir checks e SHAs e solicitar os merges específicos.

Cada grupo inclui código, testes e documentação. Cada lote termina com testes,
triagem própria e relatório de SHAs e checks antes de pedir aprovação de merge.
Os grupos 2, 3 e 5 são independentes entre si depois do 1. O grupo 4 depende do 3
por compartilhar componentes da realização.
