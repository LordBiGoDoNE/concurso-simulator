# Verificação local —2026-10-24

Estado: implementação disponível para aceite local, sem push, PR de implementação,
merge, revisão independente ou gate cumulativo executados nesta rodada. A proposta
remota #149 e a issue #148 permanecem no estado anterior até retomar o fluxo remoto.
Todos os grupos1–5 foram autorizados; as etapas de revisão/integração seguem pendentes.

## Código e execução

Imagem/commit local: `fba7bea2c57ff5710be8207fdbf1f83e90153cac`.
Origem do operador: `http://127.0.0.1:8088`. Main/stable não foram alteradas.
As imagens foram construídas de `git archive`, com contexto permitido e exclusão
real de testes/editorial/backups/segredos; sem exportar conteúdo privado à imagem.
O código de produção não mudou após esse commit; ajustes posteriores são de teste,
rótulo de relatório e documentação. O teste de retry do navegador passou a conferir
201 na criação e200 no replay, refletindo o contrato real; não foi alterada a API
para acomodar o teste.

Atualização do operador: backup privado V10 prévio; migração aditiva paraV11; mesmo
container/volume PostgreSQL. Comparação normalizada de todas as linhas de negócio
originais confirmou igualdade após migração e preservação após importar candidatos.
Na comparação de `qb_question`, a nova coluna gerada `purpose` é excluída para
conferir os valores antigos. Flyway é verificado separadamente; sessões não entram
no hash de negócio pois têm ciclo técnico próprio. Não houve restauração in-place.

As251 bases elegíveis de SIMULADO e três famílias parametrizadas foram preservadas.
255 bases próprias foram inicialmente importadas como DRAFT. Após pedido explícito
do usuário para habilitar tudo localmente, as255 receberam SUBMIT/APPROVE pelos
UseCases editoriais, com fingerprints exatos, exclusivamente nesse banco local.
BackupV11 foi criado antes da habilitação. O launcher deliberado foi executado em container separado com
rede do PostgreSQL local e configuração privada; nenhuma credencial é publicada.

## Evidências locais

| Verificação | Resultado e alcance |
| --- | --- |
| Backend completo |257 testes,49 classes, sem falhas; domínio/aplicação/JPA/HTTP real/ArchUnit e compatibilidade1/2. PostgreSQL18.6 efêmero. |
| Regressão HTTP adicional |3 testes de prática passaram, incluindo recusa400 de destinação/IDs/quantidade arbitrários sem gravar tentativa. |
| Interface |178 testes em20 arquivos; build TypeScript/Vite passou. |
| Chromium de layout |8 casos passaram em390/1440px; prática própria, fila do simulado preservada, aulas/foco/leitura. Outros testes da suíte inicial não alterados; um teste de login com Google permanece condicional ao ambiente. |
| Docker/imagens |JAR/SPA sem fixtures/deps de teste, usuários sem root,51 aulas acessíveis, falha de API limitada. |
| Docker/runtime |Banco vazioV11, CSRF/cookies, isolamento de portas, falha/retomada do PostgreSQL e reinício passaram. |
| Docker/navegador |9 casos passaram com conta/visitante, conflito, duas sessões/404 de outra conta, desktop/celular, replay e correção final da prática. |
| Docker/recuperação |Backup/restoreV11 em destino novo, integridade das capturas/históricos, cookies restaurados revogados e reinício passaram. Dados/sessões sintéticos; sem restaurar banco do operador. |
| Scripts |4 testes editoriais,5 guardas de backup/restore,4 guardas do contexto de imagens passaram. |
| OpenSpec |`validate lesson-practice --strict` passou. Informa que o delta de study-experience ainda não pode ser arquivado enquanto a spec anterior estiver em changes; nenhum arquivamento foi solicitado/executado. |
| Operador real8088 |251 questões públicas padrão,51 aulas, tela própria, zero empréstimos entre acervos, simulado preservado e sem overflow em390px. |

Logs gerados/ignorados ficam em `backend/build/lesson-practice/`. A exportação Docker
foi criada sob `.local-app-export-lp-fba7bea2/` no workspace principal para evitar
paths excessivamente longos no Docker Desktop. O teste de status de replay foi
corrigido na cópia de teste dessa exportação; o binário exercitado permaneceu o da
imagem acima. O journal privado do operador fica em
`backend/build/local-app/operator-updates/2026-10-24-lesson-practice/` no workspace
principal. Nenhum dump/cookie/session fixture/segredo acompanha esta documentação.

