# Regras do projeto concurso-simulator

As **regras globais** do usuário valem primeiro: `~/.claude/rules/*.md` e os agentes em `~/.claude/agents/` (OpenSpec, épicas e PRs encadeados, planejamento/autorização, revisão e integração, teste local, hotfix, issues, arquitetura de backend, Java/Spring, CI/deploy/segredos e precedência de regras).

Este arquivo só **detalha** o que é próprio do projeto e registra as **exceções** aprovadas. Conflito sem exceção registrada abaixo: vale a regra global (`precedencia-de-regras.md`).

## Contexto

- Roadmap, critérios de entrega e decisões abertas: `docs/application-reformulation.md`. Uma etapa do roadmap não autoriza spec nem implementação.
- Stack e versões escolhidas pelo usuário: ver `openspec/config.yaml` (Java 25 LTS, Spring Boot, PostgreSQL 18, Flyway, Gradle Wrapper 9.8.0 com Kotlin DSL, React/TypeScript/Vite).

## Documentos de estado

Declaração exigida por `pendencias-e-encerramento.md`.

- `docs/application-reformulation.md`: estado entregue, roadmap e seção "Registro de entregas" (conferida pelo job `openspec` do CI).
- `README.md`: visão geral e como executar.
- `openspec/config.yaml`: contexto e estado atual do projeto para o OpenSpec.
- Registro de entregas: docs/application-reformulation.md
- Branches permanentes: main, stable, pages-store

## Arquitetura (detalhamento das regras globais)

- Antes de implementar ou revisar backend, ler `docs/architecture/adr/0001-domain-and-application-boundaries.md` (decisões aprovadas em 2026-10-08).
- Antes de criar ou revisar endpoints, ler `docs/architecture/http-endpoint-review.md` e aplicar seu checklist.
- Identidade OAuth opcional: ao recuperar conflito de integridade, preservar o rollback e não tratar toda falha de integridade como identidade já vinculada.

## Teste local (detalhamento de `teste-local.md`)

- Habilitar para teste todo o escopo autorizado, inclusive a elegibilidade das questões preparadas/conferidas. A habilitação local dispensa nova pausa para aprovação editorial local; registrar seleção, fingerprints/digests e destino ao habilitar. Preservar os dados existentes.
- Habilitação local não é aceite final, aprovação editorial de conteúdo real, merge nem publicação.

## Site estático, GitHub Pages e previews

- `stable` preserva o site estático anterior; `main` recebe a stack completa e não publica no Pages (decisão do usuário de 2026-10-23).
- Correções independentes do site estático seguem `hotfix.md` com branch de destino `stable`, sem trazer a stack completa para o site estático.
- Preview estático é manual: Actions → Publish GitHub Pages → Run workflow, executado a partir de `stable`, informando a branch de material no campo `branch`. `main` e `pages-store` não são entradas publicáveis.
- A referência do workflow e a única branch autorizada no ambiente `github-pages` são `stable`; previews usam o material de `stable` como produção. A cópia do workflow em `main` serve à descoberta pelo Actions e não publica nessa referência.
- Previews atuais são do material estático; não executam Java/PostgreSQL nem publicam a aplicação React. Deployment da aplicação completa tem spec própria.
- Preservar as aulas, `pages-store` e os previews já armazenados.

## Exceções às regras globais

| # | Regra global afetada | Regra do projeto | Motivo | Data | Aprovação |
| --- | --- | --- | --- | --- | --- |
| E1 | `ci-deploy-e-segredos.md`: deploy manual por padrão | Push em `stable` publica automaticamente o site estático no GitHub Pages | Manter o site estático legado em produção com o fluxo que já existia antes da reformulação | 2026-10-23 | Decisão do usuário de 2026-10-23 |

## Histórico (não são regras vigentes)

- Reformulação entregue à `main` pelo #133 (2026-10-23); `epic/application-reformulation` é histórica e não é reutilizada.
- GitHub Stacks (piloto #33/#34, adoção em question-bank #36) foi substituído por PRs encadeados comuns: `docs/stacks-pilot.md`.
- Revisões SOL/Astra, revisão por lote, classificação de risco e gate com `spec-review.json`/digest: `docs/architecture/spec-review.md`. Revisões já executadas permanecem histórico verdadeiro.
- Medição de cota ChatGPT/Codex encerrada em 2026-11-09: `docs/review-quota.md`.
- Automação legada de revisão (`spec-review.yml`, `scripts/check_spec_review.py`) removida pela spec `remove-legacy-review-gate` (#256, 2026-11-23); `scripts/tests` roda no job `scripts-tests` do `application.yml`. Os `spec-review.json/md` antigos são histórico verdadeiro; detalhes em `docs/architecture/spec-review.md`.
- Ruleset `24766642` (alvo só `refs/heads/epic/application-reformulation`, check `spec-review`) mantido por decisão do usuário de 2026-11-23. Está órfão: a branch e o check não existem mais. Desativá-lo ou removê-lo exige aprovação específica (`planejamento-e-autorizacao.md`).
- Lotes e revisão SOL por lote: `docs/implementation-batches.md`.
- `study-experience-redesign`: base inicial em `epic/application-reformulation` 1817673 (árvore idêntica à `main` c8efceb); PR #147 retargetado para `main` com autorização do usuário.
- Primeira épica combinada: `epic/question-bank-expansion-curation` (#173 + #177).
- Execução contínua autorizada para `optional-google-identity` e a rodada inicial de question-bank (1.2, 1.3, 2.1–2.3 sobre #38) antecedem a regra global de autorização única por spec.
- Artefatos de specs abertas antes de 2026-11-17 (proposal/design/tasks) que citam Stacks, SOL, lotes ou `spec-review.json` registram o processo da época; a execução restante dessas specs segue as regras globais vigentes.
- Commits e PRs históricos são preservados. Branches seguem `revisao-e-integracao.md` (seção 8): as já contidas na `main` são apagadas no encerramento; branches permanentes: ver "Documentos de estado".
