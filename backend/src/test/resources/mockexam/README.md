# Fixture V5 real

`v5-creation.json` foi produzida executando o núcleo de simulados extraído, sem
alterações, do commit `0d0fc4848f55fc93c736aa76dcfbb73a7269eb39`, com Jackson 3.1.5.
Foram compilados os arquivos de `mockexam/domain` e `mockexam/application` em um
diretório separado, com esse diretório antes das classes atuais no classpath.
`AttemptUseCases.create` recebeu clock fixo, pool sintético com uma questão P01,
cargo assistente administrativo, quantidade 1, tópicos vazios, e porta em memória
que preservou exatamente `insert(snapshot)` e `record(command)`.

O hash V5 real é `2c65452f9a51ac3d5e56c6fa613b9e03ca2aae9d7222d38c515358e1fd43a2be`.
Snapshot e outcome não contêm `format`, `source` ou a nova opção de composição.
Os UUIDs foram atribuídos naquela execução; não são IDs editoriais nem produção.

O teste instala esses JSONs diretamente em schema V5 com a base real correspondente,
aplica V6/V7 sem reescrever linhas, testa retry ausente/false/true, resposta,
conclusão, histórico e formatos de todos os outcomes sob os triggers originais.
Não usar o serializador atual para regenerar essa evidência histórica.
