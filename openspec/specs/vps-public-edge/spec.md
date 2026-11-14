# vps-public-edge Specification

## Purpose
Publicar a aplicação em domínio próprio com TLS, redirecionamentos canônicos, área administrativa restrita e sem acesso direto pelo IP, preservando os demais serviços do host.

## Requirements

### Requirement: Endereço canônico com HTTPS
A aplicação SHALL ser servida em `https://concurso-simulator.reemsoftworks.com`
com certificado válido e renovação automática. HTTP SHALL redirecionar para HTTPS,
exceto o desafio ACME. A resposta SHALL incluir HSTS. Somente as portas 22, 80 e
443 SHALL aceitar conexões externas, em IPv4 e IPv6.

#### Scenario: Acesso por HTTP
- **WHEN** alguém acessa `http://concurso-simulator.reemsoftworks.com/qualquer`
- **THEN** recebe 301 para o mesmo caminho em HTTPS

#### Scenario: Portas internas
- **WHEN** alguém tenta acessar a web interna, a API ou o PostgreSQL pela internet
- **THEN** a conexão falha em IPv4 e IPv6

### Requirement: Domínios secundários e hosts desconhecidos
Hosts de `reemsoftworks.com.br` SHALL redirecionar com 301 para o mesmo host e
caminho em `reemsoftworks.com`. Raiz e `www` de `reemsoftworks.com` SHALL
redirecionar temporariamente para o concurso. Hosts desconhecidos MUST NOT chegar
à aplicação; na porta 80 SHALL ser recusados pelo `default_server` versionado após a
abertura, e o serviço administrativo existente pelo IP SHALL ser preservado.
A área administrativa SHALL ficar restrita a IPs autorizados, inclusive após a abertura.

#### Scenario: Domínio .com.br
- **WHEN** alguém acessa `https://concurso-simulator.reemsoftworks.com.br/x`
- **THEN** recebe 301 para `https://concurso-simulator.reemsoftworks.com/x`

#### Scenario: Host desconhecido
- **WHEN** uma requisição chega com Host não configurado
- **THEN** não é encaminhada à aplicação

#### Scenario: Área administrativa pela internet
- **WHEN** um IP fora da allowlist administrativa chama `/api/v1/admin/`
- **THEN** recebe 403 da borda e não consome o orçamento de login da API

### Requirement: Google OAuth de produção
O login Google SHALL poder ser ligado em produção com client OAuth próprio, callback
exato da origem canônica e segredos somente na VPS, sem que credenciais passem por
argumentos, conversa ou Git. Por decisão do usuário, o lançamento MAY ocorrer com o
Google desligado; ligá-lo MUST ser seguido de smoke real de login, sessão, logout e conta nova.

#### Scenario: Configuração recusada
- **WHEN** a API não confirma o Google habilitado após a configuração
- **THEN** o Google volta a ficar desligado e a aplicação segue disponível

#### Scenario: Primeiro login real
- **WHEN** um usuário entra com Google pela origem canônica
- **THEN** retorna autenticado à aplicação com cookie Secure
- **AND** logout invalida a sessão

### Requirement: Abertura controlada
O domínio SHALL permanecer restrito até a revisão final e o merge `[SPEC]`
autorizado. Na abertura, a publicação HTTP pelo IP SHALL ser removida e o volume
anterior preservado até decisão do usuário.

#### Scenario: Corte para produção
- **WHEN** o usuário autoriza a abertura
- **THEN** a release da main passa a atender o domínio sem restrição
- **AND** o endereço HTTP pelo IP deixa de servir a aplicação
