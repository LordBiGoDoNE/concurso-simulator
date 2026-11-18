# Review — lesson-depth-alignment

Evidência de revisão conforme `revisao-e-integracao.md`, seção 7. Issue: #230.

## Resumo

- Último head revisado pelo `spec-reviewer`: `0fc9c3c` (tree `7f03f86b5bca6968bb19ae32a488f54d6d38d849`).
- Head final da cadeia antes deste `review.md`: `487ef12` (tree `35934f09de2f4f7357ca9315f9acca8df644cd6f`). Entre `0fc9c3c` e `487ef12` entra só a segunda correção do CI remoto (`46b6b01`, teste Java e documentação), verificada por execução e não reverificada pelo `spec-reviewer` (limite de uma rodada por origem); ver "Falha do CI remoto".
- Épica: `epic/lesson-depth-alignment` em `27a9bcf` (artefatos da spec), criada da `main` `fc3b9e4`.
- `main` avançou depois disso para `9ed5e85` só com o arquivamento de `administrative-question-import` (#231, arquivos de `openspec/`); será integrada conforme a seção 5.
- Cobertura: 1.687 de 1.687 conceitos cobertos nas 51 aulas (linha de base 208); `python scripts/lesson_coverage.py --check` passa.
- Teste local pelo usuário: não (resposta registrada na #230).

## Modelos

- Escrita das aulas: grupos 1–3 e A01, A03, A06, A07 com Claude Fable 5.2; a partir da retomada após limite de uso (18/11/2026), Claude Opus 5.5 por decisão do usuário (A02, A04, A05, A08 e grupos 5–8).
- Revisão por grupo (`group-reviewer`): grupos 1–3 com o modelo da sessão (Claude Fable 5.2); grupos 4–8 com Claude Opus 5.5.
- Correções (`finding-fixer`): grupos 1–3 com Claude Fable 5.2; a partir do grupo 4, Claude Opus 5.5.
- Revisão final e reverificações (`spec-reviewer`): Claude Opus 5.5, esforço medium.

## Revisões por grupo

| Grupo | PR | Base → head revisado | Achados e triagem |
| --- | --- | --- | --- |
| 1. Matriz de cobertura | #233 | `27a9bcf` → `aaddff2` | M1 padrões que casavam por acaso: confirmado, corrigido (`31662af`). M2 questões com base legal sem conceito `dispositivo`: confirmado, corrigido (`1a0b342`, `3be437c`); parte ampla adiada → #232. |
| 2. Informática I01–I05 | #234 | `3be437c` → `9251203` | A1 (alto) atalho de copiar/colar formatação: refutado com fonte oficial; precisão de versões acrescentada (`036d2a9`). M1 atalhos da guia Revisão: refutado. M2 exemplo que repetia questão em I05: confirmado, corrigido (`e2e6be2`); eco em I04 refutado. M3 prompt do cmd com nomes localizados: confirmado, corrigido (`8139323`). |
| 3. Informática I06–I10 | #235 | `8139323` → `86fff77` | T1 (e2e) título duplicado "Referências e…" em I07: confirmado, corrigido. G3-1 (alto) Ctrl+Alt+T: parcialmente confirmado (interface pt-BR × inglês), corrigido. G3-2 rótulos apoiados em instalação local: confirmado, corrigido. G3-3 caminhos do Firefox: refutado. G3-4 e G3-5: confirmados, corrigidos. Tudo em `0e028dd`. |
| 4. Administrativos A01–A08 | #236 | `0e028dd` → `7be5b49` | M1 transcrição do item 12 da IN 205/1988: refutado (texto oficial confere). |
| 5. Escolares S01–S06 | #237 | `7be5b49` → `da358fd` | M1 LDB art. 24, § 1º: refutado (redação da Lei 14.945/2024). M2 Resumo de S04 atribuía regra estadual à LDB/CF: confirmado, corrigido (`f9b8664`). V1 origem oficial do manual SEDUC-SP: confirmada, página oficial acrescentada (`f9b8664`). |
| 6. Escolares S07–S11 | #238 | `f9b8664` → `a774bdf` | Sem achados. |
| 7. Matemática M01–M12 | #239 | `a774bdf` → `c56be24` | F1 regra de "caber" em M09: confirmado, corrigido. F2 "100 reais" em `math_content.py`: confirmado, corrigido. Ambos em `fa04aec`. |
| 8. Português P01–P10 | #240 | `fa04aec` → `9ebfa28` | G8-M1 "possível" em superlativos (P07): confirmado, corrigido com divergência declarada (`2284d34`). |
| 9. Consolidação | #241 | `2284d34` → `a74a586` | Coberto pela revisão final. |

Nenhuma rodada extra de revisão por grupo foi necessária (correções abaixo de ~100 linhas autorais e fora de área crítica).

## Revisão final

- Revisor: `spec-reviewer`, Claude Opus 5.5, esforço medium, contexto independente, somente leitura.
- Base/head: cadeia `27a9bcf` → `a74a586`; épica × `main` pelo merge-base `fc3b9e4` → `a74a586`.
- Cobertura: requisitos e cenários da spec, D1–D7, código (`lesson_coverage.py`, `enrich_math.py`) e testes, amostragem de conteúdo em todas as disciplinas, comparação de sequências de 8–10 palavras entre enunciados e aulas, alcance do diff.
- Limitações declaradas: sem rede (normas de 2024–2026 e alguns atalhos pt-BR não conferidos pelo revisor; foram conferidos pelos autores com fonte e data); amostragem parcial do conteúdo; sem execução de testes.

| Achado | Severidade | Triagem | Resultado |
| --- | --- | --- | --- |
| M1 aulas reproduziam cenário/redação de questões (I01, A05, M05) | médio | confirmado | corrigido nas branches de origem: I01 `4101e6e`, A05 `320c9be`, M05 `ad25067` |
| B1 total "51 assuntos" fixo no relatório | baixo | confirmado | corrigido `51a0996`, com teste |
| B3 relatório sem apontar o histórico | baixo | confirmado | corrigido `51a0996` |
| B2 cobertura sem proteção contra regressão no CI | baixo | adiado | issue #242 |
| B4 ressalva genérica de versão nas Referências de Informática | baixo | registrado | texto padrão comum a todas as aulas; cada aula de Informática declara a versão de referência |

Quatro coincidências restantes em A05 (`aracar-aa-q39`, `aracar-aa-q29`, `autoral-a05-39`, `morato-q23`) foram conferidas: são transcrições da Lei 8.159/1991 e do Decreto 10.278/2020 e definição do Dicionário Brasileiro de Terminologia Arquivística, que a própria questão cita. Não são cópia de enunciado.

### Reverificação do delta (revisão final)

- Delta `a74a586` → `3631e3b` (correções propagadas por merge a partir das branches de origem).
- Resultado: M1, B1 e B3 resolvidos; merges sem conteúdo além das correções (mesmo patch-id); documentos gerados consistentes com o manifesto nas 9 branches.

## Falha do CI remoto (origem: CI remoto)

- Achado: `spec-review-tests` (`test_curated_bank_verification`, `test_question_curation`, `test_question_expansion`) e `backend` (`CuratedPackageTests`) falhavam a partir do grupo 2 com "Snapshot editorial mudou". As 250 questões curadas de 2026-10 são extraídas dos exercícios dentro dos HTML das aulas, com o SHA-256 do arquivo inteiro na proveniência e na fingerprint; a edição das aulas mudou esses hashes.
- Causa da detecção tardia: as verificações locais por grupo rodaram só os módulos de teste das aulas, não `unittest discover -s scripts/tests` completo.
- Decisão do usuário (18/11/2026): "Fixar fontes antigas".
- Correção `0f3eda6` (base da cadeia), propagada por merges limpos até `0fc9c3c`: 104 cópias byte a byte da base `fc3b9e4` em `editorial/question-bank/2026-10/legacy-sources/` (SHA-256 iguais a `sourceFiles`), curadoria 2026-10 lendo delas, `check_links.py` ignorando a pasta, `.gitattributes` `-text`, testes `FrozenSourcesTests`; decisão D8 no design.
- Verificação: saída da curadoria byte a byte igual à da base; `CuratedPackageTests` passou localmente (JDK 25); suíte Python completa sem falha nova em relação à base (21 erros locais no Windows existem também na base: `test_spec_review`, `test_question_extraction`).
- Reverificação do delta `3631e3b` → `0fc9c3c` (`spec-reviewer`, Opus 5.5 medium): resolvido; sem bloqueador; merges idênticos à correção (mesmo patch-id).
- O CI remoto em `0fc9c3c` ainda falhou no `backend`: `CuratedPackageTests` passava a raiz do repositório ao `QuestionPackageReader`, que recalcula o hash dos arquivos-fonte lendo as aulas vivas. O teste havia passado localmente só porque foi rodado na branch do grupo 1, sem aulas editadas.
- Segunda correção `46b6b01` (base da cadeia), propagada por merges limpos até `487ef12`: o teste passa ao leitor a pasta `legacy-sources`; nenhum código de produção alterado (`QuestionBankLocal` recebe a raiz por `--root`; o import administrativo HTTP não lê arquivos-fonte). Documentado em `docs/question-local-operations.md`, no README da 2026-10 e nos limites de D8: reimportar pacotes 2026-10 pela ferramenta local exige `--root editorial/question-bank/2026-10/legacy-sources`.
- Verificação por execução no cenário que falhou (aulas editadas, `0fc9c3c` + `46b6b01`): `CuratedPackageTests`, `QuestionLocalImportTests` e `LessonPracticePackageTests` com BUILD SUCCESSFUL (JDK 25); testes Python da curadoria OK; sem a correção, o teste falha como no CI.
- Não houve nova rodada do `spec-reviewer` para esse delta (limite de uma rodada por origem); a decisão fica com o usuário na aprovação do pacote.

## CI local (`act`)

- Imagem `catthehacker/ubuntu:act-latest`, sem segredos reais.
- `study-material` e `static-pages`: verdes em todos os grupos.
- `frontend` no contêiner: unitários e build verdes; e2e instável no contêiner (tempo de carga); por isso o frontend foi verificado também nativamente.
- Não verificados no `act`: `backend` e `identity-browser` (`./gradlew: Permission denied` no checkout Windows) e `local-delivery` (`git rev-parse HEAD` falha em worktree). Os testes Python de `local-delivery` foram rodados nativamente no grupo 1; `CuratedPackageTests` nativamente na correção do CI.

## Testes nativos

- Head do grupo 8 (`9ebfa28`): `npm test` 322/322; `npm run test:e2e` 39/39; `npm run build` e `build_pages.py` OK.
- Em grupos anteriores, a suíte Vitest teve falhas por limite de 5 s sob carga da máquina; isoladas, passaram; controle no head anterior reproduziu as mesmas falhas nas mesmas condições.
- `compile_native_lessons.py --check`, `lesson_coverage.py --check`, `check_links.py` e testes Python das aulas: OK no head revisado.

## Teste de ponta a ponta

- Pilha local separada (projeto `concurso-simulator-app-local-lda`, porta 8091, imagens do commit `2284d34`); o ambiente do operador (8088) não foi tocado.
- Leitor nativo: 51/51 aulas completas em 375 px sem rolagem horizontal; 16 aulas de todas as disciplinas conferidas em desktop; seções "Aprofundamento" no Mapa das aulas enriquecidas de Matemática.
- Painel "Pratique esta aula": consulta a API e informa corretamente ausência de questões (banco novo vazio).
- Não executado: prática com questões reais nesta pilha (carga do acervo depende de backup privado fora do Git; o restore disponível exige a mesma imagem de API). A spec não altera código da prática, da API nem do frontend além dos dados das aulas.
- Conteúdo: amostra reprodutível (`random.seed(20261118)`) de 20 questões, 4 por disciplina — 20/20 explicáveis pela aula.

## CI remoto

Todos os checks verdes (12/12 em cada PR: `backend` ×2, `frontend` ×2, `identity-browser` ×2, `local-delivery` ×2, `study-material` ×2, `static-pages`, `spec-review-tests`):

| PR | Head |
| --- | --- |
| #233 | `46b6b01` |
| #234 | `52266f1` |
| #235 | `164628b` |
| #236 | `d497f45` |
| #237 | `d3cb80d` (um teste e2e intermitente, `new-student.spec.ts` em 390 px, reexecutado e verde) |
| #238 | `0ede12d` |
| #239 | `273cd34` |
| #240 | `17da90c` |
| #241 | `487ef12` |

## Visualização pelo usuário

Instância separada (porta 8092, imagens de `487ef12`) aberta para o usuário conferir as aulas em 18/11/2026; o usuário aprovou o que viu. Instância desligada em seguida (volume preservado). Não substitui a aprovação do pacote.

## Questões com fundamentação a revisar (fora do escopo)

Registradas nos PRs de cada grupo (#234–#240): resoluções com base legal desatualizada ou imprecisa, atribuições de fonte não confirmadas e sugestões de reclassificação de assunto. Alterar questões está fora desta spec.
