# Gate cumulativo — lesson-practice-by-topic

Estado do gate: **pending**, pois ainda não existe PR final real. A **revisão
semântica cumulativa independente foi concluída, sem achados verificáveis**.
Isso não autoriza merge nem permite declarar validate_report aprovado com PR fictício.

## Escopo e cobertura

Spec #161, grupos 1–4, risco crítico. Base main
`16ca8aef382d64e7652b280b413122c357775ead`; head real da épica
`850c81a858c5ed3fd6ef757ae97c9394ff342bfc`; digest canônico
`d32450e611c56536e2db8368abc4754ab49b7157aa26e761e9e0b593a90cd969`.
52 arquivos do diff completo e combinação resultante.

Revisor `/root/spec_reviewer_cumulative_161`, contexto novo (`fork_turns=none`),
separado da implementação e do lote. Seleção explícita `gpt-6.1-sol`/`xhigh`,
correspondente ao perfil aprovado `openai/gpt-6.1-sol#xhigh`, somente leitura.
A análise semântica inicial terminou antes de ler relatos locais ou a revisão do lote.

Conferidos requisitos (assunto sem cargo, pool aprovado/fixo/exclusivo e 1–5 bases),
fronteiras Java puro/portas/JPA/transações, contratos HTTP/OpenAPI/frontend,
segurança/CSRF/posse/modalidade/expiração/correção privada, V14/formatos/replay e
compatibilidade, testes estáticos e manutenção/backup/editorial. Cobertura detalhada
em spec-review.json. A revisão de lote não foi usada para certificar o gate final.

## Achados e triagem

Nenhum achado bloqueador, recomendado ou opcional verificável identificado.
Triagem sem pendências. A conferência documental posterior não trouxe achados
adicionais. Ausência de achados não é autorização de merge nem garantia de corretude.

## Evidências e limitações

O revisor conferiu 37 hashes do manifesto local, ancestralidade e git diff --check
contra o conteúdo commitado. Não executou testes, build, app ou OpenSpec; não
acessou rede/DB/CI/refs remotas/logs privados e não escreveu. Contagens de testes e
aceite são relatos do coordenador, não execuções certificadas pelo revisor.
O conflito 409 de navegador é injetado; concorrência real possui testes PG
inspecionados. Retorno com cargo público incompatível está implementado, sem E2E
específico dessa alternativa encontrado pelo revisor. A ferramenta não retornou
atestado adicional de modelo efetivo nem telemetria de tokens; nada foi estimado.

PR #175 integrado por autorização explícita, merge commit 850c81a, head dc45568
com todos os checks aprovados. O coordenador conferiu árvore idêntica ao merge e
main ainda em 16ca8aef. PR/branch documental #162 preservados, sem fechamento.

A revisão automática de aprovação bloqueou abrir o PR final [SPEC] para main por
considerar essa publicação separada da autorização de merge #175. Nenhum PR final
foi criado; pr permanece null e validate_report não foi executado com número
fictício. Os dois arquivos de gate estão preparados apenas localmente. Publicá-los
na épica e abrir o PR final serão apresentados como ações concretas para autorização.
Main, arquivamento e deploy continuam sem execução. #173, grupos 2/8, aguardam main.

## Revalidação

O digest foi apurado independentemente no snapshot real base/head acima e confere
com o do coordenador. Nenhuma mudança de produto foi feita depois da revisão.
Só estes dois arquivos podem mudar sem invalidar o digest. Após autorização da
publicação e criação do PR real, conferir novamente base/head/ancestralidade/digest,
registrar seu número e executar validate_report com os valores reais, além dos
checks atuais. Mudança em qualquer outro arquivo ou na base exige revalidação
semântica, ainda que o diff pareça idêntico. Este registro não antecipa tal validação.
