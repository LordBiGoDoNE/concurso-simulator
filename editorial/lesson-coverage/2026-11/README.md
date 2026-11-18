# Conceitos cobrados pelas questões, por assunto

`concepts.jsonl` lista o que as questões do acervo de expansão de 2026-11 exigem
do estudante em cada um dos 51 assuntos. É a referência usada para aprofundar as
aulas (spec `lesson-depth-alignment`, issue #230).

A lista é curada: foi levantada pela leitura de todas as questões de simulado e de
prática de cada assunto, com assistência de IA, sem revisão humana. Ela descreve o
que as questões cobram; não altera questões nem aulas.

## Formato

Uma linha JSON por conceito:

| Campo | Conteúdo |
| --- | --- |
| `id` | `<ASSUNTO>-<slug>`, único |
| `topicId` | assunto (`P01` … `S11`) |
| `concept` | nome curto do conceito, com o detalhe técnico |
| `kind` | `termo`, `dispositivo` (norma e artigo), `comando` (atalho, função, instrução, menu), `procedimento` ou `calculo` |
| `patterns` | expressões regulares, sem diferenciar maiúsculas, aplicadas ao texto da aula compilada; todas precisam aparecer para o conceito contar como coberto |
| `questionKeys` | chaves de origem das questões que cobram o conceito |
| `outOfScope` | opcional: justificativa para o conceito não entrar na aula |

Toda questão do acervo aparece em pelo menos um conceito do seu assunto. Toda
questão com base legal (`legalBasis.provision`) aparece também em pelo menos um
conceito `dispositivo`; `--check-structure` falha quando essa ligação falta. A
validação não confere o que os padrões desse conceito exigem (ver Limites).

## Uso

```bash
python scripts/lesson_coverage.py                    # resumo
python scripts/lesson_coverage.py --write-report     # regrava docs/lesson-coverage.md
python scripts/lesson_coverage.py --check-structure  # lista válida e relatório atualizado
python scripts/lesson_coverage.py --check            # idem, e nenhum conceito ausente
python scripts/lesson_coverage.py --questions I08    # questões de um assunto, para curadoria
python scripts/lesson_coverage.py --lesson I08       # texto da aula compilada
```

O comando só lê os pacotes de questões e as aulas; o único arquivo que grava é o
relatório, com `--write-report`.

## Histórico da cobertura (spec `lesson-depth-alignment`)

| Etapa | Conceitos cobertos | Ausentes |
| --- | ---: | ---: |
| Linha de base, depois da revisão da lista (grupo 1) | 208 de 1.687 | 1.479 |
| Informática I01–I05 (grupo 2) | 359 | 1.328 |
| Informática I06–I10 (grupo 3) | 491 | 1.196 |
| Conhecimentos Administrativos A01–A08 (grupo 4) | 890 | 797 |
| Conhecimentos Escolares S01–S06 (grupo 5) | 1.076 | 611 |
| Conhecimentos Escolares S07–S11 (grupo 6) | 1.282 | 405 |
| Matemática e Raciocínio Lógico M01–M12 (grupo 7) | 1.546 | 141 |
| Português P01–P10 (grupo 8) | 1.687 | 0 |

A partir do grupo 8, `--check` passa: nenhum dos 51 assuntos tem conceito ausente
e nenhum conceito foi marcado como fora de escopo.

## Limites

- Padrão presente indica que a aula menciona o conceito, não que a explicação é
  boa. A qualidade é conferida na revisão do conteúdo.
- Parte dos conceitos `dispositivo` identifica o dispositivo só pelo artigo ou só
  pelo conteúdo, sem exigir a norma e o artigo nos padrões. Por isso, "coberto"
  não prova que a aula cita a norma e o artigo; isso é conferido na revisão do
  conteúdo.
- O texto de esquemas (SVG) e de tabelas conta como texto da aula.
- A lista reflete o acervo de 2026-11. Questões novas exigem atualizar a lista.
