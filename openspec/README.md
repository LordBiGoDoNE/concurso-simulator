# OpenSpec no concurso-simulator

O projeto usa OpenSpec **1.15.0**, com documentos em português. A OpenSpec não é necessária para executar o site nem a aplicação.

## Usar a CLI sem instalação global

```sh
OPENSPEC_TELEMETRY=0 npx --yes @fission-ai/openspec@1.15.0 list
OPENSPEC_TELEMETRY=0 npx --yes @fission-ai/openspec@1.15.0 validate --all
OPENSPEC_TELEMETRY=0 npx --yes @fission-ai/openspec@1.15.0 archive <change-name> -y
```

Esses comandos desativam a telemetria só durante a execução e não alteram preferências globais.

## Estrutura

- `specs/`: capacidades vigentes do sistema entregue.
- `changes/<change-name>/`: specs em andamento (proposta, design, deltas e tarefas).
- `changes/archive/`: changes entregues. Os deltas delas já estão aplicados em `specs/`.

As regras particulares do projeto ficam em `openspec/config.yaml`. Não implemente sem spec aprovada e não arquive changes ainda não integradas à `main`.

## Ciclo atual

Novas specs partem da `main` atualizada e seguem tarefas → `epic/<spec>` → `main`. O ciclo completo (spec, issue, autorização, revisão por grupo e final, pacote e aprovação únicos, arquivamento) segue as regras globais em `~/.claude/rules/` e o `AGENTS.md` do projeto, nessa precedência. A aplicação está em produção na VPS (ver `infra/vps-production/README.md`). `stable` publica apenas o material estático.

## Registro histórico do planejamento inicial (não vigente)

Os incrementos são revisados separadamente, mas a **entrega é única**: as branches das specs integram em `epic/application-reformulation`, não diretamente em main. A base inicial é a primeira spec desse pacote. Roadmap e critérios de entrega: `docs/application-reformulation.md`.

1. Base local da aplicação: proposta atual.
2. Identidade e regras de acesso, antes de persistir histórico privado.
3. Banco de questões, revisão editorial e importação controlada.
4. Geração de simulados, tentativas versionadas e correção.
5. Questões parametrizadas, com validação matemática.
6. Desempenho por conceito e revisão de dificuldades.

A hospedagem da aplicação completa foi entregue depois pela spec `vps-staging-delivery` (#224). O GitHub Pages continua publicando apenas o material estático.
