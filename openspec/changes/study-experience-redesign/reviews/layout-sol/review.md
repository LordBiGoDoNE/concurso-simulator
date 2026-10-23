# Revisão independente do lote — grupos2–6

Modelo `openai/gpt-6.1-sol#xhigh`, sessão `/root/study_layout_batch_sol`, contexto novo/somente leitura. Risco crítico confirmado. PRs #141/#142/#144/#145/#146 e combinação conferidos no snapshot do JSON, head65b53f7, base2e330716. Digest157214bytes, `2ab5226639f7b4f2d9379541e74570d0ef06a64d7dd7b80ca1eb88d22338181b`. Excluídos somente estes dois arquivos próprios.

## Achados e triagem

- LAYOUT-SOL-001, bloqueador confirmado: a fronteira da prática da aula não observa operação pendente da realização; pode descartar fila/comandos ao substituir. Corrigir e revalidar com gravação/fila/recarga/conclusão suspensas.
- LAYOUT-SOL-002, recomendação aceita: propagar realização ativa ao shell e aplicar cabeçalho/navegação reduzidos conforme desenho aprovado.
- LAYOUT-SOL-003, recomendação aceita: servir CSS com MIME correto no mock e conferir aplicação.

Nenhum desses itens está declarado resolvido neste commit. Merges bloqueados até correção, verificações e revalidação.

## Cobertura e limites

Revisor conferiu cada diff/combinação e arquivos relacionados, requisitos/arquitetura, identidade/contexto privado, composição, fila/idempotência, resultado/histórico/revisão, aulas/cargos/URLs/recursos/sandbox e capturas/foco/CSS. IDs51 compatíveis com manifesto;51 fontes sem scripts/517 referências locais existentes. Leu verificações documentais após análise semântica, sem certificar sua execução.

Não executou testes/build/rede/DB nem editou/delegou. Cota5h/semanal e tokens indisponíveis antes/depois; comparação mínima validada, sem delta atribuível ou isolamento garantido. Nenhuma comparação de modelos. Revisão do lote não substitui gate cumulativo em outro contexto nem aprovação do usuário.

## Correções preparadas para revalidação

O coordenador implementou os três itens (1c2b08d/49e8782, teste real no fabb593) e concluiu174 unitários,14 Chromium gerais,24 integrados OIDC/PG,5 de variações e nova execução Docker limpa1–8 com7 cenários de navegador. Cabeçalho reduzido e MIME/estilos conferidos nas novas capturas. Essa triagem não é conclusão independente: os itens aguardam revalidação de todos os novos diffs/combinação. O snapshot inicial e o resultado3 achados permanecem históricos.

## Revalidação independente concluída

SOL6.1xHigh, sessão independente original `/root/study_layout_batch_sol`: os três achados estão **fixed**, sem novos blocking/recommended/optional. Risco permanece crítico. Todos os cinco diffs e a combinação foram reexaminados/recalculados nos novos SHAs; base2e330716, head58cb5fb2644b6512345b97766aa4eee17d0942ab;180962bytes, digest `d5f2b6db510f34970294e10471613b568007dffefc051d190631d1e72b2f74c6`. Bases/heads/digests individuais constam no JSON.

A guarda consulta referências operacionais vivas e antecede confirmação/desmontagem; cobre aula, outra composição e histórico. Falha já encerrada permite decisão explícita; logout/troca de identidade conservam a limpeza privada. Menu reduzido e retorno do shell geral conferidos. MIME e aplicação real da folha corrigidos. Revisor examinou regressões pertinentes sem executá-las. Conferiu12 hashes de capturas e inspecionou realização/leitor em desktop/celular.

O diff entre fabb593 e o head analisado contém somente documentação/evidências, conferido por Git; isso não certifica a execução dos testes do coordenador. Revisor não fez testes/build/scripts/rede/DB/escrita/delegação. Não usou Graphify nesta rodada. Worktree limpo.

Snapshots mínimos antes/depois comparados por review_quota.py: janelas5h/semanal e tokens indisponíveis, sem estimativa/delta atribuível/isolamento garantido; nenhum outro modelo executado para comparação. Este parecer não autoriza merge nem substitui gate cumulativo em outro contexto. O registro inicial de1 bloqueador/2 recomendações permanece histórico verdadeiro.

## Nova alteração de leitor para revalidação

Código/título do assunto selecionado agora aparecem explicitamente junto ao botão e em sua descrição acessível. O teste navega M01→M02 dentro do material e confere que a prática anunciada como M01 preenche M01.174 unitários,build e14 Chromium gerais passaram. A mudança invalida a aplicação do parecer anterior ao novo snapshot; estado pending-revalidation até nova conferência independente. O gate cumulativo mantém seu próprio contexto/evidência.
