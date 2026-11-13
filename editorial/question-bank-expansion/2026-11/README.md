# Edição de expansão 2026-11

Issue [#173](https://github.com/LordBiGoDoNE/concurso-simulator/issues/173), grupos 1–8
autorizados localmente. [Guia de ferramentas e operação](../../../docs/question-bank-expansion.md).

- `edition.json` fixa os hashes de `sources.json` e dos dez JSONL por
  disciplina/modalidade. Arquivos vazios são pendência explícita, não acervo criado.
- `inventory-baseline.json` e `inventory-baseline.md`: reconciliação offline das
  102 células, preservando 251/255 como histórico e estado do destino desconhecido.
- `source-inventory.json`: provas/gabaritos oficiais prospectados, hashes baixados,
  classificação preliminar por posição e déficit de dez originais por assunto.
  Prospecção não equivale a fonte reutilizável, seleção, resposta conferida ou aprovação.
- `reviews.json` e `pair-decisions.json`: vazios até registrar conferências reais;
  evidências vinculadas aos fingerprints, sem revisão humana/modelo inventada.

Meta preservada: 25 bases e cinco originais por célula; 2.550 bases e 510 originais
distintas no conjunto. Este diretório versionável é o esqueleto; a retomada prepara
79 candidatas reais em JSONs externos, no diretório ignorado explícito descrito no
guia, sem importação/habilitação ou revisão integral. A avaliação de reutilização
está em [documento próprio](../../../docs/question-source-reuse-assessment.md),
sem atribuir licença expressa à banca. Os grupos de conteúdo 3–7 permanecem abertos inteiros;
não se declara entrega de disciplina, grupo 1 completo ou expansão concluída.
Inventário do destino, dossiês/fontes e conferências ainda têm pendências explícitas.

Não acrescentar conteúdo às fontes de outubro para esta expansão. PDF/gabarito e
recibo privados ficam no diretório de trabalho ignorado do operador; não em fontes
públicas, assets, migrações ou builds. A existência de um link não prova permissão.

A #161 foi integrada à main pelo PR #176 e o grupo 2 retomou o teto 25 sobre esse
contrato por assunto sem cargo, com V15 autorizada. Grupo 8 ainda depende de 2–7.
Código/conteúdo ainda não publicado; revisão independente e integração pendentes.

## Entrega final — 13/11/2026

- `packages/`: 102 pacotes finais (2.587 itens) da auditoria v10, com `manifest.json`,
  requisição de prévia, digests, seleção e dossiê por assunto/finalidade.
- `final/`: conferência (`final-check.*`), inventário das 530 originais, revisões
  individuais, auditoria v10, leitor/prévia e conferência às cegas de Matemática.
- `local-files-manifest.json`: SHA-256 dos arquivos mantidos só no computador local.
- Os JSONL e `reviews.json`/`pair-decisions.json` deste diretório continuam vazios: a
  edição de trabalho fica no acervo local; os pacotes são o artefato versionado.
  [Relatório](../../../docs/question-bank-expansion-content.md).
