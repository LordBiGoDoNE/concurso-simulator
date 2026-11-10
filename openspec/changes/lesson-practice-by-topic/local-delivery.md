# Entrega local — prática por assunto (#161)

## Escopo e estado

Em 2026-11-10, o usuário autorizou os grupos completos 1–4 na conversa central
“Automatizar projetos na VPS”. Implementação neste worktree exclusivo, branch
`task/lesson-practice-by-topic/topic-practice`, base remota conferida
`16ca8aef382d64e7652b280b413122c357775ead`. A proposta histórica #162, commit
`069b027dd0d2621cc956b2804d872f73cdcf3c0f`, e `epic/lesson-practice-by-topic`
permanecem preservadas. Nenhum push de implementação, PR novo, merge, retarget,
sync/rebase de stack, arquivamento ou deploy foi executado.

#158/#160 e os refinamentos de aulas nativas já estão na base. V12/V13 foram
ocupadas por importação/ADM: a migração da #161 é **V14**, sem editar V1–V13.
A branch local parte da main atual porque a épica documental é anterior a essas
integrações; publicar/compor PRs na épica exigirá conciliar essa base de forma
explícita. Nenhuma branch remota/documental foi movida para resolver isso.

Risco **crítico** conforme `docs/architecture/spec-review.md`: schema/capturas,
idempotência/concorrência, contratos privados e executor compartilhado. Usuário
testa antes de push/revisões por modelo. Revisão independente de lote SOL 6.1 xhigh
e gate cumulativo independente continuam **pendentes e separados**. Não houve
revisão semântica por modelo, medição de cota ou inventário estimado de tokens.

## Comportamento e compatibilidade

- Novos inícios: `POST /api/v1/lesson-practices` com `{commandId,topicId}`.
  Disponibilidade: `GET /api/v1/lesson-practices/availability?topicId=P01`.
  `roleId` no início ou na query é recusado com 400, sem efeitos.
- Projeção própria `PracticeView`: id, topicId, createdAt, finishedAt, version,
  items. Histórico/encerramento próprios sem cargo de realização. `roleIds` dos
  itens continuam sendo metadados editoriais; não filtram a nova prática.
- Pool aprovado PRATICA_AULA/fixo/do assunto, min(5, disponíveis), bases distintas.
  Zero não persiste nem busca outro acervo. Sem cargo escolhido/atribuído e sem
  Composition substituta; novas capturas formato 4 têm `composition: null`.
- Criação canônica `lesson-practice/v2` + assunto. Mesmo ator/comando/intenção
  recupera a captura; outro assunto/modo ou criação histórica v1 incompatível
  retorna 409. Hashes anteriores não são migrados. Resposta/encerramento preservam
  seus hashes, posse, modalidade, expiração e versão antes do replay.
- Formatos 1/2/3 continuam legíveis. Práticas formato 3 podem responder/encerrar,
  mantendo composição, itens, correção e comandos originais. A projeção omite
  cargo sem apagar a composição persistida. V14 não regrava dados.
- V14 conserva travas anteriores de identidade, modalidade, assunto e fonte,
  amplia o CHECK de finalidade e vincula itens formato 4 à captura ao commit.
  Formatos sem campo `format` são ignorados pela nova trava específica.
- `RunView`/`RunTransport` compartilham apenas os campos mínimos da fila.
  `AttemptView` continua exigindo roleId; `PracticeView` exige topicId. Não há
  roleId vazio, cargo automático nem relaxamento do validador de simulados.
- Painel/seleção/início pendente/correspondência usam assunto. Cargo público pode
  mudar na mesma aula sem perder prática ou respostas; recolher/navegar preserva
  um executor montado. Troca de identidade invalida ambos os contextos e respostas
  tardias. Retorno à aula conserva cargo público compatível ou abre exploração livre.

## Responsabilidades HTTP

