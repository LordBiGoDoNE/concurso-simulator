# Protótipo de experiência de estudo

Quatro áreas em um HTML independente: início, composição, realização e aulas.
A área de aulas contém biblioteca pesquisável e prévia de leitura, totalizando cinco estados visuais.
Abra `index.html` ou sirva esta pasta em uma porta local. Não há build ou dependências.

Use a barra superior para comparar as telas; o fluxo principal é “Montar meu
simulado” → configurar → “Começar simulado”. Escolha/limpeza de alternativa,
mapa, anterior/próxima e confirmações operam somente na memória da página.

`catalog.json` é a captura pública que originou o catálogo embutido no HTML.
Nenhuma requisição à API, conta, banco, analytics ou geração de modelo é feita.
Aulas na navegação e na página inicial abre a biblioteca do protótipo. O leitor usa
`lessons.json`, derivado de51 arquivos públicos e do mapa versionado, com caminhos,
hashes e153 trechos. É uma prévia textual: figuras, tabelas, fórmulas e exercícios
continuam no material completo, aberto em `http://127.0.0.1:8088/material/`.
Busca/filtros e navegação são locais. “Praticar assunto” preenche a composição
demonstrativa com um assunto e uma questão, sem criar tentativa no servidor.

Links diretos: `?tela=aulas` abre a biblioteca e `?tela=leitura&assunto=P01` abre uma
prévia específica. IDs desconhecidos não fornecem caminho: usa-se a primeira aula.

Os totais251/51/2 representam o acervo local no momento do estudo. A realização
usa três perguntas demonstrativas repetidas; não aplica filtros, não faz correção
real e não cria tentativa. Dados e progresso demonstrativos estão identificados.

O projeto OpenDesign “Concurso Simulator — nova experiência de estudo” mantém
cinco artefatos derivados deste HTML, com `data-start` home/compose/practice/lessons/lesson.
As capturas são exportadas pelo renderizador oficial do OpenDesign em1440px e390px.
Capturas/relatórios locais ficam ignorados, separados dos fontes deste protótipo.

Direção visual em `DESIGN.md`; proposta e limites em
`openspec/changes/study-experience-redesign/`. O React, Docker, acervo, main, stable
e Pages não são alterados. Proposta de produto, revisão e aceite visual pendentes.

As dez capturas geradas estão em `previews/`; comandos executados e limites em [verification.md](verification.md). A exportação de página longa pode repetir elementos fixos entre trechos. Confira a interação no HTML local.
