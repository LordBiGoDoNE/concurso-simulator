# Design

## Context

Entrega local concluída em main, com acervo no PostgreSQL da aplicação. O usuário
solicitou melhorar a experiência após testar a interface. O protótipo independente
no OpenDesign foi aprovado por completo e integrado pelo PR #140. O usuário autorizou
todos os grupos 2–6: integração React, regressões e gate cumulativo próprio.

## Goals / Non-Goals

Objetivos: separar início, composição e realização; tornar ações e hierarquia claras;
conferir desktop/celular e integrar o layout aprovado. Preservar regras e contratos
de resultado, histórico, painel, autenticação e backend. Organizar as aulas mantendo
o conteúdo completo e suas URLs existentes. Não criar metas ou progresso pessoal fictício.

## Decisions

- HTML independente e sem dependências, para comparar fluxos antes da integração.
- OpenDesign gerencia o projeto e renderiza as capturas reais pelo seu Chromium.
- Tokens e linguagem em `DESIGN.md`; percurso e preservações em `design-brief.md`.
- Nenhuma sessão ou resposta do usuário entra nos artefatos. O catálogo capturado é
  público; os exemplos da realização são fictícios e marcados como demonstrativos.
- Manter limites de 1–100 questões, cargo, assuntos opcionais, variações opcionais,
  limpeza de resposta, navegação e confirmação; não inventar progresso permanente.
- Biblioteca com busca e filtros locais; leitor com trechos textuais identificados,
  índice de aulas e ligação com a composição demonstrativa por assunto.
- Snapshots de leitura derivados das51 fontes públicas, com hashes. Nenhum HTML
  original é injetado no protótipo: títulos, parágrafos e listas são escapados no render.
- Produção: React usa os contratos existentes, contagens reais no início e navegação
  com atalho ao conteúdo/foco explícito. A realização fica montada ao alternar áreas;
  identidade em verificação oculta conteúdo e troca de conta desmonta o contexto.
- Composição mantém comando idempotente/validações; realização mantém a máquina de
  fila/versão e distingue confirmação, pendência, falha, conflito e escolhas preservadas.
- Biblioteca usa catálogo público versionado, sem depender da API. O leitor incorpora
  o HTML completo pelo mapa de teoria, sem scripts, preservando figuras/MathML/exercícios
  e oferecendo o link original. A página incorporada mantém sua apresentação e rolagem;
  iframe remoto não certifica status HTTP, portanto há fallback/retry explícitos.
- Praticar uma aula usa a fronteira de composição da realização. Recusa durante operação
  ou verificação de acesso; confirma a substituição de tentativa e, ao cancelar, conserva
  escolha/fila/comando. Aceitar só preenche assunto/cargo/uma questão; não cria automaticamente.

## Risks / Trade-offs

Protótipo não comprova integração, acessibilidade completa ou salvamento. A
implementação deve preservar estados de erro/retry/conflito, posse, identidade,
retomada, tentativas ativas e privacidade. Não transportar sucesso fictício para a UI real.
O renderizador de exportação pode montar imagens maiores que o viewport solicitado;
registrar largura CSS separadamente do raster e conferir interação no HTML local.

## Migration Plan

Sem migração de schema/conteúdo. Grupos 2–6 escolhidos pelo usuário; implementação em
camadas próprias de rascunho da stack #143, com testes/docs e revisão conjunta ao final.
Protótipo #140 integrado por autorização específica. Merges das implementações, integração
da spec, main, arquivamento e deploy exigem suas autorizações; não são presumidos.

## Open Questions

Layout completo, incluindo aulas, aprovado pelo usuário em 2026-10-23. O usuário escolheu
todos os grupos2–6 para implementação. Não há aprovação implícita
de outras áreas, novos recursos ou merges.
