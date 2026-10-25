# Tasks

Proposta ampliada aprovada em2026-10-24; todos os grupos1–5 autorizados para implementação local. Pedido do usuário:
separar prática de aula e simulado; decisão confirmada de correção somente ao encerrar.
O usuário também solicitou questões próprias por modo; proposta ampliada para
acervos exclusivos, com cinco questões novas por aula255 no total aceitas. Todos os grupos completos1–5 foram escolhidos para implementar e testar localmente.
Aceite da proposta não concede aprovação editorial a itens ainda não redigidos.

## 1. Núcleo e persistência — base dos demais grupos

- [x] 1.1 Modelar modalidade da tentativa e destinação exclusiva da base/revisões/variantes, início por assunto/cargo e política até cinco/correção final.
- [x] 1.2 Persistir ambas as distinções por migração aditiva e importação/curadoria, preservando legado como SIMULADO, formatos/comandos e transações/replay.
- [x] 1.3 Filtrar acervo antes de contagem/seleção/captura e separar histórico/métricas antes de paginação/limite; manter conta/visitante/expiração.
- [x] 1.4 Concluir testes de domínio/aplicação/PostgreSQL/compatibilidade/ArchUnit, docs e revisão independente do grupo.

## 2. HTTP e privacidade — depende de1

- [x] 2.1 Expor DTOs/endpoints próprios de prática, disponibilidade e histórico, preservando consulta pública padrão de SIMULADO e contratos/erros tipados.
- [x] 2.2 Integrar a cadeia de segurança real: posse, visitante, sessão, CSRF, acesso por modalidade e respostas no-store.
- [x] 2.3 Verificar criação/retry/conflito/salvamento/conclusão, bloqueio de gabarito antecipado e cruzamento de modos/acervos por IDs ou parâmetros.
- [x] 2.4 Concluir regressões HTTP/PG, checklist de responsabilidades, documentação e revisão independente do grupo.

## 3. Interface própria — depende de1–2

- [x] 3.1 Iniciar prática pela aula com cargo e contexto claros, carregamento/retry/sem questões, sem passar pelo compositor.
- [x] 3.2 Implementar realização/correção final/histórico próprios e retorno à aula, com salvamento confirmado.
- [x] 3.3 Preservar simulado e prática independentes, filas/identidade/logout/respostas tardias e troca explícita de prática.
- [x] 3.4 Concluir regressões React/Chromium, teclado/foco/desktop/celular, documentação e revisão independente do grupo.

## 4. Verificação integrada e gate — depende de1–3 e do conteúdo aprovado de5

- [x] 4.1 Ensaiar migração/legado em PostgreSQL isolado, separação disjunta de bases/variantes/contagens, modos/contas/visitante e paginação de históricos/métricas.
- [x] 4.2 Verificar Docker aula→prática→correção final com acervo próprio aprovado, zero empréstimos entre modos, simulado preservado e todas as51 aulas.
- [ ] 4.3 Consolidar regressões/limites/procedimento de atualização e executar gate cumulativo independente SOL6.1xHigh.
- [ ] 4.4 Conferir checks/SHAs/triagem/cota, solicitar merges específicos e atualizar a issue após cada mudança/integração.

## 5. Acervo exclusivo de prática — volume255 aprovado; integração depende de1–2

- [x] 5.1 Preparar cinco questões autorais novas por cada um dos51 assuntos (255), com cargos elegíveis/fontes mapeados e conteúdo distinto do acervo SIMULADO, após escolha deste grupo no lote.
- [x] 5.2 Revisar enunciados/alternativas/resoluções/referências e duplicatas/linhagem, registrar identidade/destinação/fingerprint e cobertura dos51 assuntos.
- [x] 5.3 Validar os pacotes/importação em banco isolado, sem aprovação automática, apresentar seleção exata/digests e obter decisão editorial específica antes de tornar elegível no destino acordado.
- [x] 5.4 Concluir testes/auditorias/editorial, documentação e revisão SOL6.1xHigh; verificar disponibilidade própria por aula sem modificar as251 bases atuais ou as aulas.

Todos os grupos incluem código, testes e docs necessários. PRs pequenos dividem
entrega, não o compromisso do grupo. A aprovação da proposta não presume escolha
do lote nem autorização de merges; estes estados ficam separados na issue.


Decisão de execução em2026-10-24: todos os grupos1–5 autorizados. Desenvolver,
testar e apresentar localmente antes de novos pushes/PRs/revisões com modelo.
Commits locais são permitidos para proveniência/build. Revisões independentes e
gates continuam pendentes até estabilizar o resultado local e autorização de envio;
não presumir review check, integração remota ou elegibilidade editorial do conteúdo.

## Estado local histórico —2026-10-24

Caixas marcadas representam implementação/verificação local, sem integração ou
aprovação de revisão. Cada grupo mantém a revisão independente pendente. O código
Docker está em8088, com251 questões de simulado preservadas e255 práticas
habilitadas para teste por autorização explícita do usuário. A seleção exata e seus
fingerprints foram preservados. Disponibilidade conferida para102 combinações de
assunto/cargo e fluxo positivo com conteúdo real verificado em P01/M01/S01, com
correção final e preservação do simulado. Conta/visitante e históricos também foram
verificados em Docker isolado.4.3/4.4 e revisões de1.4/2.4/3.4/5.4 ficam após aceite local, conforme
pedido expresso de não subir/disparar modelos enquanto o usuário ajusta o resultado.

## Integração das tarefas — 2026-10-25

Após o aceite local, o usuário autorizou envio e merge da entrega completa. Os
PRs #150, #151 e #152 foram revisados no lote SOL6.1xHigh, com achados corrigidos
e revalidação concluída nos heads 2440b44/4f2400e/7b7a787. Todos os checks estavam
verdes. A stack153 foi integrada atomicamente à epic/lesson-practice em
8cdd97787c4183279652eb3948b5e25d367b23ae, preservando a árvore revisada e os commits.
Nenhuma troca manual de base ou sincronização pós-merge foi executada.

As tarefas1.4/2.4/3.4/5.4 estão concluídas e integradas na épica. Os itens4.3/4.4
continuam pendentes do gate cumulativo independente e da integração final à main.
O bloco local anterior é histórico, não o estado atual da revisão.
