# Revisão independente — subject-review, grupos3 e4

Revisão inicial e revalidação concluídas pelo SOL6.1 xhigh. Inicial: zero bloqueadores,
um recomendado e um opcional. Ambos confirmados/corrigidos; **nenhum achado aberto na
combinação revalidada**. Escopo completo3.1–3.4/4.1–4.4,16/24 tarefas verificadas,
8/24 integradas. Risco **crítico** por segurança/principal/sessão/CSRF,privacidade HTTP/
browser,capturas históricas e concorrência. Issue104,stack115,trunk epic/subject-review.

Sessão independente `/root/subject_round2_sol`, modelo `openai/gpt-6.1-sol#xhigh`.
Harness aceitou `model=gpt-6.1-sol`,`reasoning_effort=xhigh`,`fork_turns=none`;
perfil versionado coincide,sem fallback. Revalidação na mesma sessão;nenhum ID adicional
do provedor exposto. Registro temporal do coordenador,não telemetria do provedor.

## Composição e identidade

| PR | Base imutável | Head de fonte revalidado | SHA-256 |
|---|---|---|---|
| #111 | `a16b69acf3ffcb4f82eac87018fd1270fae4aea8` | `79dc2c4167d7435c0e9b8facd2fd8cdc09c4d440` | `976e9d1a74aac4de708dee1d5dab49df3fe0bab5ee99814e806a653e049bd802` |
| #112 | `79dc2c4167d7435c0e9b8facd2fd8cdc09c4d440` | `ebe169b8eeca4f28c3d9c2cad459eb38a9f09413` | `e07b01644c989405f233794ceabaff42dd4a167de87c3a7cf4170b539fa2285f` |
| #113 | `ebe169b8eeca4f28c3d9c2cad459eb38a9f09413` | `d41a8cbefb025c38c88fa344649a46a502b07e59` | `6394d3c8c476c20c8229ec6f16fac559f70971b67d72278038f4404e4f24606d` |
| #114 | `d41a8cbefb025c38c88fa344649a46a502b07e59` | `4b72f79901ded4425c0d14ddf2dbf3eff5394e77` | `edaedc164a65a8cb30cf7b70e2c1b26dcc564421e8725a1fa3a65844c3d052ba` |

Combinado `a16b69acf3ffcb4f82eac87018fd1270fae4aea8` → `4b72f79901ded4425c0d14ddf2dbf3eff5394e77`:
`f808636a2e8d3a1141a58ae23a1d1405d68b2c23d0c0b4fd4109cea07de9b33b`. Inicial tinha head792c0a95f99feea6eca7819e728d8a78e000bf91,
digest `ed65f89b23b515a9796bfcb4fb02e21b98fb94f7c7a2e4df164c3c38024f5480`;metadados integrais das duas fases no JSON.
Todos os cinco digests/merge-bases conferidos em cada fase,com argumentos canônicos.
Somente spec-review.json/md excluídos;verification-round2.md incluído criptograficamente.
Estes dois registros posteriores não integram o source analisado nem mudam produto.

## Achados e triagem

- **SR-R2-SOL-001,recomendado,confirmado/fixed em#114:** intenção de foco booleana
  sobrevivia à falha/cancelamento e podia focar o painel numa atualização automática
  após conclusão. Agora vinculada à consulta explícita,descartada em erro/inatividade/
  substituição. Regressão do App reproduziu o foco incorreto e passou após correção;
  teste de cancelamento e Playwright real reforçam o cenário. Foco explícito preservado.
- **SR-R2-SOL-002,opcional,confirmado/fixed em#114:** coerção String aceitava array em
  disciplineId. Tipo string agora verificado antes do enum;controles negativos para
  painel e individual falharam antes/passaram depois. Sem efeito visual/vazamento
  concreto identificado no caso original. Código não calcula correção no cliente.

Correções em4b72f79901ded4425c0d14ddf2dbf3eff5394e77. O **#113 isolado mantém os achados
originais**;histórico/head preservados. Este parecer sem achados abertos exige a entrega
conjunta **#111,#112,#113 e#114**, incluindo a camada corretiva. Não aprova prefixos.

## Cobertura

- **requirements:** Grupos completos3.1–3.4 e4.1–4.4, quatro diffs e combinação; proposta/design/specs versionados. Grupos1/2 lidos como pré-requisitos e5/6 não antecipados.

- **architecture:** Controllers/DTO/advice e fronteira Java puro/infra; UseCases, domínio/cálculo e produtor de evidência. Sem SQL/JPA ou autenticação manual no controller.

- **http_security:** Ator verificado antes do MVC, painel conta, principal inválido, posse/expiração/conclusão, entradas estritas,401/403/404/409/500/503,CORS e POST com CSRF preservado.

- **privacy_persistence:** Projeções sem correção/captura/identidade privada;no-store antes da segurança;503 pré-MVC; leitura limitada/transaçãoRR e validação em lote relacionadas, sem comandos novos.

