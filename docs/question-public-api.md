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
