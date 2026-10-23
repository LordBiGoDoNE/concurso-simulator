# Sincronização de infraestrutura Pages para a entrega local

Decisão direta do usuário em 2026-10-23: preservar a main anterior em stable,
transferir Pages para stable e então integrar a reformulação em main. Este ajuste
é de integração e documentação; não acrescenta funcionalidade de produto.

Main recebeu a infraestrutura pelos PRs #135/#136, sem a aplicação completa.
O site anterior foi publicado de stable. A sincronização direta #137 conflitou
no README. Esta branch de integração preserva ambos os históricos, resolve esse
conflito e propaga a infraestrutura para a épica geral por um PR próprio.

## Requisitos

- Preservar todos os commits de main e da épica, sem rebase/force-push ou deleção.
- Incorporar o mesmo workflow/builder/testes de stable/main à épica geral.
- Manter as regras arquiteturais da aplicação e corrigir suas instruções ativas
  de publicação para stable no README, AGENTS e roadmap.
- Não trazer a aplicação completa para stable; não publicar main no Pages.
- Revisar o diff completo de integração em contexto novo SOL6.1xHigh e manter
  o gate cumulativo final da reformulação separado, atualizado após integrar.

## Limites

Base da épica `a9168c3200ce494b5ca6a1bed6694a8f58848c16`; main de infraestrutura
`c2eead021379390f272592858a8e7ef3115fe795`. O #137 é preservado como registro do
caminho direto conflitante; o novo PR substitui esse caminho sem publicar a stack.
Issue #134 acompanha a ordem operacional. Arquivamento e exclusões não fazem parte.
