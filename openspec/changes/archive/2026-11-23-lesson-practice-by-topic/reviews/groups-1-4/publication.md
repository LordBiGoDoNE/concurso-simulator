# Publicação do lote — #161 / PR #175

Em 2026-11-10, após o bloqueio inicial da revisão automática de aprovação, o usuário
autorizou explicitamente o avanço da épica, o push da branch e a abertura do PR
em rascunho. A primeira tentativa Git ficou pendente de autenticação, foi encerrada
e a conferência de refs mostrou ausência de efeitos remotos; a repetição pelo helper
GitHub CLI concluiu sem alterar configuração global nem expor credenciais.

- Épica: avanço direto de `f61d57d5d55d6d61609815a2faa4b415ed8dca4b` para
  `16ca8aef382d64e7652b280b413122c357775ead`, já integrado à main; base anterior
  preservada em `refs/preserve/lesson-practice-by-topic/epic-before-finalization`.
- Branch `task/lesson-practice-by-topic/topic-practice` publicada inicialmente em
  `7aab592e68f2028e9107de52b4d9e170989834e0`.
- PR em rascunho: https://github.com/LordBiGoDoNE/concurso-simulator/pull/175,
  para `epic/lesson-practice-by-topic`; base/head inicial conferidos.
- PR documental #162/branch/head `069b027dd0d2621cc956b2804d872f73cdcf3c0f`
  preservados. Nenhum retarget, fechamento ou merge de PR executado.
- Uma entrega coordenada, fora de Stacks; nenhuma alteração de metadados de stacks.

## Ampliação de verificação após publicar

A inspeção dos jobs do CI identificou fixture pública antiga em
`frontend/e2e/study-layout.spec.ts`: ainda devolvia roleId na prática. A fixture
passou a usar PracticeView por assunto, conservar o assunto entre criação/leitura
e exigir ausência de roleId na disponibilidade e corpo `{commandId,topicId}`.
Não foi relaxada nenhuma asserção de isolamento nem alterado código de produto.

Toda a suíte pública Playwright passou localmente: **28 cenários**, incluindo
material offline, 51 aulas nativas, jornada pública, composição, mapa/teclado e
prática enquanto a fila do simulado permanece montada. Porta exclusiva 5372;
material estático gerado em diretório ignorado. Comando equivalente ao CI:
`STUDY_ROOT=<pacote local> npx playwright test`, com configuração local ignorada
para evitar portas de outras conversas. Log ignorado:
`backend/build/lesson-practice-by-topic/finalization/public-suite.log`.

A alteração da fixture exige nova revalidação do lote no conjunto atualizado.
Os resultados de CI serão conferidos no novo head, sem reaproveitar runs antigos.
`spec-review-tests` não certifica revisão cumulativa nem semântica por si só.

## Integração e dependência #173

A autorização recebida cobre publicação/abertura de PR; merge de #175 ainda exige
aprovação desse PR com head/checks explícitos. O futuro PR `[SPEC]` da épica para
main terá gate cumulativo em contexto independente e autorização própria. Sem
arquivamento ou deploy. A #173 pode continuar seu conteúdo/cobertura previamente
autorizados; o grupo 2 (teto 25) aguarda integração autorizada da #161 na main.
