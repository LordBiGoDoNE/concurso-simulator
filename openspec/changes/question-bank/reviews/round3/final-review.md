# Grupos 6 e 7 — relatório único do lote

PR **#62**, draft, `task/question-bank/initial-curation` → `epic/question-bank`.
Escopo completo: **6.1–6.25 e 7.1–7.3**. Uma única mudança coesa, sem stack artificial.
Risco **crítico**: conteúdo/respostas e evidência determinam elegibilidade editorial;
projeções públicas precisam preservar a ausência de campos corretivos.

## Snapshot e autoria da revisão

- Base: `dcbe086af47a6fafca114290850a419f0886cc92`.
- Head da revisão completa: `1ff1a947b693e44aefc631e517c30be84ac0bc81`.
- SHA-256 canônico: `503fe84c566262af6be5ab9bff857679fe5a3b1d8990aa6c268f3993daba73ae`.
- Revisor independente: `/root/round3_sol_final`, contexto novo; dispatch
  `gpt-6.1-sol`, esforço `xhigh`, correspondente a `openai/gpt-6.1-sol#xhigh`.
  Não há telemetria adicional para confirmação independente do modelo ou tokens.
- Tentativa anterior interrompida por limite de uso: não produziu parecer concluído.
  Após o usuário pedir continuação, retomada em 2026-10-14 20:20:50 UTC;
  conclusão às 20:29:24 UTC, aproximadamente **8min34s**.
- Parecer: **zero bloqueadores, zero recomendações, uma observação opcional O1**.
  Não houve edição pelo revisor, escrita no banco/GitHub, Graphify ou outro agente.

## Cobertura e evidência independente

O revisor examinou os 25 arquivos do diff e a combinação, requisitos aprovados,
AGENTS, ADR 0001, checklist HTTP e procedimentos. Leu os 250 enunciados e todas as
alternativas, respostas, resoluções e notas; verificou cálculos, condições de
ferramentas, fontes/dispositivos e classificação. Abriu as 45 URLs de referência.
Não identificou erro concreto de gabarito, dupla resposta correta ou exposição
corretiva na projeção examinada. Isso não certifica revisão humana pedagógica nem
cobertura integral do edital ou de todas as obras referenciadas.

Conferiu em memória os 104 hashes de fontes, manifesto, 25 pacotes de dez e 250
fingerprints/registros, 50 por disciplina e 51 assuntos. Inventário preservado:
510 origens/509 candidatos, com P05-Q01 inválida e não selecionada.

Por SELECT no PG18.6, confrontou os **250 payloads persistidos** (incluindo resposta,
resolução e proveniência) com o conteúdo revisado e as **500 decisões** com evidência
e comandos: IDs, sequência, responsável, checklist, justificativa e datas. Conferiu
500 hashes de comandos e hashes de manifesto/elegibilidade/JAR. Original: 250
elegíveis; cópia de retirada: 249, mantendo 250 revisões e a versão histórica.

Executou independentemente:

- **49 testes Python PASS**, zero skips, Linux sem rede e repo somente leitura;
  nove testes novos também passaram nativamente.
- HTTP: 250 detalhes, três páginas, 102 combinações AND, cinco totais disciplinares,
  projeção exata e rejeições 400/404.
- Browser real em 390/1280: dez detalhes e **40 corpos públicos** nesta execução;
  foco/teclado, filtros/paginação, fontes, overflow, storage, falha/retry e aulas PASS.
  Script executado em memória sem gravações; autoria é do revisor, apesar da etiqueta
  fixa `executedBy: coordinator` no script. Capturas anteriores também examinadas.
- Inspeção de JAR/bundle, diff check, head/digest e worktree limpo.

Não executou clean/build, importação/aprovação/retirada. Os 112 testes backend,
73 frontend, OIDC/offline e OpenSpec permanecem atribuídos ao coordenador/CI,
conforme `operational-verification.md`. XML local direcionado não comprova sozinho
o clean build anterior. Python Windows teve limitações de encoding/symlink;
execução Linux cobriu a suíte completa sem skips ou relaxamento.

Relatórios anteriores foram lidos somente depois da avaliação técnica inicial,
para coerência documental; essa exposição histórica não certificou o snapshot atual.

## Triagem e revalidação

**O1 — opcional, precisão documental.** README afirmava que os demais alertas de
Português não eram selecionados, embora P10-Q03 represente outro grupo e tenha sido
reformulada. Não havia duplicação de representantes ou inflação de contagens.
Correção: explicitar P07-Q02 e P10-Q03 como representantes de grupos distintos,
sem outro selecionado. Causa: resumo manual incompleto dos alertas. Regra positiva:
documentar cada representante selecionado, sem inferir rejeição dos outros grupos.
Nenhuma questão, fingerprint ou decisão mudou; não se adicionou teste espelhando prosa.

O commit documental `5137557301a528ef44c1064c25665b36c34543f9` corrige O1 e aponta
os estados documentais a este relatório. Sua revalidação SOL precisa ser registrada
abaixo após execução real; o parecer de 1ff1a94 não é reaproveitado por mera igualdade.

## Checks, consumo e limites de entrega

CI do head **1ff1a94**: Application checks PR
[`38630338706`](https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/38630338706),
push [`38630332288`](https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/38630332288)
e guard [`38630338727`](https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/38630338727)
SUCCESS. Backend/frontend/identity-browser/study-material e `spec-review-tests` verdes.
Esses runs não certificam heads posteriores; reconferir antes de pedir merge.

Cota inicial/revalidação e coordenação: ferramenta `quota_status` indisponível no
harness. Observações mínimas antes/depois validadas/comparadas privadamente pelo script;
5h e semanal **unavailable**, sem delta atribuível. Isolamento da conta não verificado;
durante a revisão o coordenador não gerou implementação. Tokens de entrada nova,
cache read/write, saída e raciocínio indisponíveis. Não se estima zero nem cobrança.
Sem comparação nova entre modelos; Astra não executado neste lote.

**44/44 implementadas/verificadas; 16/44 integradas na épica da spec.** As 28 novas
tarefas aguardam aprovação/merge explícitos do #62. 250 aprovações editoriais locais
não aprovam PRs nem populam outros destinos. Nenhum merge, publicação, arquivamento ou
integração na épica geral/main. Gate cumulativo final Astra medium permanece separado.
