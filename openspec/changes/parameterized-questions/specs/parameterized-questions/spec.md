# Spec Delta

## Purpose

Permitir prática com variações numéricas das três famílias escolhidas, verificáveis
e preservadas por tentativa, mantendo elegibilidade editorial e contratos privados.

## ADDED Requirements

### Requirement: Famílias limitadas e domínios finitos
O sistema SHALL oferecer somente regra de três direta (M03), porcentagem e juros simples (M04), para os cargos atuais de Limeira 01/2026, com domínios v1 de respectivamente 64, 20 e 27 tuplas definidos no design. A geração MUST usar aritmética exata no backend, sem IA ao vivo, código de entrada ou parâmetros escolhidos pelo cliente.

#### Scenario: Tupla de juros simples
- **WHEN** gera capital 2000, taxa de 2% ao mês e prazo de 3 meses
- **THEN** pergunta os juros de 120 reais, distingue-os do montante e apresenta resolução com valores/unidades desse caso somente após conclusão.

#### Scenario: Família ou parâmetro não permitido
- **WHEN** uma definição seleciona fórmula desconhecida, domínio externo ou campo adicional
- **THEN** é rejeitada sem executar código, acessar rede ou aceitar silenciosamente a extensão.

### Requirement: Validação de todas as combinações
Cada versão SHALL enumerar seu domínio integral e validar enunciado, quatro alternativas numericamente distintas, única correta, resolução, unidades, acessibilidade e proveniência, usando oráculo independente do gerador. Manifesto versionado MUST identificar a definição e todas as saídas; uma combinação inválida MUST bloquear a habilitação da versão inteira.

#### Scenario: Alternativas equivalentes
- **WHEN** dois distratores ou um distrator e a correta têm mesmo valor, ainda que formatados diferentemente
- **THEN** a validação aponta a tupla e impede aprovar a versão, sem descartar a combinação para reduzir o domínio.

#### Scenario: Alteração de saída previamente validada
- **WHEN** geração diverge do manifesto registrado para a tupla sorteada
- **THEN** a criação da tentativa inteira falha e não grava conteúdo parcial nem substitui silenciosamente por questão fixa.

### Requirement: Aprovação editorial exata e separada
Templates SHALL ter versões imutáveis, estados editoriais e decisões explícitas com responsável, data, checklist e justificativa referindo digests da definição/domínio/manifesto e revisão-base exatos. Validação automática, aprovação da questão-base, importação e CI MUST NOT aprovar a família implicitamente.

#### Scenario: Testes verdes sem decisão editorial
- **WHEN** 111 combinações passam nas verificações mas a versão ainda não foi aprovada editorialmente
- **THEN** nenhuma família pendente gera variações para novos simulados e o relatório mantém aprovação pendente.

#### Scenario: Definição alterada após revisão
- **WHEN** muda fórmula, domínio, texto, distratores, resolução, fontes ou vínculo à base
- **THEN** exige nova versão/validação/decisão sem herdar elegibilidade ou reescrever decisões anteriores.

### Requirement: Operação local controlada e atômica
Prévia SHALL funcionar sem escrita; registro e decisão SHALL exigir destino/seleção/digest explícitos, confirmar commit e tratar bases/conflitos sem mudanças parciais. Importação repetida MUST ser idempotente e MUST NOT reativar retirada. Nenhuma rota HTTP ou startup normal MUST executar operação editorial.

#### Scenario: Base mudou durante a operação
- **WHEN** outra decisão altera a versão esperada antes do registro/decisão
- **THEN** falha sem sobrescrever o concorrente; conflito conhecido só é recuperado depois de rollback, e falha desconhecida não vira sucesso.

### Requirement: Elegibilidade conjunta com a revisão-base
Geração SHALL exigir base atualmente elegível e template aprovado correspondente à revisão/fingerprint exatos, capturados no mesmo snapshot transacional da tentativa. Retirada/troca de base MUST afetar somente novas capturas e MUST NOT modificar instâncias anteriores.

#### Scenario: Base substituída
- **WHEN** outra revisão da mesma questão passa a ser elegível sem template correspondente aprovado
- **THEN** uma nova tentativa usa essa revisão como fixa e não aplica nela o template aprovado para a anterior.

#### Scenario: Retirada após captura
- **WHEN** a versão-base ou template é retirado após confirmar a criação
- **THEN** dono continua podendo retomar/concluir/ver o conteúdo capturado, conforme posse e prazo existentes.

