# Spec Delta

## Purpose

Reunir evidências privadas de tentativas concluídas para orientar a revisão dos
51 assuntos existentes, sem alterar resultados ou criar histórico de visitante.

## ADDED Requirements

### Requirement: Painel privado por cargo e janela explícita
O sistema SHALL consultar as 100 tentativas concluídas mais recentes da conta e
cargo selecionados, ordenadas por finishedAt decrescente e UUID decrescente. Filtros
SHALL anteceder o limite. Janela, contagem, datas extremas e hasOlder SHALL refletir
a mesma visão transacional; até um cabeçalho extra pode detectar corte sem seus itens.

#### Scenario: Janela com outros cargos e tentativas abertas
- **WHEN** há mais de 100 conclusões do cargo e registros de outro cargo/em andamento
- **THEN** somente as 100 conclusões daquele cargo entram e hasOlder indica o corte, sem materializar os itens do 101º registro.

#### Scenario: Conclusão concorrente
- **WHEN** outra tentativa termina durante a consulta
- **THEN** ela aparece por inteiro na visão atual ou somente na próxima, sem contagens e itens de janelas diferentes.

### Requirement: Última evidência por identidade-base
O painel SHALL reter uma observação por questionId da janela, a maior na ordem de
conclusão/UUID, antes de agrupar por assunto. Fixa, revisões e variantes da mesma base
MUST NOT aumentar o número de bases distintas. Cargo, classificação e correção SHALL
vir do conteúdo capturado, sem depender da elegibilidade ou geração atuais.

#### Scenario: Repetição com nova variante
- **WHEN** uma base aparece cinco vezes e a observação mais recente é um erro
- **THEN** ela contribui uma base errada para o assunto capturado nessa observação; as cinco exposições podem ser informadas separadamente.

#### Scenario: Mudança de classificação e retirada editorial
- **WHEN** a classificação da base muda entre tentativas e depois ela é retirada
- **THEN** vale a classificação capturada da observação retida, sem reclassificar JSON histórico ou apagar evidência por retirada.

### Requirement: Métricas e ausência de dados
Os 51 assuntos SHALL ter acertos, erros, em branco, bases avaliadas, exposições e
última conclusão retida. Bases avaliadas SHALL ser a soma dos três desfechos.
Percentual SHALL ser acertos/bases vezes 100 com duas casas HALF_UP, ou null sem bases.
Exposições brutas SHALL contar itens por classificação capturada e MUST NOT alterar
o denominador deduplicado. A UI SHALL explicar janela e última observação por base.

#### Scenario: Conta sem conclusões
- **WHEN** consulta um cargo sem evidência
- **THEN** recebe 51 assuntos com zero contagens, percentual/data ausentes e estado sem dados, sem dificuldade fabricada.

### Requirement: Prioridade transparente e determinística
Prioridades SHALL conter somente assuntos com ao menos três bases e algum erro ou
branco, ordenados por proporção exata de erros+brancos decrescente, depois quantidade
de erros+brancos decrescente e topicId ascendente. A UI SHALL mostrar razão e amostra,
sem afirmar domínio cognitivo ou previsão de aprovação.

#### Scenario: Poucos dados e empate
- **WHEN** um assunto tem duas bases erradas e outros empatam na fração exata com três ou mais
- **THEN** o primeiro informa poucos dados sem entrar na prioridade; os demais seguem desempates estáveis, sem usar percentual arredondado.

### Requirement: Revisão individual após conclusão
Visitante e conta SHALL consultar métricas por assunto somente de uma tentativa
concluída própria. Seus itens distintos dispensam deduplicação histórica. A resposta
MUST NOT agregar outras tentativas. Visitante SHALL manter expiração e limpeza atuais.

#### Scenario: Resultado temporário e tentativa em andamento
- **WHEN** visitante conclui uma tentativa válida
- **THEN** pode revisar seus assuntos nessa sessão, sem progresso permanente; antes da conclusão recebe 409 sem métricas.

### Requirement: Segurança e falha sem resumo enganoso
Segurança SHALL autenticar o painel antes do controller; núcleo SHALL proteger posse,
conclusão e expiração. Ausência de conta no painel SHALL retornar 401; tentativa
ausente/alheia/expirada 404 indistinguível. Entradas extras/repetidas/inválidas SHALL
retornar 400. DTOs tipados SHALL expor somente métricas e contexto permitido, com
no-store, sem dados de correção, captura, conta, itens ou parâmetros privados.

#### Scenario: Acesso cruzado e entrada manipulada
- **WHEN** outra conta conhece o UUID ou envia owner/accountId/limite/parametrização arbitrários
- **THEN** recebe404 para recurso inacessível ou400 para entrada inválida, sem alterar dados ou revelar existência/correção.

#### Scenario: Integridade ou banco indisponível
- **WHEN** uma evidência tem vínculo/formato inconsistente ou a leitura falha
- **THEN** a resposta inteira falha genericamente com 500 de integridade ou 503 de indisponibilidade, nunca pula registros ou informa ausência/zero como sucesso.

### Requirement: Leitura compatível e limitada
Consultas SHALL ler V5 e formato 2 sem regravar JSONs/hashes, nem criar perfil, cache
persistente ou nova decisão editorial. Leitura de itens/instâncias SHALL ser em lote,
com até 100 tentativas/10.000 itens no painel. Índice novo SHALL usar Flyway aditivo,
se justificado, sem alterar migrações V1–V9 ou dados históricos.

#### Scenario: Snapshot legado e geração desligada
- **WHEN** há tentativas V5, novas fixas e geradas com geração desabilitada
- **THEN** o resumo permanece reproduzível por conteúdo capturado, sem invocar gerador nem modificar os bytes persistidos.
