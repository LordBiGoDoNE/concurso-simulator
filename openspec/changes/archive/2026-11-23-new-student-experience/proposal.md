# Proposal

Acompanhamento: [issue #158](https://github.com/LordBiGoDoNE/concurso-simulator/issues/158).

## Why

Novos estudantes precisam reconhecer o escopo do material e encontrar uma primeira
atividade sem conhecer a organização interna da aplicação. A navegação em memória
não permite compartilhar aulas nem restaurar o contexto ao atualizar.

## What Changes

- Entrada orientada para Limeira01/2026, Assistente Administrativo e Secretário
  de Escola: cargo → aula → prática, com login opcional.
- Catálogo local, biblioteca por cargo e estados públicos no fragmento da URL.
- Consulta de disponibilidade antes de oferecer o início explícito da prática.
- Regressão de histórico do navegador, filas, privacidade, teclado e responsividade.
- Refinamentos solicitados em 2026-11-08: prática recolhível ao final da aula,
  índice acima do título e conversão das 51 aulas para conteúdo nativo do layout.
  Teoria, exemplos resolvidos, esquemas e fontes permanecem na aula; exercícios
  ficam exclusivamente no painel de prática, conforme escolha explícita do usuário.

Plano e quatro grupos completos solicitados pelo usuário em2026-11-05.
Implementação local não representa aceite visual, revisão independente ou integração.

## Capabilities

### New Capabilities

- `new-student-experience`: entrada, navegação pública e descoberta da primeira prática.

### Modified Capabilities

<!-- Nenhuma capability consolidada: openspec list --specs não encontrou specs arquivadas. -->

## Impact

Frontend React, testes e documentação. Reutiliza HTTP existente, sem dependências
novas, migrações, importações no banco, curadoria das questões, Google ou publicação.
Inclui compilação reproduzível de conteúdo público para dados inertes do frontend,
sem modificar os arquivos estáticos preservados ou os registros editoriais.
`stable`, Pages e aplicação publicada permanecem preservados.

## Workflow

Base local: main `f61d57d5d55d6d61609815a2faa4b415ed8dca4b`.
Épica local `epic/new-student-experience`; trabalho em
`task/new-student-experience/student-journey`, worktree separado.
A navegação, os componentes controlados e a combinação dos testes formam uma
entrega local coesa. A separação dos PRs será registrada antes do envio autorizado;
nenhuma stack anterior será alterada. Em 2026-11-08, o usuário autorizou criar a
issue e enviar o trabalho para continuar no notebook. O envio não confirma aceite
visual, revisão independente, merge ou publicação da aplicação.
