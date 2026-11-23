# Design

## Contexto

O ADR 0001 define `web → application → domain` e `infrastructure → portas`, e
`http-endpoint-review.md` atribui um proprietário a cada decisão HTTP. A segurança
atual cumpre o comportamento (os testes HTTP passam), mas a declaração das rotas está
espalhada e várias classes de infraestrutura importam `web`. O ArchUnit protege casos
conhecidos classe a classe. Esta spec consolida sem mudar rotas, regras de acesso,
cache ou corpos de sucesso; as únicas mudanças observáveis são as da tabela da
proposta.

Estado de partida relevante (main `9dea050`):

- Cadeias de segurança, por ordem: `@Order(-1)` conta administrativa
  (`AdministrativeHttpSecurity`); `@Order(0)` importação e curadoria
  (`ImportSecurityConfiguration`, `AdministrativeCurationSecurityConfiguration`);
  `1` status; `2` métodos OAuth rejeitados; `3` questões públicas (stateless);
  `4` cadeia padrão com sessão, que termina em `anyRequest().denyAll()`.
- Correspondência por `request.getServletPath()` com `equals`/`startsWith`/regex.
- `SessionIndependentRoutesFilter` desvia o Spring Session para status, OAuth
  rejeitado e questões públicas.
- Spring Session JDBC usa a serialização Java padrão; o `SecurityContext` gravado
  contém `identity.web.UserPrincipal`.

## Objetivos

- Uma rota nova é declarada em um único lugar, na funcionalidade dona.
- Teste falha se um mapeamento HTTP não tiver política, ou se uma política apontar
  para rota inexistente.
- O contrato de erro tem forma, códigos e status previsíveis, sem depender de
  exceções genéricas da JDK.
- As dependências `infrastructure → web` desaparecem e ficam proibidas por regra
  genérica.

## Não objetivos

- Mudar rotas, métodos, autorização, CSRF, CORS efetivo, cache ou sessão.
- Unificar as cadeias administrativas na cadeia padrão: elas têm filtros próprios de
  corpo, limite de taxa e login local, e continuam como cadeias separadas.
- Generalizar `REQUIRES_NEW` ou mudar transações.

## Decisões

### D1. Política de rota declarada pela funcionalidade

Contrato técnico em `shared/infrastructure/http` (mecanismo realmente compartilhado,
sem regra de negócio):

```java
public record ApiRoute(HttpMethod method, String pattern, Access access, Cors cors,
                       Session session, boolean passwordChangeExempt) {}
public enum Access { PUBLIC, VISITOR_OR_ACCOUNT, ACCOUNT, ACCOUNT_PRINCIPAL, OWN_CHAIN }
public enum Cors { NONE, PUBLIC_READ, CREDENTIALED }
public enum Session { STATELESS, STATEFUL }
public interface ApiRoutePolicy { List<ApiRoute> routes(); }
```

- Cada funcionalidade expõe um bean `ApiRoutePolicy` na sua `infrastructure`:
  `platform` (status), `identity` (sessão, CSRF, config, logout, OAuth, conta
  administrativa), `questionbank` (públicas, importação, curadoria), `mockexam`
  (tentativas, práticas, históricos), `subjectreview` (revisão por assunto).
  `AttemptRoutes`, `PublicQuestionRoutes`, `SubjectReviewRoutes` e o `Set` de
  isenções de `ActiveAccountFilter` são substituídos pelas declarações.
- `platform` compõe: matchers das cadeias 1–4, `CorsConfigurationSource`, decisão
  401/403 com ou sem corpo em `ApiAccessFailureHandler`, desvio de sessão em
  `SessionIndependentRoutesFilter` e isenções em `ActiveAccountFilter`, todos lendo
  o mesmo registro (`ApiRoutes`, que agrega os beans).
- `OWN_CHAIN` marca rotas atendidas pelas cadeias administrativas: elas usam o
  registro para `securityMatcher` e CORS, mas mantêm a autorização e os filtros
  próprios.
- O registro falha na inicialização se duas políticas declararem o mesmo
  método+padrão.
- Correspondência: `PathPatternParser` sobre o mesmo `servletPath` usado hoje.
  Padrões com variável (`/api/v1/attempts/{id}/result`) substituem as regex
  `[^/]+`. Barra final, caminho codificado e segmento vazio entram na matriz de
  caracterização (grupo 1) para provar equivalência.
- A dependência entre módulos passa a ser só para `shared`, o que elimina o motivo
  de `PublicQuestionRoutes`/`SubjectReviewRoutes` estarem em `shared` e não cria
  ciclo (`functionalModulesHaveNoCycles`).

Alternativas rejeitadas: anotação nos controllers (`@PublicRoute`), porque leva
decisão de segurança para o adaptador web, contra `http-endpoint-review.md`; uma
única classe central de rotas, que é o problema atual com outro nome.

### D2. Cobertura mapeamento ↔ política

Teste de contexto Spring que lê `RequestMappingHandlerMapping`:

