# Rodadas de implementação e revisão

Decisão atual do usuário: entregar os grupos completos escolhidos para cada lote,
sem limite fixo de tarefas, em PRs coesos, sem pausas por subtarefa nem merges.
Se o escopo não vier explícito, listar grupos/tarefas pendentes e perguntar quais serão
feitos antes de começar. “Faça 3, 4 e 5” inclui todas as subtarefas desses três grupos;
não encerrar em 3.3 deixando 3.4 para depois. PRs podem ser pequenos, mas o lote só
termina com seus grupos inteiros implementados/testados/documentados/revisados.
Bloqueio real é reportado sem declarar conclusão ou ampliar escopo automaticamente.
Revisões externas acontecem depois da implementação do lote e
antes de pedir sua aprovação. [Procedimento](architecture/spec-review.md).

## Política atual — SOL em todas as revisões (2026-10-15)

- Somente SOL 6.1 xhigh revisa os lotes, incluindo os críticos e documentais.
  A decisão substitui a revisão dupla por risco de 2026-10-12. Registrar classificação/
  justificativa antes do disparo; dúvida adota crítico, sem acionar outro modelo.
- Segurança, concorrência, persistência/integridade e controles executáveis de proteção
  exigem cobertura/regressões adequadas. SOL examina os diffs completos por PR e a
  combinação, não um prefixo conveniente. O [procedimento](architecture/spec-review.md)
  detalha critérios, reclassificação e limites, sem garantir corretude por revisão única.
- O gate cumulativo final `[SPEC]` e o da épica geral também usam SOL 6.1 xhigh.
  Revisão de lote não os substitui; cada gate exige contexto independente próprio.
- Medir antes/depois de cada revisão SOL executada. Relatar
  ao usuário variação de cota por janela/fase e seus limites, não custo financeiro.
  Não disparar segundo modelo por risco/medição nem inventar comparação em revisão única.
- Triagem/correção/revalidação, diffs completos por PR/combinação, modelos sem fallback,
  autorização individual de merge e proibição de publicação/arquivamento presumidos
  permanecem. Esta decisão não inicia outra rodada nem reescreve revisões históricas.

## Histórico — rodada inicial de question-bank

- Cinco tarefas novas: 1.2, 1.3, 2.1, 2.2, 2.3; dependência existente: #38 (1.1).
- Usuário autorizou especificamente adotar #38 como camada inicial de stack nova,
  preservando seus commits/base/head. Sem rebase/retargeting/merge/fechamento.
- Trunk `epic/question-bank`; somente dependências reais, sem ampliar a stack sintética #35.
- Dois revisores independentes: Astra medium e SOL 6.1 xhigh; mesmas refs/requisitos,
  relatórios por PR e combinação. Correção exige revalidação do diff relevante.
- CI/testes do coordenador não são testes executados pelo revisor; fixtures não são
  aprovação editorial do acervo. Implementado/verificado não é aprovado/integrado.
- Relatório único ao usuário: PRs/SHAs/checks, achados/triagem, sobreposição/contribuição
  adicional e tokens registrados, com limites das estimativas.

## Comparação anterior — tarefa 1.1, #38

As duas revisões estáticas examinaram os oito arquivos, base `600cf6a`, head `cf857f1`,
digest `63a8ffc5d6e09707d81f56f25780b396172ae5212d56e836b2b44d70277dfddf`.
Nenhum achado em ambas. Compartilhamento da classificação foi conferido; identidade/
versão/contagem pelos filtros permanecem nas tarefas futuras. Nenhuma execução de testes
pelos revisores. Build anterior do coordenador: 66 testes, zero falhas/erros/skips.

| Modelo efetivo / sessão | Entrada nova | Cache read | Saída | Raciocínio reportado | Cache write |
| --- | ---: | ---: | ---: | ---: | ---: |
| Astra medium / `ses_ed3ab774bffeUSyFk8l4u3zRak` | 55.041 | 115.072 | 1.819 | 15 | 0 |
| SOL 6.1 xhigh / `ses_ed3ab04d2ffe7ULCQNA7Tbw9Xh` | 66.982 | 181.248 | 2.525 | 2.774 | 0 |

Seis chamadas registradas por revisor, via API de mensagens paginada, excluindo
coordenador. Houve sobreposição de conclusões, sem benefício acionável adicional
demonstrado nesta amostra; não generalizar equivalência/economia a outros PRs.

Este registro não substitui relatórios detalhados do lote, gate final `[SPEC]` ou
aprovação de merge. Revisão de lote não autoriza main/publicação/arquivamento nem
sync pós-merge de Stacks ainda não ensaiado. [Limites de Stacks](stacks-pilot.md).
