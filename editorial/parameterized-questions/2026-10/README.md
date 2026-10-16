# Conteúdo parametrizado — lote 3 e 4 em andamento

**111 variantes preparadas; nenhuma família registrada ou aprovada.** A base real
de juros simples está em DRAFT; falta a decisão editorial explícita dessa revisão
e dos digests finais das três famílias, reunidos em [content-approval.md](content-approval.md).
Não marcar o grupo 4 concluído nem inferir aprovação a partir destes arquivos/CI.

| Família | Base real observada | Variantes preparadas | Aprovação do template |
| --- | --- | --- | --- |
| Regra de três direta | M03-Q01, revisão `632ee809-9a3b-4e97-8144-9539f6b951bb` | 64 | Pendente |
| Porcentagem | M04-Q01, revisão `f4cfd6d7-96a5-4268-b257-638f4d6ae494` | 20 | Pendente |
| Juros simples | M04-Q05, revisão `80f32d25-9f88-4616-8ba0-8afafbeea9ab` em DRAFT | 27 | Pendente, base não elegível |

Os IDs/fingerprints exatos observados estão em `bases.json`. A observação veio do
PostgreSQL editorial local. As duas primeiras revisões estão APPROVED com ponteiros
elegíveis iguais; a terceira está em DRAFT, sem ponteiro. A observação não é promessa
de elegibilidade futura. O banco original tem M04-Q01–Q04, de porcentagem/aumento/
desconto; M04-Q05 foi importada somente em cópia isolada após autorização do usuário.
Não reutilizar identidades de porcentagem para juros nem tratar o título do assunto
M04 como prova de compatibilidade pedagógica.

Dependência concreta: M04-Q05 do material original pergunta juros de R$ 2.000 a
3% ao mês por quatro meses: R$ 60 por mês e R$ 240 de juros. Ela não integra as
250 bases aprovadas. O usuário autorizou revisar/preparar essa base. O pacote agora
explicita a pergunta sobre juros, unidades e três distratores, com resolução em etapas;
importação/reimportação reais preservaram a mesma revisão DRAFT, sem SUBMIT/APPROVE.
O acervo original mantém 250 elegíveis. A cópia tem 251 identidades, 250 elegíveis e
uma em rascunho; suas tabelas editoriais de templates continuam vazias. Aprovar essa
base acrescentará uma identidade à cópia; as variantes não acrescentam identidades.
IDs da nova base vieram do commit real da importação, não de fixtures ou placeholders.

## Arquivos e reprodução

`direct-proportion.json`, `percentage.json` e `simple-interest.json` são entradas exatas
para o launcher local, com expectedLatest 0. O registro de juros só será possível após
aprovar sua base. Seus UUIDs de template são identidades novas planejadas;
os UUIDs das bases são reais observados. `*-preview.json` são saídas completas do
launcher Java sem acesso ao banco. Incluem manifesto, gabarito e resolução e são
artefatos editoriais do repositório; não são recursos do cliente ou endpoints públicos.

`variants.md` contém todas as 111 tuplas preparadas, enunciados, quatro alternativas,
correta, resolução, cálculo independente e digest de cada saída. `audit.json` registra
111/111 e a base de juros pendente. São **arquivos gerados**, separados do código autoral.
O oráculo Python usa somas de frações (preço por unidade/centésimo/juros mensais),
sem chamar o gerador/oráculo Java, e recompõe digests binários da definição/domínio/
saídas/manifesto. Isso complementa a validação Java, sem garantir revisão pedagógica.

```text
./backend/gradlew -p backend parameterizationLocal -PtemplateAction=preview -PtemplateRoot=<raiz-absoluta> -PtemplateFile=editorial/parameterized-questions/2026-10/direct-proportion.json
./backend/gradlew -p backend parameterizationLocal -PtemplateAction=preview -PtemplateRoot=<raiz-absoluta> -PtemplateFile=editorial/parameterized-questions/2026-10/percentage.json
./backend/gradlew -p backend parameterizationLocal -PtemplateAction=preview -PtemplateRoot=<raiz-absoluta> -PtemplateFile=editorial/parameterized-questions/2026-10/simple-interest.json
python scripts/audit_parameterized_content.py --directory editorial/parameterized-questions/2026-10 --output editorial/parameterized-questions/2026-10 --allow-pending-bases
```

No Windows usar `backend\gradlew.bat`. `--allow-pending-bases` permite somente preparar
o relatório de uma base não elegível e mantém a pendência explícita. Sem essa opção,
o auditor rejeita o pacote enquanto a base estiver pendente. `--allow-partial` continua
separada para relatórios com famílias ausentes. Nenhuma opção aprova/libera conteúdo.
O launcher requer raiz/arquivo explícitos;
prévia não grava nem usa credenciais. Não converter esse comando em aprovação automática.

## Revisão editorial assistida e limites

Responsável pela preparação: Codex, assistido por IA, sem revisão humana independente.
As saídas são adaptações autorais; as referências IMPA/OBMEP são apoio temático,
sem alegar autoria/validação oficial. A proveniência aponta ao código exato de textos
e resoluções (`NumericGenerator.java` e SHA-256); a origem das bases e os hashes dos
HTML originais permanecem em `bases.json` e na curadoria do banco anterior.

Regra de três: preço unitário constante explícito, quantidades positivas, pergunta em
reais; distratores usam custo inicial, soma de quantidades ou produto sem divisão.
Porcentagem: pergunta somente a parte percentual em reais; distratores omitem divisão
por cem ou respondem aumento/restante. Juros simples: capital e taxa mensal explícitos,
prazo em meses e pergunta apenas dos juros; distratores respondem montante, só um mês
ou omitem a divisão por cem. O prazo mínimo de dois meses evita coincidência com o
distrator de um mês. Em todas as 111 tuplas, opções positivas/distintas,
sem equivalência com a correta; textos têm etapas/substituições, unidade e contraste
com erros plausíveis. A ordem editorial A correta é privada; a tentativa atribui UUIDs
opacos e embaralha alternativas. Texto puro, sem figura essencial, cor ou áudio.

Nenhum texto original foi editado. M03/M04 mantêm arquivos, URLs e gabaritos separados;
recursos locais seguem independentes de API/login. As variantes preservam a mesma base:
as variantes adicionam **zero identidades** às metas; a curadoria separada da nova base
adiciona uma identidade na cópia isolada. A prova de não repetição por base está nos
testes do núcleo de tentativas. O relatório integral 111/111 está preparado; operação
de aprovação e liberação continua pendente. Aprovação da base não aprova o template, revisão de código
não aprova conteúdo, e CI não registra decisões editoriais.

Verificação local desta preparação: hashes dos quatro HTML M03/M04 iguais aos
registros das bases; montagem estática isolada com 9 módulos/90 respostas recolhidas,
108 páginas e 1.757 referências locais válidas. Nenhum servidor/API ou credencial
foi necessário. As prévias editoriais não são copiadas para essa montagem estática.

## Decisão pendente

[approval-selection.json](approval-selection.json) fixa arquivos/SHA-256, IDs e
digests da base e das três versões. [content-approval.md](content-approval.md) apresenta
o conteúdo e o escopo exatos. Após autorização editorial explícita, atribuir timestamps
reais e registrar SUBMIT/APPROVE da base, registros DRAFT dos templates e suas decisões
exatas. Depois conferir replay/retirada em cópia de ensaio e obter a revisão independente
SOL 6.1 xhigh do lote 3/4. Nenhum merge ou gate cumulativo é aprovado por essa decisão.