Para operar esta cópia temporária com as ferramentas V11 antes de atualizar main,
o launcher privado no workspace principal é
`python backend/build/local-app/lesson-practice-operator.py status|start|stop|backup`.
Ele usa o código do worktree e o estado existente, mantendo os guardas de propriedade.
Não usar o backup da main antigaV10 para esse bancoV11. O restore continua exigindo
imagem exata/destino novo, sem downgrade para binário incapaz de ler snapshots3.

## Conteúdo e decisões pendentes

A seleção dos255 itens tem digest
`6b5054ab68526793e0a11d8f2cc49f9ad8b6ab2f7b6b3d0619b762f6287b43c2`.
Cinco por cada um dos51 assuntos;56 cálculos com oráculo exato; nenhum par sinalizado
pela triagem textual final frente aos509 candidatos legados válidos e entre os255.
Essa triagem não garante originalidade semântica nem substitui decisão editorial.
Pacote, fingerprints, fontes, respostas e resoluções foram apresentados na seleção
local gerada pelo compilador. Elaboração assistida por IA, sem revisão humana
independente; importação validada nos contratos Java/Python e em banco efêmero.

O usuário autorizou expressamente habilitar os255 itens locais para teste e
estabeleceu a orientação de manter funcionalidades/conteúdos do escopo autorizado
habilitados no local antes do envio ao GitHub. As decisões foram aplicadas pelo
DecideQuestionUseCase, sem alteração direta dos estados por SQL. A seleção e os
fingerprints continuaram idênticos aos apresentados; não há revisão humana
independente ou aceite para publicação implícito nesse registro.

A conferência da aplicação real passou para102 combinações assunto/cargo: cinco
questões próprias nos cargos elegíveis e zero no cargo incompatível. O Chromium
exercitou P01, M01 e S01 com conteúdo real; conferiu correção somente ao encerrar,
retorno à aula, preservação do simulado ativo e ausência de overflow em390px.
As251 questões de simulado e as capturas/tentativas anteriores foram preservadas.
Evidências privadas: enabled-local-practice.json e enabled-browser-smoke.json no
journal do operador, sem credenciais publicadas. Nenhuma base SIMULADO foi aprovada
novamente ou modificada na habilitação.

Depois do teste/aceite do usuário, retomar publicação dos commits em PRs revisáveis,
revisão independente SOL6.1xHigh e gate cumulativo separado, com riscos/triagem/cota
quando observáveis. Nenhuma revisão/modelo/sessão/cota/tokens foi inventada. A
classificação de risco é crítica por persistência, privacidade e migração; não
aciona outro modelo. Aprovação local não presume merge, publicação ou arquivamento.

## Preparação do envio autorizado

Depois do aceite local, o usuário autorizou envio e merge. A proposta#149 foi
revisada e integrada à epic/lesson-practice em37b71a2; tarefas publicadas na
stack#153, PRs#150/#151/#152, mantendo os commits locais originais. A revisão
inicial crítica do lote confirmou dois bloqueadores, registrados historicamente
em reviews/batch-1/initial.json, sem certificar o gate final.

Correções:23 itens editoriais reescritos e comparação ampliada às250 revisões
curadas atuais; seleção corrigida a47c4eae5b122b35cac8b9a51fb094e98a5e8bc0348d556ddfdf12b75014ecaf.
A origem estável das bases é mantida. O retry de criação agora respeita o
salvamento pendente da prática anterior.179 testes da interface e build passaram,
incluindo regressão da fila; o teste real de login passou a conferir o aviso
dos dois modos e a ausência de transferência, em vez do texto antigo singular.
A revalidação do lote e o gate cumulativo continuam separados. A seleção6b5054ab
anterior é histórico da primeira habilitação local, não prova da versão corrigida.

A comparação ampliada da primeira revalidação identificou41 pares remanescentes.
Todos foram reescritos com nova pergunta/operação, registrados em
editorial/lesson-practice/2026-10/replacement-review.json. A seleção atual é
9f44d2652b71433dfe9edc3da9dd57cce797d105484a9b2bd726bf058fde3f4a.
A triagem tem regressão de referências Excel após casefold, totalizando7 testes
Python editoriais. O registro da revalidação anterior é histórico; a rodada
seguinte e o gate final não são presumidos. A sincronização de AttemptContext
agora espera um botão acessível após conferência de acesso, mantendo todas as
assertivas de preservação da fila e de criação única.

