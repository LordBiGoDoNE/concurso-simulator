# Ciclo editorial — tarefa 1.3

`Question.decide` protege DRAFT → IN_REVIEW → APPROVED. Pendência devolve IN_REVIEW
a DRAFT com justificativa; correção de conteúdo exige outra revisão. Evidência inclui
questão/revisão/fingerprint, responsável identificado, data, checklist e justificativa/link.
APPROVE exige todos os sete itens (correção, distratores, clareza, passo a passo,
acessibilidade, fontes, classificação); submissão não aprova. Não é autenticação Google
nem prova de competência do responsável. Fixtures não aprovam material real.

Somente aprovação explícita troca o ponteiro elegível. Revisão anterior permanece
APPROVED no histórico; nova aprovação não pode regredir para número antigo. Retirada
com motivo remove elegibilidade sem apagar conteúdo/eventos nem voltar a DRAFT.
Reaplicação de pacote não deve chamar decide ou reativar retirada.

O histórico é imutável para o chamador; restore reproduz decisões e valida suas invariantes.
`DecideQuestionUseCase` coordena uma unidade atômica por `QuestionStore`: carregar para
alteração, conferir ponteiro e sequência esperados, decidir e salvar. O UseCase confere
também a sequência editorial esperada (quantidade monotônica de decisões) dentro da trava
para detectar ciclos antigos, mesmo quando o ponteiro retorna ao mesmo valor. Comando
antigo é rejeitado; reler/revisar o snapshot, nunca renovar token silenciosamente para retry.
O adaptador deve bloquear a linha e garantir commit ou rollback antes de retornar.
A porta não expõe JPA/Spring/SQL.
É uma fronteira específica da escrita de questões, não generalização automática do
UnitOfWork da identidade. Concorrência/banco serão demonstrados nas tarefas 2.1–2.3.

Nenhum endpoint administrativo, CLI editorial, dado de sessão ou permissão Google foi
adicionado. UseCase só será conectado a um adaptador editorial local deliberado na 3.4.
Testes puros exercitam rejeição de evidência desatualizada, checklist pendente, estados,
substituição/retirada/histórico e coordenação com porta substituível.
