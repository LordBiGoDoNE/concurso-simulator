# visual-foundation

## ADDED Requirements

### Requirement: Fonte única de valores visuais

A interface React SHALL definir cores, sombras e tipografia por tokens CSS em um
único arquivo de tokens. As regras de componentes MUST NOT declarar cores literais
fora da lista de exceções documentada.

#### Scenario: Nova cor em um componente

- **WHEN** um estilo de componente usa uma cor literal fora da lista de exceções
- **THEN** a verificação automatizada do frontend falha e indica o arquivo e a linha

### Requirement: Tema claro e escuro

A interface SHALL oferecer tema claro e escuro com os mesmos tokens. Por padrão, o
tema segue a preferência do sistema. O estudante SHALL poder escolher claro, escuro
ou automático. A escolha MUST ser guardada apenas como preferência visual, sem
respostas, identidade ou dados de tentativa.

#### Scenario: Primeiro acesso com sistema escuro

- **WHEN** o navegador informa preferência escura e não há escolha salva
- **THEN** a interface abre no tema escuro sem piscar o tema claro antes

#### Scenario: Escolha manual persistente

- **WHEN** o estudante escolhe o tema claro e recarrega a página
- **THEN** a interface abre no tema claro mesmo com o sistema em modo escuro

#### Scenario: Armazenamento indisponível

- **WHEN** o navegador recusa leitura ou escrita da preferência
- **THEN** a interface segue a preferência do sistema e continua funcionando

### Requirement: Contraste e foco nos dois temas

Texto SHALL ter contraste de pelo menos 4,5:1 sobre as superfícies em que é usado,
ou 3:1 a partir de 24px ou de 19px em negrito. Bordas de campos, contornos que
indicam estado, ícones com significado e o anel de foco SHALL ter pelo menos 3:1
em todas as superfícies, nos dois temas. Botões e cartões identificados pelo
próprio texto podem usar borda decorativa, conforme a WCAG 1.4.11.

#### Scenario: Navegação por teclado no tema escuro

- **WHEN** o estudante navega com Tab pela realização no tema escuro
- **THEN** cada elemento focado mostra um anel com pelo menos 3:1 sobre o fundo

#### Scenario: Teste de pares de tokens

- **WHEN** a suíte do frontend é executada
- **THEN** cada par de texto ou controle declarado é medido nos dois temas
- **AND** qualquer par abaixo do mínimo falha o teste

#### Scenario: Questão respondida no mapa

- **WHEN** uma questão aparece como respondida no mapa de questões
- **THEN** o contorno que marca esse estado tem pelo menos 3:1 sobre o cartão e
  sobre o próprio preenchimento, nos dois temas

### Requirement: Fonte empacotada e local

A tipografia SHALL usar arquivos de fonte empacotados com o frontend e fallback
para a fonte do sistema. A interface MUST NOT requisitar fontes ou estilos de
serviços externos.

#### Scenario: Execução local sem internet

- **WHEN** a aplicação roda no Docker local sem acesso à internet
- **THEN** textos são exibidos com a fonte empacotada ou com o fallback, sem erro de rede de fonte

### Requirement: Estados não dependem só de cor

Estados de questão (respondida, em branco, atual), de salvamento e de correção
SHALL ser identificáveis por texto, forma ou ícone, além da cor.

#### Scenario: Mapa de questões em escala de cinza

- **WHEN** o mapa de questões é visto sem cor
- **THEN** respondida, em branco e atual continuam distinguíveis pela legenda, pelo preenchimento ou pelo contorno
