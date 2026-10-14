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

## Revisão, aprovação e retirada deliberadas

```sh
./backend/gradlew -p backend questionBankLocal -PlocalArgs='report --editorial-local true'
./backend/gradlew -p backend questionBankLocal -PlocalArgs='decide --editorial-local true --decision /tmp/opencode/decision.json'
```

Relatório privado retorna IDs/revisões/fingerprints/estado/ponteiro/sequence e cobertura
por disciplina e assunto. Inventário original refere-se aos 510 IDs preservados;
importadas/pendentes/em revisão/retiradas usam classificação da última revisão,
e elegíveis usam a revisão aprovada corrente. Categorias não são partição exclusiva:
uma questão pode ter versão elegível e uma correção pendente, inclusive em outra
classificação. Conta identidades únicas por assunto/disciplina, não versões/cargos.
Ausência de questões é 0 importadas, não curadoria/meta cumprida.

Arquivo JSON `question-decision/v1` tem campos obrigatórios `format`, `decision`,
`expectedEligible` (UUID/null **explícito**) e `expectedEditorialSequence` (inteiro).
`decision`: `action` (SUBMIT/RETURN_TO_DRAFT/APPROVE/WITHDRAW), `questionId`, `revisionId`,
`fingerprint`, `responsible`, `at` (ISO-8601 UTC), `checklist` e `rationale`.
APPROVE exige checklist CORRECTNESS, DISTRACTORS, CLARITY, STEP_BY_STEP, ACCESSIBILITY,
SOURCES e CLASSIFICATION; o domínio exige a revisão/conteúdo exatos e transição válida.
SUBMIT envia rascunho à revisão; RETURN_TO_DRAFT retorna com motivo; WITHDRAW retira
ponteiro corrente sem apagar histórico. Não reaproveite approval após retirada.
Sequence é monotônica, evita ABA e protege decisões mesmo sem mudança de ponteiro.

Antes de conectar, mostrar ação/destino/IDs/fingerprint/checklist/base esperada e exigir
`DECIDE <DB_URL> <SHA-256 do arquivo>` exatamente. Arquivo é lido uma vez, decisão
permanece imutável durante confirmação, e bases são rechecadas sob trava no commit.
O operador deve revisar conteúdo/fontes/distratores e identificar responsável real:
nomes no arquivo são rastreabilidade, não autenticação nem prova de competência.
Com CI=true ou GITHUB_ACTIONS=true, launcher recusa APPROVE **antes de conectar**;
nenhum override de fixture/--yes está disponível. Testes diretos do adaptador injetam
ambiente de fixture e banco/schema isolados; não aprovam acervo real em CI.
Isso não protege contra um operador privilegiado removendo variáveis/alterando código;
controle de acesso ao banco e execução externa continuam responsabilidade operacional.
Startup web normal não descobre o configuration nonweb nem executa ações editoriais.

## Correções de revisão do lote

Origem e caminhos do manifesto correspondem ao **módulo histórico** da chave, não ao
assunto editorial atual. Reclassificar P01-Q01 para P02 mantém identidade/proveniência,
exige versão-base, gera novo rascunho e preserva a elegível P01 até aprovação explícita.
O relatório privado pode mostrar essas classificações distintas sem duplicar a questão.

Comparações `x < y e y > z` são texto inerte válido. Detector estrutural comum bloqueia
tags/atributos ativos/declarativos em vez de qualquer trecho entre < e >; não é um
sanitizer nem promessa de detectar toda intenção semântica. Recursos mantêm hash/tipo/
raízes e revisão editorial, React nunca interpreta HTML bruto.

Launcher `main` usa `launch`: seleção inválida retorna código1 e JSON sanitizado de
chave/código (`INVALID_SELECTED_ITEM`/`UNKNOWN_SELECTED_KEY`) **antes de conectar**;
não ecoa mensagem/payload de pacote ou segredo. Prévia válida apresenta por chave as
pendências de fonte, fundamentos legais e suspeitas, sem inventar correções/aprovações.
Consulte arquivo local de extração para mensagens detalhadas de fonte, não stdout público.
Conflito de base recebe STALE_BASE; erro técnico/desconhecido permanece genérico.
Testes exercitam o caminho usado pelo main com stdout/stderr/código de saída e sentinelas
privadas, PG isolado/reclassificação e positivos matemáticos versus negativos ativos.

Revalidação ampliou os positivos a `a < b > c`/`x < i > y`: tags neutras exigem nome
adjacente ao <, conforme sintaxe HTML; texto com espaço após < não é tag. Markup de
risco/atributos mantém rejeição. Validações conhecidas feitas pelo domínio recebem
INVALID_QUESTION_CONTENT e chave sanitizada, sem duplicar invariantes no adaptador;
fingerprint/proveniência/caminho recebem códigos próprios. Falhas técnicas de parsing/I/O
continuam genéricas para não expor payloads ou credenciais.

A conversão tipada agora é feita por item após parsing sintático do envelope: rejeições
dos VOs durante desserialização (enunciado/alternativa/resolução vazios, recurso inválido,
campo desconhecido) têm chave sanitizada e INVALID_ITEM_STRUCTURE. Não interpretar
mensagens de exceção nem replicar invariantes. JSON sintaticamente inválido/arquivo ilegível,
em que a chave não é confiável, permanece erro genérico. Regressores passam pelo launch
com banco inacessível e exigem diagnóstico/exit1 antes de conectar, sem exposição privada.

A revalidação Astra encontrou rejeições conhecidas de origem que ainda escapavam para
erro genérico. Namespace incompatível, chave duplicada/fora do inventário e origem não
candidata agora recebem respectivamente ORIGIN_NAMESPACE_MISMATCH, DUPLICATE_ORIGIN,
ORIGIN_NOT_IN_INVENTORY e ORIGIN_NOT_CANDIDATE, com chave sanitizada; origem ausente recebe
MISSING_ORIGIN/INVALID_KEY. Não ecoar namespace/payload/exception: códigos fixos identificam
o problema e o operador confere seu pacote local. Regressão pelo launch reproduziu o
erro genérico antes da correção e exige diagnóstico sem DB/saída parcial/credenciais.

SOL-QB-06: o handler padrão do parser DOM podia imprimir nomes/fragmentos privados
de SVG malformado diretamente em `System.err`, antes da tradução pelo launcher.
O parser agora tem `ErrorHandler` explícito: warning/error/fatal rejeitam sem imprimir,
e o launcher continua retornando somente chave/UNSAFE_CONTENT. Não suprimir stderr
global em produção nem relaxar rejeição, hash, allowlist ou bloqueios de DTD/entidades
externas. Regressão pelo launch captura também `System.err` real, com restauração e
lock de teste: SVG válido chega à confirmação cancelada; tags malformadas, entidade
indefinida e DOCTYPE são recusados sem fragmentos/credenciais/saída parcial. Uma porta
local observada não recebe conexão em nenhum desses casos. O teste usa snapshot/assets
temporários e conteúdo sintético, sem modificar o acervo. Essa proteção cobre o parser
SVG desta entrada, não certifica todos os logs de bibliotecas nem a segurança semântica
de qualquer recurso; validação editorial e demais controles continuam necessários.
