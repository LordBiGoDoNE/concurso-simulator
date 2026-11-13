# Credenciais Google e confirmação da sessão administrativa

O callback recebe uma identidade OIDC verificada pelo Spring Security. O cliente OAuth
com access/refresh token deve ser removido da sessão antes de concluir o vínculo, consultar
a conta ou confirmar qualquer resposta. Spring Session pode salvar a sessão já durante
`sendRedirect`; remover credenciais depois desse ponto não desfaz a gravação intermediária.
O contexto persistido contém somente UUID e evidência administrativa mínima.

A regressão em `AdministrativeAccountHttpTests` rejeita qualquer INSERT/UPDATE de atributos
com cliente OAuth, token de acesso/refresh sintético ou ID token em PostgreSQL isolado.
Ela cobre o vínculo bem-sucedido, o conflito com restauração da conta local e as demais
transições da classe. Assim observa também a primeira gravação no redirect, além do estado
final. Essa guarda é exclusiva do teste e não modifica o schema de produção; não demonstra
comportamento de outro provedor nem substitui validação de novas versões das dependências.
