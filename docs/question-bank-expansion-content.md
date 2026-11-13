# Acervo 2026-11 — entrega de conteúdo (#173, tarefas 1.2 e grupos 3–7)

Data: 13/11/2026. Pacotes finais versionados em
[`editorial/question-bank-expansion/2026-11/packages/`](../editorial/question-bank-expansion/2026-11/packages/)
(102 pacotes, um por assunto/finalidade, exportados da auditoria v10). Evidências em
[`editorial/question-bank-expansion/2026-11/final/`](../editorial/question-bank-expansion/2026-11/final/).
Preparação e conferência não habilitam nada: elegibilidade no banco é o grupo 8.

## Totais conferidos

| Disciplina | Assuntos | Simulado (mín.) | Prática (mín.) | Originais (mín.) | Bancas das originais |
| --- | ---: | ---: | ---: | ---: | --- |
| Português | 10 | 250 (250) | 250 (250) | 100 (100) | Avança SP 96, FGV 4 |
| Matemática/Raciocínio | 12 | 310 (300) | 307 (300) | 134 (120) | Avança SP 103, Cebraspe 27, FGV 4 |
| Informática | 10 | 250 (250) | 250 (250) | 100 (100) | Avança SP 72, Cebraspe 19, FGV 9 |
| Conhecimentos Administrativos | 8 | 204 (200) | 202 (200) | 86 (80) | Avança SP 86 |
| Conhecimentos Escolares | 11 | 280 (275) | 284 (275) | 110 (110) | Avança SP 78, Fundatec 27, Crato/CE 5 |
| **Total** | **51** | **1.294** | **1.293** | **530 (510)** | 530 identidades oficiais distintas |

Todas as 102 células têm ≥25 itens e ≥5 originais de prova; tabela por célula em
[`final-check.md`](../editorial/question-bank-expansion/2026-11/final/final-check.md).
Nenhuma original repete identidade oficial entre células/finalidades; nenhuma chave ou
fingerprint se repete entre pacotes.

## Verificações executadas

```powershell
python -X utf8 -m unittest discover -s scripts/tests -p "test_question_expansion*.py"
python -X utf8 scripts/question_expansion_final.py --packages editorial/question-bank-expansion/2026-11/packages `
  --edition-root <acervo-local>/work/aggregate-wip --reviews <acervo-local>/work/aggregate-wip/reviews.json `
  --output editorial/question-bank-expansion/2026-11/final
```

- `scripts/question_expansion_final.py` (25 testes Python no total): referência pública
  recusada por nome/URL/host/caminho de prova (`study_reference`) ou por coincidir com
  qualquer URL de prova/gabarito da proveniência dos pacotes; SHA-256 de cada
  requisição contra o manifesto, digests, seleção = itens, namespace por finalidade,
  **fingerprint recalculado do conteúdo**, dossiê e proveniência coerentes, gabarito
  definitivo/questão válida, cotas por célula e disciplina, identidades únicas, revisão
  individual com fingerprint atual (2.587/2.587) e, com o acervo local, SHA-256 dos
  PDFs de prova e gabarito de cada original (530/530 `VERIFIED`; 105 provas e 66 gabaritos distintos).
- Leitor/prévia Java real (`QuestionExpansionContentCheck`) em PostgreSQL 18.6 novo:
  102 pacotes, 2.587 itens, todos `NEW`, zero questões/recibos/decisões gravados
  ([relatório](../editorial/question-bank-expansion/2026-11/final/reader-preview-report.json)).
- Auditoria v10 de pares/duplicatas contra o acervo atual, legado e revisões curadas:
  138/138 decisões `CURRENT`, zero bloqueios ([audit-v10](../editorial/question-bank-expansion/2026-11/final/audit-v10/)).
- Matemática (tarefas 4.x): seis subagentes sem acesso ao gabarito resolveram as 617
  questões vigentes (Python/`Fraction` para cálculos). 616 `SOLVED` e 1 `AMBIGUOUS`
  (item interpretativo Cebraspe), **617/617 letras iguais ao gabarito do pacote**
  ([math-blind-check.json](../editorial/question-bank-expansion/2026-11/final/math-blind-check.json)).
  Os 488 oráculos anteriores estavam vinculados a fingerprints anteriores às correções
  r1–r6 e ficam como histórico.

## 1.2 — inventário de fontes das originais

