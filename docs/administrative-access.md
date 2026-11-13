# Conta administrativa — HTTP e operação

O cadastro persistente é criado uma vez; ADMIN_USERNAME usa admin por padrão. ADMIN_PASSWORD
configura só o primeiro cadastro. Ausência gera senha temporária no console privado após
commit, exigindo troca. Reinício não redefine. O ADR 0002 registra invariantes e limites.

POST /api/v1/admin/auth/login recebe username/password e CSRF da mesma sessão; 204 rotaciona
sessão/CSRF. GET /api/v1/admin/account expõe estado privado. POST account/password verifica
currentPassword/newPassword. POST account/google-link comprova senha e inicia intenção OIDC
de cinco minutos, com URL fixa. Corpos JSON UTF-8 estritos têm até 8 KiB, sem compressão.
Cinco falhas bloqueiam por quinze minutos; 429 informa Retry-After. Não há cadastro/reset público.

Spring Security/infraestrutura possuem autenticação, confiança no principal, autorização
técnica, sessão/versão e CSRF. Controllers traduzem DTOs verificados; núcleo coordena senha
e vínculo; JPA conclui transação. Contas Google existentes preservam UUID/histórico e a
fonte com histórico impede transferência. A UI e a importação HTTP vêm nas próximas camadas.
