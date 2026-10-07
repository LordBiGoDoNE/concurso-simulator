# Proposal

## Why

O usuário escolheu login opcional e Google como primeiro provedor. Precisamos de identidade estável e acesso seguro antes de implementar histórico privado, sem impedir o estudo de quem não deseja criar conta.

## What Changes

- Adicionar entrada e saída com Google e consulta da identidade atual, usando sessão no backend.
- Persistir uma identidade interna mínima para vincular, em specs posteriores, tentativas e progresso entre dispositivos.
- Manter aulas, status da API e navegação pública acessíveis sem login. Simulados futuros continuarão disponíveis a visitantes, conforme decisão de acesso; esta spec não implementa simulados.
- Adicionar proteção de sessão, CSRF, redirects restritos e testes sem depender de contas Google reais no CI.
- Fora de escopo: senha própria, outros provedores, vinculação de contas, permissões administrativas, histórico, importação de progresso de visitantes, banco de questões, sincronização de tentativas, deployment e provisionamento de credenciais externas.

## Capabilities

### New Capabilities

- `user-auth`: autenticação Google opcional, identidade estável, sessão privada, logout e experiência pública sem autenticação.

### Modified Capabilities

Nenhuma capability consolidada existe em `openspec/specs/`. Esta mudança depende da base já integrada na épica geral e mantém seus requisitos públicos de prontidão e leitura. A negação padrão de rotas será preservada, com exceções explícitas para os novos endpoints de autenticação.

## Impact

Backend: Spring Security OAuth2 Client, armazenamento JDBC de identidade/sessão e migrações Flyway. Frontend: estados visitante/autenticado, entrar e sair, sem transportar tokens Google. Novos contratos HTTP em OpenAPI. O fluxo OAuth exige um client Google configurado externamente; a documentação orientará a configuração local, sem criar recursos ou publicar segredos. Branch da spec: `epic/optional-google-identity`, com tarefas revisadas por PR; destino final da spec: `epic/application-reformulation`, nunca main nesta etapa.
