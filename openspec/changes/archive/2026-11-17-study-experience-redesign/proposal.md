# Proposal

## Why

Após testar a entrega local, o usuário descreveu a interface como confusa e com
aparência de formulário. A página atual reúne identidade, composição, realização,
consulta de questões e diagnóstico técnico, com pouca separação entre objetivos.
A direção foi validada no protótipo; a implementação React agora aplica o layout completo.

## What Changes

- Primeira fase autorizada em 2026-10-23: prototipar no OpenDesign três telas,
  início, configuração e realização do simulado, em desktop e celular.
- No feedback, o usuário aprovou essa direção e solicitou acrescentar aulas.
  A fase visual agora inclui biblioteca, busca/filtros e prévia de leitura.
- Separar navegação geral, preparação do treino e concentração na realização.
- Propor uma linguagem visual clara, com base neutra, azul como ação principal,
  tipografia de sistema e tokens de cores documentados.
- Demonstrar configuração com total visível, assuntos recolhidos e navegação por
  questão; seleção, limpeza e confirmação de conclusão sem revelar correção antes.
- Recolher feedback do usuário e aprovar a direção antes de integrar no produto.
- Organizar as 51 aulas existentes por disciplina/assunto, com links ao material
  completo e composição demonstrativa preenchida para praticar o assunto escolhido.

O usuário aprovou o layout completo em 2026-10-23 e solicitou seguir com ele.
A direção e o percurso estão aceitos. O usuário escolheu explicitamente todos os
grupos2–6 para o primeiro lote completo, conforme AGENTS.md. Revisão dos artefatos,
autorização de merges e gate cumulativo continuam separados desse aceite visual.

## Capabilities

### New Capabilities

- `study-experience`: organização visual e navegação dos fluxos de estudo.

### Modified Capabilities

Nenhuma alteração de regras de identidade, composição, tentativas, correção,
persistência ou conteúdo. A integração preserva os contratos e comportamentos
aprovados nas specs anteriores, incluindo erros, recuperação e contexto privado.

## Impact

Artefatos em `design/prototypes/study-experience/`, estudo OpenSpec e issue própria.
Protótipo HTML independente, sem bibliotecas externas, acesso à API, banco ou login.
O catálogo público é uma captura dos cinco grupos, 51 assuntos e dois cargos;
as questões usadas na realização são exemplos demonstrativos repetidos. `lessons.json`
é uma saída derivada das fontes públicas:51 títulos, caminhos, hashes e trechos de
153 seções para prévia. Não substitui figuras, tabelas, fórmulas ou exercícios do original.

O aceite abrange as quatro áreas e cinco estados visuais do protótipo, incluindo aulas.
A escolha posterior abrange todos os grupos2–6. Aceite e escopo não autorizam merges.

Base inicial histórica: `epic/application-reformulation` em `1817673`, de árvore
idêntica à entrega em `main` `c8efceb`. A reformulação já foi entregue pelo #133;
o layout é uma evolução posterior. Por decisão do usuário em 2026-10-24, o PR
`[SPEC]` #147 de `epic/study-experience-redesign` passa a ter destino `main`
`c8efceb7bc10774902a4cfca1efb05165d9700b9`. O merge está autorizado após revalidar
base/head/digest, revisão independente e checks. Preservar os commits existentes;
nenhum sync/rebase/retarget de membros da stack é necessário ou autorizado.

Implementação em `frontend/src`, testes unitários/Chromium e verificações em pacote
Docker isolado, mantendo as fontes completas das aulas. O site `stable`, Pages, o acervo
e o banco da aplicação do operador em 8088 permanecem preservados. A autorização atual cobre a integração do PR #147 na `main` após a revalidação.
Publicação externa, arquivamento e alteração do banco do operador permanecem fora dela.
