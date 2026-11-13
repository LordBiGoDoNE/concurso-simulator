# administrative-question-curation Specification

## Purpose
Permitir que o ADM consulte o acervo da plataforma e decida em lote, com prévia e confirmação, registrando se cada aprovação teve revisão humana ou somente por IA. Atende à [issue #177](https://github.com/LordBiGoDoNE/concurso-simulator/issues/177). A plataforma oferece os mecanismos; a curadoria do acervo é do ADM.

## Requirements

### Requirement: Origem da revisão nas decisões
O sistema SHALL registrar em cada decisão APPROVE criada após esta mudança a origem da revisão, `HUMAN_PEER` ou `AI_ONLY`, informada pelo operador. Decisões históricas sem o dado MUST permanecer como "não informado", sem inferência.

#### Scenario: Aprovação exige origem
- **WHEN** uma decisão APPROVE é enviada pela API em lote ou pela CLI local sem origem da revisão
- **THEN** a decisão é recusada antes de persistir, sem alterar elegibilidade nem sequência.

#### Scenario: Consulta identifica aprovação só por IA
- **WHEN** o ADM filtra questões elegíveis por origem `AI_ONLY`
- **THEN** recebe apenas questões cuja revisão elegível foi aprovada com essa origem; históricos sem o dado aparecem como não informados e não entram no filtro.

### Requirement: Consulta administrativa somente leitura
A API SHALL oferecer busca, detalhe, consulta em lote por origem/fingerprint e exportação do acervo sob `/api/v1/admin/questions`, apenas para ADM autenticado com permissão persistida, com respostas `no-store` e sem efeitos de escrita. As rotas públicas de questões MUST manter seus contratos e continuar sem gabarito, resolução, estado ou fingerprint.

#### Scenario: Matriz de acesso à consulta
- **WHEN** visitante, sessão expirada, usuário comum, ADM com troca de senha pendente e ADM válido acessam a consulta administrativa
- **THEN** apenas o ADM válido recebe dados; os demais recebem 401 ou 403 sem conteúdo do acervo.

#### Scenario: Lookup de pacote local
- **WHEN** o ADM envia pares de origem e fingerprint de um pacote local
- **THEN** recebe, por par, se a origem não existe, se coincide com a última revisão ou com a elegível, ou se difere, com os identificadores e fingerprints atuais, sem reservar nada.

#### Scenario: Exportação reimportável
- **WHEN** o ADM exporta uma seleção
- **THEN** recebe um pacote `administrative-question-package/v1` válido pelo leitor de importação, com os mesmos fingerprints das revisões exportadas.

### Requirement: Decisões editoriais em lote confirmadas
A API SHALL oferecer prévia sem escrita e aplicação confirmada de decisões SUBMIT e APPROVE para até 100 questões por lote, sob `/api/v1/admin/editorial-decisions`, com CSRF, token de confirmação vinculado a ator, destino, seleção e estado observado na prévia, e recibo idempotente. Cada transição MUST passar pelas regras do domínio existentes.

#### Scenario: Prévia sem efeitos
- **WHEN** o ADM pede a prévia de um lote
- **THEN** recebe por item o estado atual e a transição resultante ou o motivo de recusa, e um token; nenhuma decisão, sequência ou elegibilidade muda.

#### Scenario: Aplicação do estado confirmado
- **WHEN** o ADM aplica com o token e o estado de todos os itens é igual ao da prévia
- **THEN** todas as decisões são gravadas numa única transação, com responsável igual ao ADM autenticado, origem da revisão e checklist, e um recibo é devolvido.

#### Scenario: Estado divergente da prévia
- **WHEN** qualquer item mudou entre a prévia e a aplicação
- **THEN** nenhum item é aplicado e a resposta lista os itens divergentes para nova prévia.

#### Scenario: Replay
- **WHEN** a mesma aplicação confirmada é reenviada após resposta perdida
- **THEN** o recibo original é devolvido sem decisões duplicadas; token expirado ou alterado é recusado.

### Requirement: Plataforma sem curadoria automática
O sistema SHALL limitar validações automáticas do lote à estrutura, à autorização, às transições do domínio e à integridade entre prévia e aplicação. A conferência de conteúdo, de concorrência editorial e de quase-duplicatas MUST ficar a cargo do operador, apoiada pela consulta administrativa.

#### Scenario: Sem bloqueio editorial implícito
- **WHEN** o ADM aprova em lote questões estruturalmente válidas em estado que permite a transição
- **THEN** a plataforma não as recusa por critérios editoriais próprios, como semelhança entre questões.
