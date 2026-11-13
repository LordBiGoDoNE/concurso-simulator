# Revisão final de código — épica combinada `question-bank-expansion-curation`

Specs: `question-bank-expansion` ([#173](https://github.com/LordBiGoDoNE/concurso-simulator/issues/173)) e
`administrative-question-curation` ([#177](https://github.com/LordBiGoDoNE/concurso-simulator/issues/177)).
PR `[SPEC]`: [#202](https://github.com/LordBiGoDoNE/concurso-simulator/pull/202), `epic/question-bank-expansion-curation` → `main`.
Evidência estruturada: [`spec-review.json`](spec-review.json). Este relatório não autoriza merge, publicação nem arquivamento.

## Escopo e cobertura

- Base `bfcda06b2a2fb7bfb9aeaf3b25f819b2e7ed7bca` (main, merge-base conferido); head revisado
  `d1ea518648048f489513ce937cba18ff934f322f`; `diff_sha256` `d19b6a46e0ed7d1b65e5678025a43aa0714fba3ae511a2ae4b782cfe3cfe602d`
  (exclui somente os dois arquivos de evidência). 561 arquivos; ~345 mil linhas geradas em pacotes/evidências.
- Revisor: Claude Opus 5.5 (`anthropic/claude-opus-5-5#medium`), esforço medium, subagente novo, contexto
  independente e somente leitura, conforme a decisão de 12/11/2026 no AGENTS.md e o perfil `spec-reviewer`.
  Recebeu specs, AGENTS.md, ADR 0001, checklist HTTP, `spec-review.md` e base/head imutáveis; sem conversa,
  justificativas, relatórios de verificação nem revisões anteriores na primeira leitura.
- Risco: **crítico** (elegibilidade em lote, autorização ADM, replay/recibos, captura persistida, Flyway V15–V17).
- Cobertura: requisitos das duas specs; ADR 0001; segurança (cadeia, sessão/versão, CSRF, token HMAC, limites,
  rotas públicas); persistência (V15–V17, FKs, imutabilidade, travas, tudo-ou-nada, replay, codec); OpenAPI;
  backup/restore e CLI; ferramentas manuais fora do bootJar; integridade dos 102 pacotes; testes e manutenção.

## Achados e triagem

Nenhum bloqueador, alto ou médio. Triagem aprovada pelo usuário em 13/11/2026.

| ID | Severidade | Achado | Disposição |
| --- | --- | --- | --- |
| L1 | opcional | Guarda de referências não compara host com os hosts de prova da proveniência (0 casos atuais; igualdade de URL e caminho `/provas/` já barram) | adiado |
| L2 | opcional | Nonces de recibos em `destination-coverage-corrected.json` | rejeitado: mantidos; identificadores de banco efêmero, sem conteúdo/ator/segredo |
| L3 | opcional | Habilitação local em PostgreSQL efêmero | rejeitado: modelo de ambiente paralelo decidido em 12/11; caixas = implementado/verificado |
| L4 | opcional | Export de curadoria sem semáforo (somente ADM) | adiado |
| L5 | opcional | Contagens de testes anteriores aos últimos commits | corrigido: suítes reexecutadas no head (abaixo) |
| L6 | opcional | Fixture `--restore` aceita qualquer dump (teste, loopback) | adiado |

## Evidências e limitações

Executado pelo coordenador na árvore de `d1ea518` (idêntica à de `bf53840`, head do #200 com CI 12/12 verde):

- backend `gradlew test`: 362 aprovados, 0 falhas, 1 ignorado histórico;
- frontend `vitest`: 238/238; `npm run build` ok;
- Python `test_question_expansion*`: 25/25; `test_local_app_data`: 11/11 (21 erros ambientais do Windows em
  `test_spec_review`/`test_question_extraction`, inalterados no diff, verdes no CI);
- `check_backup_schemas.py`: V10–V17 aprovados;
- `question_expansion_final.py`: 0 problemas (102 células, 2.587 itens, 530 originais);
- bootJar sem classes de teste/ferramentas;
- habilitação local com os pacotes do head: 2.587 elegíveis iguais aos fingerprints, 102/102 células ≥25/≥5.

Limitações: revisão estática (sem testes/build/banco/rede pelo revisor); sem revisão editorial das questões;
conteúdo `AI_ONLY` sem revisão humana; ambiente local efêmero; o check em `main` emite apenas
`spec-review-tests` e não certifica qualidade semântica.

## Revalidação

Histórico das rodadas sobre a mesma base:

1. head `6aafbce`: M1, B1, B3 e B4 corrigidos no #195 (triagem aprovada);
2. head `13cc3be`: A1 (referências corretivas na prática aberta), M1, B1, B3 corrigidos no #199; B4 rejeitado; B2/B5 adiados;
3. head `463977a`: M1 (evidência de habilitação desatualizada) e B1–B4 corrigidos no #200;
4. head `d1ea518`: esta rodada, sem bloqueador/alto/médio.

Qualquer mudança de base ou do diff (código, docs, testes, CI, pacotes) após `d1ea518` invalida este relatório e
exige nova revalidação independente; commits que alterem somente estes dois arquivos não mudam o digest.
Antes do merge, o coordenador executa `validate_report` com base/head/PR reais.
