# question-purpose

## ADDED Requirements

### Requirement: Destinação exclusiva da questão-base
Cada questão-base SHALL possuir destinação exclusiva SIMULADO ou PRATICA_AULA,
protegida no núcleo/persistência. Todas as revisões/variantes da mesma base SHALL
herdar a destinação. Nenhuma base SHALL estar nos dois acervos ou mudar de uso por
reaprovação/importação/novo namespace. Legado SHALL permanecer SIMULADO.

#### Scenario: Revisão ou variante da questão de simulado
- **WHEN** uma base SIMULADO recebe nova revisão, variante ou reaplicação de pacote
- **THEN** continua exclusiva de SIMULADO e não se torna elegível para a prática
- **AND** aprovações/capturas/respostas históricas permanecem preservadas

### Requirement: Seleção e contagens disjuntas
O servidor SHALL aplicar destinação junto da aprovação/assunto/cargo antes de
contar, selecionar e capturar. Prática MUST NOT pegar questões de simulado e
simulado fixo/parametrizado MUST NOT pegar bases de prática. Consulta pública
existente SHALL conservar SIMULADO como acervo padrão.

#### Scenario: Mesmo assunto nos dois acervos
- **WHEN** existem bases aprovadas dos dois destinos para o mesmo assunto/cargo
- **THEN** a interseção entre as bases selecionáveis por modo é vazia
- **AND** nenhuma variante ou revisão da base atravessa essa fronteira

#### Scenario: Acervo de prática insuficiente ou vazio
- **WHEN** há uma base PRATICA_AULA e várias SIMULADO para a aula
- **THEN** a prática usa somente a base própria, ou informa vazio se houver zero
- **AND** não empresta bases de simulado nem aumenta sua contagem com o acervo oposto

#### Scenario: Cliente tenta selecionar o outro acervo
- **WHEN** envia destinação arbitrária ou id do acervo oposto no início
- **THEN** o contrato/invariante rejeita a intenção sem gravar ou expor correção

### Requirement: Conteúdo e decisão editorial próprios
Questões novas de prática SHALL ter enunciados/alternativas/resoluções próprios,
checagem de duplicatas/linhagem e revisão editorial por fingerprint. Copiar uma
questão de simulado mudando apenas números/nomes/ordem MUST NOT contar como nova
questão de prática. Importar/testar/aprovar código MUST NOT aprovar conteúdo.

#### Scenario: Pacote de prática preparado
- **WHEN** o lote escolhido pelo usuário é redigido e validado em banco isolado
- **THEN** continua sem elegibilidade automática, com seleção/digests apresentados
- **AND** somente decisão editorial específica no destino acordado torna-o elegível

#### Scenario: Exercícios antigos publicados nas aulas
- **WHEN** o material original já contém questão do acervo SIMULADO
- **THEN** a separação de modos não declara esse conteúdo secreto/inédito
- **AND** as aulas e as251 bases atuais são preservadas, sem substituição automática