- **interface:** Escolha explícita do cargo,51 assuntos, estados distintos,janela/corte/amostra/prioridade, individual sem histórico implícito, timeout e contexto vigente.

- **context_lifecycle:** App/workspace/conclusão/revisão; cancelamento,retry,limpeza por cargo/conta/logout, suspensão na revalidação e atualização após confirmação.

- **focus_and_types:** Intenção de foco vinculada à consulta explícita, descartada em erro/inatividade/substituição; disciplineId string antes do enum, sem coerção.

- **regressions:** Fontes HTTP/ArchUnit/cliente/componentes/contexto/Playwright e relacionados de cálculo/janela/concorrência/compatibilidade inspecionadas, sem execução pelo revisor.

- **maintenance:** Causa/regra positiva/regressões/limites das duas correções documentados; histórico e três diffs inferiores preservados, correções no topo da stack.

## Verificação do coordenador/CI

- Backend gradlew --offline build Windows: Sucesso3m23,243 casos,242 executados sem falhas/erros; um symlink omitido por permissão. Backend não mudou nas correções frontend. Responsável:coordinator.

- ReviewHttpTests/ArchUnit e PostgreSQL18.6 descartável: Primeira camada passou17s; nove testes HTTP ampliados passaram22s, incluindo falhas/capturas/CORS/imutabilidade/conclusão/logout/expiração. Responsável:coordinator.

- Regressões dos achados antes/depois: Três casos falharam no source inicial. Depois das correções137 testes frontend/15 arquivos e build TypeScript/Vite passaram; cancelamento adicional também coberto. Responsável:coordinator.

- Playwright real subject-review.real.spec.ts após correções: Cinco testes passaram26,4s, incluindo foco depois de falha/conclusão,390/1280px,51 assuntos,visitante,retry/logout. Seis capturas sintéticas inspecionadas localmente. Responsável:coordinator.

- Regressão browser e material local antes das correções: 18 identidade/simulados/revisão e5 parametrizados passaram;6 públicos/offline passaram,108 páginas/1.757 referências locais válidas. Responsável:coordinator.

- CI Linux source4b72f79 run38942150820: Todos os jobs aprovados. Backend243 PASSED,zero SKIPPED; browser19 identidade/simulados/revisão e5 parametrizados;6 públicos/offline. Responsável:github-actions-verified-by-coordinator.

- Artefatos/OpenSpec/diff-check: SPA sem sentinelas privadas sintéticas, JAR sem fixtures/.env/seleção editorial;OpenSpec1.14.1 strict6/6; git diff --check limpo. Responsável:coordinator.

- Recalcular cinco digests e conferir merge-base/ancestralidade: Os cinco digests conferem nas duas fases;792c0a9 é ancestral de4b72f79 e três diffs inferiores idênticos. Nenhum script do projeto executado. Responsável:reviewer.

Todos os checks dos quatro heads de fonte passaram:backend/frontend/study-material/
identity-browser/spec-review-tests. URLs atuais registradas no JSON e PRs/issue.
CI não demonstra sozinho qualidade semântica e não aprova merge.

## Limites e integração

- Revisão de lote3/4, não gate cumulativo final ou aprovação de merge. Grupos5/6 fora da avaliação de completude.

- Correções no#114: o#113 isolado permanece com os achados originais. Parecer sem achados abertos vale somente para o conjunto#111–#114, sem aprovação de prefixos.

- Revisor somente leitura local de código/diffs/hashes; não executou testes/build/aplicação/scripts do projeto/navegador/rede/DB/escritas/delegação nem verificou CI.

- Não leu semanticamente spec-review.json/md,verification*.md,reviews ou artefatos privados;verification-round2.md integra o digest. Exposição incidental a contagens em documentação versionada não foi usada como prova própria.

- Revisor não inspecionou capturas/artefatos nem uso real de teclado/leitor de tela. Inspeção visual e execuções acima são do coordenador/CI; sem auditoria WCAG integral.

- Requisição autorizada pode terminar após logout/expiração;não há revogação retroativa nem prova de todos os schedulings. Cliente cancela/ignora resposta fora do contexto.

- Testes HTTP pausam após capturar evidência;teste relacionado do lote anterior cobre intervalo entre cabeçalhos/carregamento na transação. Reinícios de contexto não equivalem a processo/JAR.

- Contexto novo reduz viés sem garantia absoluta. Sem outro modelo ou comparação fictícia. Cota/tokens/observações privadas não publicados/inventados.

- Os dois registros reviews/round2 são posteriores ao source revalidado e não se declaram incluídos no digest. Não acompanham mudança de código/requisito.

Sem merge/sync/retarget/bypass/fila,deployment,main ou arquivamento executados por esta
revisão. Aprovação dos quatro PRs é decisão própria do usuário;gate cumulativo permanece
pendente para o término da spec. Evidências anteriores preservadas como histórico.
