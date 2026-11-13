> **Registro histórico superado na quantidade por prática.** Em2026-11-11 o usuário
> esclareceu: acervo25 e práticas aleatórias de até cinco. A [verificação atual](practice-sampling-five-verification.md)
> e o contrato atualizado prevalecem. Resultados abaixo descrevem a etapa anterior.

# Grupo 2 — teto 25 sobre #161 integrada

## Base, autorização e estado

Usuário autorizou grupos completos 1–8 e pediu continuar após #161. PR #176
mergeado em main: `226d2e5b07208887941811beff10ac29295b04b3`, conferido via GitHub e
Git. Branch de trabalho `task/question-bank-expansion/practice-capacity`, worktree
exclusivo `7ac1`. Épica local atualizada por fast-forward; épica remota, PR documental
#174 e branch anterior preservados. Os 34 arquivos locais anteriores foram
conferidos por SHA-256 antes/depois da troca de base.

A V14 integrada também limita o banco a cinco. O usuário pediu continuar depois
da proposta concreta/pergunta específica da [V15](capacity-schema-amendment.md).
V15 altera somente o CHECK de quantidade para formato 4; nenhuma tabela/coluna,
formato ou regravação. Não houve edição de V1–V14, banco do operador/VPS, push de
implementação, revisão independente, merge remoto, arquivamento ou deploy.

Estado: tarefas 2.1–2.6 implementadas e verificadas localmente, com ambiente
disponível para teste do usuário. Aceite, revisão independente, publicação e
integração continuam pendentes. Expansão editorial não concluída.

## Comportamento e responsabilidades

- `Attempt` protege 1–25 em formato 4, mantendo limite cinco no formato 3 histórico,
  identidades únicas, assunto, modalidade e fonte fixa. `AttemptUseCases` seleciona
  `min(25,pool próprio)` e informa contagem real/maximum 25. Não há cargo atribuído.
- Início mantém `{commandId,topicId}` e hash v2, sem quantidade livre ou novo recurso.
  Replay de criação v2 antiga recupera cinco. Criação v1 não vira v2; leituras,
  respostas/conclusões históricas conservam suas regras e comandos.
- Controller/DTOs traduzem HTTP; identidade técnica/sessão/CSRF continuam na cadeia
  Spring Security. Núcleo não ganhou SQL/Spring; JPA/Flyway/transações permanecem
  na infraestrutura. Não há autenticação duplicada ou Service de repasse.
- OpenAPI e cliente exigem `maximum:25`, leituras/histórico aceitam até 25. Textos,
  mapa, foco e navegação usam a captura retornada pelo servidor. Simulados e suas
  quantidades não foram alterados.
- Backup/restore aceita histórico fechado V15, com mesmas tabelas de V14. Continuam
  exigidos checksum, imagem exata, destino novo e limpeza de sessões restauradas.
  Binário anterior de teto cinco não lê práticas maiores; não truncar para rollback.

### Achado confirmado no reinício e correção

O ensaio com dois processos encontrou 503 ao salvar após reiniciar: o codec
reserializava integralmente capturas formato 3/4. A ordem de iteração de `Set`
de cargos editoriais pode mudar entre JVMs; o array de cargos dentro dos itens
mudava, acionando corretamente a trava de captura imutável do banco. Não se tratava
de cargo atribuído à prática ou de nova regra de seleção.

Regra positiva: ao atualizar qualquer formato, preservar o JSON original dos campos
imutáveis e alterar somente `answers`, `finishedAt` e `version`; conferir que o JSON
resultante ainda decodifica para o snapshot esperado. Criação inicial continua
serializando uma vez. Não foi relaxada a constraint nem regravado acervo histórico.
`PracticeCodecCompatibilityTests` força o array na ordem oposta à serialização da
JVM e verifica resposta/conclusão dos formatos 3/4; o ensaio real verifica reinício.
Essas regressões cobrem o caso confirmado, sem prometer detectar todo problema de
canonização ou compatibilidade futura. Risco continua crítico.

Checklist HTTP: finalidade e público preservados; rota/DTO/principal concretos;
decisões de negócio no núcleo; 401/403/404/409 e no-store exercitados pela cadeia
real. Testes de 30 bases/25 capturadas complementam casos vazios, entrada inválida,
posse/expiração/CSRF e ausência de correção antes de encerrar. CI/ArchUnit não são
aprovação semântica. Risco crítico por captura, replay, persistência/fila e recuperação.

## Comandos reproduzíveis e isolamento

