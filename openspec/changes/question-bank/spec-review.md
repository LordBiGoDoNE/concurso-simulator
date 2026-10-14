# Revisão cumulativa de question-bank — SOL xHigh pendente

## Escopo e cobertura

PR **#63**, `[SPEC] Banco de questões versionado, curadoria e consulta pública`,
`epic/question-bank` → `epic/application-reformulation`. As 44 tarefas estão
implementadas/verificadas/aprovadas/integradas na épica da spec; falta o gate final.
Risco crítico: conteúdo/elegibilidade, persistência/transações/concorrência,
segurança/sessões e projeção sem campos corretivos. Main permanece fora da integração.

Snapshot candidato, **não certificado por revisão**:

- Base `f72fcc5d9c92c326ad789e5cf72eef54665913cf`.
- Head `28932edf719e4df9399944c00f831f6a80387f92`.
- Digest canônico `4cf769cf0c3cd97ba62acb8dbd6979f24e42e5b41f81feb8001a8409df5dc537`.

## Achados e triagem

**Sem parecer executado; lista vazia não significa zero achados.** Revisões SOL das
tarefas permanecem históricas e não substituem esta análise independente cumulativa.
Triagem pendente. Não há gate aprovado nem merge executado do #63.

## Evidências e limitações

### Tentativa histórica não executada

O catálogo local `opencode models` listou `openai/gpt-6-astra`. Perfil versionado
`.opencode/agents/spec-reviewer.md` fixa `openai/gpt-6-astra#medium`, somente leitura,
sem shell/testes/escritas/merges. Foi solicitado exatamente esse modelo/variante em
sessão nova `ses_ec3a808f9ffeOk1KG1AChTQ011`, sem conversa ou pareceres prévios.

A chamada retornou **provider.auth HTTP 401: Missing bearer or basic authentication
in header**, antes da análise. O catálogo demonstra disponibilidade nominal, não
autenticação funcional ou modelo efetivamente executado. Não se trocou modelo,
reutilizou credencial de outro aplicativo ou relaxou o perfil. Não houve revisão,
achados nem testes executados pelo revisor nesta tentativa.

Sessões ativas no OpenCode antes do disparo: vazias. Isolamento da conta não verificado;
observações mínimas de cota antes/depois registradas e validadas privadamente. Janelas
5h/semanal e tokens indisponíveis, sem delta ou custo estimado; diagnósticos privados
não publicados. A tentativa foi encerrada como falha, não revisão concluída.

### Escolha temporária do usuário — Codex

Em 2026-10-14, o usuário informou que está usando Codex, suspendeu temporariamente
Astra nesta validação e solicitou **SOL xHigh**. O perfil efetivo de execução
`spec-reviewer-codex` fixa `openai/gpt-6.1-sol#xhigh`; seu hash está no JSON.
Contexto novo/somente leitura, sem testes/build/escritas/merges/DB/Graphify. No Codex,
comandos estritamente de consulta substituem read/glob/grep quando necessário.
Não se alteraram permissões de aplicação/CI/deploy nem se executou outro modelo
por iniciativa do coordenador. A exceção vale para o gate #63, sem mudança permanente
presumida das demais specs. A nova revisão ainda precisa efetivamente executar.

## Revalidação

Conferir SOL6.1 xHigh no Codex, refs e snapshot atuais e iniciar revisão real em
contexto novo. Corrigir/validar os achados confirmados
e registrar o parecer somente após execução. Mudanças relevantes no diff exigem
revalidação; os dois arquivos deste registro são as únicas exclusões do digest.
CI verde das tarefas não dispensa o check `spec-review`. Sem merge, arquivamento,
publicação, fechamento da spec ou integração na épica geral/main.
