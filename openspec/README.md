# OpenSpec no concurso-simulator

Ferramenta inicializada com OpenSpec **1.14.1**, perfil `core`, integração OpenCode e documentos em português. Não há dependência de OpenSpec para executar o site ou a futura aplicação.

## Usar a CLI sem instalação global

```sh
OPENSPEC_TELEMETRY=0 npx --yes @fission-ai/openspec@1.14.1 list
OPENSPEC_TELEMETRY=0 npx --yes @fission-ai/openspec@1.14.1 status --change bootstrap-application-stack
OPENSPEC_TELEMETRY=0 npx --yes @fission-ai/openspec@1.14.1 validate bootstrap-application-stack --strict
```

Os comandos nesta documentação desativam telemetria apenas para a execução, sem alterar preferências globais. A ferramenta está disponível via `npx`, não foi instalada globalmente.

## Fluxo no OpenCode

Abra o projeto como diretório da sessão para carregar `.opencode/commands/` e `.opencode/skills/`.

- `/opsx-explore`: discutir decisões e alternativas.
- `/opsx-propose`: preparar proposta, requisitos, desenho e tarefas.
- `/opsx-update`: ajustar artefatos após a revisão.
- `/opsx-apply`: implementar uma mudança expressamente aprovada.
- `/opsx-archive`: consolidar a mudança implementada e verificada.

Os arquivos gerados da ferramenta devem ser atualizados pela CLI; regras particulares do projeto ficam em `openspec/config.yaml`. Não use `apply` antes da aprovação, nem arquive propostas ainda não implementadas.

## Primeira proposta

`changes/bootstrap-application-stack/` contém apenas o planejamento da base local Java/PostgreSQL e a preservação das aulas. Todas as tarefas de implementação permanecem pendentes.

## Próximas mudanças, em incrementos

1. Base local da aplicação: proposta atual.
2. Identidade e regras de acesso, antes de persistir histórico privado.
3. Banco de questões, revisão editorial e importação controlada.
4. Geração de simulados, tentativas versionadas e correção.
5. Questões parametrizadas, com validação matemática.
6. Desempenho por conceito e revisão de dificuldades.

Hospedagem e previews completos da aplicação terão proposta própria antes de colocar Java e PostgreSQL online. O GitHub Pages atual publica apenas o material estático.
