# Revisão independente cumulativa — subject-review

**Gate pendente:um bloqueador aberto.** PR[SPEC] #121 permanece em rascunho.
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
não integrada,não revalidada. Não há findings adicionais verificáveis nesta rodada.
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

**Ainda não executada.** Source7ce mantém o bloqueador. Alterações locais de código,
testes/docs e estados de tarefas precisam de commit/PR revisável e nova conferência do
conjunto completo pelo mesmo revisor. Estes registros não atualizam digest para fazer
o check passar. Revalidação depende do novo source;aprovação própria da correção e da
spec continuam necessárias. Sem main/deploy/arquivamento/sincronização de stack.
