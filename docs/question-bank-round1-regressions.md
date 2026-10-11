# Regressões da primeira rodada

Esta camada acrescenta regressões das tarefas 1.2/2.2/2.3 sem reescrever PRs inferiores
da stack #41. Não implementa tarefa nova, endpoint, pacote/CLI ou aprovação editorial.

## Classificação da revisão elegível

Causa da lacuna: o teste de substituição usava duas fixtures com a mesma classificação,
embora seu nome alegasse verificar elegibilidade/classificação. Regra positiva: cada
revisão mantém sua classificação; rascunho ou revisão pendente não muda a classificação
elegível. A troca ocorre somente com aprovação explícita da revisão exata.

`QuestionStorageTests` agora altera Português/P01/dois cargos para Matemática/M01/
Secretário de Escola. Recarrega de PostgreSQL e confere classificação histórica, rascunho,
pendência e aprovação. O teste não prova filtros HTTP, ainda na tarefa 4. Essa regressão
trata a recomendação QB-ASTRA-01; não foi encontrado defeito funcional na implementação.

## Conteúdo completo e pacotes concorrentes

Round-trip inclui recurso com hash/descrição acessível em enunciado/alternativa/resolução,
fundamento legal com dispositivo/data e Unicode composto/decomposto preservado. A fixture
é explícita: não valida uma norma real nem declara seguro um arquivo que não foi inspecionado.

Dois pacotes com origens iguais em ordem oposta observam a primeira origem ausente por
barreira; o UseCase ordena aquisição de travas. Um commit cria as duas questões/revisões;
o perdedor recupera após rollback e reutiliza ambos os IDs. Ausência de deadlock/duplicação
nesse cenário não garante todos os interleavings; há deadlines finitos e teste anterior
de rollback do pacote inteiro por versão-base inválida. Testes de concorrência foram
repetidos três vezes, sem falhas/erros/skips, em PostgreSQL 18.6 isolado.

## Limite opcional — trigger de elegibilidade

QB-ASTRA-02 identifica consulta global por trigger diferida por linha. Mantida nesta
rodada: não há limite aprovado de desempenho nem degradação medida; não reescrever V4
versionada. Otimização, se necessária após medição representativa, exige migração aditiva
e regressões de ponteiro/estado. Não é prova de escalabilidade nem garantia de custo
constante, e não dispensa reexaminar essa limitação antes de uso operacional maior.

Execução de testes é do coordenador. Pareceres, digests, comparação e tokens ficam em
`openspec/changes/question-bank/reviews/`, por PR independente de evidência; não preencher
o gate cumulativo da spec como concluído nem aprovar merges pelo resultado destes testes.