### Requirement: Opção de composição compatível
Criação SHALL aceitar `useParameterizedQuestions` booleano opcional, false quando ausente. Quando true, SHALL variar somente bases com template elegível, mantendo demais fixas, filtros/quantidades 1–100 e identidades-base distintas. A opção MUST integrar o hash idempotente e a composição imutável.

#### Scenario: Cliente anterior ou opção desligada
- **WHEN** cria sem a opção ou com false
- **THEN** recebe composição fixa pelas regras anteriores, sem exigir mudança de cliente.

#### Scenario: Mistura e limite de diversidade
- **WHEN** inclui Matemática e outra disciplina com variações habilitadas
- **THEN** recebe a quantidade pedida, varia apenas bases elegíveis e não conta múltiplas tuplas da mesma base como questões distintas.

#### Scenario: Reuso do comando com opção diferente
- **WHEN** envia mesmo commandId alterando o booleano
- **THEN** recebe 409 sem mudar nem duplicar a tentativa confirmada.

### Requirement: Captura reproduzível da instância
O backend SHALL sortear uma tupla por base parametrizada e preservar conteúdo renderizado, resolução/correta privadas, fingerprint da instância, versão/manifesto/gerador, revisão-base/fingerprint, parâmetros, IDs e ordens. A referência da base MUST continuar válida; conteúdo da instância MUST NOT ser apresentado como payload original dessa revisão.

#### Scenario: Resposta de criação perdida
- **WHEN** repete o comando idêntico após timeout
- **THEN** recupera os mesmos valores, IDs, ordens e conteúdo, sem novo sorteio ou geração.

#### Scenario: Leitura sem implementação antiga do gerador
- **WHEN** retoma ou conclui uma tentativa capturada depois que a implementação do template deixa de estar disponível
- **THEN** usa conteúdo e correção preservados, sem regeneração ou dependência da elegibilidade atual.

### Requirement: Persistência aditiva e histórico compatível
Migrações SHALL ser aditivas após V5, preservando FKs e imutabilidade. Codecs SHALL ler os dois formatos e preservar o formato de origem/campos imutáveis ao atualizar tentativas antigas. Rollback após primeira escrita no formato novo, mesmo fixa, MUST usar leitores compatíveis com geração desabilitada; retorno ao JAR antigo incompatível MUST NOT ser declarado seguro.

#### Scenario: Tentativa anterior ainda em andamento
- **WHEN** atualiza banco/aplicação com respostas e comandos antigos confirmados
- **THEN** consegue retomá-la, repetir comandos e concluí-la como fixa, preservando IDs, versão, conteúdo e nota.

#### Scenario: Expiração de visitante com variação
- **WHEN** limpeza remove tentativa visitante vencida
- **THEN** remove metadados dependentes dos itens sem apagar templates, bases, decisões ou histórico da conta.

### Requirement: Privacidade e responsabilidades HTTP preservadas
Rotas SHALL manter segurança/CSRF na infraestrutura, posse no núcleo e DTOs tipados sem entidades persistidas. Cliente MUST NOT enviar base/template/parâmetros/correta/nota. Projeções em andamento, erros, recursos e bundles MUST NOT expor parâmetros internos, manifesto, solver, gabarito ou resolução; resultado detalhado SHALL continuar restrito ao dono após conclusão.

#### Scenario: Inspeção ou payload adulterado
- **WHEN** inspeciona a realização ou tenta enviar parâmetros, correta ou nota na criação/resposta
- **THEN** contratos não expõem dados privados; campos extras são rejeitados sem alterar a tentativa nem influenciar geração/correção.

#### Scenario: Indisponibilidade e falha interna
- **WHEN** ocorre falha de banco ou integridade do manifesto na criação
- **THEN** retorna erro genérico 503 ou 500 conforme a causa, sem conteúdo privado, gravação parcial ou falsa confirmação.

### Requirement: Interface e material preservados
Compositor SHALL apresentar opção acessível desligada por padrão, explicar mistura/repetição possível e manter opção/commandId no retry. Resultado SHALL mostrar texto/resolução específicos da instância, com unidades/recursos compreensíveis. Aulas, URLs, offline, login e histórico existentes MUST permanecer funcionais.

#### Scenario: Fluxo com teclado e troca de sessão
- **WHEN** visitante ou conta realiza variação, salva/retoma conforme permissões e consulta resultado
- **THEN** foco/labels/mensagens e descrições permanecem acessíveis; troca de contexto limpa dados privados e não confunde escolha local com salvamento confirmado.

#### Scenario: API indisponível e material offline
- **WHEN** API falha ou material estático é aberto sem rede
- **THEN** mantém acesso às aulas/URLs e resoluções existentes, sem exigir gerador, login ou sincronização.
