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
