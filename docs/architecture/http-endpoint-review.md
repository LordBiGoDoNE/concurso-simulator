# Endpoints HTTP — responsabilidades e revisão

Complementa o ADR 0001. Aplicar antes da implementação e na revisão; registrar as evidências no PR. Vale para trabalho humano ou gerado por IA. Modelo mais capaz pode ajudar, mas não substitui regras explícitas, testes e revisão.

## Quem decide o quê

| Responsabilidade | Proprietário |
| --- | --- |
| Autenticar, reconhecer visitante, verificar principal/sessão | Spring Security e infraestrutura de autenticação |
| Autorizar tecnicamente a rota, validar CSRF e rejeitar acesso | Configuração/handlers de segurança; method security quando justificada |
| Gerar/guardar token CSRF, rotacionar/expirar sessão | Framework/infraestrutura, não regras de negócio |
| Converter HTTP, receber principal verificado, apresentar DTO/status/cache | Adaptadores web |
| Coordenar ação relevante e portas | UseCase da aplicação |
| Invariantes, transições e regras de posse de recursos | Domínio/aplicação; não deslocar regras de negócio para filtros |
| Persistir e executar transações | Adaptadores de infraestrutura |

401 indica falta de autenticação válida; 403 indica acesso negado/CSRF inválido conforme o contrato. Não converter todo 403 em 401 nem usar redirects de login nos contratos JSON da API. Visitantes continuam podendo estudar. Proteção de rota não substitui regras de posse: autenticar alguém não autoriza consultar a tentativa de outra pessoa.

## Caso concreto: `/me` e `/csrf`

- `/me` permite à SPA descobrir a identidade sem ler o cookie HttpOnly. A segurança exige autenticação não anônima e `UserPrincipal`; rejeita antes do controller com 401 JSON/no-store. O controller recebe `@AuthenticationPrincipal UserPrincipal` e devolve `MeResponse`.
- `/csrf` expõe `CsrfResponse` a partir do `CsrfToken` fornecido pelo Spring. O framework gera/valida o token. Disponibilizar CSRF a visitante não autentica ninguém; o POST de logout continua protegido.
- Não duplicar `if (authentication == null)`/`instanceof` com retorno 401 no controller de sessão. Uma inconsistência de principal deve ser barrada na segurança, não virar um 500 durante o DTO.
- Não criar `GetMeUseCase` ou `CsrfService` que apenas repassem o framework. Não proibir genericamente `Authentication` em todos os adaptadores: callbacks/handlers OIDC têm uma responsabilidade diferente.

## Contratos e tratamento de erros

Usar DTOs web tipados (records quando adequados) e retornos concretos, como `ResponseEntity<MeResponse>`. Não expor entidade JPA, credencial, token Google ou dados pessoais desnecessários. `Map`, `Object` e curingas não são o padrão para JSON estável; payload realmente dinâmico exige justificativa e testes de contrato.

Rejeições técnicas de segurança ficam nos mecanismos do Spring Security. Erros da aplicação podem ser traduzidos por adaptadores web/ControllerAdvice sem contaminar o núcleo com HTTP. Não capturar toda exceção para fingir visitante, conflito conhecido ou sucesso.

### Contrato de erro

Toda resposta de erro é um objeto JSON com `error` em `UPPER_SNAKE_CASE` e `Cache-Control: no-store`, sem mensagem de exceção, SQL, conteúdo de questão ou dado pessoal. Campos extras existentes (`correlationId`, `available`, `questionIds`, `reason`) continuam onde já estavam. Adaptadores web usam `shared/web/ApiErrors`/`ApiError`; filtros e handlers de segurança, `shared/infrastructure/http/JsonRejection`.

| Situação | Status e código |
| --- | --- |
| Entrada recusada no adaptador web (`InvalidRequest`, corpo ilegível, tipo de parâmetro) | 400 `INVALID_REQUEST` |
| Comando que referencia item/alternativa de outra tentativa (`Attempt.InvalidAnswer`) | 400 `INVALID_REQUEST` |
| Sem autenticação válida em rota que exige identidade | 401 `UNAUTHENTICATED` |
| Acesso negado ou CSRF inválido (áreas com falhas JSON) | 403 `FORBIDDEN` |
| Recurso inexistente ou de outro dono | 404 `NOT_FOUND` |
| Versão/estado divergente; acervo insuficiente | 409 `CONFLICT`; 409 `INSUFFICIENT_POOL` |
| Geração parametrizada desligada no servidor | 503 `PARAMETERIZED_GENERATION_UNAVAILABLE` |
| Banco, conexão ou transação temporariamente indisponíveis (`TransientFailures`); projeção pública insegura recusada | 503 `UNAVAILABLE` (administração: `ADMIN_UNAVAILABLE`/`IMPORT_UNAVAILABLE`) |
| Falha não reconhecida, inclusive integridade e `IllegalArgumentException`/`IllegalStateException` vindas do núcleo ou da persistência | 500 `INTERNAL_ERROR` (administração: `ADMIN_FAILURE`, `IMPORT_FAILED`, `ADMIN_CURATION_FAILED`), com log e correlação no servidor |

