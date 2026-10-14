# Curadoria inicial — grupos 6 e 7

Escopo solicitado: 6.1–6.25 e 7.1–7.3, 50 identidades por disciplina, em cinco
lotes de dez. Base de trabalho: `dcbe086af47a6fafca114290850a419f0886cc92`.

As cinco JSONL de disciplinas são intervenções editoriais, não um gerador de respostas:
cada linha identifica uma questão lida, a resolução conferida e, quando necessário,
a correção de enunciado/alternativas. Os IDs de origem e alternativas e os hashes
dos HTML originais são preservados. Fontes são apoio para estudo e conferência;
não representam autoria oficial das questões. Normas têm dispositivo e data.

Responsável: **Codex, revisão editorial assistida por IA**. Não representa revisão
humana independente, aprovação do usuário dos PRs ou o gate cumulativo da spec.
Os sete itens do checklist só podem ser registrados para o fingerprint resultante
da linha efetivamente examinada. Importar o pacote continua criando rascunhos;
o compilador não se conecta ao banco nem toma decisões editoriais.

Seleção: Português Q02–Q06 de cada assunto; Matemática Q01–Q04 de cada assunto
mais M01-Q05/M02-Q05; Informática Q01–Q05 de cada assunto; Administrativo
Q01–Q06 de cada assunto mais A01-Q07/A02-Q07. Escolar usa Q01–Q04 mais Q05
nos seis primeiros assuntos, com as substituições explícitas nas linhas JSONL.
São cobertos todos os 51 assuntos; nenhuma meta quantitativa aprova o restante.

`P05-Q01` continua inválida no inventário original. Alertas textuais compartilhados
não são fundidos: P07-Q02 e P10-Q03 representam, cada uma, um grupo distinto de
Português, sem outro representante selecionado; P10-Q03 foi reformulada explicitamente.
A02-Q05/A06-Q01/A07-Q05 são selecionados e suas
contrapartes escolares S06-Q01/S09-Q01/S10-Q02 não são. S07-Q03/Q04 também são
substituídas para evitar repetir as perguntas S01-Q01/Q05; S10-Q07 substitui
S10-Q01 para não repetir a pergunta de compartilhamento de senha S02-Q04.

Fontes indisponíveis no legado, em particular o PDF do Ministério das Cidades que
redireciona para login, são substituídas por material oficial acessível. Não se
declara conferência desse PDF. Rotinas escolares são orientações gerais: não
inventam prazo, modelo obrigatório ou regra municipal de Limeira. Não se aplica
a Lei 9.784/1999 automaticamente ao município. Versões/idioma/condições de Office
e Windows são declarados nos itens em que mudam a resposta.

Regenerar os 25 pacotes e os registros por fingerprint:

```text
python scripts/curate_questions.py --root . --output backend/build/curation
```

Saída fora do Git: `packages/<disciplina>-<lote>.json`, `review-records.json`,
`coverage.json` e `review.md`. Cada pacote contém o inventário completo e exatamente
dez itens selecionados; o acervo não selecionado é preservado nas fontes. Os hashes
de `manifest.json` fixam o conteúdo revisado: divergência exige nova revisão,
não regeneração silenciosa do aceite. Pacotes completos não entram em Flyway,
recursos do JAR, frontend, assets ou build público das aulas.

## Elegibilidade verificada no banco local

Após autorização explícita do usuário, os 25 pacotes foram importados e reaplicados
na CLI real em PostgreSQL 18.6 isolado. `eligibility.jsonl` contém 250 registros
**gerados de evidência**, com questão/revisão/fingerprint, responsável, checklist,
IDs/datas das duas decisões persistidas e SHA-256 dos comandos confirmados.
Foram cruzadas as 500 decisões do banco com esses comandos; o resultado é de
50 identidades elegíveis por disciplina, sem pendências importadas. A tolerância
temporal é de um microssegundo, conforme a precisão persistida; hashes são exatos.

`eligibility-summary.json` liga a evidência ao manifesto/JAR e registra consulta
dos 250 detalhes, 102 combinações AND, navegador 390/1280 e retirada/reaplicação
numa cópia lógica isolada. A cópia não aumenta as contagens do acervo original.
Esses arquivos não são executados, descobertos pelo startup ou incorporados ao JAR.
Representam observação no ambiente local, não aprovação automática de outro banco.

Os arquivos gerados pelo compilador (`review-records.json`/`coverage.json`) descrevem
apenas sua saída offline: `not-imported` e `databaseEligible: null` não consultam o
estado atual do banco. A evidência persistida está nos dois arquivos de elegibilidade
acima; regenerar pacotes não a atualiza nem executa aprovação.

Repetir as consultas de API em modo somente leitura:

```text
python scripts/verify_curated_bank.py --root . --report backend/build/curation/approved-report.json --api http://127.0.0.1:18180 --output backend/build/curation/http-verification.json
node scripts/verify_curated_browser.cjs
```

O primeiro requer o relatório privado real da CLI (`report --editorial-local true`)
e API local; o segundo, frontend em 5173 apontando à API 18180 e Chromium instalado.
Não são tarefas de startup ou CI. A operação editorial exige a CLI deliberada e as
confirmações documentadas em `docs/question-local-operations.md`; nenhuma credencial
de banco deve ir ao Git. Os testes automatizados continuam usando fixtures identificadas.

Situação: grupos 6/7 implementados e verificados no PR #62 em rascunho. O parecer
independente e sua atualidade estão em `openspec/changes/question-bank/reviews/round3/final-review.md`.
Integração/merge e gate cumulativo da
spec exigem seus procedimentos próprios. Não inferir integração pela existência
dos pacotes ou por aprovações editoriais no banco local.
