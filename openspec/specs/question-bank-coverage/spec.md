# question-bank-coverage Specification

## Purpose
Assegurar variedade verificável de questões nos 51 assuntos atuais, com cobertura independente para simulados e práticas, origem rastreável e habilitação editorial deliberada.

## Requirements

### Requirement: Cobertura mínima por assunto e modalidade

O acervo SHALL oferecer pelo menos 25 bases distintas elegíveis em cada combinação dos 51 assuntos atuais com SIMULADO e PRATICA_AULA. A contagem MUST usar a revisão elegível corrente e não multiplicar uma base por cargo, revisão, variante parametrizada ou reaplicação.

#### Scenario: Todos os assuntos atingem a meta
- **WHEN** a cobertura final dos dois acervos é conferida
- **THEN** cada uma das 102 combinações possui pelo menos 25 bases elegíveis, somando no mínimo 1.275 por modalidade e 2.550 no total

#### Scenario: Rascunhos e variantes não completam cobertura
- **WHEN** um assunto possui 24 bases elegíveis, rascunhos adicionais e variantes de uma dessas bases
- **THEN** sua cobertura permanece insuficiente, sem somar rascunhos, versões ou variantes como novas bases

#### Scenario: Assunto comum a dois cargos
- **WHEN** uma mesma base de Português atende aos dois cargos
- **THEN** ela conta uma vez no assunto e modalidade, mantendo a disponibilidade para ambos sem duplicar o total

### Requirement: Cota de questões originais de provas anteriores

Cada combinação assunto/modalidade SHALL incluir pelo menos cinco questões originais distintas de provas anteriores verificadas. A seleção MUST priorizar Avança SP e complementar com outras bancas compatíveis. Adaptação substancial e questão anulada não cumprem essa cota.

#### Scenario: Cobertura de originais em ambos os acervos
- **WHEN** um módulo completa seus dois conjuntos
- **THEN** pelo menos cinco originais estão em SIMULADO e outras cinco em PRATICA_AULA, sem reutilizar a mesma questão para completar ambas as cotas

#### Scenario: Fonte de outra banca é necessária
- **WHEN** não existem cinco originais adequadas e verificadas da Avança SP para determinada combinação
- **THEN** originais de outras bancas de nível e conteúdo compatíveis podem complementar a cota, registrando a seleção e sua aderência

#### Scenario: Originais insuficientes
- **WHEN** uma combinação alcança 25 bases, mas somente quatro originais de provas cumprem os critérios
- **THEN** ela permanece pendente, sem reclassificar autorais, adaptações ou fontes não verificadas para declarar conclusão

### Requirement: Proveniência comprovável por questão

Cada questão de prova MUST registrar banca, concurso, ano, cargo, caderno/versão, número, fonte da prova, gabarito definitivo e evidência de reutilização permitida. Autorais e adaptações SHALL identificar sua autoria e referências; links de estudo não representam autoria ou validação institucional.

#### Scenario: Original apta à cota
- **WHEN** uma original é selecionada
- **THEN** é possível localizar sua prova e resposta definitiva, conferir versão/número, reutilização e correspondência com o conteúdo importado

#### Scenario: Documento apenas preliminar ou fonte ausente
- **WHEN** a questão tem somente resposta preliminar, procedência não comprovada ou reutilização sem evidência suficiente
- **THEN** ela não integra a cota verificada e a pendência permanece explícita

#### Scenario: Adaptação modifica o problema
- **WHEN** a elaboração altera substancialmente contexto, alternativas ou operação de uma questão de prova
- **THEN** o item é identificado como adaptação/autoral e não como original para a cota mínima

### Requirement: Conferência editorial individual

A habilitação MUST exigir conferência de correção, distratores, clareza, resolução passo a passo, acessibilidade, fontes e classificação sobre o fingerprint exato. A evidência SHALL identificar responsável, data e limites da revisão; importação, quantidade ou CI não aprovam conteúdo.

#### Scenario: Cálculo, legislação ou programa exigem contexto
- **WHEN** uma questão depende de cálculo, dispositivo legal ou comportamento de software
- **THEN** cálculo e resolução são conferidos, e norma/versão/idioma ou recorte temporal necessários à resposta ficam identificados com fontes primárias verificadas

#### Scenario: Revisão assistida por IA
- **WHEN** a conferência é realizada com assistência de IA
- **THEN** a evidência identifica essa condição sem declarar revisão humana independente ou aprovação institucional não realizadas

