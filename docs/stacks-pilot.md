# GitHub Stacks — padrão de tarefas dependentes e evidência do piloto

## Escopo e autorização

O usuário aprovou o piloto e a proposta #31, integrada somente em `epic/question-bank`
como `952d445820cb0b1237d155e62b09610d3e62ee69`. O ajuste limitado #32 foi aprovado e
integrado como `07e1ddbbe735586aed501ecca71da08bcd4a28f4`; habilitou somente o ensaio
sintético. Após o relatório remoto, o usuário aprovou a adoção nas tarefas reais de
question-bank pelo #36, integrado somente na épica da spec como
`d04c6d74712128b716dc345bc8979a530c97c355`, e solicitou **o mesmo padrão para as próximas
specs**. A generalização segue por PR próprio para revisão/merge; não presume aprovação
desse diff nem de merges de stacks. Depois de integrado, o método não exige nova
autorização por spec, mas proposta e implementação continuam exigindo aprovação e
pedido próprios. A aprovação do fluxo não implementa as 44 tarefas de question-bank.

Exceção inicial ensaiada: dois PRs sintéticos, ambos em rascunho, no mesmo
repositório/spec, com um pequeno arquivo de documentação por camada:

```text
epic/question-bank (trunk)
  ← task/question-bank/stacks-pilot-lower
      ← task/question-bank/stacks-pilot-upper
```

PRs independentes e `epic/<spec>` → `epic/application-reformulation` não entram na
stack. Este último mantém título `[SPEC]`, revisão cumulativa independente e gate
próprios. #36 habilitou a adoção em question-bank; este registro estende o padrão a
dependências reais das próximas specs, após revisão/merge próprio, sem nova aprovação
do método por spec. Nenhuma proteção será reduzida.

## Etapa local executada

