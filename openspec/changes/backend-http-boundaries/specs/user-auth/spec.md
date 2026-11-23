# user-auth

## MODIFIED Requirements

### Requirement: Consulta privada da própria identidade
`GET /api/v1/me` SHALL retornar HTTP 200 com somente `{"id":"<uuid>"}` para sessão válida e HTTP 401 com `{"error":"UNAUTHENTICATED"}` sem sessão válida. A resposta MUST NOT aceitar identificador de terceiros como critério de acesso e SHALL usar `Cache-Control: no-store`.

#### Scenario: Consulta autenticada
- **WHEN** um cliente com sessão válida consulta sua identidade
- **THEN** recebe apenas seu identificador interno, sem tokens ou dados de outros usuários.

#### Scenario: Visitante ou sessão expirada
- **WHEN** o cliente consulta sem sessão válida
- **THEN** recebe HTTP 401 JSON, sem redirecionamento automático ao Google.
