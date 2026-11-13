# Proposal

## Why

O usuário pediu uma renovação do design em 2026-11-13. O layout integrado pelo #147
organizou os fluxos, mas a aparência ainda é a do protótipo inicial. As cores estão
espalhadas como literais no `frontend/src/style.css`, não existe modo escuro e alguns
pares de cor ficam abaixo do contraste mínimo:

- anel de foco `#d57d17`: 2,9:1 sobre o fundo geral;
- borda de campos `#ced7e3`: 1,45:1 sobre branco;
- linha de fatos da home: 4,3:1 em texto pequeno.

O resultado do simulado também é uma lista longa sem resumo visual nem caminho de
volta para a aula do assunto errado.

O design system foi extraído do código atual e foram desenhadas três direções:
Caderno de prova, Foco e Trilha. O usuário escolheu a direção **B · Foco**:
uma evolução do visual atual, com mais respiro, tipografia maior, contrastes
corrigidos e modo escuro.

## What Changes

- Centralizar cores, tipografia, espaços, raios e sombras em tokens CSS únicos,
  com tema claro e escuro.
- Adotar a fonte Figtree empacotada com o frontend, sem requisição a serviços
  externos, com fallback para a fonte do sistema.
- Corrigir os pares de contraste abaixo do mínimo e garantir anel de foco
  de pelo menos 3:1 em todas as superfícies dos dois temas.
- Aplicar a direção Foco às telas existentes:
  - navegação lateral;
  - início;
  - montagem do simulado;
  - realização, no desktop e no celular;
  - resultado e histórico;
  - biblioteca de aulas;
  - leitor de aulas nativas, incluindo os blocos de destaque, tabelas, fórmulas e figuras;
  - prática da aula.
- Resultado com resumo visual de acertos, erros e em branco. Revisão filtrável por
  "Todas" e "Erros e em branco". Atalhos "Rever a aula" e "Praticar este assunto"
  a partir do `topicId` já presente em cada questão.
- Modo escuro que segue o sistema por padrão, com troca manual pelo estudante.
- Atualizar o design system publicado para os tokens Foco, como referência.

Não muda regras de identidade, composição, seleção, tentativas, correção,
persistência, API, banco ou conteúdo das aulas e questões.

## Capabilities

### New Capabilities

- `visual-foundation`: tokens, temas, tipografia, contraste e foco da interface.

### Modified Capabilities

- `study-experience`: apresentação do resultado e retorno à aula a partir da revisão.
  Os demais requisitos de navegação, realização e privacidade continuam válidos.

## Impact

- Código: somente `frontend/src` (CSS, componentes de apresentação e testes) e os
  arquivos de fonte em `frontend/src/assets/fonts/`, com a licença OFL.
- Backend, migrações, contratos HTTP e acervo não mudam.
- O leitor usa as aulas nativas já integradas (`lesson-documents/*.json` e
  `NativeLesson`). Esta spec muda só a apresentação: os documentos, o vocabulário
  aceito pelo `parseNativeLesson` e o conteúdo das 51 aulas não mudam. O HTML das
  aulas estáticas e o site `stable` não são alterados.
- A escolha de tema é uma preferência de interface guardada no navegador. Ela não
  contém respostas, identidade nem dados de tentativa.
- Referências visuais (privadas até serem compartilhadas):
  - [Design system Concurso Simulator](https://claude.ai/artifact/NifjLGgW5EW8xUetL46JGY)
  - [Canvas de direções](https://claude.ai/artifact/7SrviGyn1AuW91QFCe5FkN), linha "B · Foco".
  Os números de resultado e progresso nessas telas são exemplos.
- Fluxo: parte da `main` atualizada e integra em `epic/focus-visual-refresh`, com
  tarefas `task/focus-visual-refresh/<assunto>` e PR final `[SPEC]` para `main`.
  O fluxo exige a revisão final de código (Opus 5.5, esforço medium, em agente novo)
  e autorização própria de merge.
  A aprovação desta proposta não autoriza implementação de grupos, merges nem publicação.