| Decisão | Proprietário e evidência |
| --- | --- |
| Identidade técnica, sessão, CSRF, acesso à rota | Spring Security/infraestrutura existentes; testes HTTP com cadeia real, ausência de CSRF, expiração e duas contas |
| Conversão JSON/query, erros, status, no-store | LessonPracticeController e DTOs concretos próprios; roleId/extra/query duplicada recusados |
| Seleção, insuficiência, comandos, carregamento | AttemptUseCases com portas; Java puro; sem SQL/controller direto na persistência |
| Contexto, bases, opções, posse, transições | Attempt; regressões de invariantes, mistura de assuntos, duplicação, cargo oculto e respostas inválidas |
| Transação, concorrência, JSON histórico e travas | JpaAttemptStore/AttemptCodec/Flyway V14; PostgreSQL 18.6 isolado |

Checklist HTTP aplicado: público/finalidade documentados; entrada/saída tipadas;
nenhuma autenticação/CSRF duplicada no controller, nenhum Service de repasse,
entidade JPA ou gabarito antecipado na resposta. A política global de JSON não foi
alterada. ArchUnit/testes estruturais têm alcance limitado e não substituem revisão
semântica. Visitantes mantêm acesso temporário, contas possuem histórico próprio;
404 entre modalidades/posse, 409 antes do resultado e 403 sem CSRF preservados.

## Verificação executada

Resultados finais e comandos estão também em `local-verification.json`.
Código local: `7a13eb9628ab3ff322a4c9f7d34ef7cd887756f1`; artefatos posteriores
registram somente verificação/entrega. Backend completo: **334 aprovados e um
ignorado** (symlink indisponível no Windows), build/bootJar aprovados. React:
**230 aprovados**, TypeScript/Vite aprovados. Testes Python locais: **23 aprovados
e um ignorado** (semântica Unix de TIME_WAIT). OpenSpec 1.14.1 strict aprovado.
Navegador: **30 cenários aprovados** (18 da jornada pública e 12 reais) em
Chromium/Edge, com revalidação dos 12 reais sobre a V14 final. Backup/restore
V10–V14 aprovado, relatório de origem no SHA do código local.
Logs completos ignorados em `backend/build/lesson-practice-by-topic/`; capturas
Playwright em `frontend/test-results/topic-practice/`. Não são CI remoto.

- Domínio/aplicação: cargos editoriais distintos no mesmo pool, pools pequenos e
  grandes, zero sem escrita, formato 4 sem composição, questões/alternativas únicas,
  replay sem nova seleção, assunto incompatível e isolamento dos simulados.
- PostgreSQL: V11→V14 conserva tabelas/capturas/comandos; legado formato 3 respondido,
  encerrado e repetido; duas criações concorrentes produzem uma tentativa/comando;
  travas de composição/itens/contexto. Dump/restore adicional de capturas 3/4 com
  leitura, correção e resposta/replay após restaurar em banco sintético novo.
- Backup local: inventários fechados V10–V14, checksum/imagem exata/destino vazio,
  sessões restauradas invalidadas; dados do operador e VPS não foram acessados.
- React/TypeScript: tipos específicos, entrada por assunto, fila/retry, histórico,
  limpeza por identidade e ausência de correção antecipada; build de produção.
- Chromium e Edge em 390/1440px: link direto sem cargo, exploração/jornada pública,
  prática abaixo do botão, cargo público alterado, recolher durante gravação,
  confirmação perdida e retry, legado e volta à aula, encerramento, simulado
  independente, conflito/reload, troca de conta com resposta tardia e expiração.
  Cenário de conflito injeta 409 no transporte; restante usa API/PostgreSQL reais.

Não foi atualizado o runtime/banco existente do operador, nem reconstruída toda a
stack Compose. Fixtures aprovam somente dados sintéticos em PG efêmero, sem efeito
editorial real. A seleção e os fingerprints editoriais versionados permanecem
idênticos à base; nenhum acervo/compilador/manifesto foi alterado. Backend antigo
não lê formato 4: recuperação exige backup e binário/imagem compatível, sem
rollback automático sobre esses dados.

