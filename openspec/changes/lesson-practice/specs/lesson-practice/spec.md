# lesson-practice

## ADDED Requirements

### Requirement: Início de prática vinculado à aula
A aplicação SHALL oferecer prática própria para o assunto e cargo explicitamente
selecionados na aula, anunciando um treino de até cinco questões fixas aprovadas
e destinadas exclusivamente a PRATICA_AULA. O acervo SIMULADO MUST NOT ser usado.
Praticar SHALL ser a ação explícita de início. Abrir uma aula MUST NOT criar prática.

#### Scenario: Acervo elegível pequeno
- **WHEN** o estudante inicia prática de assunto/cargo com três questões elegíveis
- **THEN** recebe exatamente essas três bases distintas, identificadas como prática da aula
- **AND** não acessa o compositor de simulados nem recebe questões de outro assunto

#### Scenario: Nenhuma questão elegível
- **WHEN** não há questão aprovada para aquele assunto/cargo
- **THEN** recebe estado sem questões e pode voltar à aula
- **AND** nenhuma prática parcial é persistida nem há fallback para outro assunto

### Requirement: Correção somente no encerramento
A prática SHALL permitir responder, limpar e navegar sem revelar gabarito, acerto
ou explicação. Somente após encerramento confirmado o backend SHALL liberar resultado
e resoluções. Correção MUST NOT ser calculada no navegador.

#### Scenario: Pedido antecipado de resultado
- **WHEN** o proprietário solicita resultado antes de encerrar
- **THEN** o servidor recusa e nenhuma alternativa correta ou resolução é devolvida

#### Scenario: Encerrar com questões em branco
- **WHEN** escolhe encerrar uma prática parcialmente respondida
- **THEN** a confirmação distingue respondidas/em branco e permite continuar
- **AND** após confirmar e receber conclusão do servidor vê resultado e explicações

### Requirement: Separação de modos, histórico e desempenho
A finalidade SHALL ser imutável e preservada no núcleo/persistência/replay. Práticas
MUST NOT aparecer em histórico, janela paginada ou métricas de simulados. Conta SHALL
salvar/retomar práticas em histórico próprio; tentativas antigas continuam simulados.

#### Scenario: Práticas recentes não ocupam a janela de simulados
- **WHEN** uma conta possui simulados anteriores e práticas mais recentes
- **THEN** histórico e métricas de simulados filtram modalidade antes de ordenar/limitar
- **AND** práticas aparecem somente no histórico próprio, sem reclassificar dados antigos

#### Scenario: Identificador usado em modalidade errada
- **WHEN** um id de prática é usado nas rotas de simulado ou vice-versa
- **THEN** o recurso é indisponível404 e nenhuma leitura de conteúdo ou mutação acontece

### Requirement: Segurança, idempotência e compatibilidade
Posse/expiração/identidade SHALL ser verificadas em todas as operações e no replay.
CSRF/sessão/autenticação técnica SHALL pertencer à infraestrutura. Retentativa com
mesmo comando e intenção SHALL manter a prática capturada; outro modo/intenção MUST
NOT reutilizar seu resultado. Capturas antigas e dados V1–V10 SHALL continuar legíveis.

#### Scenario: Criação confirmada com resposta perdida
- **WHEN** a criação foi commitada e o cliente repete o mesmo comando
- **THEN** retorna a mesma prática e ordem de questões/alternativas, sem duplicação
- **AND** reutilizar esse comando com outra modalidade ou contexto gera conflito

#### Scenario: Visitante e conta
- **WHEN** visitante inicia prática e depois autentica ou sua sessão expira
- **THEN** conteúdo anterior não reaparece nem é transferido automaticamente
- **AND** conta pode retomar apenas suas práticas; outra conta recebe404

### Requirement: Preservação de estados independentes
Navegar ou iniciar prática SHALL preservar o simulado ativo, sua fila e escolhas.
Trocar a prática SHALL respeitar operações pendentes e confirmação explícita; troca
de identidade SHALL limpar ambos os contextos e invalidar respostas tardias.

#### Scenario: Prática durante salvamento de simulado
- **WHEN** o estudante abre uma prática enquanto o simulado tem resposta em gravação
- **THEN** a prática tem estado próprio e o simulado não é substituído nem desmontado
- **AND** ao voltar a Simulados a confirmação/fila/escolhas continuam corretas

#### Scenario: Layout e retorno à aula
- **WHEN** realiza a prática por teclado ou em largura390/1280px
- **THEN** foco, navegação, status e encerramento são acessíveis sem corte lateral
- **AND** pode voltar à aula preservada sem criar outra tentativa implicitamente
