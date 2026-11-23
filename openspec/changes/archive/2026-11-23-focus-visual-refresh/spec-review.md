# Revisão final de código: focus-visual-refresh

PR [SPEC] #204 (`epic/focus-visual-refresh` → `main`) · issue #193. Evidência
estruturada em `spec-review.json`.

## Resultado vigente (rodada 6)

- Base `cf8741f64b1852180e7a3737cd31a1743d6840f9` (main), head
  `1623e3c67ac0c9027b52de680dfffee4ea8f19d9`.
- Digest do snapshot: `eafb12faf37db75735144a119784ea54fcbda261ed8ae387d5735534e27fd899`
  (50 arquivos, +2045/−197).
- Revisor: Claude Opus 5.5 (`claude-opus-5-5`), esforço medium, contexto novo e somente
  leitura, sem conversa, `verification.md` ou relatórios anteriores.
- Resultado: nenhum achado bloqueador, alto ou médio. Dois achados baixos foram
  adiados para a issue #216, pelo critério do usuário.
- Risco: **crítico**. A mudança é só de frontend, mas toca a fronteira de exposição
  antecipada do gabarito (desfecho derivado no validador tardio, tipo `Result` e
  bundle inicial).

## Escopo e cobertura

Cada rodada revisou o diff completo do seu snapshot (`git diff base...head`), lendo
os arquivos no head e o código relacionado fora do diff:

- **Requisitos:** specs `visual-foundation` e `study-experience`, proposal, design e
  tasks, decisão 5.5 da biblioteca e requisitos anteriores de biblioteca e atalhos.
- **Arquitetura:** a mudança é só de frontend; ADR 0001/0002 e o checklist de
  endpoints não se aplicam.
- **Integração:** merge-tree com `main` sem conflitos; `lesson-practice-api` (teto de
  25) e fixtures de `main` compatíveis com a checagem de desfechos.
- **Segurança e privacidade:** `correctAlternativeId` só no validador carregado por
  `import()` tardio; realização sem desfecho; tema guarda só `light`/`dark`.
- **Persistência:** nenhuma, além da preferência de tema no `localStorage`.
- **Testes e manutenção:** unitários de tokens, contraste, tema, resultado, validador
  e biblioteca; e2e `focus-review`, `result-history` e `lesson-themes`; README e
  cleanup de listeners e observer.

## Revalidação

| Rodada | Head | Digest | Achados | Correções |
| --- | --- | --- | --- | --- |
| 1 | `fe34d8a` (base `471d3b1`) | `3d80ced0…` | 2 médios e 6 baixos | #206 |
| 2 | `8b1245b` | `73988d3f…` | 4 baixos | #207 |
| 3 | `29321f3` | `b0c57f30…` | 3 baixos | #208 |
| 4 | `e0a5dac` | `a227f96d…` | 3 baixos (L3 aceito como limite) | #211 |
| 5 | `b1b2e0c` | `924b213e…` | 5 baixos | #215 |
| 6 | `1623e3c` | `eafb12fa…` | 2 baixos adiados | issue #216 |

A partir da rodada 2 a base passou a ser `cf8741f`, porque a `main` recebeu o #205,
que só arquiva OpenSpec e não tem arquivos em comum com a épica.

Critério de triagem (decisão do usuário em 2026-11-13, após a rodada 5): na rodada 6,
só bloqueiam achados bloqueadores, altos ou médios, ou regressões causadas pelas
correções. Achados baixos novos vão para issue própria.

## Achados e triagem

- **Corrigidos (22):** FVR-M1, FVR-M2, FVR-L1 a L6, FVR2-L1 a L4, FVR3-L1 a L3,
  FVR4-L1, FVR4-L2 e FVR5-L1 a L5. Cada correção entrou por PR revisável para a
  épica, com teste de regressão sempre que viável.
- **Adiados (3), na issue #216:**
  - FVR4-L3 e FVR6-L2: arrastar a barra de rolagem durante os 3 s em que o painel
    é mantido em vista. Aceito como limite.
  - FVR6-L1: alcance do enunciado de 18px no resultado; o requisito é ambíguo
    entre a spec e o design.
- **Bloqueadores pendentes:** nenhum.

## Evidências e limitações

### Verificações do coordenador

- Snapshot recalculado em `cf8741f`/`1623e3c`, com o mesmo digest.
- CI do #204 no head `1623e3c`, com 12 de 12 checks verdes. O run `pull_request`
  testa o merge com a `main` (`8c40565`). O check `local-delivery` inclui a
  inspeção do bundle inicial sem `correctAlternativeId`.
- Frontend local, sobre árvore igual à do head:
  - `tsc` sem erros;
  - Vitest: 29 arquivos e 314 testes passaram;
  - Playwright: 38 testes passaram e 1 foi ignorado por design.
- Testes de regressão das correções conferidos sem a correção: falham como esperado.

### Limitações

- O revisor não executou testes, build, aplicação, rede nem banco. O contraste foi
  avaliado por tokens, CSS e amostragem.
- O perfil `spec-reviewer` não estava carregado na sessão. Foi usado um subagente
  `general-purpose` com Opus, esforço medium e o texto do perfil, por escolha do
  usuário.
- Na rodada 4, o revisor gravou um arquivo temporário em `/tmp`, fora do repositório.
  A partir da rodada 5, a instrução proíbe qualquer escrita.
- O revisor não verificou o design system publicado (tarefa 1.4) nem a legibilidade
  visual das figuras SVG no tema escuro.
- `spec-review-tests` e `validate_report` não certificam a qualidade semântica.
- `tasks.md` não marca 6.3 e 6.4, para não alterar o diff revisado. O estado fica
  na issue #193.