## Teste local do usuário

Ambiente separado: UI `http://127.0.0.1:5371`, API `http://127.0.0.1:19160`, controle
exclusivo do harness `http://127.0.0.1:19161/fixture`. Contém as 51 aulas nativas,
questões sintéticas de simulado e sete bases de prática P01 com cargos editoriais
diferentes; outros assuntos demonstram insuficiência. Não é o acervo real do operador.
Ao encerrar o harness, PostgreSQL efêmero e suas respostas são descartados.

Para iniciar, em dois terminais deste worktree (JDK 25 no JAVA_HOME):

```powershell
./backend/gradlew.bat -p backend --no-daemon --console=plain topicPracticeBrowserFixture
```

```powershell
cd frontend
$env:VITE_API_URL='http://127.0.0.1:19160'
npx vite --host 127.0.0.1 --port 5371 --strictPort
```

Abrir `/#/aulas/P01`, expandir **Pratique esta aula**, iniciar e responder; recolher,
navegar e voltar; encerrar e conferir a correção. Para ensaiar histórico antigo,
o endpoint GET `/fixture` retorna cookies sintéticos/legacyId somente no harness;
os testes Playwright demonstram a instalação do cookie e a retomada. Não usar
esse controle em qualquer aplicação/banco de produção.

Aceite local do usuário ainda não registrado. Entrega para teste não significa
aprovação, integração ou permissão de publicar. O harness deve ser encerrado por
seus próprios terminais/processos, preservando os serviços das outras conversas.

## Handoff para #173

A #161 tem prioridade sobre mockexam/domínio/persistência, HTTP/OpenAPI e frontend
da prática. A #173 conserva cobertura e conteúdo; seu grupo 2 (teto 25) aguarda
integração autorizada da #161 na main. Não copiar esta implementação para outra
spec nem montar stack entre elas. Nenhum teto 25 foi implementado aqui.

Após essa integração, os pontos para ampliar o teto são:

- `Attempt.java`: máximo de itens PRATICA_AULA e invariantes de captura.
- `AttemptUseCases.java`: availability.maximum e limite de seleção.
- Migração posterior a V14: CHECK de quantidade (não editar V14 já aplicada/versionada).
- `LessonPracticeController`/OpenAPI: manter assunto como identidade; ajustar somente
  limites documentados/validados, sem reintroduzir cargo.
- `lesson-practice-api.ts`: máximo literal de disponibilidade e validadores de view/histórico.
- `LessonPracticeEntry.tsx` e `LessonPracticeWorkspace.tsx`: textos de teto.
- Fixtures/testes de pools, legado, migração/recovery e navegador.

Composição/contratos de simulados permanecem obrigatoriamente por cargo. Capturas
3/4 e comandos v1/v2 exigem estratégia compatível se um novo formato for necessário
para o teto ampliado; não relaxar histórico nem reinterpretar comandos existentes.
SHA da implementação: `7a13eb9628ab3ff322a4c9f7d34ef7cd887756f1`. A issue registra
esse commit local; a branch não foi publicada. Artefatos de verificação posteriores
não representam nova execução da revisão por modelo.

## Correções encontradas na validação

A primeira versão da nova trava SQL usava `<> '4'`; em SQL, NULL não satisfaz essa
comparação, afetando inserções históricas sem campo format. A regra positiva é
restringir a trava específica ao formato 4 com `IS DISTINCT FROM '4'`, preservando
formatos sem versão. Backup/restore e regressões de ADM passaram após a correção.
A trava também confere itens inseridos posteriormente, sem modificar as capturas.
O teste de schema foi atualizado de 13 para 14 migrações (e de 10 para 11 no
upgrade desde V3); nenhuma rejeição de integridade foi relaxada. Todos os achados
de implementação encontrados nestes ensaios foram corrigidos e revalidados;
isso não constitui revisão independente de lote/gate.
