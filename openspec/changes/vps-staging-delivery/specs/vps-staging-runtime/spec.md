# vps-staging-runtime

## ADDED Requirements

### Requirement: Operação assistida por SSH sob controle do usuário
O assistente MAY operar a VPS por SSH como `concurso-dev`, com a chave do usuário
no PC, sem agent forwarding e com verificação estrita da chave do host. Senhas,
tokens e segredos MUST NOT passar pela conversa, pelo Git ou por logs. Ações que
exigem root SHALL ser executadas pela conta administrativa com senha digitada pelo
usuário. Exclusão de volumes, imagens ou estado MUST exigir confirmação explícita.

#### Scenario: Chave do host divergente
- **WHEN** a chave apresentada pelo host diverge da registrada no PC
- **THEN** a conexão é interrompida sem aceitar a nova chave automaticamente
- **AND** a nova chave só é aceita após o usuário confirmá-la pelo console do provedor

#### Scenario: Operação privilegiada
- **WHEN** uma etapa altera nginx, certbot, firewall ou arquivos de root
- **THEN** o assistente apresenta os arquivos/comandos antes da execução
- **AND** a senha do sudo é digitada somente pelo usuário

### Requirement: Runtime de produção isolado
O runtime SHALL usar projeto Docker, volume, estado e segredos próprios no Docker
rootless do `concurso-dev`, com release identificada por commit da main e manifesto
de imagens. A web SHALL publicar somente em loopback; API e PostgreSQL MUST NOT
publicar portas no host. O operador local MUST conservar suas recusas atuais.

#### Scenario: Conflito de recursos
- **WHEN** existe colisão de porta, ownership inesperado ou imagem/origem divergente
- **THEN** a operação falha antes de adotar ou sobrescrever recursos
- **AND** o projeto antigo e seu volume permanecem intactos

#### Scenario: Reinício do host
- **WHEN** a VPS é reiniciada
- **THEN** nginx, Docker rootless e a aplicação voltam sem intervenção
- **AND** os dados persistem e a prontidão reflete o estado real dos três serviços

### Requirement: Sessão segura atrás do proxy
Em produção, o cookie de sessão SHALL ser Secure, HttpOnly e SameSite=Lax, com CSRF
atual e CORS restrito à origem canônica. URLs de login e retorno SHALL ser absolutas
e HTTPS por configuração, e a API MUST NOT confiar em `Forwarded`/`X-Forwarded-*`
enviados pelo cliente. Limites de login por origem SHALL ser aplicados pelo IP real
do cliente na borda, já que a API só enxerga o proxy interno.

#### Scenario: Cabeçalho encaminhado forjado
- **WHEN** um cliente envia `X-Forwarded-Host` ou `Forwarded` próprios
- **THEN** o proxy os descarta e a API usa somente os valores definidos pelo nginx
- **AND** URLs de OAuth e redirecionamentos usam a origem canônica

#### Scenario: Falha do banco ou da API
- **WHEN** uma dependência fica indisponível
- **THEN** a API responde erro/prontidão em prazo finito, sem HTML de falso sucesso
- **AND** o material estático continua acessível sem expor diagnóstico