Extensão oficial `github/gh-stack` **v0.2.1 fixada**, GitHub CLI 2.97.0. Sem skill/alias
instalados. Evidência do coordenador em [issue #30](https://github.com/LordBiGoDoNE/concurso-simulator/issues/30#issuecomment-6110354975),
terminada em 2026-10-11T14:59:31.920388Z, no clone isolado
`/tmp/opencode/concurso-stack-pilot-20261011`, não no clone de implementação.

- `init --base epic/question-bank`, `add`, diffs por camada e catálogo `view --json`
  compartilhado entre worktrees: aprovados.
- `top --print-path`: localizou o worktree superior sem trocar os checkouts.
- `rebase --no-trunk` com dono superior sujo: bloqueou sem mudar os SHAs; o probe foi
  preservado por commit, não descartado. Com donos limpos, o rebase da cadeia passou.
- Commits pré-rebase preservados em refs `pilot-backup/*`; o head antigo deixou de
  ser ancestral do novo e o digest mudou após as alterações. Não transportar revisão
  antiga para o novo snapshot.
- `push`: passou **somente para remoto bare local**, push-url verificada. Não enviou
  branches sintéticas para o GitHub nem atualizou a trunk de produto.
- Guard com metadados sintéticos: tarefas não aplicáveis; integração na épica geral
  bloqueada por revisão pendente. As 18 regressões existentes passaram.
- Listagem remota de stacks às 14:59:57Z: HTTP 200, `[]`. Confirma acesso de leitura,
  não criação/merge/CI. Ruleset 24766642 continuou active, sem bypass.

Log local: `/tmp/opencode/concurso-stack-pilot-20261011-results.json`, SHA-256
`8db49b3aa2817225d5388533ee4495a1343be4aef1b199ffffc315c84ba6d2a6`;
runner `/tmp/opencode/run-concurso-stack-pilot.py`. Os artefatos locais não são
distribuídos neste PR; o registro na issue resume comandos, SHAs, resultados e limites.
Na conclusão dessa etapa, o ensaio remoto ainda estava pendente. Ele foi executado
depois, com os resultados/limites abaixo; não é evidência de compatibilidade completa.

## Procedimento do ensaio remoto — histórico

1. Conferir a épica atualizada e a versão fixada. Criar clone/worktrees isolados,
   duas branches sintéticas com commits pequenos e backups das refs. Não adotar,
   retargetar ou rebasear trabalho existente. Trunk explícita `epic/question-bank`.
2. `gh stack init --base epic/question-bank <inferior>`, commits,
   `gh stack add <superior>` e `gh stack submit --auto`, que cria novos PRs em rascunho nesta versão.
   Conferir bases, estado draft, dependências, composição e ausência de auto-merge/fila.
3. Registrar números, commits e diffs isolados; obter o payload real do evento de PR
   e os SHAs/ref usados no check. Não presumir que a base da camada, a trunk avaliada
   pelo GitHub e `github.base_ref` tenham sempre a mesma interpretação em Stacks.
4. Esperar CI de cada camada e registrar runs/checks: `spec-review-tests` nas tarefas,
   backend/frontend/browser/material. Sucesso não aplicável de tarefa não deve se
   apresentar como `spec-review` da integração. Não relaxar os checks para o piloto.
5. Corrigir a inferior por novo commit, salvar refs anteriores, manter donos limpos
   e ociosos, executar rebase da cadeia e push com leases. Conferir todas as refs e
   novos eventos `synchronize`, SHAs e CI; push da ferramenta pode ser parcial.
6. A tentativa planejada de alterar/restaurar a base sintética foi **bloqueada pelo
   GitHub**, sem mudança de base. Não repetir no produto: membros de stack não
   permitem retargeting manual. `edited` foi verificado por troca/restauração de título.
7. Conferir via API o ruleset da épica geral (check `spec-review`, app 15368, strict,
   sem bypass). Registrar limite: sem ensaio de merge, não demonstramos como o GitHub
   aplica todas as proteções a um prefixo de stack ou à fila. Não testar merge real.
8. Publicar evidências/limitações na issue #30. Se eventos/checks divergirem, parar,
   manter PRs em rascunho e propor correção revisável separada; não declarar adoção
   concluída. Retomar fluxo sequencial para o produto.

No ensaio, sem `gh stack merge`, `submit --open`, prune, fechamento/deleção ou publicação.
Encerramento dos PRs sintéticos exige autorização adicional e substituição documentada
pelo relatório da issue. Não integrar fixtures nem incluí-las em stacks de produto.

## Resultado remoto e limites

[Relatório do coordenador](https://github.com/LordBiGoDoNE/concurso-simulator/issues/30#issuecomment-6110900254):
stack **#35**, PRs **#33/#34**, ambos draft, mesma spec/repositório. Criação,
diff de um arquivo por camada, worktrees, correção inferior/rebase/leases/refs e CI
inicial/pós-rebase passaram. Heads finais `88b99160082148418cecde5f64e8caffbe055664`
e `9088aff1eab507a645d168c47485a2ff1313dbdc`. Runs do guard após rebase:
[38153323108](https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/38153323108)
e [38153323770](https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/38153323770).

Logs reais confirmaram base/head por camada e `spec-review-tests` não aplicável à
integração. `edited` por título passou nos runs 38153499944/38153520282; base não mudou.
Metadados REST e env derivados do evento foram registrados, mas raw GITHUB_EVENT_PATH
e subtipo do webhook não foram exportados. Não afirmar captura integral do payload.

Erro do retarget: `Cannot change the base branch because the pull request is part of
a stack.` Não houve unstack para contornar. Mesmo com diff superior idêntico após
rebase, head anterior deixou de ser ancestral; revisão SHA/digest não é transportável.
Ruleset 24766642 continuou active/strict, app 15368, sem bypass. Merge/fila/aplicação
das proteções de prefixo, retarget pós-merge, sync pós-merge e encerramento **não
ensaiados**. Não simular que o primeiro merge produtivo já é um caminho validado.

Artefatos locais: `/tmp/opencode/concurso-stacks-remote-evidence/`; resumo JSON com
SHA256 `cba7468665f96792c998983a08a4aa96f135befbfbd690b9ae1e2e3341051ab6`.
O relatório na issue preserva comandos, SHAs, links de CI e limites; artefatos não
distribuídos integralmente. Sem revisão independente cumulativa de question-bank.

## Procedimento padrão nas specs

1. Conferir pedido/escopo da tarefa, spec aprovada, épica atualizada e ferramenta
   v0.2.1 fixada. Para dependências reais, criar stack nova, **nunca ampliar a sintética
   #35 nem misturar specs**. Camada inferior
   parte da épica; superior da tarefa inferior realmente necessária. Todos os nomes
   são `task/<change-name>/<assunto>`, trunk explícita `epic/<change-name>`,
   nunca main/épica geral. Tarefas independentes abrem PRs diretamente para a épica.
2. Abrir camadas novas em draft, com dependências e testes/docs no mesmo PR. Conferir
   base/diff/composição após submit e registrar na issue de acompanhamento da spec.
   Conferir refs/checks de cada nova stack: o piloto não prova configurações diferentes.
   Tarefas independentes e
   PR `[SPEC]` seguem separados. Não criar dependência só para usar a ferramenta.
3. Após correções, preservar refs e assegurar worktrees limpos **e ociosos**; rebase e
   push com leases/ref remotos conferidos. Esperar CI de cada head atual, verificar
   `spec-review-tests`, invalidar/revalidar revisão desatualizada. Não usar prune.
4. Não editar a base de membro da stack nem contornar via unstack/link/modify.
   Converter/reorganizar PRs existentes requer autorização e histórico preservado.
   Se checks/metadados divergirem, parar e propor correção separada ou fluxo sequencial,
   sem fechar/substituir/retargetar PRs silenciosamente.
5. Antes de qualquer integração, apresentar prefixo exato, PRs, heads, checks e limites
   do caminho não ensaiado para aprovação explícita. Cada PR incluído precisa ser
   aprovado; não usar merge sem alvo/prefixo conferido ou auto-merge/fila/bypass.
   Não executar merge produtivo só como teste, nem presumir sync pós-merge seguro.
6. Depois de merge autorizado, parar antes de sync/reorganização pós-merge ainda não
   ensaiados e obter autorização específica. Conferir preservação de commits, nova
   composição/base/diffs/CI. Se indisponível, retomar sequencial por ajuste autorizado,
   sem relaxar regras. Integrado na épica é estado distinto de implementado/verificado.

O usuário aprovou o método como padrão, não implementação automática de specs/tarefas
ou grupos sem pausas. Não repetir pilotos/propostas de processo por spec somente por
mudar o nome: conferir compatibilidade e registrar limites; divergência real exige
ajuste revisável. O padrão não concede autorização de merges, encerramento dos PRs
sintéticos, deployment ou main. A revisão cumulativa Astra medium e gate do PR `[SPEC]`
permanecem obrigatórios antes da integração na épica geral.

## Revisão e colaboração

O catálogo da stack e os journals ficam no git common-dir. Locks coordenam processos
gh-stack, não outros agentes/Git/editores. Nunca sync/rebase enquanto um worktree
afetado estiver em uso, mesmo limpo. Preservar commits antes de reescrever histórico.

Rebase/retargeting exige conferir base/head/digest e revalidar evidência quando inválida;
CI de camada não certifica revisão de spec. Merge futuro de prefixo/topo requer aprovação
de **todos** os PRs incluídos e dos commits atuais. Sem auto-merge ou bypass.
Tokens continuam medidos por sessão; Stacks não garante economia ou qualidade semântica.

Referências: [CLI v0.2.1](https://github.com/github/gh-stack/tree/v0.2.1),
[GitHub Stacks](https://docs.github.com/en/pull-requests/reference/stacked-pull-requests)
e [revisão de spec](architecture/spec-review.md).
