# lesson-animations

## ADDED Requirements

### Requirement: Animação segura nas aulas nativas

O compilador das aulas e o validador do leitor SHALL aceitar animações somente em SVG
declarativo, dentro da lista branca de elementos e atributos de animação, e SHALL
recusar qualquer animação que altere links, estilos ou eventos, que dependa de
interação do usuário para começar ou que aponte para fora da própria figura.

#### Scenario: Animação válida

- **WHEN** o manifesto lista uma animação cujo SVG usa só elementos e atributos
  permitidos
- **THEN** o documento nativo da aula inclui um nó `animation` com IDs prefixados
- **AND** o leitor a renderiza sem executar script nem aplicar CSS embutido

#### Scenario: Atributo animado proibido

- **WHEN** um SVG de animação usa `attributeName` igual a `href`, `xlink:href`,
  `style` ou a um atributo de evento
- **THEN** o compilador falha e nomeia o arquivo e o atributo
- **AND** o validador do frontend recusa o mesmo nó se ele chegar ao leitor

#### Scenario: Início por evento ou referência externa

- **WHEN** um `begin`/`end` usa evento de usuário ou um `mpath`/`url()` aponta para
  fora da figura
- **THEN** o compilador falha e nenhuma aula é gerada

### Requirement: Inventário versionado das animações

O projeto SHALL manter `assets/animacoes/manifest.json` com, para cada animação,
aula, âncora de inserção, arquivo, título, descrição, modo, quadro estático e, no
modo etapas, as etapas com tempo e legenda. O compilador SHALL validar o manifesto e
inserir cada animação depois da âncora indicada.

#### Scenario: Âncora inexistente

- **WHEN** uma entrada aponta para uma âncora que não existe na aula, ou que existe
  mais de uma vez
- **THEN** o compilador falha e nomeia a animação e a âncora

#### Scenario: Etapas inconsistentes

- **WHEN** as etapas não estão em ordem crescente de tempo, excedem a duração, passam
  de 8 ou têm legenda vazia
- **THEN** o compilador falha e nomeia a animação

#### Scenario: Material estático intocado

- **WHEN** as animações são compiladas
- **THEN** nenhum HTML legado das aulas, `/material/` ou a branch `stable` muda

### Requirement: Controles e movimento reduzido

O leitor SHALL oferecer controles de pausa e reprodução em toda animação e de etapa
anterior/próxima no modo etapas, SHALL anunciar a legenda da etapa atual a
tecnologias assistivas e SHALL não iniciar movimento automático quando o sistema
pedir movimento reduzido.

#### Scenario: Modo ciclo na tela

- **WHEN** uma animação em ciclo entra na área visível e o sistema não pede
  movimento reduzido
- **THEN** ela reproduz em ciclo e o botão de pausa fica disponível
- **AND** ela para ao sair da área visível

#### Scenario: Modo etapas

- **WHEN** o estudante aciona "Próxima" numa animação em etapas
- **THEN** a figura mostra a etapa seguinte, o indicador passa a "Etapa n de N" e a
  legenda nova é anunciada

#### Scenario: Movimento reduzido

- **WHEN** o sistema pede movimento reduzido
- **THEN** nenhuma animação começa sozinha
- **AND** a figura mostra o quadro estático (ciclo) ou a etapa atual parada (etapas)

#### Scenario: Equivalente textual

- **WHEN** a animação é exibida
- **THEN** a figura tem título e descrição acessíveis e a descrição completa aparece
  em texto abaixo dela

### Requirement: Estilo e limites

Cada animação SHALL seguir o guia de estilo de traço à mão, ter rótulos em texto
real com contraste mínimo de 4,5:1, pesar até 30 KB, ter ciclo de até 12 s ou até 8
etapas, ser legível em 360 px de largura e ser gerada de forma determinística pela
ferramenta de autoria. Cada aula SHALL ter no máximo 2 animações.

#### Scenario: Limites conferidos

- **WHEN** a verificação das animações roda
- **THEN** ela falha se algum arquivo passar de 30 KB, algum ciclo passar de 12 s,
  alguma aula tiver mais de 2 animações ou o SVG versionado diferir do gerado pela
  ferramenta

### Requirement: Animação fiel ao texto da aula

Cada animação SHALL mostrar somente conceitos que o texto da aula já explica, com a
mesma terminologia e, em Informática, a versão de referência das aulas, sem capturas
de tela reais.

#### Scenario: Conferência técnica

- **WHEN** uma animação é entregue
- **THEN** o PR do grupo registra, para cada etapa, a afirmação mostrada e o trecho
  da aula que a sustenta

### Requirement: Piloto em Informática e Raciocínio Lógico

A entrega SHALL incluir as 9 animações do piloto (I02, I05, I07, I08, I09, I10, M11 e
duas em M12) e nenhuma animação em outras disciplinas.

#### Scenario: Escopo do piloto

- **WHEN** o manifesto final é lido
- **THEN** ele tem exatamente as 9 animações do piloto, todas em aulas I ou M10–M12
