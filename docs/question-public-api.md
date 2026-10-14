# Consulta pública do banco de questões

Somente GET catalog/questions/questions/{UUID} sob `/api/v1/question-bank`.
Catálogo fixo de Limeira 01/2026; lista filtra disciplina/assunto/cargo por AND,
page=0/size=20 (1–100), ordem UUID da questão. Parâmetros desconhecidos/repetidos,
UUIDs/filtros incompatíveis/paginação inválidos: 400; ID ausente/rascunho/retirado: 404
indistinguível; vazio: 200. Banco indisponível/conteúdo incompatível: 503, nunca vazio
fictício. Contratos concretos/OpenAPI, respostas no-store, sem entidade JPA.

## Responsabilidades — checklist HTTP

- Segurança decide GET público/negações, CORS e ausência de sessão antes do controller.
- Controller traduz parâmetros/UUID, DTO/status/cache; não conhece identidade/cookie/CSRF.
- UseCase seleciona o acervo pela porta EligibleQuestions; não implementa SQL nem autentica.
- JPA/infra executa leitura repeatable-read (total/página coerentes), filtros vinculados
  e ponteiro aprovado. SQL nativo se justifica por projetar somente campos permitidos
  já no PostgreSQL, sem carregar resposta/resolução/proveniência no resultado.
- DTOs públicos listam campos permitidos. Guard rejeita markup ativo/caminhos de recursos
  corretivos/URLs de gabarito, sem retirar conteúdo silenciosamente. Imagens têm referência
  local/hash/descrição acessível; essa spec não cria endpoint de arquivos arbitrários.
- Revisão independente verifica semântica; CI verde não é aprovação nem autoriza merge.

API projeta somente classificação da **revisão elegível**, mesmo com rascunho mais recente.
Lista não inclui alternativas/fontes; detalhe inclui alternativas estáveis e fontes nomeadas,
não ID correto, resolução, estado editorial, checklist, autoria/caminho do gabarito ou ORM.
Fontes são apoio de estudo, não atribuição de autoria a uma banca.

Não há sigilo global: aulas/gabaritos estáticos legados continuam públicos, com mesmas URLs
e uso offline. Fronteira protege o contrato novo e não promete impedir alguém de estudar
com gabaritos antigos. Questões ainda precisam de revisão editorial real; fixtures não contam.

## Fronteira sem sessão

Matcher técnico compartilhado entre Spring Security e o único SessionRepositoryFilter:
todo namespace question-bank entra em cadeia stateless de **leitura ou negação**; apenas
os três caminhos GET permitem conteúdo. Caminhos adicionais/métodos de escrita/HEAD
são negados antes do controller, sem lookup/save/delete de sessão. Preflight CORS GET
nas rotas exatas aceita somente origens configuradas, sem allow-credentials; outros
métodos/origens são negados. Dispensa CSRF somente nessa cadeia sem escritas; logout
continua na cadeia normal protegida e OAuth/readiness conservam suas cadeias/ordem.
Não abrir `/api/**` nem implementar regras de autenticação nos controllers.

HTTP real + spy do repository verificam visitantes/cookies conhecidos/desconhecidos/
contexto autenticado ou principal inválido com os mesmos contratos, zero lookup/commit,
sem Set-Cookie, timestamps/acervo/histórico inalterados. Ordem e registro único dos filtros
são conferidos. Spy é evidência de chamada, não prova completa de inexistência de I/O
em todo futuro filtro; testes e revisão de responsabilidades se complementam.

## Proteção da projeção e controles negativos

Contratos recursivos/OpenAPI com `additionalProperties=false` verificam lista e detalhe.
Controle negativo de serialização injeta ID correto/resolução no JSON válido e confirma
que o mesmo verificador falha — ocultação por CSS não passa. É um mutante do resultado,
não uma afirmação de execução com o serializer de produção alterado. Fixtures aprovadas
deliberadamente incompatíveis (markup/data-correct, recurso com marcador corretivo, URL
percent-encoded de gabarito) geram 503 genérico/no-store, sem payload/cookies. Banco
derrubado gera 503 no prazo, não 200 vazio nem 404 fictício; catálogo fixo segue independente.

Não há heurística que comprove semanticamente todo texto/figura: guard cobre formas
estruturais/marcadores e restrições de caminhos, e recursos aceitos ainda precisam de
revisão editorial do conteúdo, tipo/hash/descrição na operação local. A API não publica
diretórios/arquivos arbitrários nem lê byte de arquivo remoto. Renderização do cliente
usa texto/descrições, nunca HTML bruto ou carregamento indiscriminado de assets legados.

Correção de revisão: detector técnico QuestionTextSafety também usado pelo leitor local
aceita `x < y e y > z` e índices literais sem tratar comparações como tags. Projeção
continua recusando scripts/tags/atributos ativos/declarativos; não é um sanitizer genérico
nem garantia semântica de todo texto. O controller/UseCase/segurança não mudam de papel.
HTTP real cobre positivos em resumo/detalhe e negativos existentes: no-store/503 genérico
mantidos para conteúdo efetivamente incompatível, sem relaxar routes/CSRF/sessão.

Positivos adicionais `a < b > c` e `x < i > y` conferem que tags neutras exigem nome
adjacente ao < (não espaço após <). Uma sequência literal que tenha exatamente sintaxe
de tag conhecida, como `<b>`, continua incompatível com esta representação; comparações
com variáveis usam texto inequívoco/espaçado. Nenhuma string passa a ser renderizada como HTML.
