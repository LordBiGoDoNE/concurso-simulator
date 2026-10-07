# Google OIDC opcional

Desativado por padrão (`AUTH_GOOGLE_ENABLED=false`): nenhuma discovery/chamada ao Google no startup, entradas retornam 404 e `/api/v1/auth/config` retorna somente `googleEnabled:false`. Não há login simulado em produção.

Para habilitar, configure externamente, somente no backend:

| Variável | Uso |
| --- | --- |
| `AUTH_GOOGLE_ENABLED` | `true` |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Client web do Google, nunca valores `VITE_*` |
| `GOOGLE_CALLBACK_URL` | URL absoluta terminando em `/login/oauth2/code/google` |
| `FRONTEND_URL` | Destino fixo após login, sem query/fragmento |
| `SPRING_PROFILES_ACTIVE` | `local` somente para HTTP loopback |
| `CORS_ORIGINS` | Origem explícita do frontend |

Configuração habilitada incompleta/insegura recusa startup. Produção exige HTTPS. Exemplo local: callback `http://127.0.0.1:8080/login/oauth2/code/google`, frontend `http://127.0.0.1:5173`. Use o mesmo hostname nas duas pontas e no console Google; não misture localhost e 127.0.0.1.

No Google Cloud Console, configure consentimento (e usuários de teste se aplicável) e um client OAuth do tipo aplicação web. Registre o callback exato nos URIs de redirecionamento autorizados. A aplicação solicita apenas `openid profile`, não acesso offline, Gmail ou contatos; perfil recebido é transitório. Não configura credenciais nem recursos remotos automaticamente.

## Fluxo e privacidade

Navegação a `/oauth2/authorization/google` inicia code + PKCE S256. Spring Security valida state, nonce, issuer Google fixo, audience, assinatura/JWK e validade do ID token. Sucesso resolve subject validado em UUID, gira a sessão, substitui o principal OIDC por UUID/permissão e remove authorized client. Nenhum refresh token é solicitado; tokens Google não são mantidos na sessão final ou no browser storage. Não registrar payload OAuth nem ativar trace/debug de segurança/HTTP em produção; proxies não devem registrar query strings do callback (podem conter code/state).

Redirect usa exclusivamente `FRONTEND_URL`, nunca `returnTo` público. Falha/cancelamento invalida a sessão temporária e retorna `?auth=failed`, sem detalhes/token. O marcador é consumido pela UI. Conta é criada somente após validação; login não promete histórico sincronizado.

## Smoke Google real (complementar, antes da publicação)

1. Configure o client e variáveis acima sem gravar segredo no repositório; inicie PostgreSQL/backend/frontend local.
2. Confirme auth/config habilitado, visitante me=401 e aulas acessíveis sem login.
3. Entre pelo botão Google, confira consentimento mínimo, retorno fixo, UI conectada e me=200 com UUID apenas.
4. Confira cookie HttpOnly/SameSite=Lax (Secure no HTTPS), rotação da sessão e ausência de tokens na URL final/storage/sessões JDBC/logs.
5. Saia, confira 204 e me=401; entre novamente e confirme o mesmo UUID, inclusive após restart do backend.
6. Cancele consentimento e confira mensagem genérica com aulas acessíveis. Repita em duas sessões/dispositivos: sair de uma não encerra a outra.

Não executado automaticamente: depende de credenciais do usuário. CI usa provedor OIDC efêmero **somente em src/test**, com discovery, autorização, token, JWK e HTTP/cookies reais. Exercita PKCE, rotação, replay, state/nonce/issuer/audience/assinatura/expiração, configuração desativada/incompleta e ausência de tokens em logs/sessões finais. A identidade persistida e a recuperação de sessão via novo repositório têm testes separados.
