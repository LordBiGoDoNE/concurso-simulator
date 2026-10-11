# Piloto de GitHub Stacks — question-bank

## Escopo e autorização

O usuário aprovou o piloto e a proposta #31, integrada somente em `epic/question-bank`
como `952d445820cb0b1237d155e62b09610d3e62ee69`. Depois autorizou **abrir um PR**
de ajuste de processo; isso não aprova nem integra este ajuste automaticamente.
O ensaio remoto começa somente após aprovação/merge deste PR. Não é implementação
das 44 tarefas do banco nem autorização para encadear PRs reais de produto.

Exceção proposta à base de tarefas: dois PRs sintéticos, ambos em rascunho, no mesmo
repositório/spec, com um pequeno arquivo de documentação por camada:

```text
epic/question-bank (trunk)
  ← task/question-bank/stacks-pilot-lower
      ← task/question-bank/stacks-pilot-upper
```

PRs independentes e `epic/question-bank` → `epic/application-reformulation` não entram
na stack. Este último mantém título `[SPEC]`, revisão cumulativa independente e gate
próprios. Até validar o ensaio remoto e aprovar extensão do processo, tarefas de
produto seguem em PRs sequenciais para a épica. Nenhuma proteção será reduzida.

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
Estado: **etapa local aprovada; etapa remota pendente**. Sem revisão independente
cumulativa executada e sem evidência de compatibilidade completa.

## Etapa remota planejada — após aprovação deste ajuste

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
6. Em PR sintético apenas, alterar/restaurar a base para a épica com dependência
   explícita e registrar evento `edited`, diff temporariamente cumulativo e rerun do
   guard. Não usar essa manobra para simular aprovação ou mudar o PR `[SPEC]` real.
7. Conferir via API o ruleset da épica geral (check `spec-review`, app 15368, strict,
   sem bypass). Registrar limite: sem ensaio de merge, não demonstramos como o GitHub
   aplica todas as proteções a um prefixo de stack ou à fila. Não testar merge real.
8. Publicar evidências/limitações na issue #30. Se eventos/checks divergirem, parar,
   manter PRs em rascunho e propor correção revisável separada; não declarar adoção
   concluída. Retomar fluxo sequencial para o produto.

Sem `gh stack merge`, `submit --open`, prune, fechamento/deleção ou publicação.
Encerramento dos PRs sintéticos exige autorização adicional e substituição documentada
pelo relatório da issue. Para estender a exceção a tarefas reais, apresentar evidências
e solicitar aprovação própria do processo; este PR não concede essa autorização.

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
