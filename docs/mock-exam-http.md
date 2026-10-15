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
