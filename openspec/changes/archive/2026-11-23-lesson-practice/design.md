# Design

## Context

Main4ea49a9 contém o layout #147. App.practiceLesson chama
AttemptWorkspace.practice e navega para Simulados; o compositor recebe uma questão
pré-preenchida. AttemptRow/me_attempt não distingue a finalidade. O histórico
privado e JpaCompletedAttempts consultam todas as tentativas elegíveis sem esse filtro.
Somente mudar textos/tela manteria práticas misturadas aos simulados. A seleção
EligibleRevisionPool.capture e a consulta EligibleQuestions hoje não distinguem
a destinação da questão; separar só as tentativas continuaria reutilizando o acervo.

A decisão explícita do usuário é separar os percursos e corrigir somente ao encerrar.
Quantidade até cinco, histórico próprio e isolamento das métricas integram a
proposta aprovada pelo usuário em2026-10-24.
O usuário também pediu acervos próprios por modo. A proposta preserva as251 bases
atuais para simulados e prepara novas bases exclusivas de prática. O usuário aceitou
cinco questões novas por aula255 no total. Isso aprova o planejamento do volume,
sem conceder elegibilidade editorial a conteúdo ainda não redigido/revisado e sem
autorizar mover/descartar questões já aprovadas.

## Goals / Non-Goals

Prática curta do assunto escolhido, início explícito a partir da aula, realização e
resultado próprios, dados privados isolados e simulado ativo preservado. Preservar
arquitetura do ADR0001 e checklist HTTP. Não criar motor de correção no navegador,
publicar gabaritos antecipadamente ou alterar a semântica de simulados antigos.

## Decisions

- A finalidade é imutável e protegida no domínio, não uma etiqueta arbitrária no
  frontend. Capturas e comandos preservam modalidade, assunto e elegibilidade.
- A questão-base tem destinação exclusiva e imutável SIMULADO ou PRATICA_AULA,
  independente da modalidade da tentativa. Revisões, proveniência e variantes de
  uma mesma base herdam essa destinação; retirar/reaprovar/renumerar não permite
  cruzar os modos. Não criar categoria AMBOS ou fallback ao acervo contrário.
- Migração aditiva preserva todas as questões existentes como SIMULADO, sem mudar
  conteúdo, decisões editoriais ou capturas. Novas questões de prática têm identidade
  e resolução próprias. Importação/curadoria registra e confere destinação; não
  reclassifica uma base por enviar outro pacote/namespace ou criar outra revisão.
- Seleção e contagens de elegíveis aplicam destinação + aprovação + assunto/cargo
  antes da contagem/amostragem/captura. Simulado fixo e parametrizado recebem só
  SIMULADO; prática só PRATICA_AULA. Revalidar o acervo na transação de início.
  Disponibilidade pequena/vazia nunca toma questões emprestadas de outro modo.
- Consulta pública/compositor existentes conservam o acervo SIMULADO por padrão;
  disponibilidade da prática é consultada no contexto próprio. Contagens de prática
  não aumentam silenciosamente o total elegível do simulado. O cliente não escolhe
  livremente a destinação e não injeta IDs do outro acervo na criação.
- O conteúdo dedicado passa por checagem de duplicatas exatas/similaridade e revisão
  editorial: não aceitar cópia renomeada nem mera troca de valores de uma questão
  do simulado como nova questão de prática. Algoritmo é apoio, não prova de ausência
  de semelhança conceitual. Questões podem ensinar o mesmo conceito por problemas
  distintos. Registrar proveniência, referências e digests sem atribuição oficial falsa.
- Nenhuma questão nova fica elegível por geração/importação/testes. Preparar lote
  versionado e relatório por assunto, revisar resolução/alternativas/classificação,
  apresentar seleção exata/digests ao usuário e só então executar eventual decisão
  editorial especificamente autorizada no banco/destino combinado.
- Reutilizar o motor de tentativa para invariantes comuns quando adequado. O início
  da prática é uma ação relevante própria: recebe assunto/cargo e commandId; o
  servidor controla política de até cinco, elegibilidade e captura transacional.
- Novas tentativas de prática são distinguíveis por persistência e contratos.
  Migração aditiva classifica os dados existentes como simulado, sem inferir pela
  quantidade nem alterar respostas/capturas. Compatibilidade dos formatos1/2 e dos
  comandos antigos deve ser demonstrada. Não reescrever migrações anteriores.
- Idempotência inclui a finalidade; replay após resposta perdida retorna a mesma
  prática. Reuso de commandId em outro modo/critérios é conflito, sem cruzar dados.
- Endpoints próprios sob `/api/v1/lesson-practices` atendem criar, consultar,
  salvar/limpar, concluir e ler resultado. Histórico privado em
  `/api/v1/me/lesson-practices`. DTOs concretos; controllers traduzem HTTP e recebem
  identidade validada. UseCases não acessam JPA/JDBC nem dependem de Spring/HTTP.
- Posse, modalidade e expiração são verificadas no núcleo/adaptador de aplicação
  antes de leitura/escrita/replay. Usar id de prática nas rotas de simulado e vice-versa
  não entrega conteúdo nem grava alterações; resposta indisponível404.
