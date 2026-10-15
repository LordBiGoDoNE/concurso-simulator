# Cota da assinatura ChatGPT/Codex nas revisões

Decisão atual do usuário (2026-10-15): somente SOL 6.1 xhigh executa as revisões,
revalidações e gates cumulativos, inclusive mudanças críticas e a épica geral,
conforme o [procedimento](architecture/spec-review.md). A política
anterior de revisão dupla por risco foi substituída, sem apagar execuções históricas.
Gate cumulativo final e revisão de lote continuam separados, com contexto independente
próprio. Medir cada revisão efetivamente executada; não disparar outro modelo por risco ou para comparar consumo,
nem iniciar lote, revisão extra, merge ou publicação apenas para testar a cota.

Histórico anterior ao padrão atual: exceção aprovada em 2026-10-14 para o gate
question-bank #63 com SOL xhigh no Codex. A tentativa Astra/OpenCode com 401 fica como
falha sem parecer; medir a execução SOL separadamente, sem comparação fictícia.

Assinatura não é cobrança por token. Métrica principal: **pontos percentuais observados
da cota da conta**, separando janelas 5h e semanal. Tokens são diagnóstico complementar;
preço de API, inclusive exibido por plugins, é no máximo equivalente hipotético, não fatura.
O multiplicador de entrada nova da rodada 1 não é multiplicador de cota ou dinheiro.

## Protocolo para cada lote solicitado

1. Confirmar grupos completos escolhidos pelo usuário, sem limite fixo de tarefas;
   sem escopo explícito, listar pendências e perguntar quais entram antes de implementar.
   Preparar diffs acessíveis dentro do workspace, bases/heads/digests fixos; registrar
   risco/justificativa e confirmar SOL xhigh efetivo, tanto em lote padrão como crítico.
   Risco e reclassificação orientam cobertura/verificação, não outro modelo. Não iniciar
   o gate cumulativo final SOL como parte do lote ou como comparação de consumo.
2. Conferir sessões locais ativas por `opencode api get /api/session/active`, sem assumir
   que isso detecta uso da mesma conta em outro dispositivo. Solicitar que outros usos
   pausem durante a medição. Se não for possível, declarar ausência de isolamento.
3. Consultar a ferramenta `quota_status` imediatamente antes de iniciar o revisor.
   Copiar **somente** percentual restante e reset das entradas OpenAI 5h/Weekly, horário
   da observação e identidade do lote/fase/modelo/sessão para um JSON mínimo.
4. Executar o revisor sozinho; durante o intervalo, o coordenador não faz geração,
   correções nem disparo de outro agente. Após conclusão, consultar a cota novamente.
   Não esperar/pollar andamento de subagentes; usar notificação de conclusão do harness.
5. Comparar o antes/depois da sessão SOL e encerrar a medição dessa revisão; não iniciar
   segundo revisor. A conta inclui qualquer overhead/coordenador no intervalo; não garantir atribuição
   exata por modelo nem que a atualização do provedor esteja completa.
6. Revalidação segue o mesmo protocolo, separada de revisão inicial. Correções/implementação
   e consumo do coordenador ficam em intervalos próprios, nunca no custo isolado do revisor.
7. Registrar tokens reais por sessão/chamada como antes, sem somar raciocínio à saída.
   Apresentar ao usuário quota por fase/janela, tempo, achados e limitações no fechamento,
   sem deixar a medição somente nos artefatos locais. Comparação entre modelos e achados
   comuns/adicionais só em comparações históricas nas quais ambos executaram; modelo
   não executado é **não medido**, não consumo/achados zero. Dados privados não vão para issue/PR sem
   autorização. Consumo alto ou falha de cota deve ser comunicado antes de novos disparos;
   isso não autoriza trocar modelo ou dispensar revisão exigida.

Não há endpoint de quota adivinhado, leitura de banco/credenciais ou configuração global
neste PR. O coletor live continua a ferramenta disponível no harness; o script local
valida/compara suas observações mínimas, **não coleta sozinho** nem lança revisores.
Falha da ferramenta/janela ausente é indisponibilidade, não consumo zero.

## Template e comandos locais

Guardar observações reais **fora do Git**, por exemplo `/tmp/opencode/review-quota/<lote>/`.
Nunca copiar diagnóstico completo: contém caminhos/configuração/estado de credenciais.
Somente resumo sanitizado autorizado pelo usuário pode ser anexado à issue/PR público.

```json
{
  "version": 1,
  "batch": "<lote>",
  "phase": "initial",
  "moment": "before",
  "model": "openai/gpt-6.1-sol#xhigh",
  "session": "<sessão efetiva>",
  "observed_at": "<ISO-8601 com timezone>",
  "isolation_declared": false,
  "windows": {
    "5h": {"remaining_percent": null, "reset_at": null},
    "weekly": {"remaining_percent": null, "reset_at": null}
  }
}
```

Preencher horários/sessão/lote reais; `null/null` só quando janela indisponível. Não
declarar isolamento sem conferência operacional; `phase` também aceita revalidation e
coordinator. Depois usa-se moment=after, preservando lote/fase/modelo/sessão e atualizando
observação/reset/percentuais. Exemplo de execução (arquivos locais já preenchidos):

```sh
python3 scripts/review_quota.py snapshot < /tmp/opencode/review-quota/before-input.json > /tmp/opencode/review-quota/before.json
python3 scripts/review_quota.py snapshot < /tmp/opencode/review-quota/after-input.json > /tmp/opencode/review-quota/after.json
python3 scripts/review_quota.py compare /tmp/opencode/review-quota/before.json /tmp/opencode/review-quota/after.json
```

Saída não modifica snapshots nem faz I/O de rede. O template é deliberadamente pendente,
não uma medição executada. Testes usam somente fixtures sintéticas identificadas.

## Interpretação e validade

- 80% → 76% restantes = **4 p.p.**, não 4% de dinheiro. Não somar janelas 5h/semanal.
- `controlled-account-observation`: delta observado com isolamento declarado; ainda
  sujeito a arredondamento/atraso/overhead, não custo exato daquele modelo.
- `unattributable`: uso concorrente/isolamento ausente; não atribuir a um revisor.
- `not-resolved`: delta zero na resolução do indicador, não prova de consumo zero.
- `unavailable`, `reset-changed` ou `replenished-or-inconsistent`: sem delta válido;
  registrar limitação e recolher observação nova se houver nova execução autorizada.
- Mesmo reset não garante janela comparável se horário ultrapassou seu término.
- Precisão só do indicador; repetir em lotes reais autorizados antes de extrapolar.
  Qualidade/achados confirmados entram na comparação, sem premiar quantidade artificial.

Não medir retroativamente por interpolação de tokens. Na rodada 1, revisores rodaram
simultaneamente e faltam baselines; sua cota por modelo continua **não determinada**.
Não alterar relatórios/digests antigos para inventar essas observações. Método não relaxa
revisão cumulativa `[SPEC]`, checks, autorização de merges ou responsabilidade editorial.
