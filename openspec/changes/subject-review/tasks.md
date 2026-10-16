# Tasks

Issue #104. **Proposta aprovada, #105 integrado em `0bd6652`.**
Grupos 1 e 2 integrados por #106–#109 em a16b69a, com autorização e CI pós-merge verde.
10/24 tarefas implementadas/verificadas, 8/24 integradas/aprovadas.
Segundo lote autorizado pelo usuário em 2026-10-16: grupos completos 3 e 4.
Decisão de granularidade: 51 assuntos existentes, escolhida pelo usuário em 2026-10-16.
Primeiro lote escolhido pelo usuário em 2026-10-16: grupos completos 1 e 2.
Grupos 5 e 6 ficam fora do segundo lote; tarefa verificada não significa revisão/merge aprovado.
Cada grupo escolhido inclui todas as suas subtarefas, código/testes/documentação,
revisão SOL 6.1 xhigh e PRs coesos. Dependências reais usam Stacks da mesma spec.

## 1. Domínio, políticas e UseCases

- [x] 1.1 Modelar evidência concluída e janela por conta/cargo, identidade-base e classificação capturada; testar V5/fixa/gerada, ordenação e deduplicação antes do agrupamento; documentar política subject-review/v1.
- [x] 1.2 Implementar métricas por assunto e revisão individual com Java puro; testar 51 assuntos, zero/null, brancos, arredondamento e exposições independentes do denominador; documentar amostra e limitação.
- [x] 1.3 Implementar prioridade com mínimo de três bases/fração exata/desempates; testar poucos dados, todos certos, empate e erros+brancos; documentar heurística sem selo de domínio.
- [x] 1.4 Implementar UseCases/portas de painel e revisão individual; testar isolamento de cargo/conta, conclusão/posse/expiração e falhas sem falso vazio com portas substituíveis; documentar dependências.

## 2. Leitura PostgreSQL e compatibilidade — depende de 1

- [x] 2.1 Oferecer evidência concluída pelo contrato de aplicação de mockexam com adaptador JPA, filtro antes de limite 100 e cabeçalho 101/hasOlder; testar mais de 100, cargos, empate e planos de consulta; documentar portabilidade/limites.
- [x] 2.2 Ler e validar snapshots/itens/instâncias em lote reutilizando codecs e invariantes; testar 100×100 itens, número limitado de consultas, adulteração e ausência de N+1; documentar fail-closed.
- [x] 2.3 Verificar isolamento/concorrência e necessidade de índice por plano; se justificado adicionar índice via Flyway após V9 e testar upgrade sem reescrita; registrar decisão e rollback compatível, sem nova tabela de progresso.
- [x] 2.4 Demonstrar V5/formato 2/retirada/reclassificação/geração desligada/restart, sem escrita/leak de dados; testar JSONs/hashes e contagens antes/depois; documentar evidências de leitura.

## 3. HTTP, contratos e privacidade — depende de 1/2

- [x] 3.1 Implementar as duas consultas/DTOs concretos e OpenAPI estrito; testar cargos/UUID/parâmetros extras e repetidos/limites; documentar respostas/janela e checklist de responsabilidades.
- [x] 3.2 Integrar Spring Security/principal já verificado e posse no núcleo; testar cadeia real 401/403 do POST existente/404/409 para visitantes e contas, sem controller duplicar regras; documentar proprietário das decisões.
- [ ] 3.3 Verificar no-store e projeções/erros sem correção/captura/identidade privada, integridade 500 versus indisponibilidade 503; testar controles negativos HTTP/artefatos e zero escrita nos GETs; documentar limites.
- [ ] 3.4 Cobrir consulta concorrente a conclusão/expiração/logout e sessão alternativa da mesma conta; testar snapshots coerentes, ausência de parcial e compatibilidade dos endpoints existentes; documentar matriz de atores.

## 4. Painel e revisão por assunto na interface — depende de 3

- [ ] 4.1 Implementar painel por cargo com 51 assuntos, métricas/janela/prioridade/amostra e atualização explícita; testar sem dados/poucos dados/empates/falha e contratos estritos; documentar mensagens.
- [ ] 4.2 Integrar decomposição individual ao resultado concluído, visitante e conta sem carregar histórico implícito; testar401/404/409/expiração e revisão de capturas antigas; documentar escopo temporário.
- [ ] 4.3 Proteger ciclo de consultas e contexto, limpeza/cancelamento/logout/troca de cargo/conta/respostas tardias; testar nenhuma persistência privada no navegador e atualização após conclusão confirmada; documentar comportamento.
- [ ] 4.4 Verificar tabela/links/estados/foco por teclado e 390/1280px em testes unitários/browser reais; testar falha/retry sem falso zero e aulas independentes; documentar inspeção e limites de acessibilidade.

## 5. Aulas e prática direcionada — depende de 3/4

- [ ] 5.1 Criar mapa público controlado dos 51 campos conteudo do manifesto e links de aula na base configurada; testar acentos/encoding/subpath/traversal/gabarito e todos os links; documentar rastreabilidade sem mover material.
- [ ] 5.2 Preencher compositor existente com cargo/assunto/quantidade 1/variações false, sem criação automática; testar alteração de critérios e limpeza de demais disciplinas/contextos; documentar escolha explícita.
- [ ] 5.3 Preservar confirmação de abandono, respostas locais, CSRF/idempotência/commandId/insuficiência e variações explícitas; testar rede/duas abas/retirada de acervo sem fallback; documentar ausência de garantia de novidade.
- [ ] 5.4 Executar fluxo real desempenho→aula→composição→conclusão→revisão, visitante e conta em outra sessão; testar atualização da última base e resultado reproduzível; documentar evidências por ator.

## 6. Regressão, operação e gate cumulativo — depende de 1–5

- [ ] 6.1 Consolidar matriz requisito→código/teste, Java puro/ArchUnit/PG/HTTP/browser e builds; executar regressões anteriores, conferindo limites de consulta e integridade, sem tratar contagem como revisão semântica.
- [ ] 6.2 Ensaiar consulta de banco V9 com contas/tentativas V5/formato 2 e eventual upgrade de índice/rollback, sem reescrever dados ou reiniciar JAR V5 incompatível; documentar planos/volume/limites.
- [ ] 6.3 Verificar exportação limpa/JAR/SPA/material/links/offline e ausência de dados privados/solver no cliente; conferir 51 assuntos e 111 variantes preservados, sem novas decisões editoriais; documentar evidências.
- [ ] 6.4 Consolidar PRs/SHAs/checks/triagem/cobertura/cota/tokens e executar gate cumulativo SOL 6.1 xhigh em contexto novo após revisão/integração das tarefas; abrir/manter [SPEC] para a épica geral e pedir aprovação própria, sem main/deploy/arquivamento.
