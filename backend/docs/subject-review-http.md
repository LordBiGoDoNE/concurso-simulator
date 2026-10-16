# Contratos HTTP da revisão por assunto

Grupo 3 usa GET /api/v1/me/subject-review?roleId=... para conta e
GET /api/v1/attempts/{id}/subject-review para visitante/conta, somente conclusão própria.
O painel aceita exatamente um cargo obrigatório; individual não aceita query params.
UUID canônico e cargo conhecido são validados na web; parâmetros extras/repetidos/vazios
falham com 400. Janela tem limite fixo100; não há paginação fornecida pelo cliente.

## Checklist de responsabilidades

- [x] Spring Security/AttemptAccess reconhecem o ator e rejeitam principal inválido ou
  visitante no painel antes do controller, com401 JSON/no-store, sem redirect.
- [x] Controllers recebem @CurrentActor já verificado e traduzem entrada/DTO concreto.
  Não consultam repositories, sessão, autenticação, gerador ou correção diretamente.
- [x] ReviewUseCases coordena leitura; agregado Attempt protege posse, vigência e conclusão.
  Recurso ausente/alheio/expirado retorna404 indistinguível; aberto próprio409.
- [x] DTOs incluem somente política, cargo, amostra, nomes públicos dos assuntos e métricas.
  Nunca serializam CompletedAttempt/owner, correção, IDs de itens/base, captura ou digests.
- [x] Aplicação/domínio não dependem de HTTP/framework; wiring/transação permanecem infra.
- [x] Rejeições técnicas, CSRF e CORS pertencem à segurança, não ao controller.
  GET não exige token; POST existente continua protegido. CORS usa origens explícitas.
- [x] OpenAPI fecha objetos e enumera códigos/limites. Falhas de integridade são500
  internal_error, banco/transação503 unavailable; nenhuma vira vazio/sucesso parcial.
- [x] ArchUnit protege dependências do novo controller e records concretos, sem afirmar
  detectar toda duplicação semântica. Testes HTTP usam servidor/segurança/sessão/PG reais.

Primeira camada do lote3/4 entrega3.1/3.2, verificada com ReviewHttpTests e ArchUnit
(17s, sem falhas). Privacidade/falhas/concorrência ampliadas em
3.3/3.4; nenhuma tarefa do grupo é considerada concluída por um prefixo. Revisão
independente SOL6.1 xhigh e aprovação de merge continuam pendentes no lote em execução.
Sem endpoint de criação de progresso, dado editorial real, main ou deploy.
