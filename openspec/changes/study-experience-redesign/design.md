# Design

## Context

Entrega local concluída em main, com acervo no PostgreSQL da aplicação. O usuário
solicitou melhorar a experiência após testar a interface. A fase atual cria somente
um protótipo independente no OpenDesign, sem integrar componentes React.

## Goals / Non-Goals

Objetivos: separar início, composição e realização; tornar ações e hierarquia claras;
conferir desktop/celular e recolher decisão visual. Não implementar novas regras,
resultado, histórico, painel, navegação das demais áreas, autenticação ou backend.

## Decisions

- HTML independente e sem dependências, para comparar fluxos antes da integração.
- OpenDesign gerencia o projeto e renderiza as capturas reais pelo seu Chromium.
- Tokens e linguagem em `DESIGN.md`; percurso e preservações em `design-brief.md`.
- Nenhuma sessão ou resposta do usuário entra nos artefatos. O catálogo capturado é
  público; os exemplos da realização são fictícios e marcados como demonstrativos.
- Manter limites de 1–100 questões, cargo, assuntos opcionais, variações opcionais,
  limpeza de resposta, navegação e confirmação; não inventar progresso permanente.

## Risks / Trade-offs

Protótipo não comprova integração, acessibilidade completa ou salvamento. A futura
implementação deverá preservar estados de erro/retry/conflito, posse, identidade,
retomada, tentativas ativas e privacidade. Não transportar sucesso fictício para a UI real.
O renderizador de exportação pode montar imagens maiores que o viewport solicitado;
registrar largura CSS separadamente do raster e conferir interação no HTML local.

## Migration Plan

Sem migração nesta fase. Após aceite visual, aprovar proposta e escolher grupos
completos, implementar em branches próprias e executar revisões/gates necessários.
Manter PR atual em rascunho, sem merges, arquivamento, deploy ou mudança de dados.

## Open Questions

Direção visual, ajustes do percurso e escopo do primeiro lote de integração ainda
dependem da avaliação do usuário. As demais áreas não recebem aprovação implícita.
