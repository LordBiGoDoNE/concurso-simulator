# Operações locais — pacote de questões

Launcher separado `QuestionBankLocal`, pela tarefa Gradle `questionBankLocal`.
Não é runner nem perfil do startup web; build/startup normal não importa, envia
para revisão, aprova ou retira questões. Não existe API administrativa ou permissão
Google editorial. O acesso de escrita pertence ao operador local que controla o banco.

## Prévia e importação

1. Banco local **já migrado** pelo Flyway da aplicação; a CLI não migra/schema-update.
2. Credenciais somente em configuração externa: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
   DB_URL precisa ser explícita e loopback (localhost/127.0.0.1/::1), sem credenciais.
   Aceita apenas currentSchema, connectTimeout, socketTimeout e logServerErrorDetail=false.
   Não passe senha em argumentos, registros ou arquivos versionados. Não apontar para
   bases compartilhadas/produção; use backup e acesso restrito antes de operar.
3. Gerar pacote fora do Git com [extrator](question-extraction.md). Não modificar
   árvore de fontes durante leitura/confirmação; hash divergente recusa operação.
4. Escolher chaves explicitamente, inclusive em seleção pequena. Exemplo:

```sh
./backend/gradlew -p backend questionBankLocal -PlocalArgs='preview --editorial-local true --package /tmp/opencode/questions.json --root .. --keys P01-Q01,P01-Q02'
./backend/gradlew -p backend questionBankLocal -PlocalArgs='import --editorial-local true --package /tmp/opencode/questions.json --root .. --keys P01-Q01,P01-Q02'
```

O Gradle executa na pasta backend; passe `--root ..` para a raiz deste repositório.
Argumentos via localArgs não suportam caminhos com espaços; use classpath/launcher
Java diretamente com argumentos separados quando necessário. `standardInput` preserva
a confirmação interativa. Destino/ação/chaves/SHA-256 do pacote são apresentados antes
de abrir banco; import requer digitar `APPLY <DB_URL> <SHA-256>` exatamente. EOF/recusa
cancelam sem conexão. Não há opção genérica `--yes`. Configuração Spring externa não
pode habilitar servidor, migrations ou DDL neste launcher deliberadamente nonweb.

Leitor JSON rejeita campos desconhecidos, formatos/catalogação/IDs/proveniência/hash
inconsistentes, seleção inválida, markup ativo, links corretivos e recursos inseguros.
Enunciados/resoluções são texto, não HTML. É validação estrutural, não revisão pedagógica.
Prévia mantém inventário integral 510, pendências e suspeitas; NEW/IDENTICAL/CHANGED/STALE
por chave selecionada, sem transação de escrita ou alterações de arquivos. Nomes/UUIDs
do banco só são informativos na prévia, não reservam base para aplicação futura.

Aplicação inteira ocorre em uma unidade atômica e revalida concorrência na porta de
escrita. Mesmo conteúdo reutiliza IDs/revisões sem alterar decisões/reativar retirada;
novo conteúdo exige `expectedLatestRevision` da última revisão, acrescenta rascunho,
não aprova/substitui ponteiro. Seleção não remove questões ausentes. Pacote deve ser
salvo novamente com seu fingerprint recalculado ao alterar conteúdo; editar somente
expectedLatestRevision não muda o fingerprint. Selecionar P05-Q01 inválida recusa toda
a seleção, sem importar os demais como efeito parcial. Falha nunca é sucesso/acervo vazio.

## Evidência e limites

`QuestionLocalImportTests` lê todos os 509 fingerprints Python pelo Java e exercita
prévia/recusa/importação/reaplicação/retirada/base desatualizada/edição na CLI contra
PG18.6 em schema aleatório. Escritas e decisões são somente de fixture sintética;
nenhum conteúdo real é aprovado nem essa execução conta como curadoria do grupo 6.
Custos de relatório e triggers devem ser medidos antes de uso operacional maior.
