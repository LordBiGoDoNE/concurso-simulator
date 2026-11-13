# Acervo 2026-11 — validação integrada e entrega local (#173, grupo 8)

Data: 13/11/2026. Conteúdo e conferências por disciplina: [relatório de conteúdo](question-bank-expansion-content.md).
Evidências deste grupo: [`editorial/question-bank-expansion/2026-11/final/integrated/`](../editorial/question-bank-expansion/2026-11/final/integrated/).
Implementado/verificado localmente ≠ aceite do usuário, revisão final, integração ou publicação.

## 8.1 Auditoria conjunta

`scripts/audit_question_expansion.py --edition-root <acervo-local>/work/aggregate-wip` reexecutado
(4 min 45 s): `coverage.json` e `pairs.json` **byte a byte iguais** à auditoria v10 versionada.
102 células, 2.587 candidatas, 138/138 decisões de pares `CURRENT`, zero bloqueios; nenhuma
justificativa desatualizada. A conferência dos pacotes (`question_expansion_final.py`) cobre
fingerprints recalculados, identidades oficiais únicas (530) e o mínimo de 510 originais.

## 8.2 Ensaio da composição em PostgreSQL isolado

`QuestionExpansionCompositionRehearsal` (ferramenta de teste, fora da CI por usar os pacotes reais)
sobe PostgreSQL 18.6 novo, aplica as migrações e exercita os mecanismos existentes:

| Etapa | Resultado |
| --- | --- |
| Prévia inicial | 102 pacotes / 2.587 itens `NEW`, nenhuma escrita |
| Aplicação | 102 recibos, 2.587 questões/revisões **DRAFT**, 0 decisões, pools elegíveis vazios (24 s) |
| Prévia após aplicação e replay | 2.587 `IDENTICAL`; replay dos 102 nonces devolve o mesmo recibo, banco inalterado |
| Reaplicação com novo nonce | itens reutilizados, 0 questões novas, +1 recibo |
| Base desatualizada | prévia `STALE`; aplicação rejeita a seleção inteira, banco inalterado |
| Seleção/digest alterados | 4 confirmações divergentes (digest de pacote/seleção, ator, destino) recusadas; chave fora do pacote e SHA-256 de arquivo adulterado recusados pelo leitor |
| Rollback | falha injetada em `qb_import_file` reverte tudo (snapshot por tabela igual) |
| Concorrência (schema novo) | mesmo nonce → um recibo; nonces diferentes → mesma identidade, sem duplicatas |

```powershell
./backend/gradlew.bat -p backend --offline --no-daemon -I <init com JavaExec do classpath de teste> `
  -DtoolMain=br.com.concursosimulator.questionbank.QuestionExpansionCompositionRehearsal `
  "-DtoolArgs=--packages;<cópia de packages>;--output;<relatório>.json" runTestTool
```

A tarefa `runTestTool` vem de um init script local (JavaExec com `sourceSets.test.runtimeClasspath`);
o mesmo vale para `QuestionExpansionContentCheck`. Nenhuma tarefa nova foi adicionada ao build.
[Relatório](../editorial/question-bank-expansion/2026-11/final/integrated/composition-rehearsal.json).

## 8.3 Destino local, importação e decisões

Por decisão do usuário (12/11), habilitar 2.587 itens pela CLI `decide` (5.174 confirmações
digitadas) foi substituído pelas decisões em lote da #177; por isso a habilitação local ocorreu no
**ambiente paralelo da épica combinada** (#173 + #177, UI 5371 / API 19160), restaurado do backup
privado pré-#177 (SHA-256 `5b7042df…c6ef43`, fora do Git). O ambiente original (5393/19370,
54 sintéticas) não foi alterado. Reconciliação feita aqui, em leitura:

- 102/102 pacotes do registro de habilitação têm `requestSha256`, `packageDigest` e
  `selectionDigest` iguais ao `manifest.json` versionado;
- 102 recibos de importação; 2.587 decisões `SUBMIT` e 2.587 `APPROVE` com `review_kind = AI_ONLY`,
  em 204 lotes; Flyway V1–V17; nada habilitado por startup/upload/CI.

## 8.4 Cobertura elegível pelo banco e por HTTP

Consulta somente leitura (`begin read only`) no PostgreSQL do ambiente paralelo e chamadas aos
contratos públicos, em [destination-coverage.json](../editorial/question-bank-expansion/2026-11/final/integrated/destination-coverage.json):

- elegíveis da expansão: **2.587**, exatamente os fingerprints dos pacotes (0 ausentes, 0 extras,
  0 divergências de fingerprint ou célula); 102/102 células com ≥25 itens e ≥5 originais;
  1.294 simulado / 1.293 prática (≥1.275 cada; 2.587 ≥ 2.550); 530 originais, sem multiplicar
  por cargo/variante/revisão. As 52 sintéticas do ambiente não contam;
- HTTP, 51 assuntos: `lesson-practices/availability` (prática) e `question-bank/questions`
  (simulado) iguais ao banco em todos; `maximum = 5`; `Cache-Control: no-store`.

**Nenhuma célula pendente.** O banco do operador/VPS não foi consultado.

## 8.5 Testes

| Suíte | Resultado |
| --- | --- |
| Backend completo (`gradlew test`, PostgreSQL real por Testcontainers) | 340 aprovados, 0 falhas, 1 ignorado histórico (symlink) |
| Python de expansão (`test_question_expansion*.py`) | 22/22 |
| Frontend (`vitest run`) + `npm run build` | 238/238; build ok |
| Playwright `playwright.topic-practice.config.ts`, Chromium + Edge | 30/30 (visitante/conta, aula → prática de até cinco sorteadas → resultado, histórico, reinício, teclado/celular, filas/identidade) |

Para não ocupar as portas dos dois ambientes em uso, a fixture e a configuração aceitam
`TOPIC_PRACTICE_PORTS="ui,api,controle"` (padrões inalterados); execução feita com `5381,19180,19181`.
O Playwright usa a fixture sintética; o acervo real foi conferido por banco/HTTP (8.4). CI não
substitui revisão editorial ou semântica.

## 8.6 Teste local do usuário

- Ambiente com o acervo habilitado: http://127.0.0.1:5371/#/aulas/P01 (API 19160). Cada prática
  nova sorteia até cinco das ≥25 do assunto; simulados usam o acervo próprio de cada assunto.
- Recuperação: reiniciar a fixture com o dump pré-#177 (`--restore <dump> <sha256>`) e reaplicar o
  fluxo da #177 com os pacotes de `editorial/question-bank-expansion/2026-11/packages`
  (seleção/digests no `manifest.json`). Dumps e configurações ficam em
  `backend/build/question-expansion/local-destination-private/` do worktree `7ac1`, nunca no Git.
- Pendências fora deste grupo: teste/aceite do usuário, revisão final de código (Opus 5.5 medium)
  antes do `[SPEC]` da épica combinada para `main`, merges, publicação e carga na VPS.

## Limites

Revisões de conteúdo assistidas por IA (Claude), sem revisão humana; aprovação `AI_ONLY`
registrada como tal. Distratores fracos em A04 permanecem. A cobertura vale para o destino local
observado em 13/11/2026.
