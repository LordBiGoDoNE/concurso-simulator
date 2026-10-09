# ADR 0001 — Domínio, aplicação e persistência JPA

- Data: 2026-10-08
- Estado: decisão aprovada e registrada na épica pelo #24; implementação integrada somente conforme aprovação individual (#19/#20/#21 integrados, #22/#23 em revisão)
- Escopo: backend da reformulação, incluindo adequação dos PRs #19–#23

## Contexto

A revisão do #19 identificou `GoogleIdentityService` acoplado a SQL/JdbcTemplate, transações e constantes Google. A resolução de identidade deve ser independente do provedor e da tecnologia de persistência. O usuário aprovou monólito modular, domínio com comportamento, UseCases na aplicação e adoção imediata de JPA/Hibernate, antes de ampliar o modelo.

## Decisão e dependências

Organizar por funcionalidade, com separação interna: `domain`, `application` (incluindo portas), `infrastructure` e adaptadores de entrada `web`. `shared` contém apenas contratos/mecanismos realmente compartilhados, não um depósito de regras. Os adaptadores dependem do núcleo; o núcleo não importa adaptadores ou frameworks.

```text
web / autenticação ──→ application ──→ domain
infrastructure ──────→ portas da application/domain
```

- **Entidades:** possuem identidade e protegem suas transições/invariantes com métodos de intenção, não setters indiscriminados. Não inventar comportamento quando o modelo atual tem poucas regras.
- **Objetos de valor:** descrevem conceitos sem identidade própria e validam invariantes. Um VO pode ter comportamento; não é sinônimo de DTO. Subject externo é opaco: não fazer trim/lowercase nem vincular por e-mail.
- **UseCases:** são serviços de aplicação orientados a ações relevantes. Coordenam repositories, integrações e fronteiras transacionais; não executam SQL nem concentram regras pertencentes ao domínio. Não exigir uma classe por endpoint trivial.
- **Domain Services:** representam regras que não pertencem naturalmente a uma entidade/VO. Preferir entradas/saídas do domínio sem I/O. Não criar um apenas por existir uma camada domain.
- **Repositories/portas:** contratos com operações necessárias ao caso de uso, sem expor EntityManager, tipos JPA, Spring Data ou exceções SQL. Interfaces são justificadas em fronteiras, não obrigatórias para toda classe.
- **Infraestrutura:** JPA/Hibernate, execução transacional Spring, configuração, integrações e detalhes de concorrência. Wiring de UseCases via configuração mantém o núcleo Java puro.
- **Entrada/saída:** HTTP e callbacks traduzem dados/identidades verificadas para a aplicação. Não acessam persistence diretamente. DTO/principal de sessão não são entidades do domínio.

Application Service e UseCase frequentemente designam o mesmo papel; escolher UseCases explícitos, sem empilhar um Service que apenas repassa chamadas. Composição antes de herança. Classe abstrata requer variação real, não um possível futuro provedor.

## Persistência

Spring Data JPA/Hibernate é a base de persistência do domínio. A implementação de uma porta pode usar EntityManager diretamente; não exigir um JpaRepository intermediário sem benefício. Entidades JPA e mapeamento ficam na infraestrutura, sem contaminar o domínio. Consultas nativas são permitidas nessa fronteira quando necessárias, com parâmetros vinculados e atenção a flush/contexto de persistência.

Flyway é o único dono do schema. Hibernate usa `ddl-auto=validate`, nunca create/update; `open-in-view=false` impede persistência/carregamento implícito durante a resposta HTTP. Não reescrever V1/V2 existentes para satisfazer ORM. Identificadores atribuídos pela aplicação e constraints do banco continuam protegendo integridade; JPA não elimina transações ou corridas.

Spring Session JDBC e a consulta de prontidão permanecem infraestrutura técnica. Não representar cookies, CSRF ou configuração OIDC como entidades de negócio. JDBC pode existir nessa infraestrutura, mas não na persistência de identidade nem nos UseCases/adaptadores HTTP.

## Identidade e concorrência

O adaptador Google fornece provider/issuer canônicos e subject somente após validação OIDC. Um VO valida estrutura mínima; não autentica nem garante criptograficamente a veracidade de quem o construiu. Nunca aceitar identidade externa de input HTTP público. O UseCase genérico resolve o UUID sem habilitar provedores extras.

