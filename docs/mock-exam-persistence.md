# Persistência de tentativas

V5 é aditiva sobre V4. `me_attempt` armazena owner exclusivo, datas, `@Version` e
snapshot privado; `me_item` captura cada item com posição e FK para a revisão original;
`me_command` guarda hash/resultado por owner + operação. As FKs de comandos conferem
também o owner. Cascata de exclusão alcança apenas itens/comandos da tentativa.
Contas, sessões e revisões não são removidas. Nunca retornar essas entidades/payloads
diretamente ao navegador.

Constraints protegem owner/conta/expiração, posições, ausência de questões repetidas,
versão e referências. Trigger bloqueia mudança da composição/conteúdo capturado e
qualquer atualização após conclusão. Itens e comandos não podem ser atualizados.
Os VOs complementam essas garantias com validação de conteúdo e alternativas.

`EligibleRevisionPool` pertence ao banco de questões e só oferece captura interna.
Sua implementação exige transação já aberta pelo consumidor. Uma consulta seleciona
ponteiros elegíveis aprovados e payload completo; não usa a API pública nem cria
transação por item. O módulo do banco não depende de mockexam. O mesmo guard da
projeção pública valida enunciados/opções/recursos/fontes antes da captura privada.

Rollback operacional: desabilitar o fluxo novo e executar a versão anterior da
aplicação, preservando V5 e seus dados. Não executar downmigration, Flyway clean,
alterar V1–V4 nem apagar dados para adaptar Hibernate. Backup/restauração e deploy
serão tratados no procedimento de implantação; CI não publica a aplicação.
