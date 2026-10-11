# Fixture inferior — piloto remoto

Arquivo sintético para verificar o fluxo de Stacks aprovado no #32.
Não é tarefa do produto e não deve ser integrado sem autorização própria.

- Trunk: `epic/question-bank`.
- Branch: `task/question-bank/stacks-pilot-lower`.
- Dependência da camada superior: este commit inferior.
- PRs de teste em rascunho; sem merge/auto-merge/fila/publicação.
- Evidências na issue #30; revisão cumulativa da spec continua pendente.

## Correção sintética da camada inferior

Este novo commit serve apenas para testar rebase da superior, leases de push,
eventos de atualização e CI após mudança na dependência. Não aprova qualquer PR.
