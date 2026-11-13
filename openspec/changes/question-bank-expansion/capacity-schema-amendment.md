# Ajuste necessário após integrar #161

O usuário autorizou continuar a #173 após a integração da #161 pelo PR #176,
merge `226d2e5b07208887941811beff10ac29295b04b3`. A main agora contém V14 e captura
formato 4 sem cargo. A hipótese anterior do grupo 2 — alterar quantidade sem
migração — não corresponde à proteção efetivamente integrada no banco.

## Evidência

`V14__topic_lesson_practice.sql`, constraint `me_attempt_purpose_valid`, exige
`jsonb_array_length(payload->'items') BETWEEN 1 AND 5` também para formato 4.
Ampliar só Java/HTTP/frontend faz PostgreSQL recusar capturas de seis a 25 itens.
Os testes de persistência e concorrência detectaram a rejeição. Não foi alterada
V14 nem relaxada sua proteção em testes.

O [design aprovado](design.md), seção 5, determina “sem novas tabelas, migração ou
regravação”. Como essa restrição passou a conflitar com o resultado pedido, a
ativação de V15 exigiu esclarecer essa alteração específica de escopo com o usuário.
Após a proposta concreta e a pergunta sobre autorizar V15, o usuário respondeu
“contibue” em 2026-11-11. A continuidade foi adotada como autorização desse ajuste,
com a ação explicitada ao usuário antes da execução.

## Proposta concreta

[SQL proposto preservado](proposed/V15__expanded_topic_practice_capacity.sql) e
[V15 executável](../../../backend/src/main/resources/db/migration/V15__expanded_topic_practice_capacity.sql).
Recria a mesma constraint mudando exclusivamente o
intervalo de formato 4 de 1–5 para 1–25. Mantém limite histórico cinco em formato 3,
regras de simulado e nulidade da composição de formato 4. V1–V14 e todas as travas
de assunto, identidade/fonte, itens, imutabilidade e commit permanecem intactas.
Não há tabela/coluna nova, UPDATE de dados, mudança de formato ou comando.

Escopo autorizado: promover o SQL para V15 executável; incluir schema 15 no contrato
fechado de backup/restore com o mesmo inventário de tabelas de V14; testar upgrade
com comparação dos dados, capturas 25, recusa 26 e formatos antigos, recuperação
isolada e dois processos reais do JAR. Não iniciar binário anterior sobre capturas
novas maiores nem fazer downgrade/truncamento para recuperação.

Estado: V15 adicionada após a continuidade autorizada; verificações em PostgreSQL
efêmero próprio passaram, inclusive upgrade sem regravação, recusa de 26, formatos
anteriores e backup/restore V10–V15. Nenhuma migração/carga no banco do operador ou
VPS. Grupo 2 implementado/verificado localmente; teste do usuário e revisão própria
continuam pendentes, conforme [evidências](practice-capacity-verification.md).