JDK 25/Gradle 9.8.0 offline, Node 24.21.0; dependências travadas do npm instaladas
do cache do usuário. PostgreSQL 18.6 novo por Testcontainers; schemas/portas aleatórios.
Backups V10–V15: dois containers exclusivos com rótulo de propriedade, sem rede/portas.
Nenhuma dessas operações acessa o banco do operador.

```powershell
./backend/gradlew.bat -p backend --offline --no-daemon test bootJar localDeliveryFixtureClasses
python -X utf8 -m unittest discover -s scripts -p test_local_app_data.py -v
python -X utf8 scripts/check_backup_schemas.py
```

Frontend, a partir de `frontend/`:

```powershell
npm test -- --maxWorkers=2 --reporter=json --outputFile=test-results/practice-capacity-unit.json
npm run build
$env:PRACTICE_EXPANSION = '1'
npx playwright test --config playwright.topic-practice.config.ts
```

O flag usa UI 5393, API 19370 e controle de fixture 19371 exclusivos da #173;
`reuseExistingServer:false` impede adotar serviço alheio. Fixtures sintéticas de 30
bases aprovadas apenas no PostgreSQL efêmero permitem testar 25; não são conteúdo
editorial real e não contam nas cotas. Chromium/Edge exercitam 390/1440px, todos
os 25 itens e último item pelo teclado, filas/resposta tardia, conta/visitante,
replay perdido, histórico legado, navegação, troca de identidade e expiração.

Após compilar, ensaio de reinício em dois processos reais do JAR:

```powershell
java -cp "backend/build/local-delivery-fixture/classes;backend/build/local-delivery-fixture/lib/*" br.com.concursosimulator.mockexam.PracticeCapacityOperationalFixture backend/build/libs/concurso-simulator-0.1.0-SNAPSHOT.jar backend/build/question-expansion/capacity-restart
```

O ensaio usa outro PostgreSQL exclusivo, 12 respostas antes de desligar o primeiro
processo e 13 após iniciar o segundo, conclui/reaplica a conclusão e confere replay
v2 de cinco e leitura dos formatos 3/4. Logs/sessões ficam em build ignorado; o
relatório público registra somente resultados/hashes apropriados.

## Resultados e entrega

Backend completo após corrigir o codec: 340 testes executados, 339 aprovados,
zero falhas/erros, um ignorado histórico (`TemplateLocalTests`/criação de symlink
indisponível no Windows). Frontend: 238/238 aprovados e build aprovado; a execução
final limitou a dois workers por contenção observada em uma rodada paralela, sem
alterar assertions/timeouts. Python de recuperação: 11/11; expansão: 12/12.
Backup/restore real V10–V15 passou em destino sintético. Chromium/Edge: 30/30,
zero ignorados/falhas/flaky, repetidos após o codec corrigido. Dois processos reais
do JAR passaram, incluindo 12+13 respostas, conclusão/replay e comando anterior
de cinco. Os 79 itens externos passaram novamente pelo leitor/prévia, sem escrita.

JAR verificado: SHA-256 `8652e27327244c8a21c34f30838e2bb23e1dab52e9462c434ecfc0433e5a059b`.
O relatório de reinício está vinculado a esses bytes. Esse hash identifica o JAR
local; não é ID de imagem Docker nem substitui sua construção/entrega operacional.
Sem nova imagem ou deploy declarado neste incremento.

### Teste local do usuário

Ambiente próprio ativo em [P01 no localhost](http://127.0.0.1:5393/#/aulas/P01),
UI 5393/API 19370/controle de fixture 19371, PostgreSQL efêmero exclusivo.
Conferidos HTTP 200 na UI/status e disponibilidade `available:30, maximum:25`.
O pool de P01 é sintético para testar o percurso de 25, sem contar em cotas ou
carregar candidatas reais. Os processos permanecem locais; se encerrados, o
ambiente pode ser recriado pelos comandos de fixture/UI acima em terminais próprios.

Abrir a aula, escolher **Começar prática**, percorrer até a questão 25, responder e
encerrar; conferir mapa, retorno à aula e correção somente depois de concluir.
Conta/histórico/expiração e respostas tardias também foram exercitados pelos
testes automatizados com identidades sintéticas do mesmo harness. A confirmação
do usuário antecede revisões por modelo e publicação da implementação.

O acervo externo de 79 candidatas foi preservado fora de build; esta mudança não
altera seus fingerprints nem aprova/importa conteúdo. Grupos 1/3–7 e cobertura
integrada 8 permanecem pendentes. Usuário testa antes de publicar implementação ou
disparar revisão independente SOL 6.1 xhigh; gate cumulativo é etapa distinta.