O UseCase define lookup/criação como uma unidade atômica por uma porta transacional. A implementação Spring executa transações independentes (`REQUIRES_NEW`) para este caso de autenticação: o UUID é retornado somente após commit, e uma transação chamadora não pode adiar o rollback necessário ao retry. Não generalizar essa propagação para todo futuro UseCase.

A implementação JPA traduz somente conflito da constraint `external_identity_key` para um conflito de identidade do contrato. O UseCase recupera a identidade existente **depois** do rollback, em outra transação. Falhas de FK, chave do usuário, validação ou indisponibilidade não podem virar um falso sucesso. Um usuário criado pelo concorrente perdedor deve ser revertido junto ao vínculo.

Não persistir e-mail/nome/foto/senhas/tokens; entidades JPA não são retornadas à web/sessão. O principal mantém somente UUID e permissões mínimas fora do modelo de persistência.

## Exemplos e alternativas

- Adequado: `FinishAttemptUseCase` carrega uma tentativa, chama `attempt.finish(...)` e salva. A tentativa rejeita transições inválidas.
- Inadequado: controller usa EntityManager; UseCase modifica todos os campos e concentra invariantes; `GoogleIdentityService` contém SQL; domínio anotado com Spring/JPA; Service intermediário só delega.
- Uma política de composição/pontuação pode ser Domain Service **se** a regra não couber naturalmente numa entidade/VO. Exemplos futuros, não funcionalidades implementadas nesta spec.

JDBC-only foi substituído por decisão do usuário, não por estar obsoleto. ORM foi escolhido por gestão da persistência/ecossistema, não só economia de digitação na era da IA. Modelo JPA separado custa mapeamento, mas preserva a independência acordada. Rejeitamos herança especulativa, interface para cada classe e uma skill como única fonte das regras permanentes.

## Verificação e evolução

### Refinamento após a revisão de sessão (#20)

A revisão encontrou rejeição de visitante em `SessionController` duplicando a regra do Spring Security, além de respostas `Map`/`ResponseEntity<?>`. Os testes de contrato comprovavam o 401, mas não o proprietário dessa decisão: comportamento correto não basta para demonstrar separação de responsabilidades. O usuário autorizou a correção e sua inclusão na padronização.

Autenticação, autorização técnica da rota, sessão e CSRF pertencem à segurança/infraestrutura. Controllers recebem identidade já verificada e traduzem contratos com DTOs tipados. Regras de negócio, inclusive posse/transições de recursos, permanecem no domínio/aplicação. DTOs não são entidades; endpoints técnicos triviais não exigem UseCases/Services intermediários. CSRF pode ser exposto na web, mas é gerado/validado pelo framework.

`docs/architecture/http-endpoint-review.md` define checklist e exemplos. ArchUnit protege dependências do controller de sessão; teste de assinatura protege respostas concretas. Testes HTTP reais protegem rejeições e contratos. As proteções estruturais são deliberadamente limitadas: não detectam toda duplicação de regra, não tornam o checklist automático e não substituem revisão semântica. Nenhuma regra genérica impede callbacks de autenticação de receber tipos próprios do framework.

Regras resumidas em `AGENTS.md`; este ADR é a referência detalhada, versionada junto ao código. Skills podem futuramente oferecer workflow de revisão, não substituir essas instruções. Alterações arquiteturais exigem nova decisão/justificativa e atualização coerente dos documentos.

Testar invariantes em Java puro, fluxo do UseCase com portas substituíveis, repositories/transações/schema em PostgreSQL 18.6 isolado, e o fluxo de navegador/OIDC já existente. ArchUnit verifica dependências, ausência de frameworks no núcleo e ciclos; não decide sozinho se o comportamento está no objeto correto. Não adicionar exceções abrangentes apenas para testes passarem.

Revisão: PR de arquitetura antes do #19, depois #20–#23 encadeados. Atualizar branches por commits adicionais sem descartar histórico; nenhum merge no GitHub, arquivamento ou publicação sem autorização. `main` e o material permanecem fora desta mudança.
