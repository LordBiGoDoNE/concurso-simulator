# Proposal

## Why

Após testar a entrega local, o usuário descreveu a interface como confusa e com
aparência de formulário. A página atual reúne identidade, composição, realização,
consulta de questões e diagnóstico técnico, com pouca separação entre objetivos.
Precisamos validar uma organização e uma direção visual antes de alterar o React.

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
persistência ou conteúdo nesta fase. A futura implementação deve preservar os
contratos e comportamentos aprovados nas specs anteriores.

## Impact

Artefatos em `design/prototypes/study-experience/`, estudo OpenSpec e issue própria.
Protótipo HTML independente, sem bibliotecas externas, acesso à API, banco ou login.
O catálogo público é uma captura dos cinco grupos, 51 assuntos e dois cargos;
as questões usadas na realização são exemplos demonstrativos repetidos. `lessons.json`
é uma saída derivada das fontes públicas:51 títulos, caminhos, hashes e trechos de
153 seções para prévia. Não substitui figuras, tabelas, fórmulas ou exercícios do original.

O aceite abrange as quatro áreas e cinco estados visuais do protótipo, incluindo aulas.
A escolha posterior abrange todos os grupos2–6. Aceite e escopo não autorizam merges.

Base: `epic/application-reformulation` em `1817673`; sua árvore é idêntica à entrega
em `main` `c8efceb`. A tarefa visual usa branch própria e PR de rascunho para
`epic/study-experience-redesign`. Integração futura exige conferir/sincronizar bases,
revisões, autorização de merge e a sequência da épica, sem reorganização silenciosa.

O site `stable`, Pages, o acervo e a aplicação Docker em 8088 permanecem fora da
mudança. Não há publicação externa, arquivamento ou integração em main nesta fase.
