# Design

## Context

Main4ea49a9 contém o layout #147. App.practiceLesson chama
AttemptWorkspace.practice e navega para Simulados; o compositor recebe uma questão
pré-preenchida. AttemptRow/me_attempt não distingue a finalidade. O histórico
privado e JpaCompletedAttempts consultam todas as tentativas elegíveis sem esse filtro.
Somente mudar textos/tela manteria práticas misturadas aos simulados.

A decisão explícita do usuário é separar os percursos e corrigir somente ao encerrar.
Quantidade até cinco, histórico próprio e isolamento das métricas são propostas
adicionais deste documento, sujeitas ao mesmo aceite antes da implementação.

## Goals / Non-Goals

Prática curta do assunto escolhido, início explícito a partir da aula, realização e
resultado próprios, dados privados isolados e simulado ativo preservado. Preservar
arquitetura do ADR0001 e checklist HTTP. Não criar motor de correção no navegador,
publicar gabaritos antecipadamente ou alterar a semântica de simulados antigos.

## Decisions

- A finalidade é imutável e protegida no domínio, não uma etiqueta arbitrária no
  frontend. Capturas e comandos preservam modalidade, assunto e elegibilidade.
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
- O card da aula informa até cinco questões fixas e cargo elegível. A ação Praticar
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

Primeiro aprovar proposta e escolher grupos completos1–4. Implementação/revisão de
cada grupo inclui seus testes/docs. O gate final permanece independente da revisão
do lote. Evidências não executadas ficam pending. Atualizar a issue a cada PR
alterado/integrado, fechar somente após resolver seu escopo. Sem merge, deploy,
arquivamento ou alteração do banco do operador por mera preparação desta proposta.

## Open Questions

Correção ao encerrar foi escolhida pelo usuário. Proposta adicional a aprovar:
até cinco questões fixas, separação do histórico/métricas e preservação simultânea
do simulado. A implementação aguarda a aprovação e seleção dos grupos completos.