- A cadeia Spring Security cuida de autenticação/identidade visitante, CSRF e acesso
  técnico. Visitante preserva a janela temporária de24h; conta salva/retoma por sua
  identidade. Sem importação do visitante, tokens manuais ou storage de respostas.
- Listagens e métricas de simulados filtram a finalidade ANTES de ordenar/paginar/
  limitar. O histórico próprio de práticas mostra aula/cargo/status; a revisão de
  dificuldades de simulados não incorpora práticas nem exclui simulados históricos.
- O card da aula informa até cinco questões fixas do acervo exclusivo de prática
  e cargo elegível. A ação Praticar
  inicia o fluxo próprio, com carregamento/falha/retry/zero elegíveis claros. Não há
  quantidade por disciplina nem envio automático ao abrir/ler/trocar uma aula.
- A realização mostra assunto/cargo/progresso e ações de responder/limpar/encerrar.
  Confirmação de encerramento informa respondidas/em branco. Resoluções e resultado
  aparecem somente após conclusão confirmada pelo backend. Voltar para a aula e
  repetir uma prática são ações explícitas; repetição não promete questões novas.
- Simulado e prática têm estados/transportes separados. Iniciar prática não desmonta
  simulado nem abandona sua fila; iniciar outro treino exige preservar/confirmar a
  substituição da prática anterior e respeitar suas operações pendentes.
- Navegação para aulas/início/simulados preserva ambos; troca de conta/logout limpa
  ambos e rejeita respostas tardias. O aviso antes de login contempla prática visitante.
- Os atalhos Praticar do painel de revisão de simulados mantêm seu contrato atual;
  não ampliar essa mudança automaticamente para outro produto/fluxo.

## Risks / Trade-offs

Risco crítico: finalidade influencia posse/replay, consultas históricas/métricas,
compatibilidade de capturas e persistência. Regressões devem demonstrar ausência de
vazamento entre modos/contas, gabarito fechado até concluir e paginação correta.
Os exercícios HTML já públicos são origem de parte das questões hoje usadas nos
simulados. Os acervos exclusivos impedem repetição entre os dois modos novos, mas
não tornam esse material anterior secreto ou inédito. Remover exercícios das aulas
ou substituir todo o acervo de simulados não está autorizado nesta mudança.
Acervo pequeno limita a prática; não duplicar base, usar assunto vizinho ou chamar
falha de acervo de indisponibilidade genérica. Sessões técnicas não são entidades.
A migração exige ensaio isolado e procedimento de atualização do operador antes de
subir um novo schema no banco com questões; aprovação de implementação não é restauração.

## Migration Plan

Nova épica `epic/lesson-practice` parte de main4ea49a9. Proposta é PR documental próprio
para a épica; tarefas dependentes, quando escolhidas, usam Stacks da mesma spec.
A capacidade study-experience ainda está no diretório changes da spec anterior,
sem baseline canônica em openspec/specs. O validate estrito aceita a proposta, mas
informa que seu delta MODIFIED só poderá ser arquivado após a baseline existir.
Isso não autoriza arquivar a spec anterior nem declarar esta proposta pronta para
arquivamento; qualquer consolidação exige autorização própria e nova conferência.

O PR final `[SPEC]` integra diretamente em main, após revisão cumulativa independente
SOL6.1xHigh e autorização específica. Não reutilizar a reformulação encerrada, não
sincronizar a stack histórica do layout, não modificar stable/Pages.

Proposta atualizada e volume255 aprovados em2026-10-24; todos os grupos
completos1–5 foram escolhidos pelo usuário para desenvolvimento local primeiro. O antigo grupo4 continua sendo verificação/gate, agora dependente
também do novo grupo5 editorial para cobertura real completa. Implementação/revisão de
cada grupo inclui seus testes/docs. O gate final permanece independente da revisão
do lote. Evidências não executadas ficam pending. Atualizar a issue a cada PR
alterado/integrado, fechar somente após resolver seu escopo. Sem merge, deploy,
arquivamento ou alteração do banco do operador por mera preparação desta proposta.

## Open Questions

Correção ao encerrar, até cinco questões fixas exclusivas por prática, separação de
histórico/métricas, preservação simultânea do simulado e volume255 (cinco por aula)
integram a proposta aprovada pelo usuário. Todos os grupos completos1–5
do lote foram escolhidos; implementação/redação são feitas e testadas localmente; seleção e digests concretos serão
apresentados para a decisão editorial específica antes da elegibilidade no banco.


Decisão de execução em2026-10-24: todos os grupos1–5 autorizados. Desenvolver,
testar e apresentar localmente antes de novos pushes/PRs/revisões com modelo.
Commits locais são permitidos para proveniência/build. Revisões independentes e
gates continuam pendentes até estabilizar o resultado local e autorização de envio;
não presumir review check, integração remota ou elegibilidade editorial do conteúdo.

Implementação local: destino derivado da identidade imutável do namespace de origem; coluna gerada por V11 e filtro antes de captura/contagem. Detalhes e limites em `docs/lesson-practice.md`.
