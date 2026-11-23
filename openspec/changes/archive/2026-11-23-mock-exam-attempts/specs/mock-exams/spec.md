# Spec Delta

## Purpose

Permitir prática por simulados compostos do acervo elegível, com realização acessível,
correção confiável no servidor e preservação da versão exata usada em cada tentativa.

## ADDED Requirements

### Requirement: Composição explícita e limitada
O sistema SHALL permitir escolher um cargo atual de Limeira 01/2026, quantidades
positivas por disciplina e assuntos opcionais compatíveis, com total entre 1 e 100.
Distribuição e pontuação MUST NOT ser apresentadas como regras oficiais do edital.

#### Scenario: Composição válida
- **WHEN** o participante pede 10 questões de Português e 5 de Matemática para um cargo válido
- **THEN** recebe uma tentativa de 15 questões, com as quantidades solicitadas e sem repetir identidades.

#### Scenario: Critério inválido
- **WHEN** o pedido contém cargo externo, assunto incompatível, quantidade fracionária/não positiva ou total acima de 100
- **THEN** recebe 400 JSON, sem criar tentativa ou alterar o acervo.

### Requirement: Sorteio somente do acervo elegível
O backend SHALL sortear sem reposição entre versões elegíveis aplicáveis aos critérios
no snapshot transacional da composição. Acervo insuficiente MUST produzir conflito
explicando a lacuna, sem completar com rascunhos, duplicações ou questões geradas.

#### Scenario: Insuficiência por disciplina
- **WHEN** uma disciplina tem quatro elegíveis para um pedido de cinco
- **THEN** recebe 409 JSON com a insuficiência daquela seleção e nenhuma tentativa parcial.

#### Scenario: Retirada editorial concorrente
- **WHEN** uma retirada já foi confirmada antes do snapshot usado para compor nova tentativa
- **THEN** essa versão não é selecionada; retirada posterior ao snapshot não reescreve uma tentativa já composta.

### Requirement: Tentativa reproduzível
A tentativa SHALL preservar IDs de questões/revisões, fingerprints, conteúdo exato,
política de composição/correção versionada, parâmetros e ordens de questões e
alternativas. Retomar ou corrigir MUST NOT sortear novamente nem consultar a revisão atual.

#### Scenario: Banco atualizado após início
- **WHEN** uma questão recebe nova versão elegível ou é retirada depois da composição
- **THEN** a tentativa conserva texto, alternativas, ordens, resposta e resolução da versão capturada.

#### Scenario: Ordem das alternativas
- **WHEN** a mesma tentativa é retomada ou sua resposta é corrigida
- **THEN** cada posição continua associada ao mesmo ID de alternativa, independentemente do rótulo visual.

### Requirement: Respostas e conclusão explícita
O participante SHALL poder selecionar, alterar ou limpar uma resposta por questão e
navegar livremente enquanto a tentativa estiver em andamento. Conclusão MUST ser
explícita, irreversível e permitir questões em branco, sem conclusão por mera consulta.

#### Scenario: Limpar uma escolha
- **WHEN** o participante remove a resposta de uma questão e salva a alteração
- **THEN** ela fica em branco, sem alterar as outras respostas ou a ordem da tentativa.

#### Scenario: Concluir com respostas pendentes
- **WHEN** solicita concluir enquanto há salvamento de resposta sem confirmação
- **THEN** a interface solicita resolver o salvamento antes de concluir, sem fingir persistência.

#### Scenario: Alteração depois da conclusão
- **WHEN** tenta responder uma tentativa já concluída
- **THEN** recebe 409 e o resultado/respostas finais permanecem intactos.

### Requirement: Gabarito somente após conclusão
As projeções de tentativa em andamento SHALL excluir resposta correta, resolução,
pontuação parcial, indicadores corretivos e URLs de gabarito. Resultado detalhado
SHALL ser disponibilizado somente ao dono após conclusão, sem ampliar os contratos públicos do banco.

#### Scenario: Inspeção durante realização
- **WHEN** inspeciona JSON, DOM, atributos acessíveis, recursos e respostas a salvamentos
- **THEN** não encontra campos/indicadores corretivos nem endpoint que antecipe o resultado.

#### Scenario: Resultado solicitado antes da conclusão
- **WHEN** o dono consulta o resultado de uma tentativa em andamento
- **THEN** recebe 409 JSON sem gabarito, resolução ou pontuação parcial.

### Requirement: Correção no servidor e resolução da versão usada
Após conclusão, o backend SHALL atribuir um ponto por acerto e zero por erro/em branco,
retornando contagens, total de pontos, percentual e revisão por questão com escolha,
resposta correta, resolução e fontes da versão capturada. Dados de correção do cliente MUST ser rejeitados.

#### Scenario: Resultado conhecido
- **WHEN** conclui cinco questões com três acertos, um erro e uma em branco
- **THEN** recebe 3/5, 60%, contagens 3/1/1 e resoluções das cinco versões utilizadas.

#### Scenario: Manipulação de pontuação
- **WHEN** envia nota, alternativa correta ou resolução como campo de um comando
- **THEN** recebe 400 e esses valores não influenciam a correção.

### Requirement: Repetição segura e concorrência
Comandos SHALL exigir identificador de operação e, nas alterações, versão esperada da
tentativa. Repetição do mesmo comando SHALL recuperar seu resultado confirmado;
reuso com outro conteúdo e comandos concorrentes incompatíveis MUST falhar sem sobrescrita silenciosa.

#### Scenario: Resposta perdida na criação
- **WHEN** repete o comando de criação com mesmo identificador e critérios após timeout
- **THEN** recupera a mesma tentativa com mesmos IDs/ordens, sem novo sorteio ou duplicação.

#### Scenario: Duas abas alteram a mesma base
- **WHEN** duas alterações diferentes partem da mesma versão confirmada
- **THEN** somente uma avança, a outra recebe 409 e a interface oferece recarregar sem descartar escolhas silenciosamente.

#### Scenario: Conclusão repetida
- **WHEN** repete uma conclusão já confirmada com o mesmo identificador/conteúdo
- **THEN** recebe o mesmo resultado final, sem segunda correção ou alteração do histórico.

### Requirement: Interface acessível e falhas recuperáveis
A interface SHALL oferecer composição, progresso, navegação, respostas, confirmação de
conclusão e resultado operáveis por teclado, com foco e mensagens acessíveis. Falhas
SHALL permitir retry sem falsa confirmação e manter acesso às aulas e consulta do banco.

#### Scenario: Teclado e alternativas
- **WHEN** realiza um simulado por teclado em largura de 390 ou 1280 px
- **THEN** identifica cada alternativa e estado de resposta, navega e conclui sem depender de cor ou foco perdido.

#### Scenario: Banco ou rede indisponível
- **WHEN** a consulta/salvamento excede prazo ou falha
- **THEN** vê erro recuperável, distingue escolha local de resposta salva e mantém acesso ao material de estudo.
