# study-experience

## ADDED Requirements

### Requirement: Direção visual Foco nas telas de estudo

A navegação lateral, o início, a montagem, a realização, o resultado, o histórico, a
biblioteca, o leitor de aulas nativas e a prática da aula SHALL seguir a direção Foco.
Ela usa superfícies em cartões, uma ação principal por etapa e enunciado de pelo
menos 18px. Os comportamentos, textos de estado e contratos aprovados nas specs
anteriores MUST permanecer inalterados.

#### Scenario: Realização no celular

- **WHEN** o estudante responde a uma questão em largura de 390px
- **THEN** progresso e acesso ao mapa ficam no topo, e "Anterior" e "Próxima" ficam no rodapé
- **AND** não há rolagem horizontal da página

#### Scenario: Gabarito durante a realização

- **WHEN** o estudante seleciona uma alternativa em qualquer tema
- **THEN** a interface marca apenas a seleção, sem cor ou texto de acerto ou erro

### Requirement: Biblioteca pede a disciplina antes de listar

Decisão do usuário em 2026-11-13. Sem disciplina escolhida nem busca, a biblioteca
SHALL pedir a escolha de uma disciplina e MUST NOT listar as aulas. Escolhida a
disciplina, SHALL listar somente as aulas dela, em cartões de mesma altura. A busca
continua atravessando as disciplinas e identifica a disciplina de cada resultado.
As 51 aulas permanecem acessíveis por disciplina e pela busca.

#### Scenario: Abrir a biblioteca sem filtro

- **WHEN** o estudante abre a biblioteca sem disciplina nem busca
- **THEN** vê os cartões das disciplinas e o pedido de escolha, sem lista de aulas

#### Scenario: Escolher uma disciplina

- **WHEN** o estudante escolhe Português
- **THEN** vê apenas as aulas de Português, todas com a mesma altura de cartão

### Requirement: Aulas nativas legíveis nos dois temas

O leitor SHALL exibir as aulas nativas com tokens nos dois temas, incluindo blocos de
destaque, tabelas, fórmulas e grades de percentual. Figuras SVG SHALL permanecer
legíveis no tema escuro. O conteúdo e a validação dos documentos MUST NOT mudar.

#### Scenario: Ler uma aula no tema escuro

- **WHEN** o estudante abre qualquer uma das 51 aulas no tema escuro
- **THEN** texto, rótulos de bloco, tabelas e fórmulas atendem ao contraste mínimo
- **AND** as figuras aparecem sobre placa clara, sem perda de traços ou legendas

### Requirement: Resumo e revisão filtrável do resultado

Após o encerramento, o resultado SHALL mostrar pontos, percentual e a proporção de
acertos, erros e em branco, com legenda numérica. A revisão SHALL permitir ver
todas as questões ou somente erros e em branco. Cada questão revisada exibe o
estado em texto.

#### Scenario: Filtrar erros e em branco

- **WHEN** o estudante escolhe "Erros e em branco"
- **THEN** a revisão lista só as questões erradas ou não respondidas, mantendo a numeração original

### Requirement: Retorno à aula a partir da revisão

Cada questão revisada SHALL oferecer "Rever a aula" e "Praticar este assunto" para o
assunto da questão. Os atalhos MUST aparecer somente após o encerramento e MUST
NOT encerrar ou substituir outra realização em andamento.

#### Scenario: Rever a aula de uma questão errada

- **WHEN** o estudante escolhe "Rever a aula" em uma questão de P07
- **THEN** o leitor abre a aula nativa P07 completa
- **AND** o resultado continua disponível pelo histórico

#### Scenario: Praticar com uma prática em andamento

- **WHEN** o estudante escolhe "Praticar este assunto" tendo outra prática em andamento
- **THEN** a interface segue a troca explícita de prática já definida, sem descartar respostas silenciosamente
