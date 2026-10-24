# Prática independente das aulas

Spec `lesson-practice`, issue #148. Implementação e testes locais; sem integração
remota nem revisão independente nesta rodada. O usuário escolheu todos os grupos
1–5 e pediu estabilização local antes de novos pushes/PRs/revisões.

## Comportamento

Aulas → escolher cargo → **Praticar** inicia diretamente um treino do assunto, com
até cinco bases próprias, sem abrir a configuração de simulados. Se houver de uma
a quatro bases elegíveis, usa essas bases; zero retorna insuficiência, sem buscar
questões no outro acervo. Correção, gabarito e resolução aparecem ao encerrar.
**Voltar à aula** retorna à seleção que originou o treino.

Simulado e prática possuem realização, fila de respostas, comando idempotente e
histórico independentes. Iniciar prática não substitui um simulado. Trocar a própria
prática exige confirmação e é bloqueado durante operação pendente. Navegar entre
áreas mantém as filas montadas; troca de identidade/logout limpa ambos os contextos
e ignora respostas tardias. Visitante mantém tentativas temporárias por até24h,
sem histórico permanente nem transferência automática ao entrar na conta.

O painel de desempenho e revisão por assunto continua usando somente simulados.
O histórico da prática é acessível somente pela conta e mostra o assunto do treino.

## Identidade dos acervos e compatibilidade

A destinação é parte da identidade imutável da coleção de origem. Namespace
`lesson-practice-v1` significa `PRATICA_AULA`; demais origens representam
`SIMULADO`, preservando a interpretação de todas as bases existentes. `QuestionOrigin`
deriva essa destinação; não há flag enviada pelo navegador para mover bases.

V11 adiciona `qb_question.purpose` gerado dessa origem, índices e proteções de banco.
Um namespace reservado preexistente impede a migração, para não reclassificar dados
antigos silenciosamente. Origem/chave não podem ser alteradas; revisões não mudam
a coleção da base. Duplicar semanticamente um item sob uma identidade nova exige
triagem editorial: a separação de IDs não prova originalidade de conteúdo.

Consulta pública padrão e pool do compositor filtram antes de contar/paginar ou
capturar. Histórico e janela de métricas filtram modalidade antes do limite. A
prática captura modo/assunto em snapshot formato3 e aceita apenas fontes fixas;
formatos1/2 continuam simulados. O codec preserva campos imutáveis do JSON antigo
ao salvar resposta/conclusão, sem acrescentar modalidade nesses formatos.

V1–V10 não foram reescritas. JPA continua `validate` e `open-in-view=false`.
Após criar snapshots3, a versão antiga da aplicação não é um rollback de leitura
compatível. Uma recuperação usa backup privado e sua imagem exata em destino
novo; não iniciar binário anterior sobre registros de prática.

## HTTP e responsabilidades

- `POST /api/v1/lesson-practices`: `{commandId,roleId,topicId}`. Não recebe quantidade
  livre nem destinação. Comando repetido recupera a mesma tentativa; payload
  incompatível retorna409.
- `GET /api/v1/lesson-practices/availability?roleId=...&topicId=...`: disponibilidade
  própria e limite5, sem conteúdo de correção.
- `GET /api/v1/lesson-practices/{id}`, `PUT /{id}/answers/{itemId}`,
  `POST /{id}/finish`, `GET /{id}/result`: mesma posse/expiração/controle de versão e
  projeção privada dos simulados. Resultado antecipado retorna409.
- `GET /api/v1/me/lesson-practices`: histórico próprio paginado; visitante401.
- Usar um ID no endpoint da outra modalidade retorna404. Contratos JSON tipados,
  erros genéricos e `no-store`; nenhum endpoint expõe gabarito antes de encerrar.

Spring Security identifica conta/visitante e aplica acesso técnico/CSRF antes do
controller. O controller converte HTTP/DTOs; os UseCases coordenam portas; domínio
valida posse, modalidade, captura e transições; infraestrutura executa transações
e consultas. Não foram criados filtros com regras de posse nem tokens manuais.
Os testes HTTP passam pela cadeia real, incluindo visitante, CSRF, duas contas,
404 entre modalidades, replay, insuficiência e disponibilidade pública separada.
Testes estruturais não substituem a revisão semântica pendente.

## Conteúdo e testes locais

`editorial/lesson-practice/2026-10/*.jsonl` contém255 candidatos novos, cinco por cada
um dos51 assuntos, com enunciados, alternativas, resolução e fontes. O compilador
`scripts/compile_lesson_practice.py` gera pacote/seleção/relatório em diretório ignorado
`backend/build/lesson-practice/editorial/`. UUIDs de alternativas são determinísticos;
fingerprints usam o contrato binário Java, conferido por teste de integração.

O compilador não acessa rede/DB nem aprova. O launcher deliberado `questionBankLocal`
aceita o pacote específico, confere fontes/hash/classificação/seleção/fingerprint
antes de abrir o banco e importa exclusivamente rascunhos. A elegibilidade exige
decisão editorial específica sobre seleção/digests/destino, conforme tarefa5.3.

`LessonPracticePackageTests` compila o lote real e ensaia preview, recusa de
confirmação, importação e replay em PostgreSQL efêmero, mantendo todos os255 em
rascunho. Fixtures de navegador são sintéticas e isoladas; suas decisões editoriais
não aprovam conteúdo real nem escrevem no banco do operador.

Backup/restore local aceita explicitamente schema10 ou11 completos, registra a
versão real e mantém exigência da imagem exata e destino vazio. Schema desconhecido
ou incompleto é recusado. Imagens continuam excluindo fontes editoriais, testes,
pacotes, backups e segredos do contexto permitido.

Para testar manualmente após atualização local: abrir uma aula e iniciar prática;
responder e conferir que não há correção antecipada; voltar ao simulado e verificar
seu estado; encerrar prática e conferir a resolução; retornar à aula. Na conta,
retomar cada modo por seu histórico. Em celular, conferir mapa, foco/teclado e
confirmação final. Não publicar ou disparar review check até o aceite local.

## Habilitação para teste local

Em2026-10-24 o usuário autorizou habilitar os255 itens no operador local8088 e
definiu manter funcionalidades/conteúdos autorizados habilitados nesse ambiente
antes de testar/validar e enviar ao GitHub. A seleção exata foi submetida/aprovada
pelos UseCases editoriais; há cinco questões próprias por cada um dos51 assuntos.
Não é necessário pedir novamente aprovação local genérica para preparar o teste
do escopo autorizado. Registrar fingerprints/digests/destino continua necessário;
aceite final, revisão por modelo, merge e publicação seguem seus próprios marcos.
