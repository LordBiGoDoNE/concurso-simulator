# Revisão independente de integração de spec

## Quando e por quê

PRs de tarefas são revisados individualmente e integrados na épica da spec. Depois,
`[SPEC] <Título>` propõe `epic/<spec>` → `epic/application-reformulation`. Este gate
examina o conjunto, não uma nova implementação. CI verde e revisão de tarefas não
provam que as responsabilidades e a integração estão corretas.

1. Abrir PR `[SPEC]` em rascunho com escopo, requisitos, evidências e pendências.
2. Escolher com o usuário o modelo/variante de raciocínio aprofundado; descobrir o ID
   disponível, sem presumir nome/capacidade/custo. Sem escolha/disponibilidade, parar
   a revisão, informar e manter relatório pendente. Não há fallback silencioso.
3. Capturar base/head imutáveis e snapshot. Disparar `spec-reviewer` em sessão nova,
   sem reutilizar sessão do implementador. O perfil OpenCode V2 é somente leitura e
   não fixa modelo: o coordenador deve selecioná-lo explicitamente ao disparar.
4. Fornecer spec aprovada, AGENTS.md, ADRs/padrões, commits e diff completo; permitir
   explorar código relacionado. Não fornecer conversa/defesas da implementação,
   relatórios de findings ou verification.md na primeira rodada. Decisões versionadas
   são a fonte de requisitos e não devem ser ocultadas.
5. Revisor analisa responsabilidades, invariantes, dependências, integração, contratos,
   segurança, privacidade, persistência/concorrência/migrações, testes e regressões.
   Ele reporta, não edita/aprova merge. O coordenador executa testes necessários e
   registra quem executou; neste perfil o revisor não tem shell nem ferramentas de escrita.
6. Triar achados com o usuário: corrigir, refutar com evidência ou adiar melhoria não
   bloqueadora. Mudança arquitetural exige decisão, não alteração silenciosa de escopo.
   Ausência fundamentada de achados é válida. Não inventar complexidade/findings.
7. Corrigir por commits/PRs revisáveis, adicionar regressões/regras quando viável e
   revalidar com o revisor. Qualquer mudança relevante requer novo snapshot; confirmar
   findings resolvidos não dispensa conferir o novo conjunto. Não copiar o digest atual
   para uma revisão antiga sem realmente revalidar.
8. Com relatório atual, triagem encerrada, sem bloqueadores pendentes e CI/gate
   aprovados, solicitar aprovação explícita do merge. Aprovação da spec não autoriza
   main, publicação ou arquivamento.

## Pedido inicial ao revisor

> Modelo/variante explicitamente aprovado: `<provider/model#variant>`.
> Spec: `<nome>`. PR: `<número>`. Base: `<sha>`. Head: `<sha>`.
> Revise o diff completo contra essa base e a combinação resultante. Confronte a spec,
> AGENTS.md e ADRs. Investigue o código necessário. Não modificar nem fazer merges.
> Não assumir conformidade por CI/aprovações anteriores. Retorne achados verificáveis,
> distinguindo bloqueador, recomendado e opcional. Inclua localização, evidência,
> requisito/ADR, impacto e sugestão; cobertura/limitações e testes não executados.

O coordenador prepara o diff desses commits (não só a lista de arquivos) e informa
se há arquivos binários/gerados ou limitações de contexto. Revisão parcial não pode
ser declarada revisão completa. A sessão/ID efetivo do modelo fica no relatório.

## Evidências e atualidade

Arquivos por spec: `openspec/changes/<spec>/spec-review.json` (gate) e
`spec-review.md` (explicação humana). Ambos começam **pending**, não executado.
Para gerar a identificação, na raiz do repo:

```sh
python3 scripts/check_spec_review.py --snapshot --base <base-sha> --head <head-sha> --spec <nome>
```

O snapshot contém base/head e SHA-256 do diff completo `base...head`, incluindo docs,
requisitos, CI e testes, excluindo **somente** esses dois arquivos de relatório.
A base deve ser a base atual do PR, não um merge-base antigo escolhido pelo autor.
O head analisado deve ser ancestral do head atual e conter o mesmo diff relevante.
Mudança de base exige revalidação mesmo quando o diff aparenta não mudar. Registrar
relatório e explicação pode gerar commits posteriores sem invalidar a revisão;
mudar qualquer outro arquivo, incluindo os guardas, exige nova revisão.

JSON versão 1, quando concluído:
- `status: completed`, `spec`, `pr`, `base_sha`, `reviewed_head_sha`, `diff_sha256`;
- `reviewer.model` (provider/model com variante se escolhida), `reviewer.session`,
  `reviewer.independent_context: true`, `reviewed_at` ISO-8601 com timezone;
- `coverage`: explicação não vazia para requirements, architecture, integration,
  security, persistence, tests e maintenance (inclusive não aplicável justificado);
- `limitations`: lista explícita; `verification`: lista de objetos com `command`,
  `result` e `executed_by` (não confundir testes do implementador com os do revisor);
- `findings`: lista, possivelmente vazia. Cada item: `id`, `severity` (blocking,
  recommended, optional), `location`, `evidence`, `reference`, `impact`, `suggestion`,
  `disposition` (fixed, rejected, deferred) e `resolution`. Bloqueador não pode deferred;
  correções/refutações exigem evidência no texto de resolution;
- `triage_complete: true`. Isso atesta triagem, **não a aprovação do usuário**.

O Markdown deve explicar escopo/cobertura, achados/triagem, evidências/limitações e
revalidação, usando os títulos do relatório inicial. Modelo ainda não escolhido:
`reviewer.model: null`, `status: pending`; nunca preencher conclusão fictícia.

## Check e proteção no GitHub

`Spec review gate` executa testes do guard em todos os PRs, inclusive alterações de
título/base. Na épica geral publica `spec-review`; nas tarefas, `spec-review-tests`,
para que sucesso não aplicável de tarefa não certifique revisão de spec no mesmo SHA.
Tarefas são não aplicáveis; na épica geral
exige branch `epic/<spec>` e título `[SPEC]`, evitando bypass só por renomear título/branch.
`main` → épica geral é sincronização do site existente, não integração de spec;
o check registra essa exceção e não autoriza seu merge.

O check exige relatório concluído, snapshot atual, cobertura e triagem registradas.
Uma regra GitHub na **épica geral apenas** exige PR e check `spec-review` da aplicação
GitHub Actions. Não alterar proteção de main/deploy nem regras existentes. A ativação
é registrada no PR; sem workflow integrado, o check fica ausente e bloqueia integração,
inclusive #25, até instalar o processo e executar a revisão.

Limites: o check valida evidência estrutural, não prova qualidade, identidade do modelo
ou autenticidade do relatório. Workflow/script são versionados no PR e também precisam
ser revisados: alteração maliciosa pode falsificar sucesso. Sem uma automação confiável
instalada na base/default branch não há attestation forte; não usar pull_request_target
para executar código não confiável. Administradores podem alterar/desativar regras.
Proteção não é garantia absoluta contra dono malicioso ou erro semântico. O mecanismo
reduz omissões normais, e AGENTS/revisão/aprovação continuam obrigatórios.
