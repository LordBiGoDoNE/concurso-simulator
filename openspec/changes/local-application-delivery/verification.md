# Verificação do coordenador — lote grupos1–4

Execução Windows/Docker Linux em2026-10-18. **15/16 verificadas;0/16 integradas**.
Item4.4/gate cumulativo permanece pendente após merges autorizados e contexto próprio;
esta verificação não é parecer independente nem autorização de merge.

Risco crítico: sessão/segredos/proxy, persistência/recuperação e CI. PRs#125–#128,
stack#129 v0.2.1, trunk epic/local-application-delivery. Base da spec6283c4d.
Nenhum schema, controller, UseCase, proteção de produção ou Pages alterado.
Proxy traduz transporte; segurança/session/CSRF continuam no Spring Security,
posse/regras no núcleo e persistência nos adaptadores existentes.

## Matriz executada

| Verificação | Resultado e limites |
|---|---|
| Ferramentas locais |11 testes, contexto/sentinelas/links, ownership, locks, checksum, confirmação e bloqueio de recuperação |
| Java build |246 testes,245 aprovados e1 symlink ignorado no Windows; ArchUnit/domínio/JPA/PG/HTTP presentes |
| Frontend |164 testes e build aprovados |
| Python legado |63 testes aprovados em Linux; Windows esbarrou em symlink/encoding/configuração global Git, sem relaxar testes |
| Material |108 páginas e1757 referências no pacote;51 aulas;9 módulos/90 resoluções/fórmulas/âncoras |
| Conteúdo numérico |111/111 variantes:64 regra de três,20 porcentagem,27 juros; auditoria não concede aprovação editorial |
| Interface/material offline |6/6 Playwright, incluindo estudo offline sem JavaScript |
| OIDC separado |23/24 inicialmente; teste que excedeu timeout passou isolado1/1. No CI das três primeiras camadas, suíte completa aprovada. Provedor sintético, sem Google real |
| Browser empacotado |4/4, visitante/CSRF/retry, duas sessões/conflito/retomada, outra conta404,51 assuntos/aulas/prática em390/1280px |
| Recuperação real |Dump custom18.6 com API parada;20 tabelas não técnicas comparadas integralmente; sessões técnicas incluídas no dump e invalidadas no destino; V5/formato2/V10; histórico/retomada; destino ocupado e falha bloqueados |
| Exportação limpa |Sete fases aprovadas: imagens/helper/dependências/inspeção/runtime/recuperação/browser, fonte commitada apenas |

## Identidade da execução limpa

Commit de fonte **`1046522ff8d0993c0852b06ee140c27fc677150a`**, origem de ensaio127.0.0.1:18088.

- API:`sha256:a52d846ee6935c19a86edf4952cec5b06a03143b6692124073a9e017c76e08a9`, usuário10001:10001.
- Web:`sha256:3317f0f8803d5d507b0b36346c8d68a1013f72779b6fa8a36665bc33d5ccada2`, usuário101:101.

Registro privado ignorado:backend/build/local-app/clean-delivery-check.json.
Imagens construídas de git archive por allowlist, não do workspace. Metadados registram
hashes de contexto/arquivos, pins e IDs; sem promessa de determinismo byte a byte.
Mudanças posteriores de documentação/diagnóstico não são apresentadas como novo build
executado. CI verifica o head publicado separadamente.

A inspeção real extrai JAR/SPA para verificar ausência de fixtures/dependências de teste,
metadados/solver numérico no cliente e contrato corretivo no bundle inicial. Material
legado conserva soluções públicas separadas. Configuração ausente faz API falhar;
API ausente e PostgreSQL parado produzem falhas limitadas sem impedir aulas.
Configtree e segredos por arquivo, portas somente na web loopback e V10 vazio foram
exercitados. Filtro/inspeção não são detector universal de segredo em código permitido.

## Correções e limitações

Launcher de Gradle em exportação Windows passou a usar caminho absoluto. Exportação
profundamente aninhada provocava erro I/O em bind mount pelo usuário10001 no Docker
Desktop; mesmo arquivo no caminho curto passou. Usar diretório ignorado curto na raiz,
sem elevar runtime a root. Estado/segredos preservam permissões próprias. Logs brutos,
dumps e cookies ficam privados; diagnóstico público só mostra arquivo/linha/categoria.

CI novo de entrega local falhou no head anterior, na etapa runtime; cabe ao head atual
confirmar Linux. Checks aprovados de camadas anteriores não certificam mudanças novas.
Não há publicação/preview nem upload dos dados do ensaio. Não se copiou nem leu banco
do operador. Todos os projetos do ensaio têm namespace/owner próprios e são removidos
somente após conferência; start/stop do operador preservam volume.

Etapa7 pública continua pendente: hospedagem/domínio/TLS, backups externos,
observabilidade/operação pública, smoke Google real e publicação/rollback.
