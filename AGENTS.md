# Organização do projeto

## Specs, épicas e tarefas

- Toda spec OpenSpec usa `epic/<change-name>` como branch de integração.
- Uma tarefa ou grupo pequeno e coeso usa `task/<change-name>/<assunto>` e abre PR para a épica, nunca diretamente para `main`.
- Inclua código, testes e documentação necessários à tarefa no mesmo PR. Evite separar testes da implementação.
- Prefira PRs revisáveis: cerca de 300–500 linhas autorais quando possível; identifique lockfiles e arquivos gerados à parte. Não divida artificialmente uma mudança inseparável.
- Branches novas partem da épica atualizada. Tarefas dependentes aguardam os PRs anteriores; PR antecipado deve ser rascunho com dependências explícitas. Não marque tarefas integradas à épica antes da revisão/merge.
- Use uma issue da spec com checklist e links dos PRs. Registre tarefas implementadas/verificadas separadamente das integradas/aprovadas.
- Ao concluir, abra PR `epic/<change-name>` → `main` para revisão de integração. O diff final é cumulativo; os PRs menores são a evidência da revisão por partes.
- Nunca faça merge, feche PRs sem substituição documentada, arquive OpenSpec ou relaxe regras de deploy sem autorização.
- Preserve branches/commits anteriores ao reorganizar trabalho. Não apague implementação existente.

## CI e preview

- CI verifica builds, testes e regressão; não publica previews.
- Produção estática continua automática em push para `main`.
- Preview é manual: Actions → Publish GitHub Pages → Run workflow, executar o workflow de `main` e informar a branch desejada no campo `branch`.
- A referência do workflow é `main`, não a branch da tarefa: preserva as regras do ambiente github-pages. Antes do merge dessa automação, use a branch autorizada `infra/spec-task-workflow` como referência do workflow.
- Previews atuais são do material estático; não executam Java/PostgreSQL nem publicam a aplicação React. Deployment da aplicação completa terá spec própria.
- Nunca publicar segredos, arquivos `.env` ou dados privados. Preserve as aulas e os previews já armazenados.
