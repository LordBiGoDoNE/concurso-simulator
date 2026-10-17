# Revisão independente cumulativa — subject-review

**Gate do source real pendente; candidata revalidada sem achados abertos.**
PR[SPEC] #121 permanece em rascunho.
Revisão inicial concluída com SOL6.1xHigh (`openai/gpt-6.1-sol#xhigh`),contexto novo
`/root/subject_final_sol`. Risco crítico por segurança/privacidade/transação/concorrência.
Não confundir exame concluído com gate aprovado;6.4 permanece aberta.

## Escopo e cobertura

Diff completo base119ec25afe6ca1859ca615c95632096badcc9dd0 →
head7ce292f3edc1da47029f263747d71de95495ea02,digest canônico
`c6002c4a64996a2a33d6c51b2e2ca77d61086fffb5c8d5a2a92508c93a298b8a`.
Somente estes dois arquivos de evidência são excluídos pelo guard. O revisor recalculou
o digest e confirmou checkoutd15b6b8 ehead7ce com árvore idêntica3ef38241069941bffeb5edc626e572f63824b04e.
Examinou todos grupos,requisitos/ADR/checklistHTTP,código de produção e testes relacionados:
janela/UUID/última base/métricas/prioridade,Java puro/portas/JPA,principal/posse/expiração,
CSRF/CORS/no-store,integração compositor/realização/contexto,retry/insuficiência,
compatibilidadeV5/formato2/índiceV10 e mapa51conteudo com arquivos existentes.

## Achados e triagem

**SR-FINAL-SOL-001 — blocking — corrida temporal causa404 e risco de perder escolha local.**
ReviewUseCases captura now antes da transação;JpaCompletedAttempts.recent selecionava
conclusões sem created_at<=now antes do limite. Outra sessão pode criar/concluir entre
esse instante e o primeiroSELECT. Attempt.requireAccessible rejeita a captura futura;
Advice traduz Unavailable em404 e o cliente desmonta o contexto privado.
Referências:subject-performance/painel por cargo/conclusão concorrente,design1/3 e
guided-practice/preservação da realização e contexto. Não é falta de autorização real.

Coordenador confirmou com duas regressões que pausam ANTES do primeiroSELECT:
PostgreSQL lançouUnavailable eHTTP retornou404 em vez de200. Correção local aplica o
predicado de criação acessível no mesmo instante antes de ordenar/limitar,sem filtrar
depois do limite,enfraquecer domínio ou mudar somente o status. Limite extra/hasOlder
mantêm a mesma visão transacional. Consulta seguinte pode incluir as novas capturas.
O cenário anterior após cabeçalhos continua sendo outra intercalação válida.

**Aberto para integração:**correção candidata preparada e testada em task própria,
não integrada. A primeira revalidação manteve o bloqueador na fronteira de precisão
do bind SQL. Não há outros achados independentes verificáveis nessa rodada.
Triagem final depende do novo source e parecer;nenhuma aprovação de merge é inferida.

## Evidências e limitações

Revisor somente leitura:não executou testes/build/aplicação/scripts do projeto/rede/DB,
nem atestaCI,artefatos,ensaios operacionais,desempenho ou inspeção visual. Não leu
reviews/,spec-review/verification anteriores. Houve exposição incidental a referências
SR-R2-SOL-001/002 nos docsfrontend após inspeção independente;relatos não certificaram
conformidade. Não houve auditoriaWCAG/leitor de tela ou prova de todos schedulings.

Coordenador:dois testes novos falharam antes da correção com erros exatos acima;
rodada dirigidaPG/HTTP/planos/limites/ArchUnit passou após ajuste. Build backend completo
passou245 casos,zero falhas/erros,um symlink omitidoWindows. Frontend164 testes passou
sem paralelismo de arquivos,sem mudar assertions/timeouts;uma rodada concorrente com
Java teve timeout no carregamento do compositor de teste existente. BuildTypeScript/Vite
passou e10 fluxos reais de revisão/prática passaram51,6s. Nenhum desses resultados
representa execução pelo revisor ou prova da correção já integrada.

