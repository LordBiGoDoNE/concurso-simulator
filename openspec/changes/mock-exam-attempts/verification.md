# Matriz de verificação — mock-exam-attempts

Escopo aprovado: visitante temporário e conta com salvar/retomar; grupos 1–6 completos.
Grupos 1/2 integrados pelos #66–#70 em `ec4f85ec133991c6e791914da54bd56ab088906a`.
Grupos 3–6 usam a stack #76, trunk `epic/mock-exam-attempts`; implementação, revisão e
integração são estados separados. SHAs/checks/parecer atual ficam em spec-review.json/md
e na issue #64. Esta matriz não certifica o gate cumulativo final nem autoriza merges.

## Requisitos e evidência

| Tarefa | Evidência principal | Comportamento verificado |
| --- | --- | --- |
| 1.1 | AttemptDomainTests, AttemptPersistenceTests | Limites/cargo/disciplina/assuntos, insuficiência, seleção sem reposição |
| 1.2 | AttemptDomainTests, AttemptPersistenceTests | IDs, associação de alternativas, limpar e snapshot/ordens imutáveis |
| 1.3 | AttemptDomainTests, AttemptHttpTests | 3/5=60%, branco/erro, arredondamento e irreversibilidade |
| 1.4 | AttemptUseCasesTests | Portas substituíveis, posse antes de replay/estado, clock/sorteio/retries |
| 2.1 | AttemptSchemaCaptureTests, QuestionSchemaTests | Novo banco/V4→V5, contas/sessões/acervo preservados, constraints e validate |
| 2.2 | AttemptPersistenceTests | Pool real, filtros, retirada antes/depois da captura, insuficiência atômica |
| 2.3 | AttemptPersistenceTests | Corridas de criação/resposta/conclusão, CAS, rollback, hash e falhas desconhecidas |
| 2.4 | AttemptHistoryExpiryTests | Páginas privadas, 24h, startup/scheduler, cascata e conta preservada |
| 3.1 | AttemptActorHttpTests, SessionAccessTests | Ator técnico, principal inválido, sessão expirada, CORS/CSRF reais |
| 3.2 | AttemptHttpTests, OpenAPI | DTOs estritos, 200/201/400/401/403/404/409/503, controle negativo de gabarito |
| 3.3 | AttemptHttpTests, attempts.real.spec.ts | Resultado precoce negado, dono, snapshot histórico, conclusão/replay |
| 3.4 | AttemptHttpTests, QuestionPublicHttpTests, ArchitectureTests | Histórico/retomada, no-store, indisponibilidade, sessão e stateless preservados |
| 4.1 | attempt-api.test, AttemptComposer.test | Prazo inclui CSRF/JSON, abort, limites, duplo clique/retry e insuficiência |
| 4.2 | AttemptPractice.test, attempts.real.spec.ts | Fila/versão, limpar, duas abas, conflito com escolhas preservadas |
| 4.3 | AttemptPractice.test, attempts.real.spec.ts | Confirmação/foco, fila pendente bloqueia, retry e nota do servidor |
| 4.4 | attempts.real.spec.ts, screenshots locais | Teclado/radios/dialog/progresso em 390/1280, falha/retry, aulas e controle negativo |
| 5.1 | AttemptHistory.test, AttemptHttpTests | Páginas/ordem/estado, vazio, nota somente final e abrir sem novo sorteio |
| 5.2 | attempts.real.spec.ts | OIDC/PG reais, mesma conta em outro navegador após restart, outra conta negada |
| 5.3 | AttemptContext.test, attempts.real.spec.ts | Aviso visitante/login, ausência de mescla, logout e expiração |
| 5.4 | AttemptContext.test, AttemptHistory.test, attempts.real.spec.ts | Logout/401/troca de conta, resposta tardia, storage/cache vazios e aulas |
| 6.1 | MockExamOperationalFixture, docs/mock-exam-operational-verification.md | JAR anterior real sobre V5, restart/retomada/resultado, sessão/acervo e limpeza |
| 6.2 | attempts.real.spec.ts | Visitante/conta, duas abas/contas, edição editorial após início e correção histórica |
| 6.3 | build/test/browser/scripts/check_attempt_artifacts.py | Builds, ArchUnit/HTTP, aulas/offline, OpenSpec, JAR/assets sem fixtures |
| 6.4 | Esta matriz, issue #64, spec-review.json/md | Composição de PRs/SHAs/checks, triagem e limites de revisão/integração separados |

## Execuções locais da combinação

- Backend Windows/Java25/PostgreSQL18.6: **149 testes, zero falhas/erros/skips**, test bootJar PASS.
- Frontend: **105 testes PASS**, incluindo invalidação por headers e preservação ao reabrir ID ativo; TypeScript/Vite build PASS.
- Navegador real Chromium/API/PG/OIDC: **14 cenários PASS**, oito de simulados e seis regressões; renderer sem download posterior à conclusão.
- Navegador de aulas/entrada/login: **6 cenários**, incluindo nove módulos e 90 resoluções offline.
- Python3.14/Linux isolado, repo somente leitura e rede desativada: **54 testes** PASS, sem skips.
- Links originais: 108 páginas/1661 referências locais; pacote gerado: 108 páginas/1757 referências PASS.
- OpenSpec 1.14.1: todas as quatro mudanças estritamente válidas.
- Artefatos: 191 classes da aplicação no bootJar, quatro arquivos no dist; checker PASS.
  Quatro controles sintéticos rejeitam fixture/env no JAR, payload privado em chunk e escape de entrada.
- Operação deliberada adicional: JAR anterior de `54896da` sobre V5 e retorno à aplicação atual PASS,
  preservando conta/sessão/acervo/tentativas e removendo somente visitante vencido no startup.

Os checks remotos executam build/test/PG, frontend, aulas, navegador OIDC e spec-review-tests;
CI não publica preview/deploy. A execução operacional com JAR histórico é adicional local,
não um hook automático de build/CI. IDs dos runs e heads conferidos ficam na evidência/issue.

## Limites e triagem

Risco crítico: dados privados/posse, sessão/CSRF, snapshot/correção, concorrência e expiração.
A ausência de campos corretivos em View não é promessa de sigilo global do legado público.
Screenshots de realização/resultado foram inspecionados em ambas as larguras. Testes de
estrutura/sentinelas/ArchUnit não provam toda semântica editorial ou decisão arquitetural.

O harness Python completo em Windows nativo encontrou privilégios de symlink, defaults
CP1252 e configuração global de assinatura em repositórios temporários de fixture; a suíte
completa foi executada em Linux isolado, sem mudar permissões/configuração global ou omitir
casos. Os quatro testes novos de artefatos também passaram em Windows. A CLI de extração
mantém sua regressão explícita de stdout UTF-8 sob ambiente legado, inclusive na suíte Linux.

Revisão independente do lote e gate final são evidências distintas. O gate cumulativo
da spec permanece separado; não extrapolar a exceção de modelo de question-bank, integrar
a épica geral/main, arquivar ou publicar. Cota/tokens privados ficam fora do Git; o usuário
recebe a disponibilidade/limites na apresentação do parecer, sem consumo estimado.