A segunda revalidação confirmou as41 substituições e manteve três pares
remanescentes, agora alterados conforme additionalReplacements no ledger. A
seleção vigente é beee31e08d6348ae2cb86ca634f741e55fdb0e951885b73d5f6396e611d3c05b.
O compilador passou a gerar IDs por chave/conteúdo, sem posição da alternativa;
a regressão permuta a ordem e o índice correto e exige identidade estável por
texto. Foram executados8 testes editoriais, com55 oráculos aritméticos no lote
atual (uma subtração tornou-se uma tarefa de pertinência por elementos). Não se
presume que fontes editoriais públicas sejam secretas. A nova rodada independente
deve conferir essas alterações, além das correções já preservadas.

## Conferência da seleção vigente e integração das tarefas — 2026-10-25

A seleção beee31e08d6348ae2cb86ca634f741e55fdb0e951885b73d5f6396e611d3c05b
foi aplicada no PostgreSQL do operador por UseCases de importação/decisão, com
novas revisões e preservação das capturas e dos dados anteriores. Backup privado
anterior à atualização; sem atualização direta dos estados editoriais por SQL.
Continuam255 práticas elegíveis e251 questões de simulado; nenhuma base SIMULADO
foi alterada. O journal privado released-local-practice.json registra a seleção.

Chromium na instância real8088 conferiu102 combinações assunto/cargo, fluxos
P01/M01/S01, IDs/revisões correspondentes ao pacote vigente, ausência dos antigos
UUIDs posicionais nas opções novas, correção somente ao encerrar, retorno à aula,
preservação do simulado e largura móvel. Oito testes editoriais passaram, com55
oráculos; os dois LessonPracticePackageTests foram forçados com --rerun e passaram,
pois os arquivos editoriais externos não são inputs declarados do task Gradle.
CI faz clean build. Os179 testes React e build também passaram.

A revalidação independente do lote concluiu sem achados abertos no conjunto
2440b44/4f2400e/7b7a787. Registro em reviews/batch-1/revalidation-3.json; não é o
gate cumulativo final. PRs #150/#151/#152 integrados pela stack153 em8cdd977,
com árvore idêntica à revisada e ancestralidade conferida. Todos os checks verdes.
Cota/tokens indisponíveis, sem estimativas; nenhuma comparação com outro modelo.

## Correções do gate cumulativo — 2026-10-25

O gate independente inicial em68d40dc encontrou LP-GATE-001 (três pares
editoriais equivalentes) e LP-GATE-002 (cobertura arquitetural recomendada).
O registro inicial é histórico. Foram substituídos28 itens após conferência
ampliada dos255 enunciados/respostas frente ao inventário atual curado, incluindo
pares entre disciplinas. O ledger cumulativeGateReplacements registra hashes
e a operação nova. Seleção vigente:
fdf1c00e2959629d5f945846f4aa08dc70f76aefda4631f05136d6073fa96da8.
Fontes primárias pertinentes consultadas; não há garantia semântica exaustiva.

O controller de prática entrou no teste arquitetural específico já existente.
Passaram15 testes de arquitetura,8 testes editoriais/55 oráculos e os dois
LessonPracticePackageTests forçados novamente no pacote vigente. Os guardas
estruturais protegem dependências/contratos conhecidos, não toda responsabilidade
semântica. Revalidação cumulativa obrigatória após estas correções.

A seleção corrigida foi habilitada no PostgreSQL do operador usando os UseCases
existentes e expectativa da revisão anterior, após backup privado. Preservados
as251 bases de simulado, todas as linhas antigas de revisões/capturas e255
práticas elegíveis. O Chromium conferiu102 combinações assunto/cargo e conteúdo
real em P01/P09/I02/M01/S01, incluindo IDs/revisões novos, correção final e
preservação do simulado ativo. Journals gate-local-practice.json e
enabled-browser-gate.json são privados e não versionados. Runtime permanece na
imagem anterior: a correção altera conteúdo editorial e um teste, sem código de
produção Java/React. Backup não substitui as credenciais/configurações locais.
