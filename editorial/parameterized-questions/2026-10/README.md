# Conteúdo parametrizado — lote 3 e 4 em andamento

**Preparação parcial; nenhuma família registrada ou aprovada.** Faltam a base real
de juros simples e a decisão editorial explícita dos digests finais das três famílias.
Não marcar o grupo 4 concluído nem inferir aprovação a partir destes arquivos/CI.

| Família | Base real observada | Variantes preparadas | Aprovação do template |
| --- | --- | --- | --- |
| Regra de três direta | M03-Q01, revisão `632ee809-9a3b-4e97-8144-9539f6b951bb` | 64 | Pendente |
| Porcentagem | M04-Q01, revisão `f4cfd6d7-96a5-4268-b257-638f4d6ae494` | 20 | Pendente |
| Juros simples | Nenhuma base aprovada compatível no banco observado | 0/27 | Bloqueada pela base |

Os IDs/fingerprints exatos observados estão em `bases.json`. A observação veio do
PostgreSQL editorial local, com revisões APPROVED e ponteiros elegíveis iguais;
não é promessa de elegibilidade futura. O banco observado tem M04-Q01–Q04, de
porcentagem/aumento/desconto. Não reutilizar essas identidades para uma questão de
juros, nem tratar o título do assunto M04 como prova de compatibilidade pedagógica.

Dependência concreta: M04-Q05 do material original pergunta juros de R$ 2.000 a
3% ao mês por quatro meses: R$ 60 por mês e R$ 240 de juros. Ela não integra as
250 bases aprovadas. O design exige decisão antes de ampliar a curadoria do banco;
a autorização para revisar/preparar essa base foi solicitada ao usuário. Até a resposta,
não importar, aprovar ou inventar IDs de questão/revisão para essa família.

## Arquivos e reprodução

`direct-proportion.json` e `percentage.json` são entradas exatas para o launcher
local, com expectedLatest 0. Seus UUIDs de template são identidades novas planejadas;
os UUIDs das bases são reais observados. `*-preview.json` são saídas completas do
launcher Java sem acesso ao banco. Incluem manifesto, gabarito e resolução e são
artefatos editoriais do repositório; não são recursos do cliente ou endpoints públicos.

`variants.md` contém todas as 84 tuplas preparadas, enunciados, quatro alternativas,
correta, resolução, cálculo independente e digest de cada saída. `audit.json` registra
84/111 e a família ausente. São **arquivos gerados**, separados do código autoral.
O oráculo Python usa somas de frações (preço por unidade/centésimo/juros mensais),
sem chamar o gerador/oráculo Java, e recompõe digests binários da definição/domínio/
saídas/manifesto. Isso complementa a validação Java, sem garantir revisão pedagógica.

```text
./gradlew -p backend parameterizationLocal -PtemplateAction=preview -PtemplateRoot=<raiz-absoluta> -PtemplateFile=editorial/parameterized-questions/2026-10/direct-proportion.json
./gradlew -p backend parameterizationLocal -PtemplateAction=preview -PtemplateRoot=<raiz-absoluta> -PtemplateFile=editorial/parameterized-questions/2026-10/percentage.json
python scripts/audit_parameterized_content.py --directory editorial/parameterized-questions/2026-10 --output editorial/parameterized-questions/2026-10 --allow-partial
```

No Windows usar `gradlew.bat`. A opção `--allow-partial` documenta a lacuna; sem ela,
o auditor rejeita este pacote incompleto. O launcher requer raiz/arquivo explícitos;
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
por cem ou respondem aumento/restante. Em todas as 84 tuplas, opções positivas/distintas,
sem equivalência com a correta; textos têm etapas/substituições, unidade e contraste
com erros plausíveis. A ordem editorial A correta é privada; a tentativa atribui UUIDs
opacos e embaralha alternativas. Texto puro, sem figura essencial, cor ou áudio.

Nenhum texto original foi editado. M03/M04 mantêm arquivos, URLs e gabaritos separados;
recursos locais seguem independentes de API/login. As variantes preservam a mesma base:
esta preparação adiciona **zero identidades** às metas. A prova de não repetição por
base está nos testes do núcleo de tentativas; operação real e relatório integral
111/111 continuam pendentes. Aprovação da base não aprova o template, revisão de código
não aprova conteúdo, e CI não registra decisões editoriais.

Verificação local desta preparação: hashes dos quatro HTML M03/M04 iguais aos
registros das bases; montagem estática isolada com 9 módulos/90 respostas recolhidas,
108 páginas e 1.757 referências locais válidas. Nenhum servidor/API ou credencial
foi necessário. As prévias editoriais não são copiadas para essa montagem estática.
