# Revisão independente — optional-google-identity — PR #25

**#27/#28 aprovados e integrados; cinco achados anteriores revalidados como fixed.**
**B04 blocking aberto na spec; correção autorizada e candidata no PR #29.**
Estado JSON permanece `pending`, `triage_complete: false`, aguardando aprovação/merge
do candidato e revalidação independente do conjunto integrado.
Não há aprovação da integração da spec na épica geral.

- Modelo escolhido/efetivo: `openai/gpt-6-astra#medium`.
- Sessão independente nova: `ses_ed74c3f8bffeDtZHRjlE71ZeV4`, Explore somente leitura.
- Base analisada: `6664e2c516facf61169c52095c732a77ee68e0cf`.
- Head analisado: `35883adae490a56bbbefe015229395bf55364614`, após merges #27/#28.
- Digest relevante: `8c83e6c8ceca2cf2a4ee83bf8688a77d112948fcc783562cd6b90a4ba03d9b46`.
- Registro do retorno final: 2026-10-11T02:09:43Z.
- Revisão anterior preservada: head `fba7719`, sessão `ses_ed9ab0a5affevkCQABqq93wgIK`,
  resultado histórico 3 blocking/2 recommended, agora corrigidos no escopo abaixo.

Perfil customizado está versionado com o modelo aprovado; o runtime desse perfil não
foi ensaiado. A sessão utilizou Explore com modelo explícito, sem fallback de modelo.

## Escopo e cobertura

Revisor leu o diff cumulativo **inteiro: 86 arquivos, 5.325 linhas**, 83 entradas na
primeira rodada e os três arquivos de evidência depois das conclusões iniciais.
Confrontou proposta/design/tasks/user-auth, foundation `bootstrap-application-stack`,
AGENTS.md, ADR 0001 e checklists. Cobriu responsabilidades/núcleo puro, JPA/Flyway,
atomicidade/concorrência/rollback, OIDC/PKCE/claims, privacidade/sessões/CSRF/CORS,
contratos/rotas/headers, frontend, fixtures, testes/browser, material/offline e CI/gate.
Também leu probes/resultados do coordenador; existência/execução de testes não foi
usada como substituto da revisão. Cobertura declarada pelo revisor: todos os paths
do diff, mais os arquivos de contexto necessários à análise.

## Achados e triagem

**Atual: B04 blocking aberto.** B01/B02/B03 canônico/R01/R02 fixed, sem achados opcionais.

### B04 — blocking — callback alternativo fora da whitelist

`backend/src/main/java/br/com/concursosimulator/platform/infrastructure/SecurityConfiguration.java:82–87,94–102`
e `identity/infrastructure/OAuthRequestMethods.java:9–12`. Matcher do filtro continua
wildcard `/login/oauth2/code/*`; cadeia de rejeição cobre só caminho exato Google.
Probe atual `/login/oauth2/code/other` sem código invalidou sessão autenticada; com
query de fluxo OIDC legítimo pendente, concluiu login nessa rota alternativa. Viola
negação técnica por padrão e delimitação de callback do design. **Não é identidade
forjada nem bypass state/nonce/PKCE.** Sugestão: matcher efetivo estrito do callback/
método, com regressões de paths alternativos/métodos/sessão/autorizações pendentes,
negando sem efeitos. Correção autorizada e implementada no PR #29, ainda sem merge;
descrição acima refere-se ao código atual da épica, não ao candidato corrigido.

### Fechamento independente dos cinco anteriores

| ID | Evidência de fechamento no head atual |
| --- | --- |
| B01 | DTO concreto GoogleLoginController:14–17 e regressão ArchitectureTests |
| B02 | SessionIndependentRoutesFilter:18–25 antes wrapperJDBC, cadeia stateless, testes registro/ordem/cookies UP-DOWN/headers/prazo |
| B03 | SecurityConfiguration:60–73 nega antes OAuth, com testes CSRF/provedor nos dois paths canônicos; não certifica paths alternativos, B04 aberto |
| R01 | identity-api:3–27 prazo headers+JSON/abort/limpeza; testes recuperação e logout sem corpo |
| R02 | LoginPanel:29–68 gerações/mutação e regressões de respostas tardias/sucesso/falha/retry |

Os blocos B01–R02 a seguir descrevem **defeitos históricos no head fba7719**, não o
código corrigido35883ad. Disposition atual desses cinco é fixed conforme tabela.

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
PR. Aprovações de #27/#28 aplicadas, integração/revalidação concluídas para esses cinco.
B04 permanece open até aprovação/integração do candidato e revalidação. Sem bloqueador adiado.

### Candidatos após autorização — piloto de sessões curtas

#### B04 — PR #29, ainda sem integração

