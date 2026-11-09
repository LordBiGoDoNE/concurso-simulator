# Revisão independente do lote — SOL 6.1 xhigh

Relatório do revisor independente `/root/spec_reviewer`, contexto novo no harness
Codex collaboration, modelo selecionado explicitamente `gpt-6.1-sol`, esforço
`xhigh` (`openai/gpt-6.1-sol#xhigh`). Não houve substituição de modelo.

## Resultado

Revisão do lote new-student-experience, grupos 1–8 e atualização documental da
política de cota, concluída sem achados abertos no conjunto analisado. Risco
mantido como crítico, pela convivência entre navegação pública, identidade e
realizações privadas. Esta revisão não conclui o gate cumulativo final nem
aprova merge.

## Snapshots conferidos pelo revisor

Os commits existem e os três digests foram recalculados com as opções canônicas
de snapshot, sem importar ou executar código do produto.

| Range | Base | Head | SHA-256 |
| --- | --- | --- | --- |
| Original PR #159 | f61d57d5d55d6d61609815a2faa4b415ed8dca4b | 37ccccf1563955d431591629f66c15bb7810a31a | a9ca7917cd5877ddd7b738e4196e97e281962bae49bec90d47637b73d58ccf4e |
| Correções locais | 37ccccf1563955d431591629f66c15bb7810a31a | 2e9949dbde31ae5cd6e9d3478ef1819aad533c57 | cfbeed6960975ace703c07573aea6f1a8b54ef1f178031b86c8727176d13bdad |
| Combinação | f61d57d5d55d6d61609815a2faa4b415ed8dca4b | 2e9949dbde31ae5cd6e9d3478ef1819aad533c57 | 7b3127f0ed3fdd89aec75522e908769ed25c86e44720aa074f16fa68eb69b343 |

## Constatações do original, já corrigidas

- **NSE-R01 — recommended, fixed.** Em scripts/compile_native_lessons.py:177,
  no snapshot 37ccccf, compile_all criava o temporário dentro de backend/build
  sem assegurar sua existência. Apenas a CLI criava esse diretório; a chamada
  direta usada pela regressão dependia de build anterior. Isso contrariava a
  verificação reproduzível em checkout limpo. A combinação cria o diretório
  dentro de compile_all, na linha 184 do head revisado, e inclui regressão com
  cópia das fontes sem backend. Correção confirmada por leitura; o revisor não
  executou a reprodução.
- **NSE-R02 — recommended, fixed.** O clean_text original, linhas 40–41,
  conservava orientação sobre gabarito separado, presente nos 51 documentos
  gerados, embora o leitor nativo removesse essa navegação. O texto encaminhava
  o estudante a um recurso não oferecido pela experiência, contrariando o
  requisito de exercícios exclusivamente na prática. A normalização atual
  aponta para Pratique esta aula e correção após encerramento. A comparação
  dos dados confirmou exatamente 50 substituições de uma orientação e uma da
  variante de M04, sem mudanças estruturais.

No range de correções não foi encontrado novo defeito confirmado. Na combinação
não foi identificado bloqueador ou recomendação corretiva pendente.

## Cobertura realizada pelo revisor

| Área | Evidência examinada e conclusão |
| --- | --- |
| Requirements | Proposta, design, tarefas e requisitos confrontados com entrada por cargo, catálogo local, URL, disponibilidade, prática recolhível, índice e leitor nativo. A navegação não cria realizações. |
| Architecture | ADR 0001 e checklist HTTP lidos. O lote reutiliza endpoints e transporte existentes; não introduz acesso direto à persistência nem desloca regras do backend para o frontend. |
| Integration | Conferidos App, navegação, biblioteca, workspace, AttemptPractice e useAttemptRun. A única instância da prática permanece montada; assunto/cargo incompatível oculta a realização. Filas e retry conservam o executor existente. |
| Security | Conferidas chaves de identidade, invalidação por desmontagem, aborto e rejeição de respostas tardias, guarda de início e contrato exato da disponibilidade. URLs contêm somente contexto público. O leitor usa vocabulário permitido, sem HTML bruto, scripts ou URLs ativas de conteúdo. |
| Persistence | Nenhuma migração, importação ou mudança de schema/repository no lote. Gravações continuam pelo transporte existente, com comandos idempotentes e controles de substituição. Nenhum banco foi acessado pelo revisor. |
| Tests | Inspecionados testes de rotas, contratos, disponibilidade, leitor, filas, retry, identidade e E2E alterados. As adaptações preservam verificações relevantes e substituem expectativas do iframe pela experiência aprovada. O revisor não executou testes. |
| Maintenance | Conferidos compilador, manifesto, README, configuração e política de revisão. A retirada da medição de cota está coerente em AGENTS, procedimento de revisão e documento histórico, preservando modelo, independência e gates. |

## Dados gerados

Os JSONs não foram descartados da análise. Nos dois heads, foram conferidos os
hashes LF dos 51 documentos e 115 fontes, todos coincidentes com os respectivos
manifestos. Foram inspecionadas a seleção do compilador, categorias e referências
dos dados, incluindo SVG, tabelas, MathML, exemplos e fontes. Na combinação há
50 SVGs, 25 tabelas e 13 elementos MathML, além dos componentes percentuais nativos.

A comparação estrutural de todos os documentos entre os heads confirmou ausência
de alterações em elementos, atributos, listas e referências: as correções mudam
somente as orientações descritas acima. Isso confirma a extensão da regeneração,
sem equivaler a uma nova auditoria editorial ou visual de cada explicação.

## Limitações e verificações solicitadas ao coordenador

- O revisor não leu docs/handoffs/new-student-experience.md, verification.md,
  spec-review.json/md, reviews/ ou logs de execução. Esses arquivos fazem parte
  do diff; portanto não há cobertura documental integral das evidências pelo
  revisor independente.
- Não executou testes, build, aplicação, gerador, rede, GitHub ou banco; não
  confirmou checks remotos nem resultados relatados pelo coordenador.
- Teclado/layout examinados pelo código e pelas regressões existentes, sem
  inspeção visual nesta sessão.
- O isolamento da prática está sustentado pela chave no App e pela desmontagem
  que aborta operações. O revisor recomendou conferir o cenário integrado de
  troca de conta com prática inline ativa e escrita/criação suspensa. Trata-se
  de lacuna de evidência específica, não de vazamento confirmado. Resultado do
  coordenador registrado separadamente, sem atribuí-lo ao revisor.
- Totais de tempo/tokens da sessão e identificador interno adicional do provedor
  não foram disponibilizados pelo harness; não foram estimados.

Nenhum arquivo foi alterado pelo revisor. Este relatório não é aprovação de merge.
