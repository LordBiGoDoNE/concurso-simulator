# Revisão independente — optional-google-identity — PR #25

**Análise independente concluída; correções autorizadas e candidatas nos PRs #27/#28.**
Ambos aguardam aprovação/integração próprias e revalidação independente do conjunto.
Estado JSON permanece `pending`, `triage_complete: false`, com cinco achados abertos.
Não há aprovação da integração da spec na épica geral.

- Modelo escolhido/efetivo: `openai/gpt-6-astra#medium`.
- Sessão independente: `ses_ed9ab0a5affevkCQABqq93wgIK`, Explore somente leitura.
- Base analisada: `6664e2c516facf61169c52095c732a77ee68e0cf`.
- Head analisado: `fba7719ec9b4fd58ae5be4a52855290cab845129`, após merge aprovado #26.
- Digest relevante: `773682c4e010a356bc1fddb8d0bd0510cebb92bdab9935ad4cad608ef78473ea`.
- Registro do retorno final: 2026-10-10T15:12:51Z.

Perfil customizado está versionado com o modelo aprovado; o runtime desse perfil não
foi ensaiado. A sessão utilizou Explore com modelo explícito, sem fallback de modelo.

## Escopo e cobertura

Revisor leu o diff cumulativo **inteiro: 80 arquivos, 4.412 linhas**, 77 entradas na
primeira rodada e os três arquivos de evidência depois das conclusões iniciais.
Confrontou proposta/design/tasks/user-auth, foundation `bootstrap-application-stack`,
AGENTS.md, ADR 0001 e checklists. Cobriu responsabilidades/núcleo puro, JPA/Flyway,
atomicidade/concorrência/rollback, OIDC/PKCE/claims, privacidade/sessões/CSRF/CORS,
contratos/rotas/headers, frontend, fixtures, testes/browser, material/offline e CI/gate.
Também leu probes/resultados do coordenador; existência/execução de testes não foi
usada como substituto da revisão. Cobertura declarada pelo revisor: todos os 80 paths
do diff, mais os arquivos de contexto necessários à análise.

## Achados e triagem

**3 blocking e 2 recommended confirmados; nenhum optional. Todos abertos.**

### B01 — blocking — contrato genérico de auth/config

`backend/src/main/java/br/com/concursosimulator/identity/web/GoogleLoginController.java:15–17`
retorna `ResponseEntity<?>` com Map para contrato fechado. Viola AGENTS.md:42–44,
checklist HTTP e ADR 0001; não é vulnerabilidade/JSON incorreto demonstrado. Sugestão:
DTO web concreto e regressão da assinatura, sem Service/UseCase artificial.

### B02 — blocking — prontidão acoplada à sessão JDBC

`backend/src/main/java/br/com/concursosimulator/platform/infrastructure/SecurityConfiguration.java:56–62`
e `backend/src/main/resources/application.yaml:16–19`. Teste atual de perda de banco
(`ConcursoSimulatorApplicationTests.java:84–96`) só consulta sem cookie. H01 foi
confirmada: banco indisponível, sem cookie retorna 503 JSON DOWN; com cookie válido
autenticado ou ID desconhecido retorna 500 HTML. Stack passa pela leitura JDBC de
sessão. Viola contrato público de prontidão da foundation e status independente de
sessão do design. Sugestão: isolar sessão na infraestrutura, preservando probe real,
CORS/headers, e testar matriz cookies/UP/DOWN/prazo; não capturar tudo no controller.

### B03 — blocking — métodos OAuth passam antes da whitelist

`backend/src/main/java/br/com/concursosimulator/platform/infrastructure/SecurityConfiguration.java:48–53,60–68`
e `identity/web/GoogleLoginHandlers.java:67–75`. Com Google habilitado, entrada e
callback processam GET/POST/HEAD/OPTIONS/PUT com 302 apesar da whitelist GET-only.
Callback inválido executa handler de falha e invalida sessão; casos usaram CSRF
válido, **não há bypassCSRF/autenticação indevida/vazamento demonstrados**. Viola
negação técnica das demais rotas/métodos do design. Sugestão: restringir antes dos
filtros OAuth ou nos matchers/resolver, não em controllers, e testar rejeição sem
efeitos sobre sessão com provedor habilitado/desabilitado e com/sem CSRF.

### R01 — recommended — timeout termina antes do JSON

`frontend/src/identity-api.ts:3–10,17–21` e `frontend/src/LoginPanel.tsx:37–40,70`.
Timer é cancelado quando chegam headers; JSON pendente deixa config/me/CSRF e UI
presos. Viola intenção de recuperação/erros acessíveis de user-auth. Sugestão: prazo
até consumir JSON, diferenciando logout204 sem corpo, com regressões de recuperação.

### R02 — recommended — refresh atrasado sobrescreve logout

`frontend/src/LoginPanel.tsx:22–31,33–52`. Foco durante logout dispara me; resposta
autenticada tardia após 204 restaura texto conectado e botão Sair junto ao alerta sem
sessão. Não restaura sessão no servidor. Sugestão: invalidar operações anteriores e
coordenar refresh/logout, com promises determinísticas de regressão.

### Hipótese refutada — páginas geradas abertas

