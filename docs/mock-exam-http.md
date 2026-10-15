# HTTP de tentativas

Checklist de responsabilidades (ADR 0001 e http-endpoint-review):

- Spring Security autoriza métodos/rotas, valida principal e CSRF e aplica CORS explícito.
- `AttemptAccess` resolve conta verificada ou visitante técnico antes do controller;
  histórico exige conta. Principal inválido não degrada para visitante.
- A infraestrutura MVC entrega `@CurrentActor AttemptOwner` já verificado; o controller
  não autentica, lê sessão ou verifica CSRF. Domínio/UseCases continuam decidindo posse,
  expiração, versão e transições em todas as operações.
- Tentativas não integram o bypass stateless do banco público. Cookie HttpOnly/Lax e
  validade técnica da sessão continuam no Spring Session; visitante não cria app_user.

O UUID visitante é atributo técnico da sessão. Na primeira resolução, deriva de SHA-256
com namespace sobre o ID aleatório da sessão (128 bits), garantindo o mesmo valor em
requisições iniciais concorrentes/processos distintos. O ID da sessão não sai da infra;
nem seu derivado é aceito do JSON ou apresentado na resposta. Guardar o atributo mantém
o owner em rotações técnicas; login usa a conta e logout invalida a sessão, sem transferência.
Isso não é um token de autorização nem um mecanismo de autenticação personalizado.

`AttemptActorHttpTests` atravessa a cadeia real com PostgreSQL isolado: visitante,
conta, contexto inválido, expiração, CSRF, métodos negados e CORS. Um controller probe
exclusivo de teste confirma a entrega do ator via argumento MVC, sem endpoint de fixture
em produção. Isso não comprova posse ou contrato de recurso, cobertos na camada HTTP seguinte.

## Composição e respostas

POST /api/v1/attempts aceita commandId, roleId e lista de disciplinas com quantidade
inteira e topicIds opcionais. Responde 201 na criação, 200 no replay. GET /{id} e
PUT /{id}/answers/{item} retornam somente View tipada, incluindo seleção própria,
sem resposta correta, resolução, proveniência ou nota parcial. PUT aceita commandId,
expectedVersion e alternativeId nullable para limpar. IDs são UUIDs canônicos.

Jackson rejeita campos desconhecidos e quantidades fracionárias/nulas. Proprietário,
correção e nota enviados pelo cliente são inválidos; a segurança fornece o ator.
400 inválido, 401 contexto técnico inválido, 403 CSRF/CORS, 404 ausente/outro dono/
expirado indistinguíveis, 409 estado/versão/payload/insuficiência, 503 banco/conteúdo
incompatível. Insuficiência usa lista tipada de disciplina/disponíveis. Todas as
respostas de tentativas usam no-store. OpenAPI define objetos sem propriedades extras.

AttemptHttpTests atravessa HTTP/Security/JPA/PG18.6 real com fixtures sintéticas;
confere contratos, retries, limpar, associação por ID, owner, ausência de efeitos
inválidos e controle negativo que falha ao inserir um campo de gabarito no JSON.
O validador de teste cobre estrutura/tipos usados; não substitui um validador geral
OpenAPI nem demonstra ausência semântica de dicas em todo texto editorial.

## Conclusão, resultado e histórico

POST /{id}/finish exige commandId/expectedVersion e CSRF; devolve tentativa fechada e
resultado tipados. GET /{id}/result é 409 enquanto aberta e só retorna gabarito/resolução
após concluir. Respostas posteriores são conflito. Repetir o mesmo comando de conclusão
recupera o resultado fechado; percentuais/pontos são exclusivamente do domínio.

GET /api/v1/me/attempts exige conta pela segurança; page 0–100000, size 1–100, finished
opcional true/false, sem parâmetros repetidos/desconhecidos. Ordena createdAt/id DESC e
não anuncia total; a interface pode avançar enquanto a página estiver cheia. Sessões
diferentes da mesma conta retomam o mesmo snapshot; outra conta/visitante não o recebem.

Indisponibilidade de Spring Session pode ocorrer antes do MVC. Um filtro técnico,
restrito a rotas de tentativas e anterior ao filtro de sessão, traduz somente causas
DataAccessException/TransactionException em 503/no-store quando a resposta ainda não
foi confirmada. Não decide autenticação, posse ou CSRF; erros desconhecidos propagam.
A consulta stateless do banco e prontidão mantêm cadeias próprias. Teste interrompe PG
para conferir 503 de sessão/resultado e preservação do catálogo fixo, sem falso vazio.

Testes reais também verificam 3/5=60%, branco/erro, resultado precoce, conclusão repetida,
negação de outro dono/CSRF, irreversibilidade, páginas e retomada em outra sessão,
logout sem apagar tentativas e ArchUnit sobre dependências/contratos concretos dos
controllers. O checklist é evidência de responsabilidades; ArchUnit não prova toda
semântica e a indisponibilidade com resposta já confirmada não pode ser reescrita.

O 503 anterior ao MVC limpa somente o buffer/campo de comprimento e preserva headers
CORS já aplicados pelo framework, para que a origem autorizada possa ler o erro JSON.
A regressão interrompe PG e exige 503 com Allow-Origin explícito/credentials, além de
no-store. Não refaz autenticação/CORS manualmente nem traduz falhas desconhecidas.
