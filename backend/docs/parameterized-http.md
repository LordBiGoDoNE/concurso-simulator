# Opção pública de variações

`POST /api/v1/attempts` aceita `useParameterizedQuestions` booleano opcional, false
quando ausente. Null, strings, números e campos extras são 400; ausência e false
preservam o hash V5/replay. True altera o hash: reutilizar o commandId com outro valor
retorna 409. Pode misturar fixas com variações das três famílias elegíveis, sem aumentar
identidades ou prometer novidade. Nenhum endpoint editorial/solver é exposto.
O deserializador limitado a essa opção distingue ausência de null; o parser global
estrito não foi relaxado (Jackson trata ausência do primitivo como null sem esse ajuste).

Responsáveis pelo checklist HTTP: Spring Security verifica sessão/principal/rota/CSRF;
o controller existente traduz o DTO; UseCase coordena a captura; domínio protege
elegibilidade/integridade/posse; infraestrutura JPA confirma a transação. Não há nova
camada delegadora nem autenticação duplicada no controller. Respostas tipadas e
schemas de tentativa/resultado/histórico permanecem iguais; metadados não são públicos.

Falha interna de estado da geração é 500 `internal_error`, no-store e sem conteúdo
privado; falhas de banco/projeção permanecem 503. Não há fallback de integridade.
O advice só trata os controllers de tentativas; não transforma falhas desconhecidas
em sucesso. Não substitui validação interna, segurança real ou revisão semântica.

Regressões `AttemptHttpTests` usam HTTP/segurança reais e PostgreSQL isolado: parsing
exato/CSRF/extra fields, cliente legado/replay/conflito, visitante e conta com três
geradas/uma fixa, schema estrito/sem metadados, correção capturada e nova sessão após
retirada. Falha controlada na porta de templates verifica erro/rollback das quatro
tabelas; não é evidência de aprovação editorial real. Posse, expiração, principal
inválido e indisponibilidade continuam cobertos pela cadeia existente.
