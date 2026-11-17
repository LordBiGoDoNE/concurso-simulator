# Tasks

Issue: [#230](https://github.com/LordBiGoDoNE/concurso-simulator/issues/230).
Épica: `epic/lesson-depth-alignment`. Spec formalizada em 2026-11-17.
**Implementação não autorizada**: aguarda confirmação explícita de início e a
resposta sobre teste local. Uma confirmação cobre todos os grupos.

Cada grupo é um PR em rascunho. O grupo 1 parte da épica; os demais são
encadeados, cada um sobre o anterior, porque todos regeneram
`frontend/src/lesson-documents/manifest.json`. Em cada grupo de conteúdo: editar
os HTML de conteúdo, recompilar com `scripts/compile_native_lessons.py`, rodar
`--check`, a matriz, os testes do projeto e o CI local via `act`; revisão por
grupo antes de abrir o PR. JSON nativos e manifesto são identificados no PR como
gerados.

## 1. Matriz de cobertura aula × questões

- [x] 1.1 Levantar, por assunto, os conceitos cobrados pelas questões dos pacotes
  de simulado e prática e registrar em `editorial/lesson-coverage/2026-11/concepts.jsonl`.
- [x] 1.2 Implementar `scripts/lesson_coverage.py` (relatório e `--check`), somente
  leitura, com testes em `scripts/tests/`.
- [x] 1.3 Gerar `docs/lesson-coverage.md` com o estado inicial (linha de base) dos
  51 assuntos e documentar o uso do comando.

## 2. Informática I01–I05 — depende de 1

- [x] 2.1 Declarar a versão de referência e aprofundar I01 e I02 (arquivos,
  atalhos, Configurações, Área de Transferência, acessibilidade).
- [x] 2.2 Aprofundar I03 e I04 (formatação, tabulação, revisão, quebras de seção,
  tabelas, índices).
- [x] 2.3 Aprofundar I05 (referências, intervalos, nomes definidos).
- [x] 2.4 Recompilar, atualizar resumo/checklist/referências e registrar a matriz
  antes e depois no PR.

## 3. Informática I06–I10 — depende de 2

- [ ] 3.1 Aprofundar I06 (funções cobradas, com sintaxe, exemplo e resultado; erros).
- [ ] 3.2 Aprofundar I07 (tabela dinâmica, Análise Rápida, tipos de gráfico,
  classificação, duplicatas).
- [ ] 3.3 Reescrever o núcleo de I08 com leitura de VBA (objetos, métodos e
  argumentos cobrados, segurança e formatos).
- [ ] 3.4 Aprofundar I09 e I10 (mídia e narração; URL, cache, extensões, e-mail).
- [ ] 3.5 Recompilar, atualizar resumo/checklist/referências e registrar a matriz.

## 4. Conhecimentos Administrativos A01–A08 — depende de 3

- [ ] 4.1 A01–A03: CF art. 37, Lei 9.784/1999 e redação oficial, com os
  dispositivos cobrados conferidos na fonte.
- [ ] 4.2 A04–A06: IN SEDAP 205/1988, Lei 8.159/1991 e Lei 13.460/2017.
- [ ] 4.3 A07–A08: LGPD e Lei 4.320/1964 (receita, créditos adicionais, balanços).
- [ ] 4.4 Recompilar, atualizar resumo/checklist/referências com fonte e data, e
  registrar a matriz.

## 5. Conhecimentos Escolares S01–S06 — depende de 4

- [ ] 5.1 S01 e S04: LDB (arts. cobrados, reclassificação, frequência, educação
  bilíngue de surdos).
- [ ] 5.2 S02, S03 e S05: escrituração, documentos individuais e coletivos,
  carga horária, protocolo e arquivo.
- [ ] 5.3 S06: redação oficial escolar.
- [ ] 5.4 Recompilar, atualizar resumo/checklist/referências com fonte e data, e
  registrar a matriz.

## 6. Conhecimentos Escolares S07–S11 — depende de 5

- [ ] 6.1 S07 e S08: CF arts. 205–214 e ECA, com os dispositivos cobrados.
- [ ] 6.2 S09 e S10: LBI no atendimento escolar e Decreto 1.171/1994.
- [ ] 6.3 S11: Censo Escolar (papéis, prazos e operações cobrados).
- [ ] 6.4 Recompilar, atualizar resumo/checklist/referências com fonte e data, e
  registrar a matriz.

## 7. Raciocínio Lógico M10–M12 e ajustes de Matemática — depende de 6

- [ ] 7.1 M10 e M11: sequências e ciclos, tabela-verdade, equivalências, negação
  de quantificadores.
- [ ] 7.2 M12: três conjuntos, "exatamente dois", casa dos pombos.
- [ ] 7.3 M01–M09: somente os conceitos que a matriz apontar como ausentes.
- [ ] 7.4 Recompilar, conferir fórmulas acessíveis e registrar a matriz.

## 8. Português P01–P10 — depende de 7

- [ ] 8.1 P01–P03: coesão referencial, valor de conectivos, ambiguidade, parônimos.
- [ ] 8.2 P04–P06: casos cobrados de pontuação, ortografia/acentuação e classes.
- [ ] 8.3 P07–P10: casos especiais de concordância e regência, colocação
  pronominal e crase (obrigatória, proibida e facultativa).
- [ ] 8.4 Recompilar, atualizar resumo/checklist e registrar a matriz.

## 9. Consolidação e verificação final — depende de 8

- [ ] 9.1 Rodar a matriz completa: nenhum assunto com conceito ausente sem
  justificativa; atualizar `docs/lesson-coverage.md` com antes e depois.
- [ ] 9.2 Conferir que o diff da épica não toca questões, backend, migrações,
  workflows nem `stable`.
- [ ] 9.3 Teste de ponta a ponta no sistema local: abrir aulas alteradas de cada
  disciplina, fazer a prática do assunto e conferir, por amostra, que a resolução
  é explicável pela aula.
- [ ] 9.4 Atualizar `frontend/README.md` e a documentação de conteúdo afetada.
