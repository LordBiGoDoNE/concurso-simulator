# Gate cumulativo — lesson-practice / PR#154

## Escopo e cobertura

Revisão independente de todo o diff main→lesson-practice em contexto novo,
separado do lote. Modelo efetivo selecionado no dispatch: gpt-6.1-sol/xhigh,
perfil openai/gpt-6.1-sol#xhigh; sessão /root/spec_reviewer. Risco crítico por
persistência/migração, concorrência/replay, sessão/posse/privacidade e elegibilidade.
Base `4ea49a90007ff6ac218ce55183cdde4be605eccf`; head revisado `4393ba7941382480e005677940b7ef3b8e7c77c6`;
digest `3a09c55554c2b35ad70ebbda8552d9580e9b86c15bdc75f397906c5e769f3f23`. Conferência final2026-10-25T04:55:24Z.

Foram examinados requisitos, fronteiras do núcleo/adaptadores, HTTP/CSRF/posse,
V11/formatos1–3/JSON antigo, filas/identidade, históricos/métricas e operação.
Leitura dos255 itens e ampliação ao legado completo;32 pares da última rodada
revalidados. O corretivo#155 também foi examinado isoladamente contra68d40dc.
Código/editorial/testes preservados por identidade na última revalidação documental.

## Achados e triagem

- LP-GATE-001, bloqueador editorial: corrigido. Três pares iniciais e outros32
  confirmados na ampliação ao legado foram substituídos. O corretivo inclui28
  substituições preventivas/iniciais e32 posteriores. Hashes/tarefas no ledger;
  o revisor conferiu conteúdo/contrapartes e não apenas suas declarações.
- LP-GATE-002, recomendado: corrigido. Controller de prática incluído no teste
  arquitetural específico, sem inventar regra global.

Sem novos achados confirmados na revalidação atual.255 itens/51 assuntos/57
checkExpr. Seleção editorial vigente:
`1c7ff1ad0a916d5edbfb9a58f1a6302484667ca86cc0748a8ec66cbce7125ae1`.
Não limitar a distinção editorial às bases APPROVED nem inferir DRAFT/rejeição
pela ausência no manifesto. Capturas e bases SIMULADO preservadas.

## Evidências e limitações

O revisor executou somente consultas de código/Git e hashes/snapshot. Não fez
testes/build/compiler/rede/DB/navegador/escritas nem consultou CI/operador.
O coordenador verificou8 testes editoriais,57 oráculos,15 arquiteturais e2 testes
Java de pacote forçados na seleção vigente, além da CI e regressões anteriores.
Todos os13 checks de#155 em1d24ddb passaram; checks do head final são conferidos
separadamente antes de merge. spec-review-tests não certifica revisão semântica.

No operador local8088, nova seleção aprovada/habilitada pelos UseCases após backup,
255 práticas e251 simulados preservados. Chromium:102 combinações e10 assuntos,
IDs/revisões atuais, correção final/retorno/celular e simulado ativo preservado.
Dados/dumps/cookies/credenciais/journals privados não são publicados.

Fontes primárias pertinentes foram consultadas pelo coordenador; a leitura do
revisor não certifica atualização externa. Leitura e triagem textual não garantem
ausência exaustiva de equivalências semânticas. Cota5h/semanal e tokens não foram
observáveis no harness, nas fases inicial/revalidações; sem estimativas ou
comparação com outros modelos. Não houve fallback para outro modelo.

## Revalidação

Gate inicial68d40dc/digest81d66ac8 encontrou dois achados. Revalidaçãoc8c9c7d/
digest6616a690 confirmou32 pares adicionais e a correção arquitetural.
Em1d24ddb/digest12665566, ambos os achados foram confirmados corrigidos.
A revalidação atual4393ba7 conferiu o novo digest completo e a identidade de
todo o restante, após atualizar somente tasks.md/local-validation.md. Os blocos
locais e relatórios anteriores preservam o histórico, sem certificar versões futuras.

Somente estes dois arquivos de evidência são excluídos do digest. O coordenador
executa validate_report com base/head/PR reais, confere ancestralidade e digest
após incorporar#155 e todos os checks atuais antes do merge autorizado de#154.
A autorização vigente do usuário cobre envio e merge da entrega completa; o
parecer do revisor não concede autorização. Sem publicação externa/Pages ou
arquivamento; stable e branches/commits anteriores preservados.
