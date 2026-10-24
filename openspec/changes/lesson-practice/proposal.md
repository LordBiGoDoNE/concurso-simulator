# Proposal

## Why

Após testar o layout integrado pelo #147, o usuário apontou que Praticar aula o leva
à configuração de um simulado. O percurso atual mistura dois objetivos diferentes.
Em2026-10-24 solicitou separar prática de aula e simulado e escolheu explicitamente
ver a correção somente ao encerrar a prática. Em seguida solicitou questões próprias
para prática e simulado, evitando treinar no mesmo acervo usado para avaliação.

## What Changes

- Criar uma realização própria de prática vinculada à aula e ao cargo escolhido no
  leitor; não redirecionar para o compositor nem apresentar distribuição de disciplinas.
- Proposta para o primeiro lote: o card da aula anuncia um treino de até cinco
  questões fixas aprovadas. Praticar M01 é a ação explícita de início; o servidor
  escolhe min(5, elegíveis), todas do mesmo assunto/cargo, sem repetir a mesma base
  dentro da prática. Zero elegíveis produz estado vazio real, sem trocar o assunto.
- Separar o acervo por destinação exclusiva da questão-base: SIMULADO ou
  PRATICA_AULA. Uma base, suas revisões e variantes nunca alimentam ambos os modos.
  Proposta de migração: preservar as251 questões atuais para simulados e preparar
  novas questões autorais de prática, com revisão/decisão editorial próprias.
  O usuário aprovou em2026-10-24 cinco questões novas por aula:255 no total,
  sujeitas à revisão e decisão editorial específica antes da elegibilidade.
  Não copiar questões do simulado nem apenas alterar números/nomes para a prática.
- Manter escolhas e salvamento sem mostrar acerto, gabarito ou resolução durante
  a realização. Ao encerrar explicitamente, apresentar resultado e explicações.
- Identificar a modalidade no núcleo e na persistência. Práticas não entram no
  histórico ou nas métricas de simulados. Conta salva/retoma no histórico próprio
  de práticas; visitante mantém o acesso temporário da sessão, sem transferência.
- Preservar um simulado e uma prática em andamento independentemente ao navegar,
  inclusive filas e retries. Troca de identidade/expiração limpa ambos os contextos.
- Preservar os contratos existentes, capturas antigas, aulas, questões e regras
  editoriais. Alinhar README/configuração do OpenSpec ao ciclo pós-entrega #133 já
  estabelecido no AGENTS, sem alterar automação ou proteções.

## Capabilities

### New Capabilities

- `lesson-practice`: início por aula, realização/correção final e histórico próprio.
- `question-purpose`: acervos exclusivos de prática/simulado, com seleção e linhagem
  protegidas no núcleo/persistência e curadoria de conteúdo dedicado.

### Modified Capabilities

- `study-experience`: prática da aula passa a ter percurso e realização próprios,
  preservando o simulado ao alternar de área. Substitui o redirecionamento desse
  botão ao compositor; atalhos do painel de revisão fora da aula ficam fora do lote.

## Impact

Domínio/aplicação/adaptadores de tentativas e suas consultas, nova migração Flyway
sem reescrever V1–V10, DTOs/endpoints de prática, UI de aulas e fluxo próprio,
regressões Java/PostgreSQL/HTTP/React/Chromium/Docker, documentação e issue própria.
Catálogo/importação/editorial passam a preservar a destinação da questão-base;
contagens/seleção filtram por acervo antes de limitar/capturar. Novo grupo5 prepara
o conteúdo de prática; aprovação editorial no banco exige seleção/digests próprios.
Reutilizar seleção, captura, posse, comandos e correção onde houver semântica comum,
sem duplicar motor nem criar camadas apenas delegadoras.

Não inclui correção imediata, cronômetro, gamificação, adaptação, variações na
prática, reclassificação de tentativas antigas, publicação ou arquivamento. Não se
presume aprovação editorial de questões novas por aprovar código ou esta proposta.
Parte do acervo atual veio dos exercícios públicos das aulas; separar os modos
não desfaz essa exposição e não é promessa de sigilo/ineditismo dos simulados.
A proposta ampliada e o volume255 foram aprovados pelo usuário em2026-10-24.
A escolha explícita dos grupos completos1–5 do lote está pendente; nenhuma
implementação começou. O aceite não autoriza merges nem atos editoriais no banco
automaticamente. Aprovação editorial continua vinculada à seleção/digests concretos.