#### Scenario: Fingerprint muda depois da revisão
- **WHEN** enunciado, alternativas, resposta, resolução, classificação ou proveniência alteram o fingerprint
- **THEN** a evidência anterior não aprova a nova revisão e a conferência deve ser atualizada

### Requirement: Diversidade entre os acervos e o legado

A seleção MUST triar duplicatas exatas, paráfrases e equivalências de estrutura/operação entre candidatos, dois acervos e legado. A mera troca de números, nomes ou IDs SHALL ser insuficiente para contar uma nova base; suspeitas devem ter decisão editorial rastreável.

#### Scenario: Mesmo problema sob origens diferentes
- **WHEN** candidatos de prática e simulado diferem apenas em nomes ou valores, mantendo a mesma tarefa e raciocínio
- **THEN** eles não são usados como duas bases distintas para cumprir as metas e o candidato equivalente é substituído ou fica pendente

#### Scenario: Triagem automatizada encontra semelhança
- **WHEN** o relatório sinaliza um par potencialmente equivalente
- **THEN** a seleção exige conferência registrada sobre os conteúdos atuais; o limiar textual não decide sozinho originalidade ou aprovação

### Requirement: Preservação e edição editorial incremental

A expansão SHALL preservar fontes históricas, bases válidas, identidades, revisões e decisões existentes. Novos itens MUST integrar uma edição separada e rastreável; o inventário deve distinguir evidência versionada, estado observado no destino e déficit real, sem inferir banco atual a partir de arquivos.

#### Scenario: Banco difere do histórico versionado
- **WHEN** o destino contém menos ou mais bases elegíveis que as 251 de simulado e 255 de prática registradas como referência
- **THEN** o relatório identifica destino/data e recalcula os déficits por assunto/modalidade sem inventar importações ou apagar bases extras válidas

#### Scenario: Base existente continua válida
- **WHEN** novas questões são acrescentadas para completar um assunto
- **THEN** as bases válidas anteriores e suas capturas permanecem preservadas, sem recriação sob novas origens para aumentar a contagem

### Requirement: Preparação e importação deliberadas

A preparação SHALL produzir pacotes separados por assunto/modalidade, com até 100 itens, seleção e fingerprints/digests verificáveis. A API existente MUST importar como DRAFT, preservar idempotência e recusar bases desatualizadas; habilitação continua uma decisão editorial separada.

#### Scenario: Pacote de um assunto é aplicado
- **WHEN** o operador confirma seleção, digest e destino pela importação administrativa existente
- **THEN** o recibo registra o resultado real e itens novos são rascunhos, sem mudar elegibilidade por efeito do upload

#### Scenario: Reaplicação ou conflito
- **WHEN** o pacote é reaplicado ou sua base esperada diverge do destino
- **THEN** a operação mantém os efeitos idempotentes existentes ou recusa o conflito, sem duplicação, reativação editorial ou efeitos parciais

### Requirement: Evidências corretivas permanecem fora da prática aberta

A expansão MUST preservar a proteção contra correção antecipada: gabaritos definitivos e evidências corretivas usados na curadoria não devem integrar projeções de questões abertas ou links corretivos oferecidos antes do encerramento. Aulas e referências de estudo permitidas continuam acessíveis.

#### Scenario: Original possui gabarito em documento separado
- **WHEN** a questão é apresentada em uma realização ainda aberta
- **THEN** o cliente recebe o conteúdo permitido, sem resposta, resolução ou URL corretiva extraída do dossiê editorial

#### Scenario: Conteúdo editorial está versionado
- **WHEN** fontes autorizadas são publicadas no repositório
- **THEN** isso não habilita sua distribuição por assets/builds públicos ou pelas projeções antecipadas, nem representa promessa de sigilo do repositório público

### Requirement: Entrega local verificável

A conclusão SHALL demonstrar cobertura elegível e cota de originais em todas as combinações no destino local identificado, preservando dados existentes e registrando seleção, fingerprints/digests e resultados reais. Essa habilitação MUST ser distinta de aceite, merge, publicação e carga na VPS.

#### Scenario: Entrega pronta para o teste do estudante
- **WHEN** o lote é disponibilizado localmente para validação do usuário
- **THEN** o relatório mostra contagens, origens e pendências reais por assunto/modalidade, sem apresentar rascunhos ou fixtures como acervo habilitado
