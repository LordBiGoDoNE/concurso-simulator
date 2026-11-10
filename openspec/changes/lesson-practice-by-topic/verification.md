# Verificação documental — 2026-11-08

Entrega solicitada: regra permanente de planejamento → spec/issue → confirmação
separada para implementar, e formalização de lesson-practice-by-topic.
Acompanhamento: [issue #161](https://github.com/LordBiGoDoNE/concurso-simulator/issues/161).

## Escopo desta entrega

- Proposta, design, requisitos, tarefas e metadados OpenSpec.
- AGENTS.md e contexto do OpenSpec: criar/vincular spec e issue, apresentar grupos
  e obter confirmação explícita antes de implementar. Aprovação documental não
  concede autorização de desenvolvimento.
- Branch documental task/lesson-practice-by-topic/proposal e épica própria,
  baseadas na main/origin/main f61d57d5d55d6d61609815a2faa4b415ed8dca4b.
- Worktree isolado; alterações locais da jornada #158 não foram incorporadas ao
  commit documental nem sua branch foi reorganizada. A mesma regra foi espelhada
  localmente em AGENTS.md/configuração do workspace ativo para orientar sua continuação.

## Verificação

OpenSpec 1.14.1 `validate lesson-practice-by-topic --strict` e `git diff --check`
aprovados. Artefatos descrevem contratos, formatos históricos/novo, migração
futura, fluxo por assunto, testes e dependência da interface #158. Grupos 1–4
permanecem sem implementação/verificação/aprovação/integração.

A inspeção do commit e dos links remotos acompanha a publicação na issue #161.
Validação documental não é execução de testes de produto nem revisão semântica.
Risco crítico registrado no design refere-se à futura mudança de persistência,
replay, contratos privados e fila, não a implementação já executada nesta etapa.

## Ponto de parada

Não houve código de produto, migração, carga, alteração editorial, mudança de
runtime/VPS, publicação de aplicação, revisão por modelo, merge ou arquivamento.
Após publicar os documentos e conferir links, apresentar os grupos e perguntar
explicitamente se o usuário quer iniciar a implementação e quais grupos autoriza.
