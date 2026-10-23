# Spec Delta

## Purpose

Permitir autenticação opcional com Google e identificação privada por sessão, mantendo o estudo público acessível a visitantes e preparando a associação segura de histórico em mudanças futuras.

## ADDED Requirements

### Requirement: Acesso público independente do login
A aplicação SHALL manter acesso às aulas e ao status público sem autenticação, sem redirecionar visitantes automaticamente ao Google. A interface MUST NOT afirmar que já salva ou sincroniza histórico nesta etapa.

#### Scenario: Visitante abre a aplicação
- **WHEN** o usuário abre a página sem sessão autenticada
- **THEN** vê o acesso às aulas e, quando Google estiver habilitado, uma opção de login, sem cadastro obrigatório.

#### Scenario: Google indisponível
- **WHEN** o provedor de login não responde ou o usuário cancela a autorização
- **THEN** o acesso ao material continua disponível e nenhuma sessão autenticada é criada.

### Requirement: Login Google validado
A aplicação SHALL autenticar somente callbacks vinculados a uma solicitação válida e a uma identidade Google verificada, com validação de assinatura, emissor, audiência, validade e nonce. Callback inválido MUST NOT criar usuário ou sessão autenticada.

#### Scenario: Login válido
- **WHEN** o usuário inicia o login e conclui uma resposta Google válida para essa solicitação
- **THEN** obtém uma sessão autenticada e retorna ao destino fixo configurado da aplicação.

#### Scenario: Callback adulterado ou repetido
- **WHEN** o callback tem state incorreto, nonce incorreto, token inválido ou código reutilizado
- **THEN** a autenticação falha sem criar conta nem expor dados internos.

### Requirement: Identidade interna estável
A aplicação SHALL associar a identidade Google a um identificador interno estável, sem usar e-mail como chave nem solicitar senha própria. Logins repetidos ou concorrentes da mesma identidade MUST NOT criar contas duplicadas.

#### Scenario: Retorno do mesmo usuário
- **WHEN** a mesma identidade Google autentica em outro dispositivo ou após reiniciar o backend
- **THEN** recebe o mesmo identificador interno; cada dispositivo mantém sua própria sessão.

### Requirement: Consulta privada da própria identidade
`GET /api/v1/me` SHALL retornar HTTP 200 com somente `{"id":"<uuid>"}` para sessão válida e HTTP 401 com `{"error":"unauthenticated"}` sem sessão válida. A resposta MUST NOT aceitar identificador de terceiros como critério de acesso e SHALL usar `Cache-Control: no-store`.

#### Scenario: Consulta autenticada
- **WHEN** um cliente com sessão válida consulta sua identidade
- **THEN** recebe apenas seu identificador interno, sem tokens ou dados de outros usuários.

#### Scenario: Visitante ou sessão expirada
- **WHEN** o cliente consulta sem sessão válida
- **THEN** recebe HTTP 401 JSON, sem redirecionamento automático ao Google.

### Requirement: Proteção e expiração da sessão
A aplicação SHALL usar cookie de sessão HttpOnly, SameSite=Lax e Secure fora do ambiente HTTP local explícito, trocar o identificador da sessão após autenticação e expirar sessões após inatividade configurável, com padrão de 30 minutos. Tokens Google e segredos MUST NOT ser expostos ao frontend, URLs de retorno ou logs.

#### Scenario: Tentativa de fixação de sessão
- **WHEN** o usuário autentica após possuir uma sessão anônima
- **THEN** o identificador anterior não concede acesso à identidade autenticada.

#### Scenario: Expiração
- **WHEN** a sessão excede o período de inatividade
- **THEN** as consultas privadas retornam HTTP 401 e a interface volta ao estado visitante sem bloquear as aulas.

### Requirement: CSRF e origens explícitas
Requisições que alterem a sessão SHALL exigir token CSRF válido. CORS SHALL permitir credenciais somente para origens explícitas configuradas e MUST NOT conceder acesso a origens desconhecidas. URLs fornecidas pelo usuário MUST NOT controlar o destino do callback.

#### Scenario: Logout sem proteção
- **WHEN** um cliente envia logout sem token CSRF válido
- **THEN** recebe HTTP 403 e a sessão não é encerrada.

#### Scenario: Origem ou destino não autorizado
- **WHEN** uma origem desconhecida consulta a API ou uma requisição tenta fornecer retorno externo
- **THEN** não recebe permissão CORS nem redirecionamento para esse destino externo.

### Requirement: Logout da sessão atual
`POST /api/v1/auth/logout` SHALL encerrar apenas a sessão atual, invalidar seu cookie e retornar HTTP 204 com CSRF válido, inclusive quando já não houver autenticação. Logout MUST NOT excluir a identidade persistida nem desconectar outros dispositivos.

#### Scenario: Saída concluída
- **WHEN** o usuário envia logout válido
- **THEN** consultas posteriores com o cookie anterior recebem HTTP 401 e a interface apresenta o estado visitante.

### Requirement: Obtenção de token CSRF
`GET /api/v1/csrf` SHALL responder HTTP 200 com JSON contendo somente `token` e `headerName`, ambos strings, vinculado à sessão atual e com `Cache-Control: no-store`, inclusive para visitantes. O frontend SHALL obter token atualizado antes de logout se a sessão tiver mudado.

#### Scenario: Visitante prepara operação de sessão
- **WHEN** um visitante consulta o endpoint de CSRF
- **THEN** recebe um token válido para sua sessão, sem conceder autenticação ou acesso a dados privados.

### Requirement: Configuração segura e login desativável
A aplicação SHALL permitir desativar o login Google por configuração externa e informar sua disponibilidade por `GET /api/v1/auth/config` com HTTP 200 e somente `{"googleEnabled":true|false}`. Quando habilitado, configuração obrigatória ausente SHALL impedir startup; quando desativado, iniciar login SHALL retornar HTTP 404 sem contato com Google.

#### Scenario: Ambiente sem credenciais
- **WHEN** o backend inicia com login desativado
- **THEN** funciona normalmente, informa googleEnabled=false e a interface não oferece um fluxo de login quebrado.

#### Scenario: Login habilitado sem configuração
- **WHEN** o backend inicia com login habilitado e sem client ID, secret ou URLs obrigatórias
- **THEN** falha de forma clara sem imprimir segredos.

### Requirement: Erros compreensíveis e interface acessível
A interface SHALL oferecer entrada com Google, saída e indicação de sessão autenticada ou visitante, com navegação por teclado. Cancelamento, falha de rede ou sessão expirada SHALL produzir mensagem compreensível, sem mostrar tokens ou detalhes técnicos e sem perder o acesso às aulas.

#### Scenario: Falha no retorno
- **WHEN** o usuário retorna de um login não concluído
- **THEN** vê uma mensagem genérica e pode tentar novamente ou continuar como visitante.
