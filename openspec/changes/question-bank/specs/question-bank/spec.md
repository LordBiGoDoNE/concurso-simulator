# Spec Delta

## Purpose

Manter um acervo de questões rastreáveis, versionadas e editorialmente aprovadas, consultável sem login e preparado para simulados futuros, sem confundir importação ou validação estrutural com revisão pedagógica.

## ADDED Requirements

### Requirement: Catálogo limitado ao escopo inicial
O banco SHALL organizar questões por disciplina, assunto e cargo, mantendo apenas Limeira 01/2026 e Assistente Administrativo/Secretário de Escola no escopo inicial. Assuntos MUST pertencer a uma disciplina; associações compartilhadas MUST NOT duplicar a questão.

#### Scenario: Questão comum aos dois cargos
- **WHEN** uma questão de Português é aplicável aos dois cargos
- **THEN** ambos os filtros a encontram com a mesma identidade e versão, sem duplicar sua contagem na disciplina.

#### Scenario: Associação inconsistente
- **WHEN** o pacote associa assunto a outra disciplina ou inclui cargo fora do escopo
- **THEN** a validação aponta o item e impede a aplicação do pacote sem alterar o catálogo existente.

### Requirement: Meta editorial por disciplina
O acervo SHALL perseguir 50–100 questões aprovadas por disciplina, distribuídas entre assuntos, contando identidades distintas e apenas versões elegíveis. A meta MUST NOT impor limite de armazenamento, excluir material existente ou aprovar itens automaticamente para completar quantidade.

#### Scenario: Inventário acima da meta
- **WHEN** a importação encontra 120 questões de Matemática/Raciocínio
- **THEN** todas permanecem rastreáveis como candidatas ou itens com pendência; nenhuma é apagada por exceder 100, e a seleção aprovada depende da revisão.

#### Scenario: Disciplina sem quantidade suficiente
- **WHEN** o relatório encontra menos de 50 questões elegíveis em uma disciplina
- **THEN** explicita a lacuna e sua distribuição por assuntos, sem contar rascunhos, versões antigas ou gerar preenchimento; a entrega não declara a meta atendida sem curadoria ou mudança de escopo aprovada.

### Requirement: Versões e alternativas estáveis
Cada questão SHALL ter identidade estável e versões identificáveis com conteúdo imutável: enunciado, alternativas identificadas, única resposta correta, resolução, classificação, fontes e origem. Alteração desse conteúdo MUST criar nova versão sem reescrever anteriores nem herdar aprovação.

#### Scenario: Revisão de uma questão aprovada
- **WHEN** o conteúdo da questão é corrigido
- **THEN** surge uma nova versão em rascunho, a anterior permanece íntegra e elegível até substituição aprovada ou retirada explícita; a nova aprovação troca a versão elegível atomicamente.

#### Scenario: Alternativa correta inválida
- **WHEN** a versão contém IDs de alternativa repetidos ou a resposta não identifica exatamente uma alternativa existente
- **THEN** a versão é rejeitada antes de persistir ou aprovar, com diagnóstico do item e sem mudanças parciais.

### Requirement: Aprovação editorial da versão exata
O banco SHALL distinguir rascunho, em revisão e aprovado. Aprovação MUST exigir registro explícito da versão/conteúdo revisados, responsável editorial, data e checklist de correção, clareza, resolução didática, fontes e classificação. Testes/importação MUST NOT fabricar essa aprovação.

#### Scenario: Importação de pacote válido
- **WHEN** um pacote passa em todos os testes estruturais e é aplicado
- **THEN** questões novas entram em rascunho e não aparecem nas consultas do acervo aprovado.

#### Scenario: Registro editorial desatualizado
- **WHEN** a aprovação referencia outra versão ou conteúdo diferente do revisado
- **THEN** a operação falha sem alterar a versão elegível nem aceitar a evidência antiga.

#### Scenario: Questão aprovada com erro descoberto
- **WHEN** o responsável registra a retirada editorial de uma versão elegível e o motivo
- **THEN** ela deixa de aparecer em listas/detalhes atuais, mas conteúdo e registro histórico não são apagados nem alterados.

### Requirement: Proveniência e referências honestas
Cada versão SHALL manter chave de origem, localização no material, impressão do conteúdo e referências com nome/URL. O banco MUST distinguir autoria de referência temática e registrar fundamento/revisão de norma quando aplicável, sem atribuir autoria ou validação oficial inexistentes.

#### Scenario: Questão autoral com referência institucional
- **WHEN** o importador encontra uma questão autoral com link para Inep ou legislação
- **THEN** preserva a origem autoral e apresenta o link como referência de estudo, sem afirmar que o órgão elaborou ou validou a questão.

#### Scenario: Referência insuficiente ou norma não conferida
- **WHEN** a revisão identifica associação temática não comprovada, URL sem identificação ou fundamento legal sem conferência registrada
- **THEN** registra a pendência e não aprova a versão até esclarecê-la; validação sintática de URL não substitui esse julgamento.

