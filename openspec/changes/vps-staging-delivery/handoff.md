# Retomada — #156

## Status

Escopo ampliado em 2026-11-13: lançamento público em
`https://concurso-simulator.reemsoftworks.com`, com Google e acervo aprovado.
Somente documentos; nenhum grupo de implementação autorizado.

Issue: https://github.com/LordBiGoDoNE/concurso-simulator/issues/156

## Decisões do usuário em 2026-11-13

- O assistente acessa a VPS por SSH como `concurso-dev`, com a chave do PC,
  sem agent forwarding. Sudo só com senha digitada pelo usuário.
- Endereço `concurso-simulator.reemsoftworks.com`; `.com.br` redireciona para `.com`;
  DNS na Hostinger.
- Lançamento público com Google ligado e smoke real antes da abertura.
- Somente o acervo atual aprovado; sem contas/sessões/tentativas do PC.
- Esta spec é ampliada em vez de criar outra.

## Histórico preservado

- Proposta original (privada, Rocky Linux) integrada por #157 em 5dc237c.
- `task/vps-staging-delivery/server-preparation` (local, não enviada): grupo 1 de
  Rocky, superado pela reinstalação Ubuntu de 2026-10-31.
- Publicação HTTP pelo IP feita em 2026-10-31 fora desta spec, documentada em
  `/opt/vps-control` na VPS. Continua ativa até o corte do grupo 7.

## Como continuar

Ler AGENTS.md, proposal.md, design.md, tasks.md e os deltas desta pasta. Conferir
a chave do host antes de conectar. Pedir ao usuário quais grupos completos entram.