[`originals-inventory.json`](../editorial/question-bank-expansion/2026-11/final/originals-inventory.json)
lista as 530 originais com assunto, finalidade, identidade oficial, banca, concurso,
cargo, ano, número, URL e SHA-256 da prova e do gabarito definitivo. A cota de cinco
originais distintas por célula foi conferida item a item (meta histórica de dez por
assunto entre as duas finalidades). O inventário provisório anterior
(`source-inventory.json`) fica como histórico. Reutilização: art. 8º, IV, da Lei 9.610/1998
conforme [avaliação](question-source-reuse-assessment.md); não se afirma licença expressa da banca.

## Arquivos que ficam só no computador local

Por decisão do usuário de 13/11/2026, provas/gabaritos (711 PDFs), dossiês, entradas,
geradores, camadas de correção e auxiliares de `backend/build` (≈1,5 GB) não vão para o
GitHub. [`local-files-manifest.json`](../editorial/question-bank-expansion/2026-11/local-files-manifest.json)
registra caminho, tamanho e SHA-256 dos 3.892 arquivos dos commits locais `a6b286e..39e410d`
(branch local `task/question-bank-expansion/practice-capacity`, preservada). Backups,
credenciais e logs privados nunca entraram nesses commits.

## Limites

- As revisões individuais (r1–r6) e a resolução às cegas de Matemática são assistidas
  por IA (Claude), em contextos próprios. Não há revisão humana nem aprovação institucional.
- Vários distratores autorais de A04 são fracos (eliminação fácil); não foram
  reprovados pelo critério “só se grave”.
- Triagem textual de pares não decide toda equivalência semântica; decisões de pares
  foram adjudicadas pelo próprio preparador.
- Fontes de concordância/regência sem manual oficial público usam dicionários (Aulete/Michaelis),
  indicado na nota da referência.

## Correção após a revisão final — 13/11/2026 (achado A1)

A revalidação independente da revisão final (Opus 5.5 medium, head `13cc3be`) encontrou 27
originais Cebraspe (M03, M11, M12) cuja referência pública de estudo, exibida durante a
realização aberta, era a página do concurso com nome "prova e gabarito definitivo oficiais".
Isso viola o requisito "Evidências corretivas permanecem fora da prática aberta". Triagem aprovada pelo usuário:

- **Camada r7** (`reviews-r7`, acervo local): as 27 passam a citar a referência de estudo das
  demais originais de Matemática (OBMEP, Banco de Questões); prova e gabarito ficam só na proveniência.
- **Revisão individual independente** das 27 (12 PASS, 15 FIX) e **camada r8**: 11 itens de
  lógica formal de M11 passam a citar `logica` (OBMEP, Introdução à Lógica Matemática), 3 itens de
  conjuntos de M12 ("Diagramas Lógicos e Situações-Problema") passam a citar `conjuntos`, e duas
  resoluções foram corrigidas. A reclassificação sugerida para os itens de conjuntos foi rejeitada:
  o pedido ao revisor descrevia M12 de forma errada, e o título real cobre diagramas de conjuntos.
- **Segunda conferência** dos 15 itens: 14 PASS e 1 FIX. Essa FIX (referência específica de razão
  e proporção em M03) foi rejeitada pelo coordenador: as outras 49 questões de M03 usam a mesma
  referência OBMEP, que não é corretiva. O limite fica registrado na revisão do item.
- **Pares:** 20 decisões ficaram STALE e foram mantidas (v7, todas DISTINCT); enunciado e
  alternativas estavam inalterados. Auditoria v11: 138/138 CURRENT, zero bloqueios.
- **Pacotes:** mudaram só 6 (M03, M11, M12 nas duas finalidades) e 27 fingerprints; contagens e
  originais inalterados (2.587; 530). Conferência final: 0 problemas. Leitor/prévia Java: 102
  pacotes NEW, sem escrita. Matemática às cegas: vínculo dos 27 atualizado após conferir enunciado,
  alternativas e gabarito idênticos.
- **Guarda nova:** `study_reference` (`scripts/question_package.py`) recusa no compilador e na
  conferência final referência pública cujo nome ou URL indique prova/gabarito, página `/concursos/`
  ou host de banca. Recusa as 27 antigas e nenhuma das 2.786 referências atuais.
- Evidências em `final/review-r7-r8/` e `final/audit-v11/`; `audit-v10/` fica como histórico.
  Arquivos locais novos/alterados: `local-files-manifest-2026-11-13.json`.
