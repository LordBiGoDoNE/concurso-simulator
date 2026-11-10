# Spec Delta

## Purpose

Permitir configurar o primeiro administrador, entrar por senha ou Google e manter permissões persistentes sem editar UUIDs no ambiente. Extensão autorizada da [issue #160](https://github.com/LordBiGoDoNE/concurso-simulator/issues/160), posterior à implementação local dos grupos 1–3; implementada/verificada localmente, sem revisão independente ou integração.

## ADDED Requirements

### Requirement: Primeiro administrador persistente
A aplicação SHALL criar um único ADM na primeira inicialização sem cadastro administrativo anterior, usando nome configurado ou `admin`. Conta, credencial protegida, permissão e conclusão do cadastro MUST ser persistidas atomicamente. Reinício MUST preservar o cadastro; a primeira pessoa que acessar a aplicação MUST NOT receber privilégios por esse fato.

#### Scenario: Primeira inicialização concorrente
- **WHEN** duas instâncias iniciam sobre um banco ainda sem cadastro administrativo
- **THEN** somente uma cria o ADM e confirma o cadastro; a outra utiliza o estado persistido, sem segunda conta ou credencial divergente e sem alterar contas Google existentes.

#### Scenario: Reinício ou administrador desabilitado
- **WHEN** o cadastro inicial já foi concluído, inclusive após troca de senha, vínculo Google ou desabilitação do ADM
- **THEN** a aplicação não cria outro administrador, não sobrescreve credenciais por configuração e não reabre o cadastro público.

### Requirement: Senha inicial gerada quando ausente
Sem senha inicial configurada, a aplicação SHALL gerar senha temporária imprevisível para o primeiro ADM e apresentá-la uma vez ao operador após o commit. A senha MUST ser substituída antes de importar ou vincular Google. Credenciais MUST NOT aparecer em respostas HTTP, URLs, logs de acesso ou relatórios públicos.

#### Scenario: Senha gerada
- **WHEN** a instância vencedora conclui o cadastro inicial sem senha fornecida
- **THEN** mostra a senha temporária no console restrito do operador; no primeiro login, permite somente concluir a troca e sair. Não mantém cópia recuperável da senha no banco.

#### Scenario: Perda da saída inicial
- **WHEN** o processo cai depois do commit e antes de entregar a senha gerada
- **THEN** reiniciar não muda a credencial; o operador utiliza recuperação local deliberada, sem liberar um novo cadastro por HTTP.

### Requirement: Login administrativo permanente por senha
O ADM SHALL poder entrar com nome e senha mesmo sem Google habilitado ou vinculado. Credenciais SHALL usar armazenamento adaptativo com sal, proteção contra tentativas repetidas e sessão do framework. O login MUST rotacionar a sessão e retornar erros genéricos, sem aceitar cadastro público de administradores.

#### Scenario: Login local sem Google
- **WHEN** o ADM usa credencial válida e a integração Google está desativada
- **THEN** autentica na própria conta e pode usar as operações permitidas após cumprir eventual troca inicial; aulas e demais acessos públicos continuam disponíveis.

#### Scenario: Credencial inválida ou ataque repetido
- **WHEN** nome/senha são inválidos ou a proteção de tentativas é atingida
- **THEN** o acesso é negado com resposta estável, sem enumerar contas ou registrar senhas; nenhuma permissão ou sessão autenticada é criada.

### Requirement: Troca e recuperação da senha local
O ADM SHALL poder trocar a senha após verificar a credencial atual. O operador do servidor SHALL poder emitir nova senha temporária por comando local deliberado. Ambas as operações MUST preservar histórico e vínculo Google, invalidar acessos administrativos antigos e impedir importação até concluir eventual troca obrigatória.

#### Scenario: Recuperação local
- **WHEN** o operador confirma uma recuperação de senha para o cadastro existente
- **THEN** somente a credencial local é substituída; Google, permissões e dados permanecem. Não existe recuperação anônima por HTTP nem alteração automática de senha no startup.

### Requirement: Vínculo Google comprovado pelo administrador
O vínculo SHALL exigir sessão ADM ativa, senha local verificada recentemente e autenticação Google válida vinculada à mesma intenção. E-mail informado, coincidência de perfil ou configuração do cliente OAuth MUST NOT conceder o vínculo. A senha local SHALL continuar funcionando após vincular Google.

#### Scenario: Google ainda não cadastrado
- **WHEN** o ADM confirma uma identidade Google verificada ainda sem conta na aplicação
- **THEN** ela passa a autenticar na mesma conta ADM e no mesmo UUID, sem criar outro usuário ou alterar históricos. Login por senha permanece disponível.

#### Scenario: Cancelamento ou callback indevido
- **WHEN** o fluxo é cancelado, expira, é repetido, usa sessão diferente ou falha na validação Google
- **THEN** não altera credencial, permissão ou vínculo. A sessão ADM original só é mantida quando seu estado ainda é válido; acesso indevido não é restaurado.

### Requirement: Conta Google existente preservada
Quando o Google já pertencer a outra conta, o vínculo SHALL preservar o UUID e todo o histórico dessa conta. Somente se o ADM local estiver sem histórico, a credencial local e a permissão administrativa SHALL ser associadas atomicamente à conta Google existente, sem copiar ou reatribuir dados de estudo.

#### Scenario: Vínculo com conta Google utilizada
- **WHEN** o Google autenticado já possui conta com histórico e o ADM local possui apenas seu cadastro inicial
- **THEN** senha e Google passam a entrar no UUID da conta Google; históricos, tentativas, comandos, revisões e dados anteriores mantêm seus identificadores e conteúdo. A conta local substituída não é apagada nem continua autenticando separadamente.

### Requirement: Histórico local impede reatribuição automática
O vínculo com outra conta Google SHALL ser recusado se houver tentativas de estudo, comandos ou recibos de importação atribuídos ao ADM local. A verificação MUST ser atômica com a transferência de credencial/permissão e impedir novas gravações concorrentes para a conta local substituída. Mesclagem automática permanece fora do escopo.

#### Scenario: Duas contas com dados próprios
- **WHEN** o ADM local já tem tentativa ou recibo de importação e o Google pertence a outro UUID
- **THEN** recebe conflito sem qualquer transferência, exclusão ou reescrita de dados; ambas as contas continuam preservadas. Migração futura exige proposta e autorização próprias.

#### Scenario: Gravação simultânea ao vínculo
- **WHEN** uma tentativa ou importação tenta gravar dados para o ADM local enquanto ele é associado à conta Google existente
- **THEN** a ordem de commit determina um único resultado coerente: o dado anterior bloqueia o vínculo, ou a conta substituída impede a nova gravação com rollback. Não fica histórico inacessível criado após a transferência.

### Requirement: Permissões sem configuração de UUID
A importação SHALL usar permissão administrativa persistida e estado vigente da conta em cada requisição. Nome de login, senha fornecida no pacote, dados de perfil Google e privilégios antigos em sessão MUST NOT bastar para autorizar. Concessão e revogação SHALL dispensar reinício e edição de UUID no ambiente.

#### Scenario: Identidade normal ou sessão antiga
- **WHEN** usuário Google comum, ADM desabilitado ou sessão administrativa invalidada tenta importar
- **THEN** o acesso é recusado antes do processamento do pacote, sem recuperar privilégios por configuração legada ou por campo enviado pelo cliente.

### Requirement: Interface mínima de conta administrativa
A aplicação SHALL oferecer entrada ADM, troca obrigatória/normal de senha e ação de vincular Google, com teclado, mensagens acessíveis e limpeza de segredos após uso. Estado visitante e estudo público MUST permanecer funcionais. Painel editorial, gestão de outros administradores e interface de upload não fazem parte desta extensão.

#### Scenario: Operador conclui o cadastro
- **WHEN** o operador entra com senha temporária, escolhe sua senha e vincula Google
- **THEN** a interface confirma cada etapa, informa qual conta será preservada e permite entrar depois pelos dois meios, sem mostrar UUID como tarefa de configuração.

### Requirement: Contratos e dados anteriores preservados
O login Google comum, `/me`, `/csrf`, configuração pública Google e logout SHALL manter seus contratos. Senhas em claro e tokens Google MUST NOT ser persistidos. Cadastro ADM e vínculo MUST usar migrações aditivas e operações deliberadas, sem importar, aprovar ou publicar questões durante a inicialização.

#### Scenario: Atualização sobre banco existente
- **WHEN** a extensão é aplicada sobre banco com usuários, sessões, tentativas e questões existentes
- **THEN** os dados anteriores permanecem íntegros; a permissão administrativa só surge pelo cadastro/vínculo autorizado e a criação do ADM não certifica revisão editorial de nenhuma questão.