- `IllegalArgumentException` só vira 400 quando a recusa acontece na conversão da entrada (`RequestValues.parse`); dentro da aplicação ou da persistência ela indica dado inconsistente e é 500.
- 503 não é usado para violação de integridade: constraint não reconhecida reverte a transação e responde 500.
- Exceção nova da aplicação que deva virar 4xx precisa ser nomeada e mapeada explicitamente no advice da funcionalidade.

## Declaração de rotas

Cada funcionalidade declara suas áreas HTTP (`ApiArea`) na própria `infrastructure` (`IdentityRoutes`, `QuestionBankRoutes`, `MockExamRoutes`, `SubjectReviewRoutes`, `PlatformRoutes`): caminhos possuídos, cadeia (`STATELESS`, `SESSION` ou cadeia própria `OWN`), CORS, se as falhas são JSON e cada rota com método, padrão e acesso técnico (`PUBLIC`, `VISITOR_OR_ACCOUNT`, `ACCOUNT`, `ACCOUNT_PRINCIPAL`, `ADMINISTRATIVE`). O registro `ApiRoutes` (`shared/infrastructure/http`) é a única fonte lida pelas cadeias de segurança, CORS, `ApiAccessFailureHandler`, desvio de sessão, isenção de troca de senha e filtros de indisponibilidade; `platform` só compõe.

- Rota nova: declarar no mesmo PR, na funcionalidade dona; não editar `platform` nem outra funcionalidade.
- Rota não declarada continua negada pela cadeia padrão.
- A correspondência usa `PathPattern` sobre `servletPath`, sem barra final opcional; variáveis com regex são aceitas.
- `ApiRouteCoverageTests` falha se um mapeamento de controller não tiver exatamente uma rota declarada ou se uma rota declarada não tiver controller nem estiver na lista de rotas do framework. Prova presença e unicidade, não que o acesso escolhido esteja certo.
- `HttpRouteMatrixTests` compara status, cache, tipo, erro, sessão e CORS de cada rota e variante com `src/test/resources/http/route-matrix.tsv`. Mudança no arquivo é mudança de contrato e precisa de justificativa no PR.

## Checklist obrigatório para endpoints alterados

- [ ] Cada endpoint tem finalidade e público explícitos; a rota não existe apenas por conveniência de implementação.
- [ ] A rota está declarada uma única vez na área da funcionalidade dona, com acesso, CORS e isenções explícitos; `platform` não ganhou caminho literal.
- [ ] Cada decisão tem um proprietário (segurança, web, aplicação ou domínio); não há regra duplicada nem camadas apenas delegadoras.
- [ ] Autenticação/principal, acesso técnico e CSRF são tratados antes do controller quando aplicáveis; regras de negócio não foram deslocadas para filtros.
- [ ] Entrada/saída e principal são tipados; contratos, códigos de erro, cache e privacidade estão documentados. Exceções ao padrão têm justificativa.
- [ ] Testes passam pela cadeia HTTP/segurança real: visitante, contexto/principal inválido, autenticação válida, expiração e CSRF conforme o endpoint; confirmam ausência de efeitos indevidos e vazamentos.
- [ ] Limites arquiteturais foram revisados além do resultado HTTP; regressão estrutural foi adicionada quando viável, com alcance/limitações explícitos.
- [ ] PR inclui evidências e achados resolvidos; build/CI verde não foi usado como aprovação semântica nem autorização de merge.

## Proteções e seus limites

`ArchitectureTests` impede o controller de sessão de depender de autenticação bruta, contexto/sessão e mecanismos de geração/validação CSRF; verifica DTOs concretos em seus métodos HTTP. `SessionAccessTests` testa a cadeia real e preserva 401/403, cookies, cache, expiração e isolamento. CI já executa ambos.

Essas regras protegem o caso conhecido, não provam que todo controller do projeto está semanticamente correto. Uma repetição pode ser escrita de outras formas sem violar dependências. O checklist continua sendo revisão humana/assistida; ampliar testes para novos achados é preferível a uma regra global que proíba integrações legítimas.

O achado original passou porque os testes verificavam a resposta, não a localização da decisão; a orientação genérica de “controller fino” não explicitava esta fronteira. A correção inclui regra escrita, evidência funcional, proteção estrutural e revisão, sem promessa de eliminar todos os erros.