- [#29](https://github.com/LordBiGoDoNE/concurso-simulator/pull/29), base `73180bf`,
  head `80be9b7a26a48137b56d197abc8a46a058ba4d62`, três arquivos/80 linhas adicionadas.
  Uma linha de infraestrutura com `loginProcessingUrl` exato; sem novo filtro/camada.
- Implementador sessão nova `ses_ed7307d39ffeTGAOUuxIk6JnnY`: 26 testes focais e
  clean build **59 testes, sem falhas/erros/ignorados**. Negativo retirando só linha
  adicionada: três das seis regressões falham com `/other`302; linha restaurada antes
  build final. Coordenador conferiu XMLs e executou **3 cenários browser reais**,
  OpenSpec strict e diff-check, todos aprovados.
- Regressões `/other` e `/google/extra`, GET/POST/HEAD/OPTIONS/PUT com CSRF válido,
  sessão autenticada e autorização pendente visitante/autenticada: 403 sem redirect/
  cookie/troca de token/criação de usuário, identidade/atributos preservados; callback
  canônico posterior conclui com PKCE. Não esgota variantes de método/caminho.
- Revisor em contexto novo `ses_ed72c4feaffefOMthLjzkHYXqP`, Astra `medium`, leu diff
  integral/contexto e depois evidências: **sem findings confirmados, candidato B04
  validado no escopo da tarefa**. Bytecode da dependência real Security7.1.1 extraído
  pelo coordenador confirma matcher instalado no filtro, sem override de endpoint;
  métodos ficam com cadeia existente. Revisor não executou testes/Git/API.
- Não é revisão cumulativa nova do #25, aprovação ou fechamento oficial de B04.
  Sem merge, publicação, arquivamento ou Google smoke real.
- CI do PR #29 aprovado: application run38106109008 backend/frontend/browser/material,
  spec-review-tests run38106108987. Este check de tarefa não satisfaz o gate do #25.

| Sessão B04 | Chamadas | Entrada nova | Cache read | Saída | Raciocínio registrado |
| --- | ---: | ---: | ---: | ---: | ---: |
| Implementador, nova | 21 | 57.312 | 726.912 | 5.278 | 970 |
| Revisor Astra medium, nova | 10 | 37.813 | 175.104 | 2.401 | 48 |
| Coordenador, parcial | 11 | 282.464 | 1.336.960 | 3.660 | 1.986 |

Consulta SQLite somente leitura2026-10-11T02:43:43.674Z; coordenador desde autorização
02:33:22.647Z, exclui registro/finalização posteriores. Implementador/revisor: sessões
completas. Cache write zero, raciocínio separado sem soma à saída; sem conversão quota/
custo ou comparação controlada. Snapshot cumulativo acima permanece35883ad: candidato
não integrado não atualiza digest nem fecha finding.

#### Histórico #27/#28

- [#27](https://github.com/LordBiGoDoNE/concurso-simulator/pull/27), backend B01/B02/B03,
  head `937860d6d1e124ffc4a4132f7b58831b95def8aa`, sessão nova
  `ses_ed97dd6d1ffeWbi2NVFMBjLuJC`.
- [#28](https://github.com/LordBiGoDoNE/concurso-simulator/pull/28), frontend R01/R02,
  head `91ffeeb5ebc2ce5346a864771b21869e895010a8`, sessão nova
  `ses_ed971ee8dffeURLAX4iZp3FoHQ`.
- Base comum `c3fb7f5`, PRs independentes aprovados pelo usuário e integrados somente
  na épica da spec: #27 `dba0d247883506e7fe44844020b5f202982ed88f`, #28 `35883ad`.
  Implementadores GPT-6.1 Sol high; revisor permanece Astra medium.
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

**Revalidação atual:** código/testes/docs/CI relevantes de35883ad são idênticos ao
candidato127testes, confirmado por Gitdiff excluindo apenas estes dois relatórios.
Não houve repetição redundante dessa execução idêntica; CI do head integrado também
passou aplicação/material/browser (run38103864534); gate38103864499 falha pending.

Três probes HTTP reais atuais em worktree isolado
`/tmp/opencode/concurso-identity-revalidation-probes`, apenas classe de teste nova:
GET páginas login/login?error/logout403 anônimo/autenticado, semHTML/redirect/cookie,
preservando sessão; inferência inicial de páginas abertas **retirada pelo revisor**.
Callbackother inválido302/me401; com querylegítima pendente302/me200. Callbackgoogle/
extra e entradaother403 preservam sessão. Fonte/XML lidos pelo revisor; execução pelo
coordenador, sem alteração de produção. Três probes caracterizam, não validam correção.

**Histórico da primeira reprodução no head fba7719:**

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

**Cinco achados corrigidos/integrados e revalidados; B04 novo aberto.** Captura do
novo snapshot só ocorreu após revisão independente do conjunto atualizado, não para
fazer check passar. Correção de B04 foi autorizada e passou regressões/revisão da tarefa
no PR #29; exige aprovação/integração e revalidação cumulativa antes de fechamento.
Nenhuma aprovação/merge do #29/#25/main/publicação/arquivamento.

### Tokens desta revalidação

| Sessão | Chamadas | Entrada nova | Cache read | Saída | Raciocínio registrado |
| --- | ---: | ---: | ---: | ---: | ---: |
| Revisor Astra medium, nova | 25 | 123.322 | 1.940.608 | 2.931 | 454 |
| Coordenador Sol high, parcial | 12 | 217.877 | 2.309.632 | 5.156 | 3.419 |

Consulta em 2026-10-11T02:09:43.971Z; coordenador desde aprovação às02:02:17.342Z,
exclui registro/finalização posteriores. Raciocínio separado sem soma à saída; cache
write zero. Sem conversão para quota/custo ou alegação de comparação controlada.

Mudança de base ou diff relevante exige nova revisão; não atualizar digest
artificialmente. Só estes dois relatórios são excluídos para permitir registrar análise.
O commit que os registra não implementa correção nem autoriza integração.
