# Grupo 5 — acervo: evidências

Executado em 2026-11-13 pelo assistente como `concurso-dev`, no banco de produção (V17).

## Escopo decidido pelo usuário

Expansão 2026-11 e conjuntos de outubro. A expansão (102 pacotes, 2.587 itens) foi
carregada pelo fluxo administrativo HTTP. Os conjuntos de outubro (250 simulado,
255 prática, 3 famílias parametrizadas) só entram pela CLI local; a doc da CLI veda
apontá-la para produção e as famílias exigem nova aprovação editorial com IDs novos.
Pendente de decisão do usuário.

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
