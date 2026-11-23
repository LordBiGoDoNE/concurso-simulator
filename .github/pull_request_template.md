## Resultado e escopo

<!-- Spec/issue, tarefas, base/dependências. Implementado não significa integrado. -->

## Responsabilidades e contratos

<!-- Backend HTTP: aplicar docs/architecture/http-endpoint-review.md.
Indicar quem decide (segurança/web/aplicação/domínio), DTOs/erros e exceções justificadas.
Para PR sem endpoints/backend, indicar não aplicável. -->

- [ ] Revisei responsabilidades e ausência de duplicação, não apenas resultados dos testes.
- [ ] Contratos, segurança/privacidade e dependências respeitam o ADR/checklist aplicável.

## Evidências

<!-- Comandos/resultados, regressões novas, alcance e limites dos testes; pendências explícitas. -->

- [ ] Código, testes e documentação necessários estão neste PR.
- [ ] Diferenciei verificação/CI de aprovação e integração; sem publicação/merge presumido.

## Integração de spec (somente PR `[SPEC]`)

<!-- Evidência vigente: openspec/changes/<spec>/review.md (~/.claude/rules/revisao-e-integracao.md,
seções 4, 5 e 7). Tarefas não precisam desta seção. Não marcar concluído o que não foi executado;
CI verde não é aprovação semântica. -->

- [ ] Revisão final independente executada em contexto novo sobre o diff completo da cadeia.
- [ ] `review.md` da spec linkado, com head revisado e tree SHA.
- [ ] Achados das revisões triados; bloqueadores/altos corrigidos e reverificados no delta.
- [ ] Merge aguarda a aprovação do pacote pelo usuário.

## Arquivamento (somente `chore/archive-*`)

<!-- Encerramento após o merge do [SPEC] (~/.claude/rules/revisao-e-integracao.md, seção 8, e
pendencias-e-encerramento.md). O job `openspec` falha se a change arquivada não tiver linha no registro. -->

- [ ] Diff de `openspec/specs/` conferido e registrado neste PR; nada fora de `openspec/` além dos documentos de estado.
- [ ] Registro de entregas e documentos de estado (seção "Documentos de estado" do `AGENTS.md`) atualizados neste PR.
- [ ] Issue da spec a fechar depois do merge, com os links do `[SPEC]`, do merge commit e deste PR.
