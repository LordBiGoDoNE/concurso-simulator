# Tasks

Issue: [#245](https://github.com/LordBiGoDoNE/concurso-simulator/issues/245).
Épica: `epic/lesson-animations`. Spec formalizada em 2026-11-18.
Implementação autorizada em 2026-11-19 (grupos 1–7); teste local pelo usuário:
sim. Merges dependem da aprovação do pacote final.

Cada grupo é um PR em rascunho. O grupo 1 parte da épica; os demais são
encadeados, cada um sobre o anterior: o grupo 3 usa nos testes de ponta a ponta a
animação de referência do grupo 2, e os grupos 2, 4, 5 e 6 alteram
`assets/animacoes/manifest.json` e regeneram `frontend/src/lesson-documents/manifest.json`.
Em cada grupo: testes do projeto, CI local via `act` e revisão por grupo antes de
abrir o PR. JSON nativos e manifesto são identificados no PR como gerados.

## 1. Formato seguro, manifesto e compilador

- [x] 1.1 Definir o esquema de `assets/animacoes/manifest.json` (D3) e sua validação
  no compilador: arquivo, âncora única, modo, quadro estático, etapas.
- [x] 1.2 Ampliar `scripts/compile_native_lessons.py` com a lista branca de animação
  (D2), prefixo de IDs em `begin`/`end`/`mpath` e o nó nativo `animation`
  inserido depois da âncora.
- [x] 1.3 Espelhar a validação em `frontend/src/native-lesson.ts`.
- [x] 1.4 Testes de aceitação e de recusa (Python e Vitest): `attributeName`
  proibido, evento de usuário, referência externa, âncora ausente ou duplicada,
  etapas fora de ordem; confirmar que aulas sem animação não mudam.

## 2. Ferramenta de traço à mão, guia e animação de referência — depende de 1

- [x] 2.1 Implementar `scripts/sketch_svg.py`: formas, setas, rótulos e etapas a
  partir de uma descrição simples, com semente fixa e saída determinística.
- [x] 2.2 Implementar a verificação de limites (`--check`): até 30 KB, ciclo até
  12 s, até 8 etapas, até 2 por aula, SVG versionado igual ao gerado.
- [x] 2.3 Escrever o guia `docs/lesson-animations.md` (paleta, contraste, fonte,
  espessuras, durações, como criar e regenerar).
- [x] 2.4 Criar a animação de referência de I05 (alça de preenchimento com
  referências relativa, absoluta e mista), com conferência técnica no PR.
- [x] 2.5 Recompilar I05 e testar a ferramenta (determinismo, limites).

## 3. Componente de animação no leitor — depende de 2

- [x] 3.1 Criar `LessonAnimation` em `NativeLesson.tsx`: figura, título, descrição
  textual, controles e indicador de etapa (D4).
- [x] 3.2 Modo ciclo com início ao entrar na tela e parada ao sair; modo etapas com
  Anterior/Próxima/Reproduzir tudo e legenda em `aria-live`.
- [x] 3.3 Respeitar `prefers-reduced-motion`; estilos em `lessons.css` com os
  tokens dos temas claro e escuro e largura de 360 px.
- [x] 3.4 Testes Vitest e e2e com a animação de I05: controles por teclado,
  anúncio da legenda, movimento reduzido, sem rolagem horizontal da página.

## 4. Informática I02, I07 e I08 — depende de 3

- [x] 4.1 I02: copiar, recortar e colar pela Área de Transferência e histórico
  (Win+V).
- [x] 4.2 I07: tabela dinâmica com campos levados a Linhas, Colunas e Valores.
- [x] 4.3 I08: laço `For Each` percorrendo um intervalo e o efeito de
  `ClearContents`.
- [x] 4.4 Recompilar, rodar `--check` e registrar no PR a conferência técnica de
  cada etapa.

## 5. Informática I09 e I10 — depende de 4

- [x] 5.1 I09: animação de objeto dentro do slide × transição entre slides (ciclo).
- [x] 5.2 I10: caminho do e-mail (SMTP, servidor, IMAP/POP) e quem vê Cc e Cco.
- [x] 5.3 Recompilar, rodar `--check` e registrar a conferência técnica.

## 6. Raciocínio Lógico M11 e M12 — depende de 5

- [x] 6.1 M11: tabela-verdade da condicional preenchida linha a linha e
  equivalência com a contrapositiva.
- [x] 6.2 M12: três conjuntos preenchidos de dentro para fora.
- [x] 6.3 M12: casa dos pombos (ciclo).
- [x] 6.4 Recompilar, rodar `--check` e registrar a conferência técnica.

## 7. Verificação final e documentação — depende de 6

- [x] 7.1 Conferir o escopo do piloto (exatamente 9 animações em I e M10–M12) e
  que HTML legados, `/material/` e `stable` não mudaram.
- [x] 7.2 Teste de ponta a ponta no app local: as 9 animações nos temas claro e
  escuro, em 360 px e com movimento reduzido.
- [x] 7.3 Atualizar README e `frontend/README.md` com o novo tipo de figura e
  preparar a lista de avaliação do piloto para o usuário.
