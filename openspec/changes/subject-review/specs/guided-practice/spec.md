# Spec Delta

## Purpose

Converter a revisão por assunto em acesso à aula e composição explícita de prática,
reutilizando os contratos seguros de simulados existentes.

## ADDED Requirements

### Requirement: Aula por assunto em mapa controlado
A interface SHALL oferecer links para as aulas dos 51 assuntos por mapeamento
versionado topicId→conteudo do manifesto, resolvido contra a base configurada do
material. MUST NOT inferir arquivos pelo título, usar gabarito ou aceitar traversal/
esquemas arbitrários. Aulas, URLs existentes e funcionamento offline SHALL ser preservados.

#### Scenario: Assunto com espaços e acentos no path
- **WHEN** abre Rever aula para um assunto
- **THEN** chega ao arquivo de conteúdo correto com encoding adequado, sem depender da API estar disponível e sem mudar os arquivos originais.

### Requirement: Preenchimento explícito da prática
Praticar este assunto SHALL abrir o compositor com cargo, disciplina e assunto
escolhidos, quantidade 1, demais disciplinas zeradas e variações false. O usuário
SHALL poder mudar critérios e confirmar; o atalho MUST NOT criar tentativa sozinho.
Semântica de seleção/elegibilidade/idempotência/correção SHALL permanecer a existente.

#### Scenario: Confirmação e acervo insuficiente
- **WHEN** abre a composição e confirma quantidade maior que o acervo elegível
- **THEN** recebe insuficiência real e pode ajustar/repetir, sem troca silenciosa de assunto ou gravação de tentativa parcial.

#### Scenario: Retry e opção numérica
- **WHEN** a criação perde a confirmação e os mesmos critérios são reenviados
- **THEN** mantém commandId; mudança de critérios usa outro comando e opção numérica só é ativada por escolha explícita, sem promessa de novidade.

### Requirement: Respeito à realização ativa e contexto
Atalhos SHALL preservar a confirmação existente antes de abandonar realização ativa,
sem descartar escolhas locais implicitamente. Troca de conta/cargo, logout, expiração
e respostas tardias SHALL impedir reaparecimento de dados privados ou preenchimento
do contexto anterior. Visitante MUST NOT ser incorporado à conta automaticamente.

#### Scenario: Resposta tardia após troca de conta
- **WHEN** uma consulta antiga termina após logout ou login de outra conta
- **THEN** métricas, atalhos e preenchimento antigos não reaparecem nem são persistidos no navegador.

### Requirement: Interface acessível e estados distintos
Painel/revisão SHALL distinguir carregamento, falha, sem dados e poucos dados;
permitir atualização explícita e após conclusão confirmada. Tabela, prioridade,
links e compositor SHALL funcionar por teclado/foco e em 390/1280px, sem depender
apenas de cor e sem usar cálculo de correção no cliente.

#### Scenario: Visitante, falha da API e estudo
- **WHEN** visitante acessa a aplicação ou uma consulta de desempenho falha
- **THEN** o painel privado não expõe histórico, a falha não vira nota zero e as aulas continuam acessíveis.