### Requirement: Importação controlada e idempotente
A importação SHALL aceitar somente pacote local versionado, oferecer prévia sem escrita e relatório de inclusão, reutilização, novas versões, erros e possíveis duplicações. Aplicação inválida MUST ser atômica; repetição idêntica MUST preservar IDs/versões/estados e não reativar versão retirada.

#### Scenario: Reaplicação do mesmo pacote
- **WHEN** o operador reaplica conteúdo idêntico já importado, inclusive de modo concorrente
- **THEN** não surgem questões/versões duplicadas, aprovações implícitas ou perda de dados; conflitos reais são apresentados sem transformar falhas desconhecidas em sucesso.

#### Scenario: Conteúdo alterado ou duplicação suspeita
- **WHEN** uma chave conhecida tem conteúdo diferente ou outra chave tem conteúdo aparentemente equivalente
- **THEN** a prévia indica nova versão ou suspeita a revisar; exige confirmação da versão-base para aplicar alteração e não funde nem descarta questões por similaridade automática.

#### Scenario: Extração incompleta ou pacote inválido
- **WHEN** falta enunciado, alternativa, gabarito, resolução, origem, referência ou associação válida em um item selecionado
- **THEN** o relatório identifica a chave e o problema, a aplicação daquele pacote não grava itens parciais e a origem estática permanece intocada.

### Requirement: Consulta pública somente de questões elegíveis
A consulta SHALL oferecer catálogo e questões aprovadas/elegíveis sem login, filtrando por disciplina, assunto e cargo, com paginação limitada e ordenação determinística. Rascunhos, revisões pendentes, versões substituídas ou retiradas MUST NOT aparecer, mesmo por consulta direta de ID.

#### Scenario: Visitante consulta com filtros
- **WHEN** um visitante seleciona disciplina, assunto e cargo válidos
- **THEN** a lista contém apenas resumos elegíveis e IDs da questão/versão; ao abrir um detalhe, recebe alternativas identificadas em ordem editorial, sem iniciar login ou executar sorteio de simulado.

#### Scenario: Consulta inválida ou ausente
- **WHEN** a paginação/filtro é inválida ou o detalhe solicitado não é elegível
- **THEN** a API retorna respectivamente 400 ou 404 JSON; filtro válido sem resultados retorna 200 com lista vazia, sem sugerir aprovação de itens pendentes.

### Requirement: Gabaritos fora dos contratos de questões
Contratos públicos de catálogo/lista/detalhe SHALL excluir resposta, resolução, campos ou marcações que indiquem a alternativa correta e URLs de gabarito. Rotas editoriais/de gabarito desta nova API MUST NOT existir; os gabaritos públicos do site legado permanecem preservados, sem promessa de sigilo global.

#### Scenario: Inspeção de resposta e interface
- **WHEN** o cliente inspeciona JSON, HTML, atributos acessíveis e recursos associados à questão
- **THEN** não encontra gabarito/resolução ou indicadores técnicos da alternativa correta nesses contratos; o banco guarda esses dados apenas para revisão e uso interno futuro.

#### Scenario: Tentativa de abrir escrita ou gabarito por HTTP
- **WHEN** visitante ou usuário com sessão Google tenta métodos de escrita, caminhos editoriais ou de resposta não autorizados
- **THEN** a segurança nega o acesso sem modificar acervo, aprovação ou sessão; login Google não concede permissão editorial.

### Requirement: Conteúdo seguro e acessível
Questões SHALL preservar legibilidade, fórmulas com representação acessível e recursos locais rastreáveis. Conteúdo importado MUST NOT executar scripts, atributos de evento ou URLs perigosas; conversão insegura ou com perda relevante MUST produzir pendência, não aprovação silenciosa.

#### Scenario: Fórmula e navegação por teclado
- **WHEN** uma questão com fórmula é consultada com teclado ou leitor de tela
- **THEN** texto equivalente, alternativas e referências são compreensíveis sem depender só de imagem/cor; foco e identificação das opções permanecem claros.

#### Scenario: Conteúdo ou caminho malicioso
- **WHEN** o pacote contém script, evento HTML, URL executável ou recurso fora das raízes permitidas
- **THEN** a validação bloqueia sua aplicação, sem executar código, acessar caminhos externos ou buscar URLs automaticamente; erros permanecem no relatório.

### Requirement: Material e identidade preservados
O banco SHALL manter aulas, URLs, gabaritos separados e leitura offline existentes sem exigir a API ou login. Erros de consulta MUST permitir tentar novamente ou acessar aulas, sem alegar histórico/sincronização e sem alterar os contratos atuais de identidade, CSRF, logout, OIDC e prontidão.

#### Scenario: API indisponível durante o estudo
- **WHEN** catálogo/questões excedem prazo de consulta ou a API está indisponível
- **THEN** a interface informa o erro de forma acessível, permite recuperação e mantém o link das aulas; consultas antigas não sobrescrevem filtros atuais e não aguardam JSON indefinidamente.

#### Scenario: Regressão offline e banco existente
- **WHEN** a nova versão é verificada sobre os dados existentes e o material é aberto sem rede
- **THEN** identidades/sessões e leitura das aulas permanecem funcionais, incluindo URLs e nove módulos/90 resoluções enriquecidas; não ocorre publicação ou alteração dos arquivos-base durante importação.