- todo par método+padrão de controller sob `/api/` tem exatamente uma `ApiRoute`
  correspondente;
- toda `ApiRoute` corresponde a um mapeamento ou está numa lista explícita de rotas
  do framework (`/api/v1/auth/logout`, `/oauth2/authorization/google`,
  `/login/oauth2/code/google`);
- rota não declarada sob `/api/` responde 403 (cadeia padrão `denyAll`).

Limite: o teste prova presença e unicidade da política, não que o nível de acesso
escolhido está certo. Isso continua na revisão e nos testes HTTP de cada rota.

### D3. Matriz de caracterização antes da refatoração

Antes de mover qualquer rota, um teste HTTP parametrizado registra, para cada rota
existente e para variantes inválidas (método errado, barra final, rota vizinha
inexistente), com visitante, conta, conta com troca de senha pendente e sem CSRF:
status, `Cache-Control`, `Content-Type`, corpo de erro, cabeçalhos CORS para origem
permitida e não permitida, e se o cookie `SESSION` foi criado. Esse teste entra
verde na `main` atual e só pode mudar no grupo 4, nos códigos de erro da tabela da
proposta.

### D4. Exceções nomeadas e mapeamento explícito

- `mockexam.application`: `ParameterizedGenerationUnavailable`, lançada por
  `AttemptUseCases.create` no lugar da `IllegalStateException`. Chamada indevida
  entre componentes (ex.: `create` numa instância configurada para prática) é erro
  de programação e continua 500, não 400.
- `Attempt.Unavailable`, `Attempt.Conflict`, `Composition.InsufficientPool`,
  `AdministrativeQuestionQueries.InvalidQuery`, `EditorialBatchUseCase.*` e
  `ApplyAdministrativeQuestionsUseCase.ConfirmationFailure` já são nomeadas e
  continuam.
