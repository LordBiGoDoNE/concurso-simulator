# ADR 0002 — Credencial ADM persistente e vínculo Google

- Data: 2026-11-09.
- Estado: decisões de produto e implementação local autorizadas pelo usuário nos grupos 4–6 da #160; revisão independente e integração pendentes.
- Complementa o ADR 0001; não altera seu registro histórico nem a política das rotas públicas.

## Contexto e decisão

A importação administrativa precisa dispensar descobrir UUID, editar allowlist e reiniciar.
O usuário escolheu primeiro ADM configurado na inicialização, senha temporária quando ausente,
login permanente por senha e Google e preservação da conta Google já utilizada. Esta é uma
exceção explícita e restrita à proibição histórica de persistir senhas: guarda-se somente
hash adaptativo da credencial administrativa, nunca senha recuperável ou tokens Google.

`identity` possui o cadastro e seus casos de uso. `AdministrativeAccount` protege transições
de credencial, obrigatoriedade de troca, falhas e versão de autenticação. O núcleo continua
Java puro; portas úteis fornecem hash e unidade atômica. Infraestrutura configura encoder,
bootstrap, sessão/OIDC/CSRF, limitação técnica, JPA e recuperação local. Controllers traduzem
DTOs e ações; não acessam persistência ou validam o principal por conta própria.

Uma linha singleton com `FOR UPDATE` serializa bootstrap, provas e mudanças. O marcador
concluído permanece mesmo com ADM desabilitado. O hash PBKDF2-HMAC-SHA256 usa sal de 16 bytes,
600.000 iterações e chave de 256 bits pelo encoder do Spring Security, com prefixo versionado;
não se implementa algoritmo criptográfico próprio nem se declara certificação FIPS. A senha
gerada usa SecureRandom de 32 bytes e só é emitida pelo vencedor após commit. Falha entre
commit e emissão exige recuperação local deliberada, nunca redefinição por restart.

O principal público continua `UserPrincipal(UUID)`. Evidência técnica de versão, sem hash,
é conferida contra permissão persistida em toda ação administrativa. Sessões antigas não
ganham privilégio automaticamente. Mudanças rotacionam sessão/CSRF e versão; uma sessão de
estudo válida não perde seus dados quando deixa de ter acesso administrativo. A conta local
substituída fica inativa e seu acesso é invalidado. Revogar permissão não depende de restart.

Vínculo requer senha atual e OIDC validado pelo framework, com intenção de cinco minutos,
state/sessão/versão e consumo único. Google novo usa a mesma conta; Google existente mantém
UUID, tentativas, comandos e recibos. Só credencial/permissão podem mudar de proprietário.
Histórico na fonte bloqueia transferência; não há merge, associação por e-mail ou terceiro
usuário temporário. Conflito conhecido de identidade só permite retry após rollback integral.

A unidade de vínculo usa READ COMMITTED explicitamente, para observar o histórico confirmado enquanto aguardava uma trava, independentemente do default do datasource. As linhas `app_user` são a barreira entre vínculo e gravações autenticadas. Vínculo tranca
as contas em ordem, revalida histórico e inativa a fonte no mesmo commit. Triggers aditivos
trancam a conta compartilhada antes de gravar tentativa/comando/recibo: se a gravação vencer,
o vínculo é bloqueado; se o vínculo vencer, a gravação antiga reverte. A integração SQL fica
na infraestrutura, sem dependência reversa entre núcleos. V1–V12 permanecem intactas.

A importação consome `AdministrativeImportPermission`, sem conhecer identidade no núcleo.
O adaptador consulta o cadastro vigente. Configuração legada não vazia é recusada; destino e
chave HMAC continuam independentes da senha. Ambos ausentes desabilitam só o importador.

## Consequências e verificação

Há novo segredo operacional no console privado e um caminho deliberado de recuperação local.
Não há gestão HTTP de múltiplos administradores, recuperação anônima, troca/desvínculo Google,
upload UI ou publicação editorial. A linha singleton serializa autenticação do único ADM;
seu custo e limitação são intencionais neste escopo.

Testes Java/ArchUnit, PostgreSQL real, cadeia HTTP e navegador OIDC cobrem invariantes,
rollback/conflitos, concorrência, versão, CSRF, privacidade e preservação do estudo. Verificação
local não representa aprovação independente/integração. Evidências e limitações efetivas estão
no relatório R2 da spec; não se reutiliza o relatório R1 como prova destes requisitos.
