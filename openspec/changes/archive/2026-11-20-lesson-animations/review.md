# Review — lesson-animations

Evidência de revisão conforme `revisao-e-integracao.md`, seção 7. Issue: #245.

## Resumo

- Head revisado (último ponto conferido pelo `spec-reviewer`): `93168c4` (tree `2c4edc53aec4c58e47c995cab07ff001ebd7216a`).
- Épica: `epic/lesson-animations` em `04a3c92` (artefatos da spec), criada da `main` `85c2d2c`.
- Piloto entregue: 9 animações (I02, I05, I07, I08, I09, I10, M11 e duas em M12), traço à mão, SVG declarativo (SMIL).
- Teste local pelo usuário: sim (resposta registrada na #245). Resultado: ver "Teste do usuário".

## Modelos

- Implementação, autoria das animações e correções (`finding-fixer`): Claude Opus 5.5.
- Revisão por grupo (`group-reviewer`): Claude Sonnet 5.5.
- Revisão final e reverificação (`spec-reviewer`): Claude Opus 5.5, esforço medium.

## Revisões por grupo

| Grupo | PR | Base → head revisado | Achados e triagem |
| --- | --- | --- | --- |
| 1. Formato seguro e compilador | #246 | `04a3c92` → `6ca06db` | M1 referências SMIL validadas contra a aula inteira no leitor: confirmado, corrigido (`fc4e0bf`). M2 `href` em elemento de animação e ID duplicado aceitos pelo compilador: confirmado, corrigido (`fc4e0bf`). |
| 2. Ferramenta e I05 | #247 | `fc4e0bf` → `c91809a` | M1 contrastes do guia fora da fórmula WCAG e texto colorido sobre fundo suave: confirmado, corrigido (`bfeb52f`). M2 grade da planilha de I05 imprecisa: confirmado, corrigido (`7a5a972`). Rodada extra do delta (`c91809a..7a5a972`, mais de 100 linhas): resolvidos, sem achados novos. |
| 3. Componente no leitor | #248 | `7a5a972` → `e42f005` | M1 ciclo não voltava a reproduzir após pausar com movimento reduzido: confirmado, corrigido (`e9fb251`). M2 `aria-pressed` em botão de rótulo variável: confirmado, corrigido. M3 testes fracos do ciclo: confirmado, 7 casos novos. |
| 4. I02, I07, I08 | #249 | `e9fb251` → `cb4d9f1` | Sem achados. |
| 5. I09, I10 | #250 | `cb4d9f1` → `c070b01` | Sem achados. |
| 6. M11, M12 | #251 | `c070b01` → `b93843a` | Sem achados. |
| 7. Verificação final e documentação | #252 | `c7fcb86` → `93a90d7` | Sem achados. |

## Teste de ponta a ponta (antes da revisão final)

- e2e no topo da cadeia: a comparação exata de `getCurrentTime()` (float32) falhava em I10. Confirmado e corrigido no grupo 3 (`03907d7`, `expect.closeTo`), propagado por merge.
- App local Docker em http://127.0.0.1:8088, com imagens do topo da cadeia:
  - **Antes da troca:** backup `20261119T133833Z-31774e5b093a.dump` (schema 17). Depois da troca, as contagens das 27 tabelas não têm nenhuma diferença; o backend não mudou e nenhuma migração rodou.
  - **Checagens:** 27 (9 animações × claro 1280 px, escuro 360 px, claro com movimento reduzido), todas OK, sem erros e sem rolagem horizontal da página.
  - **Rodada repetida:** com o head revisado `93168c4`, todas OK de novo.

## Revisão final

- **Revisor:** `spec-reviewer`, Claude Opus 5.5, esforço medium, contexto independente, somente leitura.
- **Base/head:** cadeia `04a3c92` → `93a90d7`; épica × `main` `85c2d2c` → `04a3c92`.
- **Cobertura:**
  - lista branca SMIL e paridade compilador/leitor;
  - manifesto e inserção por âncora;
  - ferramenta e limites;
  - componente e acessibilidade;
  - fidelidade das 9 animações ao texto das aulas;
  - testes, documentação e ausência de mudança em HTML legado, `/material/`, `stable`, backend, CI e deploy.
- **Limitações declaradas pelo revisor:** não executou testes nem navegador; a fidelidade foi conferida pelas seções âncora, por amostragem.
- **Achados:**
  - **M1 (médio):** I05 não mostrava a referência mista prevista na proposta e na task 2.4. Confirmado e corrigido no grupo 2 (`3a6b35e`): nova etapa `=B4*G$2`.
  - **M2 (médio):** I02 mostrava um estado "esmaecido" de arquivo, misturado ao histórico de texto. Confirmado e corrigido no grupo 4 (`b6d2956`).
  - **B1 (baixo):** a lista branca inclui `additive`, `accumulate`, `pathLength`, `visibility` e `stroke-dashoffset` além do enumerado em D2. Fica registrado aqui: são inofensivos (não animam links, estilos nem eventos), têm paridade nos dois validadores e são usados pela ferramenta (traço que se desenha e visibilidade).
  - **B2 (baixo):** o leitor aceita tags HTML dentro da figura animada. Vai para o backlog na #253.
  - **B3 (baixo):** colisão hipotética de IDs `-title`/`-description`. Vai para o backlog na #253.
  - **B4 (baixo):** tarefas sem marcação. Corrigido (`93168c4`).

## Falha do CI remoto

- **PR #248, run 44535041523:** falha intermitente no Vitest ("com movimento reduzido… o estudante pode reproduzir").
- **Causa:** defeito do componente. Um "Reproduzir" feito antes dos efeitos de montagem era desfeito pelo efeito de visibilidade.
- **Correção:** `38ab2d9` (`finding-fixer`). A intenção só é descartada numa saída real da tela. Há teste de regressão, e o arquivo passou 60 de 60 vezes sob carga.

## Reverificação do delta

- **Revisor:** `spec-reviewer` (Claude Opus 5.5, medium), sobre `93a90d7..93168c4`.
- **Resultado:** M1, M2, a falha do CI e B4 resolvidos; nenhum bloqueador ou alto novo.
- **Observações opcionais (sem ação):** a descrição de I05 cita `=B4*G$2` em C4, embora a barra de fórmulas mostre só C5 e C6 nessa fase; o teste de montagem fora de `act` restaura `IS_REACT_ACT_ENVIRONMENT` para `true`.

## CI local (`act`) e testes

- **Python (Linux, `act` job spec-review-tests):** 142 testes OK. O passo do portão de revisão falha localmente por falta do contexto de PR (esperado).
- **`study-material` (`act`):** sucesso.
- **`frontend`:** verificado nativamente, porque o download do Chromium dentro do container falha por certificado TLS da rede local. No head revisado: Vitest 362/362, `npm run build` OK, e2e 67/67.
- **Geradores:** `sketch_svg.py --check` (9 animações) e `compile_native_lessons.py --check` verificados.
- **Não rodados localmente:** `backend`, `identity-browser` e `local-delivery`, sem mudança nessas áreas; os três passam no CI remoto.

## CI remoto

Conferido em 2026-11-20, antes do pacote, nos heads da cadeia. Todos os checks passaram (`backend`, `frontend`, `identity-browser`, `local-delivery`, `spec-review-tests`, `static-pages`, `study-material`):

| PR | Base | Head |
| --- | --- | --- |
| #246 | `epic/lesson-animations` | `fc4e0bf` |
| #247 | `task/lesson-animations/format` | `3a6b35e` |
| #248 | `task/lesson-animations/sketch` | `fbd61a3` |
| #249 | `task/lesson-animations/reader` | `bbc2877` |
| #250 | `task/lesson-animations/informatica-1` | `fe44b37` |
| #251 | `task/lesson-animations/informatica-2` | `5f6c08f` |
| #252 | `task/lesson-animations/logica` | `93168c4` |

A falha intermitente anterior do #248 está descrita em "Falha do CI remoto".

## Teste do usuário

- **Ambiente:** app local Docker em http://127.0.0.1:8088, com as imagens do head revisado `93168c4`. Roteiro entregue: as 9 animações nos modos de etapas e ciclo, tema escuro, tela estreita e movimento reduzido.
- **Resultado (2026-11-20):** o usuário respondeu "testei, tudo certo". Não houve achados, então não houve rodada de correção nem reverificação; o head revisado continua `93168c4`.