GET `/login`, `/login?error`, `/logout` retorna403 habilitado; GET/POST/HEAD/OPTIONS/PUT
retorna403 desabilitado. Não manter essa hipótese como vulnerabilidade. Matriz inteira
dessas páginas habilitadas não foi ensaiada. B03 trata rotas OAuth, problema distinto.

**Decisão do usuário: corrigir os cinco achados**, com regressões e sessões novas por
PR. Implementação candidata não fecha achado: todos permanecem open até integração
aprovada e revalidação independente. Nenhum bloqueador adiado ou aprovação presumida.

### Candidatos após autorização — piloto de sessões curtas

- [#27](https://github.com/LordBiGoDoNE/concurso-simulator/pull/27), backend B01/B02/B03,
  head `937860d6d1e124ffc4a4132f7b58831b95def8aa`, sessão nova
  `ses_ed97dd6d1ffeWbi2NVFMBjLuJC`.
- [#28](https://github.com/LordBiGoDoNE/concurso-simulator/pull/28), frontend R01/R02,
  head `91ffeeb5ebc2ce5346a864771b21869e895010a8`, sessão nova
  `ses_ed971ee8dffeURLAX4iZp3FoHQ`.
- Base comum `c3fb7f5`, PRs independentes para a épica da spec, em rascunho/sem merge;
  CI de ambos aprovado. Implementadores GPT-6.1 Sol high; revisor permanece Astra medium.
- Combinação apenas local no worktree `concurso-identity-review-candidate`, commit
  `2d70d67cd8ffbf87f4e65978e48dceee314782fd`; não publicada/não integrada. Coordenador
  repetiu **53 backend + 47 frontend + 6 UI/offline + 3 browser real + 18 guard = 127
  testes aprovados, nenhum ignorado**, builds/OpenSpec estrito e pacote de 108 páginas/
  1757 referências. As regressões dos candidatos detectaram implementação antiga.
- O offline ignorado na execução inicial do agente frontend por falta de STUDY_ROOT
  foi resolvido nessa combinação: pacote montado fornecido, seis testes UI/offline OK.

Medição solicitada pelo usuário passou a **tokens registrados**, não quota variável.
SQLite foi consultado somente leitura; números das sessões completas de implementação:

| Sessão | Chamadas | Entrada nova | Cache read | Saída | Raciocínio registrado |
| --- | ---: | ---: | ---: | ---: | ---: |
| Backend #27 | 44 | 96.789 | 2.378.240 | 13.046 | 9.926 |
| Frontend #28 | 21 | 46.032 | 547.584 | 8.674 | 2.980 |
| Coordenador, parcial até 13h17m39s UTC−3 | 19 | 82.124 | 3.348.864 | 8.771 | 4.828 |

Cache write zero; raciocínio separado sem soma presumida à saída. Cache read é soma
de uso repetido, não conteúdo único. Não inclui revisor ou uso externo nem determina
custo financeiro ou percentual da quota. As duas primeiras linhas não incluem o
coordenador; a terceira é parcial entre o pedido de implementação (12h48m40s UTC−3)
e a consulta, excluindo finalização posterior. São tarefas distintas da revisão anterior:
não é comparação controlada de economia. Coordenador ainda usa a sessão longa original,
e isso continua aparecendo no volume de cache reenviado; sessões novas não zeram seu uso.

## Evidências e limitações

Coordenador executou três probes backend e quatro frontend em clone isolado do head
analisado: `/tmp/opencode/concurso-pr25-review-probes-20261010`. Só adicionou testes
nesse clone; produção/original não alterados. PostgreSQL18.6/Testcontainers e OIDC
locais em portas efêmeras, sem contas/credenciais Google reais.

- Backend: status com cookie válido/desconhecido retornou500HTML (sem cookie503DOWN).
  Hikari timeout do probe2s; PG connect/socket2/3s. Tempos não são garantia de produção.
  Matriz OAuth habilitado302 em cinco métodos; desabilitado GET404/demais403.
- Frontend: config/me JSON pendentes e signal não abortado após6s; CSRF manteve
  Saindo desabilitado; promises controladas reproduziram refresh pós-logout contraditório.
- **PASSED dos probes caracteriza defeitos; não demonstra conformidade ou correção.**
- CI aplicação do head analisado aprovado: run38061810020. Gate run38061810036 falha
  corretamente com revisão pending. Não relaxar check para integrar.
- API ruleset24766642: active só épica geral, exige PR/check do app15368, strict=true,
  sem bypass configurado. Consultado pelo coordenador, não pelo revisor.

Revisor leu fontes/resultados mas não executou testes/Git/API nem editou arquivos.
Sem ensaio remoto de avanço/retargeting ou runtime de perfil customizado. Não houve
auditoria exaustiva de bibliotecas/todas as aulas. Smoke Google real continua obrigatório
antes do deployment público, sem bloquear reprodução desses achados locais.

## Revalidação

**Nenhuma correção integrada nem revalidação independente realizada.** Candidatos
implementados/verificados como acima, aguardando aprovações de #27/#28. Após merges
autorizados, capturar base/head/diff completos novos e revalidar com Astra medium;
testes/CI do candidato não substituem esse fechamento nem autorizam merge do #25.

Mudança de base ou diff relevante exige nova revisão; não atualizar digest
artificialmente. Só estes dois relatórios são excluídos para permitir registrar análise.
O commit que os registra não implementa correção nem autoriza integração.
