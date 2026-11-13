# Spec Delta

## Purpose

Permitir que operadores explicitamente autorizados importem questões novas como rascunhos verificáveis pela API, com prévia sem persistência, confirmação exata e preservação do acervo. Esta capacidade atende à [issue #160](https://github.com/LordBiGoDoNE/concurso-simulator/issues/160).

A extensão de acesso dos grupos 4–6 e de `administrative-access` está implementada/verificada localmente. O mecanismo R1 por UUID foi substituído; ver verification-r2.md para novas evidências, separadas do relatório histórico. Revisão independente e integração permanecem pendentes.

## ADDED Requirements

### Requirement: Permissão administrativa explícita
A API SHALL exigir sessão autenticada por senha ADM ou Google vinculado e permissão administrativa persistida, conferida em cada requisição, inclusive retries. Conta desabilitada, troca inicial pendente ou sessão administrativa invalidada MUST impedir importação. E-mail, nome, campo do pacote ou lista externa legada MUST NOT conceder a permissão.

#### Scenario: Matriz de acesso
- **WHEN** visitante, principal inválido, sessão expirada, usuário Google comum e ADM autenticado por um dos dois meios acessam a importação
- **THEN** os três primeiros recebem 401, o usuário sem permissão recebe 403 e apenas o operador pode prosseguir, com CSRF válido; nenhuma rejeição acessa conteúdo do acervo ou produz importação.

#### Scenario: Revogação persistente
- **WHEN** a permissão é revogada ou a conta administrativa é desabilitada, mesmo com sessão ou confirmação anteriores
- **THEN** sua próxima requisição administrativa recebe 403 sem aplicar nem reproduzir resultado privado; a alteração não exige reinício nem edição de UUID no ambiente.

### Requirement: Fronteira HTTP administrativa própria
A API SHALL oferecer somente POST de prévia e aplicação sob `/api/v1/admin/question-imports`, com JSON tipado, respostas `no-store`, proteção CSRF e CORS restrito às origens explícitas. Rotas públicas de questões, identidade, logout e prontidão MUST preservar seus contratos.

#### Scenario: Segurança antes do processamento
- **WHEN** um operador envia POST sem CSRF, com CSRF inválido, de origem CORS desconhecida ou método não autorizado
- **THEN** a cadeia de segurança recusa o acesso conforme o contrato, sem parser de pacote, persistência ou efeitos editoriais; não há redirecionamento ao Google.

#### Scenario: Consulta pública preservada
- **WHEN** visitante consulta catálogo/lista/detalhe ou tenta escrever pela rota pública histórica
- **THEN** leituras continuam públicas sem gabarito e a escrita continua negada; o prefixo administrativo não é tratado como leitura pública stateless.

### Requirement: Pacote genérico autossuficiente
A entrada SHALL aceitar somente `administrative-question-package/v1`, com itens, arquivos incorporados em Base64, hashes e seleção explícita de chaves únicas. O pacote MUST conter enunciado, alternativas estáveis, resposta, resolução, classificação, referências e proveniência verificável; fontes necessárias MUST acompanhar seus bytes.

#### Scenario: Questão nova fora dos inventários históricos
- **WHEN** o operador envia questão válida no namespace `admin-question-v1`, ou no namespace de prática com chave prefixada por `admin.`, arquivos de fonte e recursos referenciados
- **THEN** a prévia aceita o pacote sem exigir inventários de 510/255 itens, manifesta os hashes e preserva o fingerprint binário vigente do conteúdo; a finalidade declarada é SIMULADO ou PRATICA_AULA, com namespace e chaves compatíveis.

#### Scenario: Formato ou origem fora do escopo
- **WHEN** o pacote usa formato legado, origem histórica protegida, finalidade incompatível, template, campo desconhecido ou origem duplicada
- **THEN** recebe 422 com código estável sem alteração; não sobrescreve identidades históricas ou importa formatos implicitamente.

### Requirement: Limites síncronos verificáveis
Cada requisição SHALL limitar o corpo JSON a 16 MiB, o pacote a 1–100 itens, a seleção a 1–100 chaves existentes e os arquivos a 100 entradas de até 1 MiB cada e 8 MiB decodificados ao todo. Profundidade JSON SHALL limitar-se a 32 níveis. Execução MUST ser síncrona, sem jobs ou aplicação em background.

#### Scenario: Excesso e stream sem tamanho declarado
- **WHEN** o corpo ultrapassa 16 MiB, inclusive sem Content-Length ou com codificação comprimida
- **THEN** o corpo excedente recebe 413 e codificação de conteúdo diferente de identity recebe 415, sem leitura ilimitada, persistência ou efeitos parciais.

#### Scenario: Limites internos
- **WHEN** itens, seleção, arquivos, bytes decodificados, profundidade ou coleções de conteúdo excedem o contrato do desenho
- **THEN** a operação recebe 422 antes da aplicação; entradas nos limites são verificadas com fixtures sintéticas em execução isolada, sem afirmar garantia prévia de throughput.

#### Scenario: Capacidade ocupada
- **WHEN** outra prévia ou aplicação já está sendo processada na mesma instância
- **THEN** a nova operação recebe 429 com Retry-After, sem fila de payloads; o limite por instância não substitui a proteção transacional entre instâncias.

### Requirement: Validação integral e recursos inertes
O pacote SHALL ter JSON estrito, hashes recalculados, proveniência completa e recursos PNG/JPEG/SVG inertes com descrição acessível. A validação MUST rejeitar caminhos não canônicos, referências ausentes, markup ativo, arquivos extras e Base64 inválido. Nenhuma URL ou caminho do cliente SHALL causar rede, leitura ou escrita arbitrária no servidor.

#### Scenario: Fonte adulterada ou conteúdo perigoso
- **WHEN** qualquer item do pacote contém hash/fingerprint divergente, fonte ausente, traversal, script, SVG ativo ou URL de recurso externo
- **THEN** todo o pacote é recusado com 422 e diagnóstico sanitizado, sem fonte parcial, conexão externa, escrita de arquivo ou alteração de acervo, mesmo se o item não foi selecionado.

#### Scenario: Conteúdo matemático e referência temática
- **WHEN** texto inerte contém fórmulas, comparações como `a < b > c` e referências institucionais corretamente identificadas
- **THEN** mantém seus bytes textuais e autoria explícita sem interpretação HTML, atribuição oficial fabricada ou aprovação pedagógica implícita.

### Requirement: Prévia sem persistência
A prévia SHALL informar destino lógico, seleção, namespace/chaves, fingerprints, digest do pacote e classificação NEW/IDENTICAL/CHANGED/STALE, com IDs e bases observadas quando existentes. Ela MUST NOT reservar identidades, gravar arquivos, recibos, evidências ou alterar acervo/decisões. Suspeitas de duplicação MUST ser informativas, sem fusão automática.

#### Scenario: Prévia confirmável
- **WHEN** pacote e seleção válidos não contêm base desatualizada
- **THEN** retorna 200 com relatório, avisos e confirmação verificável válida por 15 minutos, sem persistir a prévia; a confirmação não aprova conteúdo nem garante que a base permaneça atual.

#### Scenario: Prévia com conflito
- **WHEN** uma chave selecionada exige versão-base diferente da atual e seu conteúdo ainda não existe
- **THEN** retorna 200 com STALE e `confirmable=false`, sem confirmação aplicável; o operador deve revisar o pacote e gerar outra prévia.

### Requirement: Confirmação exata de aplicação
A aplicação SHALL exigir confirmação emitida pela prévia, vinculada a ator, destino, pacote completo, seleção, fingerprints e bases esperadas, com validade de 15 minutos. Alteração relevante MUST invalidá-la; igualdade do JSON por reordenação de campos ou espaços MUST preservar o digest semântico definido no desenho.

#### Scenario: Confirmação divergente
- **WHEN** a aplicação troca bytes de fonte, conteúdo, seleção, base esperada, ator ou destino, ou usa confirmação adulterada/expirada
- **THEN** recebe 409 com código estável, sem escrita; confirmação não substitui sessão, permissão ou CSRF.

#### Scenario: Campo semântico omitido no digest
- **WHEN** um teste altera isoladamente cada campo aceito do pacote, inclusive arquivos de itens não selecionados
- **THEN** alterações semânticas mudam o digest e impedem aplicar a confirmação anterior; somente ordenações declaradas como conjuntos e variações lexicais do JSON são equivalentes.

### Requirement: Rascunhos atômicos e preservação editorial
A aplicação SHALL revalidar bases sob concorrência e gravar toda a seleção, evidência privada e recibo em um único commit. Conteúdo novo MUST entrar em DRAFT sem submissão/aprovação/retirada. Qualquer falha MUST reverter o conjunto, preservando questões ausentes, revisões, decisões e elegibilidade existentes.

#### Scenario: Correção de questão importada
- **WHEN** o operador altera conteúdo de uma chave administrativa conhecida com a última revisão-base correta
- **THEN** acrescenta rascunho com nova revisão, preserva a versão elegível anterior e todo o histórico; sem base correta recebe 409 e nenhuma questão da seleção é aplicada.

#### Scenario: Falha durante commit
- **WHEN** falha a persistência de qualquer item, evidência ou recibo
- **THEN** nenhum deles fica parcialmente confirmado; erro técnico não vira sucesso, acervo vazio, identidade duplicada ou retry ilimitado.

### Requirement: Reaplicação idempotente e recibo
Reaplicação de conteúdo idêntico SHALL reutilizar IDs/revisões sem alterar decisões ou reativar retirada. Retry da mesma confirmação já concluída SHALL retornar seu recibo original enquanto válida. Concorrência idêntica MUST preservar unicidade; concorrência com mudança real MUST produzir conflito ou rollback completo.

#### Scenario: Resposta perdida e retry
- **WHEN** o commit termina, a resposta se perde e o operador repete pacote, seleção e confirmação válidos
- **THEN** recebe os IDs e instante do commit original com indicação de replay, sem novo commit de conteúdo ou efeitos editoriais; após expiração pode gerar nova prévia e reaplicar idempotentemente.

#### Scenario: Reaplicação após retirada e versão posterior
- **WHEN** conteúdo idêntico corresponde a revisão histórica, inclusive retirada ou com sucessora
- **THEN** reutiliza essa revisão e informa a reutilização sem mudar última revisão, elegibilidade ou sequência editorial; não a converte novamente em DRAFT.

### Requirement: Evidência privada e rastreabilidade real
O resultado SHALL identificar ator interno, instante do servidor, seleção, digests, destino lógico, IDs/revisões e inclusão/reutilização reais. Fontes/recursos selecionados SHALL ser preservados como evidência privada verificável. Logs HTTP MUST excluir payload, gabarito, resolução, bytes, sessão, confirmação, credenciais e DSN.

#### Scenario: Sucesso auditável
- **WHEN** uma seleção é aplicada
- **THEN** recibo, manifesto dos arquivos selecionados e evidência ficam coerentes com o mesmo commit; a resposta retorna metadados sem conteúdo corretivo ou bytes e usa no-store.

#### Scenario: Erro sanitizado
- **WHEN** parser, arquivo ou banco rejeita a requisição
- **THEN** responde com código fixo e identificador de correlação, sem stacktrace, eco de chave inválida, fragmentos privados ou falso sucesso; falhas pré-commit não têm recibo de sucesso.

### Requirement: Importação sem publicação
Upload, teste, startup e migração MUST NOT submeter ou aprovar questões. Esta API SHALL manter apenas prévia e importação de rascunhos; decisões editoriais, distribuição pública de recursos, templates e interface administrativa permanecem fora do escopo. Evidências incorporadas MUST NOT tornar-se arquivos estáticos públicos.

#### Scenario: Dados importados não aparecem ao visitante
- **WHEN** a importação termina e um visitante consulta as rotas públicas
- **THEN** os novos rascunhos e seus arquivos privados não aparecem, inclusive por ID; recursos não ganham rota pública e o material estático permanece intacto.

#### Scenario: Operação editorial indevida
- **WHEN** o cliente envia campos de aprovação/ator autodeclarado ou tenta submeter/aprovar/retirar/template pela API administrativa
- **THEN** a entrada ou rota é recusada, sem transição editorial; conta Google autorizada a importar não recebe permissão implícita para essas ações.
