# Grupo 5 — acervo: evidências

Executado em 2026-11-13 pelo assistente como `concurso-dev`, no banco de produção (V17).

## Escopo decidido pelo usuário

Em 2026-11-13 o usuário escolheu primeiro carregar também os conjuntos de outubro
(250 simulado, 255 prática, 3 famílias parametrizadas). Esses conjuntos só entram pela
CLI local, com confirmação digitada por importação. O container auxiliar
(`vps_production.py editorial-cli`) foi preparado e uma prévia sem escrita confirmou
pacotes idênticos aos aprovados (250 fingerprints iguais a `eligibility.jsonl`;
seleção de prática `1c7ff1ad…`). Em seguida o usuário decidiu **não carregar os
conjuntos de outubro**: o acervo de produção é a expansão 2026-11 carregada pela API.
Nenhuma importação de outubro foi feita.

## Carga da expansão

- Backup antes da carga; segredos de importação gerados no `start` (valores não impressos).
- Dry-run: 102 prévias com `packageDigest`/`selectionDigest` iguais ao manifesto versionado e
  SHA-256 de cada requisição conferido (3,7 s).
- Carga: 102 importações, 102 lotes SUBMIT e 102 lotes APPROVE `AI_ONLY` (checklist completa),
  em 38 s. Diário privado em `production/question-load.json`.
- Banco: 2.587 questões, 2.587 elegíveis, 102 recibos, 204 lotes, 2.587 decisões `AI_ONLY`;
  0 tentativas; única conta é a do admin.
- Reexecução: nenhuma chamada de escrita; verificação 2.587/2.587.
- Contratos públicos nos 51 assuntos: prática ≥ itens do pacote com `maximum = 5`,
  simulado ≥ itens do pacote. Conferido também de fora por HTTPS (ex.: P01 25/25, M04 25/26, S11 25/25).
- Backup pós-carga criado e copiado para o PC, SHA-256 conferido.

## Limites

O acesso da carga é pelo loopback do host (o IP da VPS não está na allowlist de
pré-lançamento); a sessão admin usa a senha inicial do arquivo privado, nunca impressa.
Revisões do conteúdo foram assistidas por IA, sem revisão humana, como registrado na #173/#177.