- Entrada HTTP: hoje `QuestionController` e `ReviewController` lançam
  `IllegalArgumentException` para UUID, número ou parâmetro inválido, e convertem
  ids com `Topic.fromId`/`Discipline.fromId`. Passam a usar um auxiliar web
  (`RequestValues`) que lança `InvalidRequest` (web) e traduz a
  `IllegalArgumentException` da conversão do VO no mesmo ponto. Só essa
  exceção, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`
  e `MissingServletRequestParameterException` viram 400.
- `IllegalArgumentException`/`IllegalStateException` vindas de dentro da aplicação,
  domínio ou persistência (invariante violada, dado inconsistente, "identidade
  duplicada no pool") deixam de ter tratador próprio e caem no tratador final
  `Exception → 500 INTERNAL_ERROR`, com log e correlação, sem eco da mensagem.
- 503: só `TransientDataAccessException`, `DataAccessResourceFailureException`,
  `CannotCreateTransactionException`, `TransactionTimedOutException` (o conjunto que
  os advices administrativos já usam), `UnsafeQuestionProjectionException` e a nova
  `ParameterizedGenerationUnavailable`. `DataIntegrityViolationException` não
  reconhecida e `InvalidDataAccessApiUsageException` viram 500.

Status para geração desabilitada: 503 e não 409, porque o estado é operacional
(configuração temporária do servidor) e não conflito com o recurso; repetir com
`useParameterizedQuestions=false` funciona. O comportamento "sem efeito" atual (nada
gravado, mesma contagem de `me_attempt`) é preservado e testado.

### D5. Formato de erro único

- `shared/web/ApiErrors`: monta `ResponseEntity` com status, `Cache-Control:
  no-store` e corpo tipado. DTOs: `ApiError(String error)` e, onde já existe
  correlação, os records administrativos atuais com `correlationId`. Os records
  `AttemptContracts.Error`, `QuestionResponses.Error` e `ReviewContracts.Error`
  passam a ser `ApiError`.
- Códigos `UPPER_SNAKE_CASE` em todas as respostas de erro, inclusive
  `ApiAccessFailureHandler` (`UNAUTHENTICATED`, `FORBIDDEN`), igual ao que
  `AdministrativeHttpSecurity` e os advices administrativos já usam. Escolhido por
  ser a convenção já majoritária (cerca de 25 códigos administrativos contra 8 de
  estudo) e a que não exige mudar o cliente de carga administrativa.
- Teste de contrato: toda resposta de erro produzida pelos testes HTTP passa por um
  verificador comum (`error` presente, casa `^[A-Z][A-Z0-9_]*$`, `no-store`, sem
  mensagem de exceção).
- Os nginx (`infra/local-app`, `infra/vps-production`) passam a responder
  `{"error":"UNAVAILABLE"}`; `check_local_images.py` acompanha.

### D6. Fronteiras web ↔ infraestrutura

| Tipo | Hoje | Depois | Motivo |
| --- | --- | --- | --- |
| `UserPrincipal` | `identity.web` | `identity.application` | Java puro (`java.security.Principal`, `Serializable`); web e infraestrutura dependem dele; `application` já é permitido para ambos. |
| `AdministrativeSession` | `identity.web` | `identity.application.port` | Assinatura só com `Access`; porta implementada pela infraestrutura. |
| `GoogleLoginHandlers` + `AdministrativeGoogleCompletion` | `identity.web` | `identity.infrastructure` | São handlers de sucesso/falha do Spring Security (mecanismo de autenticação), usados só pela configuração de segurança. |
| Resolvedor de `@CurrentActor` | `mockexam.infrastructure.AttemptActorConfiguration` | `mockexam.web` | Resolução de argumento é tradução HTTP; a chave do atributo continua `AttemptOwner.class.getName()`, sem importar a infraestrutura. |
| `@CurrentActor` | `mockexam.web` | `mockexam.web` (sem mudança) | `subjectreview.web` continua usando, dependência web→web entre módulos que já dependem no núcleo. |

`GoogleLoginController` (rota `/api/v1/auth/config`) continua em web. Se a
mudança de `GoogleLoginHandlers` exigir tipos web, a extração é feita sem trazer
regra de negócio para a infraestrutura.

### D7. Sessões persistidas com o `UserPrincipal` antigo

Mover `UserPrincipal` muda o nome da classe serializada no `SecurityContext`. Com o
desserializador padrão, ler uma sessão antiga lança exceção de desserialização, e a
requisição poderia virar 500.

Decisão: registrar `springSessionConversionService` com um desserializador que, se
a classe não for encontrada, trata o atributo como ausente e registra log sem
conteúdo da sessão. Efeito: a sessão antiga fica sem `SecurityContext`, `/me`
responde 401 e o estudo segue como visitante até a nova entrada. Nunca restaura a
identidade antiga por outro caminho nem cria conta.

Testes: bytes serializados de um `SecurityContext` com a classe antiga gravados em
`spring_session_attributes` em PostgreSQL real; `/me` → 401, `/api/v1/attempts`
como visitante → 201, nenhuma linha de conta criada, sem 500.

Alternativas rejeitadas: manter o nome antigo da classe (preserva a violação);
apagar as sessões numa migração Flyway (apaga dados sem autorização e mistura dado
técnico com schema); desserializador que mapeia o nome antigo para o novo (mantém
compatibilidade implícita sem prazo para sair).

### D8. ArchUnit genérico

- `..infrastructure..` não depende de `..web..`.
- Classes com `@RestController`/`@RestControllerAdvice` residem em `..web..`.
- Todo `@RestController`: não depende de `org.springframework.security..` (exceto
  `@AuthenticationPrincipal`), `org.springframework.session..`,
  `jakarta.servlet.http..`; todo método mapeado retorna `ResponseEntity<T>` com `T`
  record do próprio pacote web ou de `shared.web`.
- Os testes classe a classe atuais viram casos particulares e são removidos quando
  cobertos; as regras próprias de `SessionController` (CSRF) ficam.
- Exceções a uma regra genérica, se surgirem, são listadas no teste com motivo; não
  se usa exclusão por pacote inteiro.

### D9. DTOs web próprios na administração de questões

- `AdministrativeImportController`, `AdministrativeQuestionController` e
  `EditorialDecisionBatchController` passam a declarar requisições e respostas em
  records web, com enums web ou `String` onde hoje há enum do núcleo, e
  mapeadores explícitos web ↔ aplicação.
- O pacote de importação (`QuestionImportReader.Document`) ganha espelho web; a
  validação do pacote continua na aplicação, que recebe o documento convertido.
- Testes de contrato comparam o JSON de requisição e resposta antes/depois (mesmos
  nomes, ordem de campos irrelevante, mesmos valores de enum).
- Regra ArchUnit específica: records públicos de `questionbank.web` não têm
  componentes de `..domain..` ou `..application..`.

## Riscos e mitigação

- **Mudança silenciosa de acesso ao trocar regex por `PathPattern`.** Matriz de
  caracterização (D3) antes da troca; revisão por grupo com foco em segurança.
- **400 que vira 500 por parse esquecido.** Os testes HTTP de entrada inválida já
  existentes continuam exigindo 400; a revisão do grupo 4 lista cada ponto de
  conversão de entrada.
- **Sessões encerradas na atualização.** Documentado na proposta; afeta só quem
  estava conectado, sem perda de dados de estudo vinculados à conta.
- **Conflito com o formatador da spec de qualidade.** Não implementar em paralelo.
- **Tamanho dos grupos.** Grupos 2 e 4 são os maiores (~500 linhas autorais); se
  passarem disso, separar por funcionalidade mantendo o encadeamento.

## Plano de grupos

Cadeia linear: 1 (caracterização) → 2 (políticas de rota) → 3 (fronteiras e sessão)
→ 4 (erros) → 5 (DTOs administrativos e documentação final). O 2 usa a matriz do 1
como prova; o 3 altera `SecurityConfiguration`, reescrito no 2; o 4 altera
`ApiAccessFailureHandler` e os advices/controllers tocados no 2 e no 3; o 5 altera os
controllers administrativos que importam `UserPrincipal` (3) e seus advices (4).
