# Protótipo de experiência de estudo

Três telas em um HTML independente: início, composição e realização do simulado.
Abra `index.html` ou sirva esta pasta em uma porta local. Não há build ou dependências.

Use a barra superior para comparar as telas; o fluxo principal é “Montar meu
simulado” → configurar → “Começar simulado”. Escolha/limpeza de alternativa,
mapa, anterior/próxima e confirmações operam somente na memória da página.

`catalog.json` é a captura pública que originou o catálogo embutido no HTML.
Nenhuma requisição à API, conta, banco, analytics ou geração de modelo é feita.
O link para aulas abre o material existente em `http://127.0.0.1:8088/material/`.

Os totais251/51/2 representam o acervo local no momento do estudo. A realização
usa três perguntas demonstrativas repetidas; não aplica filtros, não faz correção
real e não cria tentativa. Dados e progresso demonstrativos estão identificados.

O projeto OpenDesign “Concurso Simulator — nova experiência de estudo” mantém
três artefatos derivados deste HTML, com `data-start` home/compose/practice.
As capturas são exportadas pelo renderizador oficial do OpenDesign em1440px e390px.
Capturas/relatórios locais ficam ignorados, separados dos fontes deste protótipo.

Direção visual em `DESIGN.md`; proposta e limites em
`openspec/changes/study-experience-redesign/`. O React, Docker, acervo, main, stable
e Pages não são alterados. Proposta de produto, revisão e aceite visual pendentes.

As seis capturas geradas estão em `previews/`; comandos executados e limites em [verification.md](verification.md). A exportação de página longa pode repetir elementos fixos entre trechos. Confira a interação no HTML local.
