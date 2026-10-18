# Design

## Context

Base `3715219`: Java25/Boot4.1.1/Gradle9.8.0, Node24/React/Vite, PostgreSQL18.6,
FlywayV1–V10 e sessões JDBC. `infra/compose.yaml` oferece somente o banco local.
Frontend usa configuração pública de build e os contratos existentes de identidade,
questões, tentativas e revisão. Não há servidor de produção da SPA nem imagem do JAR.
O usuário escolheu Docker local antes de hospedagem/domínio em2026-10-18.

## Goals / Non-Goals

Entregar execução completa por imagens, uma origem local, material independente da
API e recuperação de dados demonstrada. Preservar ADR0001, segurança e conteúdo.
Não escolher nuvem/domínio nem expor o ambiente à rede pública. Este incremento
não certifica produção, não publica e não conclui a preparação total da entrega.

## Decisions

### 1. Imagens e fronteira de build

Usar builds em etapas para JAR e SPA; runtime não executa Gradle, Vite dev server,
seed ou launcher de testes. Fixar versões/imagens e registrar commit e identificadores
das imagens usadas; não alegar reprodução byte a byte sem comprovação.
Aplicar lista restrita de fontes necessárias e exclusões de `.git`, `.env`, caches,
backups, dados editoriais privados e build local. Testar com sentinelas sintéticas.
Segredos não entram em ARG, imagens, bundles ou logs. O processo web/API executa
com usuário sem privilégios; PostgreSQL mantém o mecanismo próprio da imagem oficial.

### 2. Projeto Compose independente e origem única

Propor `infra/local-app/` e projeto `concurso-simulator-app-local`, volume e rede
próprios, sem adotar o volume de `infra/compose.yaml`. Somente web publica porta,
vinculada a127.0.0.1; API e banco acessíveis apenas na rede necessária dos containers.
Backend pode escutar na interface interna do container sem publicar a porta no host.
Comandos de rotina nunca removem volumes. Colisão de porta/configuração deve falhar
com instrução clara, sem parar serviços alheios nem tentar portas silenciosamente.

Servir SPA na raiz e encaminhar `/api/`, `/oauth2/` e `/login/` ao backend sem
reescrever contratos. Erros/API/callback nunca recebem index.html como sucesso.
Usar API relativa à origem (`VITE_API_URL=/`, após conferir todos os consumidores)
e base absoluta local do material. Origem/porta são parâmetros públicos de build;
alterá-los exige reconstruir a web e manter callback/frontend/CORS coerentes.
O servidor HTTP deve preservar métodos, query, status e no-store, sem registrar
cookies, cabeçalhos de autorização ou query do callback. Headers encaminhados de
origem externa não podem ampliar confiança no proxy ou mudar destinos de login.

### 3. Material, sessão e configuração

Montar o material a partir das fontes públicas versionadas e ferramentas existentes,
sem ler pages-store nem publicar previews; servir em `/material/`. Links relativos,
51 aulas, acentos, assets, fórmulas e abertura offline continuam verificáveis.
Resoluções do material legado são públicas; essa cópia fica separada do bundle da
SPA e não autoriza incluir capturas privadas, templates ou fixtures na aplicação.

Usar perfil `local` deliberado para HTTP loopback, conservando HttpOnly/SameSite=Lax,
CSRF e sessão JDBC. Perfil padrão/produção conserva Secure e validações HTTPS.
Google inicia desligado, sem client nem rede Google; quando habilitado exige client
externo/callback exato, sem relaxar a validação existente. OIDC de teste permanece
em harness separado, nunca na imagem executável da aplicação.
Segredos são arquivos locais ignorados, montados apenas nos serviços necessários;
documentar permissões e leitura pela configuração Spring/imagem PG. Compose secrets
não constitui cofre criptografado. Nenhum diagnóstico imprime configuração secreta.

### 4. Dados e recuperação

