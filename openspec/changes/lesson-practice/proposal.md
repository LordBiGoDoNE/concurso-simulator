# Proposal

## Why

Após testar o layout integrado pelo #147, o usuário apontou que Praticar aula o leva
à configuração de um simulado. O percurso atual mistura dois objetivos diferentes.
Em2026-10-24 solicitou separar prática de aula e simulado e escolheu explicitamente
ver a correção somente ao encerrar a prática.

## What Changes

- Criar uma realização própria de prática vinculada à aula e ao cargo escolhido no
  leitor; não redirecionar para o compositor nem apresentar distribuição de disciplinas.
- Proposta para o primeiro lote: o card da aula anuncia um treino de até cinco
  questões fixas aprovadas. Praticar M01 é a ação explícita de início; o servidor
  escolhe min(5, elegíveis), todas do mesmo assunto/cargo, sem repetir a mesma base
  dentro da prática. Zero elegíveis produz estado vazio real, sem trocar o assunto.
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

### Modified Capabilities

- `study-experience`: prática da aula passa a ter percurso e realização próprios,
  preservando o simulado ao alternar de área. Substitui o redirecionamento desse
  botão ao compositor; atalhos do painel de revisão fora da aula ficam fora do lote.

## Impact

Domínio/aplicação/adaptadores de tentativas e suas consultas, nova migração Flyway
sem reescrever V1–V10, DTOs/endpoints de prática, UI de aulas e fluxo próprio,
regressões Java/PostgreSQL/HTTP/React/Chromium/Docker, documentação e issue própria.
Reutilizar seleção, captura, posse, comandos e correção onde houver semântica comum,
sem duplicar motor nem criar camadas apenas delegadoras.

Não inclui correção imediata, cronômetro, gamificação, adaptação, novas questões,
variações na prática, reclassificação de tentativas antigas, publicação ou arquivamento.
A implementação e o lote dependem da aprovação desta proposta e da escolha explícita
dos grupos completos. O pedido não autoriza merges automaticamente.
