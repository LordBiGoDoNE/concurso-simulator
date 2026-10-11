# Persistência de questões — tarefa 2.1

V4 é aditiva: catálogo fixo, `qb_question`, revisões/alternativas/classificação por cargos,
referências e decisões editoriais. V1–V3 e dados de identidade/sessão não são alterados.
Flyway insere somente catálogo, **zero questões ou aprovações**. Hibernate continua
validate/OSIV false; entidades/embeddables JPA estão apenas na infraestrutura.

Origem é única por namespace/chave (`qb_question_origin_key`), revisão por questão/número;
alternativas por revisão/UUID/posição. FKs compostas impedem resposta de outra revisão,
classificação de disciplina incompatível e ponteiro para revisão de outra questão. FKs
adiadas permitem inserir revisão/opções na mesma transação; elegibilidade exige APPROVED.
Conteúdo e decisões são append-only, com proteção SQL contra alteração/exclusão.

O payload JSONB interno guarda integralmente os objetos de conteúdo (texto acessível,
recursos, alternativas, fontes, proveniência). Índices/constraints usam colunas/tabelas
normalizadas espelhadas; o adaptador deverá conferir equivalência e fingerprint ao ler,
nunca devolver esse payload à web. Essa representação de armazenamento não é o formato
do pacote editorial/contrato público. Ordem editorial está em position, não ordinais de enum.

2.2 conecta leitura/escrita/transações e teste de concorrência; 2.3 idempotência/conflitos.
Os testes desta tarefa validam schema/JPA com PG18.6 real, banco novo e upgrade desde V3,
preservando usuário/identidade/sessão/atributos. SQL direto é usado somente para testes de
constraints; não entra em UseCases/controllers. Não há conexão com banco remoto.

Rollback operacional: desabilitar acesso ao módulo, manter versões/histórico/dados e
reverter aplicação de forma compatível. Não executar downmigration, apagar tabelas ou
reescrever migrações versionadas para adaptar ORM. Deployment permanece em spec própria.

## Unidade atômica — tarefa 2.2

`JpaQuestionStore` implementa a porta por EntityManager, com transação REQUIRES_NEW
específica das escritas de questões: retorna após commit/rollback completo, inclusive
quando há transação chamadora. Não reutilizar automaticamente essa semântica em consultas
públicas. Sessão da porta só funciona durante seu callback. O core não importa JPA/SQL.

Cada alteração carrega a linha com PESSIMISTIC_WRITE; save exige carregamento na unidade,
histórico/conteúdo prefixos íntegros e usa @Version/force increment. O UseCase confere
ponteiro elegível esperado dentro da trava; duas aprovações concorrentes com a mesma
base não sobrescrevem silenciosamente a primeira. A segunda falha por base desatualizada.
Revisões/decisões são acrescentadas; salvar nunca faz merge JPA de um grafo destacado.

Leitura recompõe conteúdo, verifica fingerprint/espelhos/ordem e reproduz decisões,
conferindo estados/ponteiro persistidos. Inconsistência é falha, nunca acervo vazio.
Payload JSONB é armazenamento interno tipado por Jackson na infraestrutura, não um
DTO web nem contrato do pacote. Testes PG reais verificam round-trip, substituição,
retirada, rollback de dois agregados, concorrência e commit independente/porta encerrada.

## Idempotência e recuperação — tarefa 2.3

`ApplyQuestionCandidatesUseCase` aplica candidatos já validados em uma unidade atômica,
ordenando travas por namespace/chave. Pacote com origem repetida é rejeitado. Origem nova
gera questão/revisão DRAFT; mesma origem/conteúdo retorna IDs existentes, inclusive revisão
antiga/retirada, sem mudar estado/ponteiro/histórico. Conteúdo diferente exige ID da última
revisão esperada; base desatualizada desfaz todo o pacote, inclusive inclusões anteriores.
Não remover questões ausentes nem fundir chaves diferentes por similaridade.

O adaptador traduz **somente** `qb_question_origin_key`, fora de TransactionTemplate.execute
e portanto depois do rollback. O UseCase tenta uma segunda unidade completa; novo conflito
ou outra falha propaga. PK/FK, validação, corrupção de payload e indisponibilidade não viram
sucesso. Não continuar numa transação abortada nem retornar IDs ainda não commitados.
Travas serializam origens já existentes; constraint protege a corrida de origem ausente.

PG18.6 real força seis imports a observar a origem ausente antes da escrita: um vencedor,
cinco colisões recuperadas pós-rollback, uma questão/revisão e zero decisões. Testes também
cobrem pacote revertido, revisão-base desatualizada, reimportação de retirada e falhas de
PK/FK; porta substituível cobre indisponibilidade/ordem de recuperação. ArchUnit protege
o núcleo de questões contra dependências de identidade/framework/adaptadores.

Ainda não há extrator/JSON de pacote/prévia/CLI/relatório de similaridade: 3.1–3.4.
Esse UseCase não é chamado no startup nem exposto por HTTP; toda escrita usa fixtures
identificadas nesta rodada. O payload interno não aprova fonte/conteúdo nem deve ser enviado
a clientes. Nenhuma aprovação real ou meta editorial foi produzida por testes/importação.