Volume novo inicia sem questões ou aprovações; vazio/insuficiência são estados reais.
Startup normal aplica somente Flyway/validação, sem seed nem acesso à cópia do operador.
Testes usam fixtures sintéticas deliberadas em ambientes descartáveis. Copiar dados
reais, contas, sessões ou o banco editorial exige autorização posterior de origem,
destino e escopo; aprovações locais anteriores não autorizam publicação ou transferência.
Não relaxar as proteções loopback das CLIs editoriais para operar pela rede Compose.

Backup lógico deliberado inclui tabelas de negócio, Flyway e sessões; segredos de
configuração e papéis do cluster são tratados separadamente. Credenciais ficam fora
de argumentos/artefatos públicos. Restauração aceita somente destino novo/vazio,
sem tabelas de negócio,sessão ou Flyway, e confere identidade antes da primeira escrita.
A API permanece parada até restaurar e validar; erro não libera aplicação parcial.
Antes de liberar o destino, invalidar todas as sessões restauradas por mecanismo
de infraestrutura. O dump continua completo, mas não pode reativar cookie revogado
após a captura do backup. Contas,histórico,capturas e comandos permanecem; acesso
de conta exige nova autenticação (sessão sintética nova no ensaio das imagens).
Ensaiar dump/restore com cliente PG18.6 e imagens identificadas, comparar hashes,
capturas/comandos e retomada de conta após nova autenticação; preservar versão e
decisões editoriais. Comparação de igualdade exclui somente as sessões invalidadas.
Ensaiar backup→logout→restore antes da expiração: cookie antigo continua401,
enquanto nova sessão da mesma conta acessa histórico e retoma a tentativa.
Reinício comum conserva volume e pode recuperar sessões válidas; não é restauração.
Retorno usa binário compatível e backup; não é downgrade
de schema, remoção de migrações ou execução de JAR V5 após capturas formato2.

### 5. Verificação e caminho posterior

Testar prontidão em ordem de dependências e falhas após startup, sem tratar healthcheck
como garantia de disponibilidade. O navegador usa a web compilada/proxy/JAR/PG reais,
não Vite. Cobrir visitante, conta com sessão sintética de harness separado,
CSRF, retry, outra sessão,
conclusão/revisão/prática e indisponibilidade com material acessível.
O harness cria contas/sessões somente no banco sintético descartável, usando as
versões de serialização da aplicação, sem endpoint/override de autenticação no JAR
ou Compose normal. Isso testa o consumidor da sessão e não prova login Google.
A suíte OIDC existente roda separadamente, preservando issuer Google fixo no artefato
real e a pendência do smoke Google antes da publicação.
Demonstrar exportação limpa, preservação das51 aulas/111 variantes versionadas e
ausência de segredo/fixture/solver no bundle inicial. CI apenas verifica; mudanças
necessárias de automação integram PR revisável, sem relaxar gate/Pages/ruleset.
Cada grupo inclui seus testes/docs/revisão. Gate cumulativo independente da spec é
SOL6.1xHigh; gate geral e entrega final não são substituídos pela validação local.

## Risks / Trade-offs

Três containers e build do material aumentam o tempo/uso de disco; caches devem ser
explícitos e não incluir dados privados. Docker Desktop requer containers Linux.
Download de dependências/imagens no primeiro build não é estudo offline nem
dependência de Google. Material continua utilizável sem API após empacotado.
Backup manual no mesmo computador não oferece HA nem recuperação de perda do host.
Navegação local e OIDC sintético não substituem TLS/hospedagem nem smoke Google real.

## Open Questions

Nenhuma decisão pública de hospedagem é tomada. Proposta#124 aprovada e grupos1–4
autorizados pelo usuário em2026-10-18; tarefas#125–#128 aprovadas/integradas em0ae60a4;gate cumulativo e integração da spec na épica geral pendentes. Um incremento posterior deve definir provedor,
domínio, TLS, orçamento, disponibilidade, backups externos e publicação/rollback.

## References

- ADR0001, checklist HTTP e docs de identidade/sessão/operações locais do repositório.
- [Compose secrets](https://docs.docker.com/compose/how-tos/use-secrets/).
- [Ordem e prontidão no Compose](https://docs.docker.com/compose/how-tos/startup-order/).
- [Dump e restauração PostgreSQL18](https://www.postgresql.org/docs/18/backup-dump.html).