Cota5h/semanal indisponível antes/depois;observações mínimas privadas validadas/comparadas
por review_quota.py,sem isolamento verificável. Tokens do revisor/coordenador não
expostos;sem estimativas,conversão financeira ou execução de outro modelo.

## Revalidação

Primeira revalidação executada no mesmo contexto independente SOL6.1xHigh sobre a
candidata52cd578866a78c02bfccd4a90ef51e39b5d5817a, PR corretivo#122:
base7ce292f3edc1da47029f263747d71de95495ea02, digest
`7247071955432c6df60665db80cf76ff1fdea03581ebb15e209e9729e5249b9f`.
Cumulativo candidato sobre119ec25: digest
`531007ea5efa3a88f621c3a7bd653923137ef0f5ce6871ca82927704e6920776`.
O revisor recalculou ambos e examinou novamente requisitos,arquitetura,integração,
segurança,persistência,testes e manutenção. Leu o próprio parecer inicial e docs alterados;
não executou testes/build/scripts/rede/DB. Cota5h/semanal e tokens indisponíveis.

**SR-FINAL-SOL-001 permanece aberto:** a intercalação em segundos foi corrigida,
mas pgjdbc arredonda o Timestamp de NOW+600ns para NOW+1000ns. Uma captura criada
nesse microssegundo passa no SQL e falha no domínio com o instante original.
Coordenador confirmou em PostgreSQL real: a nova regressão falhou com Unavailable.
A candidata seguinte trunca somente o parâmetro SQL para microssegundos, conservando
a referência original do domínio; o teste exige janela antiga,hasOlder correto,
inclusão na consulta seguinte e nenhuma escrita. Não há outros achados independentes. Após ajuste de precisão, o coordenador executou
34 casos PG/HTTP/ArchUnit/limites: zero falhas/erros/skips; OpenSpec estrito6/6.
São execuções do coordenador, não do revisor.

O PR#121 real permanece em7ce292f, sem correção integrada. Nova candidata exige
snapshot/revalidação real, aprovação própria do PR#122 e conferência do source integrado;
estes registros não mudam digest para fazer o gate passar. Aprovação própria da spec
continua necessária. Sem main/deploy/arquivamento/sincronização de stack.

### Segunda revalidação: candidata corrigida

SOL6.1xHigh, mesma sessão independente, conferiu diretamente o PR#122:
base7ce292f3edc1da47029f263747d71de95495ea02 →
head07764284a575c843efdab5b25faefaab50e8e276, digest
`b04b6c0389d0c4515ac95f8b78185e558c6b2e3081c137369fc72e5c5878bbc0`.
Cumulativo sobre119ec25afe6ca1859ca615c95632096badcc9dd0, mesmo head, digest
`4e897a8a8912efd74b9173c3c59ec0e24431d99fb913243192d48b4025e64a7c`.
Ambos coincidem; somente estes dois arquivos de evidência são excluídos.

**SR-FINAL-SOL-001: fixed na candidata; nenhum novo achado.** Truncar o parâmetro
assegura created_at<=cutoff<=now; o driver não arredonda microssegundo exato para o
futuro. O domínio usa o instante original e a validação exige data da linha igual
à captura. Filtro antecede ordem/limite; hasOlder e carga continuam no mesmo snapshot.
Revisor examinou as regressões PG/HTTP/App, incluindo precisão, consulta posterior e
nenhuma escrita. Conferiu requisitos, arquitetura, integração, segurança, persistência,
testes e manutenção; detalhes no JSON. Não executou testes/build/scripts/rede/DB.
Leu o próprio parecer/evidências autorizadas; limitações anteriores permanecem.

A correção ainda exige aprovação própria do PR#122 e integração. O PR#121 real segue
em7ce292f, onde o bloqueador existe; a revisão candidata não aprova esse source nem
conclui6.4. Após integração, conferir head/base/digest/ancestralidade atuais e registrar
o gate sem modificar o diff revisado; qualquer diferença relevante requer revalidação.
Somente estes dois arquivos são alterados para registrar este parecer.
