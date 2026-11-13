# Tasks

Spec da [issue #173](https://github.com/LordBiGoDoNE/concurso-simulator/issues/173). Base: `main` `16ca8aef382d64e7652b280b413122c357775ead`; integração em `epic/question-bank-expansion`. [Proposta](proposal.md), [design](design.md), [cobertura](specs/question-bank-coverage/spec.md) e [prática](specs/lesson-practice-capacity/spec.md).

**Implementação local dos grupos completos 1–8 autorizada pelo usuário em 2026-11-10**, na central “Automatizar projetos na VPS”, ao solicitar agentes para implementar #173 e #161. A entrega inicial documental/PR #174 permanece histórica e preservada. Publicação/revisão de implementação aguardam teste local do usuário; revisão/aprovação/integração são estados separados.

Responsável desta implementação: conversa Codex exclusiva da #173, worktree `7ac1`, branch `task/question-bank-expansion/inventory-tooling`, base remota `16ca8aef382d64e7652b280b413122c357775ead`. [Registro de início](https://github.com/LordBiGoDoNE/concurso-simulator/issues/173#issuecomment-6778396819). Grupo 1 primeiro; conteúdo 3–7 depois. **Grupo 2 e validação integrada do grupo 8 aguardam #161 integrada à main por merge autorizado.** A #161 possui prioridade em mockexam/persistência/HTTP/OpenAPI/frontend; não tocar nesses contratos, empilhar specs ou fazer merge para desbloquear. Depois, conciliar teto 25 com prática por assunto sem cargo, sem reintroduzir cargo ou mudar formato/migração por quantidade. As referências abaixo a cargo descrevem a main histórica da proposta; a ordem autorizada prevalece.

Estado real deste incremento: ferramentas implementadas/testadas, inventário offline e 79 candidatas reais preparadas em JSONs externos (57 células, 43 de simulado/36 de prática). Leitor/prévia isolados e cálculos têm verificações próprias; revisão editorial integral/diversidade/cotas permanecem pendentes. Nenhum grupo completo, importação/habilitação real, revisão independente ou integração. [Guia e limitações](../../../docs/question-bank-expansion.md) e [avaliação de reutilização](../../../docs/question-source-reuse-assessment.md). As tarefas marcadas abaixo indicam apenas implementação/verificação técnica local, sem revisão/aceite/integração presumidos.

## Checkpoint solicitado pelo usuário — 12/11/2026

Trabalho interrompido para salvar o estado e criar commit local, por pedido do
usuário. A última consolidação contém 2.342 candidatas em arquivos externos
(2.043 autorais/299 originais), sem importação ou habilitação real. Não certifica
cotas, diversidade ou conclusão dos grupos. O leitor/prévia validou um snapshot
anterior de 2.270 candidatas/102 pacotes; a suíte Python atual passou 16 testes.
A correção de operadores/pontuação no auditor exige nova auditoria global;
a rodada v4 foi interrompida. [Retomada e caminhos preservados](../../../docs/question-bank-expansion-handoff.md).
Os registros de 79 candidatas abaixo descrevem a etapa histórica, não o total atual.

## 1. Inventário, fontes e ferramentas

- [x] 1.1 Reconciliar catálogo, curadoria, base M04-Q05, práticas e candidatos legados com o estado observado do destino local; entregar matriz das 102 combinações com contagens, data/destino, déficit e limites, verificando que desconhecido não vira zero nem evidência versionada vira carga atual.
- [x] 1.2 Mapear fontes de originais por assunto e possibilidade de reutilização, priorizando Avança SP; entregar inventário com prova/gabarito definitivo, identidade da questão e pendências. Verificar disponibilidade de dez originais distintas por assunto ou déficit explícito, sem declarar a cota cumprida antes da conferência por item.
- [x] 1.3 Definir edição 2026-11, arquivos isolados por disciplina/finalidade e registro de autoria/origem/fontes; implementar compilação incremental determinística para o contrato administrativo existente, reutilizando fingerprint e chaves estáveis. Testar determinismo, permutação de alternativas, namespace, identidade, separação de finalidade e rejeição de fonte/hash/catálogo/seleção inválidos sem mudar os lotes históricos.
- [x] 1.4 Implementar auditoria incremental de contagem e duplicatas contra os dois acervos/legado/revisões curadas, com decisões por par/fingerprint; testar duplicata exata, paráfrase sinalizada, equivalência numérica, base/variante/cargo contados uma vez e invalidade de justificativa antiga após edição. Documentar limites da triagem e como conferir pendências.
- [x] 1.5 Documentar preparação, revisão, pacotes/seleções, dossiê e relatórios públicos/privados; testar um pacote sintético completo pela prévia/aplicação existente em PG isolado, incluindo teto 100, DRAFT e replay. Conferir comandos documentados, fontes fora dos builds e contrato entregue aos grupos 3–7, sem novas aprovações/endpoints ou carga real.

1.1: matriz offline de 102 células entregue; 251/255 históricos reproduzidos por assunto. Estado/déficit do destino real desconhecido, sem consulta ao banco do operador/VPS. 1.2: inventário inicial de 120 posições preservado como prospecção provisória. Retomada inclui Nova Odessa, sete documentos com hash, correspondência/gabarito de 79 candidatas e avaliação fundamentada no art. 8º, IV/TJPR, sem afirmar licença expressa da banca. Araçariguama segue com download 403/hash desconhecido. Treze cálculos conferidos por oráculos separados; quatro itens em quarentena. Ainda nenhuma original integralmente revisada apta à cota: dez verificadas faltantes por assunto, disponibilidade total desconhecida. Não declarar 1, 3–7 ou a expansão completos; ausência de licença expressa não é impossibilidade demonstrada de obter fontes aptas.

## 2. Sorteio de até cinco questões do acervo de25

Observação complementar de1.1 em2026-11-11: relatório local oficial e payloads
conferidos por fingerprint, backup privado preservado, matriz pública em
`editorial/question-bank-expansion/2026-11/inventory-observed-local-2026-11-11.json`.
O destino desta conversa contém54 questões sintéticas (52 elegíveis), zero reais.
As fixtures foram identificadas pelo código gerador e pelas identidades observadas,
e não contam nas cotas. Os históricos251/255 não foram presumidos carregados.
Nenhum banco do operador/VPS foi consultado; sua cobertura permanece desconhecida.

**Correção atual solicitada pelo usuário em2026-11-11:** acervo com pelo menos25 por
assunto/modalidade e práticas de até cinco sorteadas. A entrega anterior de práticas
com25 e seus resultados abaixo são históricos; novas verificações são necessárias.
As cotas dos grupos3–7 não foram reduzidas. Práticas já criadas serão preservadas.


Retomada em 2026-11-11 após pedido do usuário: #161 integrada pelo PR #176,
`226d2e5b07208887941811beff10ac29295b04b3`. A espera registrada na coordenação
inicial acima foi satisfeita. Branch local `task/question-bank-expansion/practice-capacity`,
mesmo worktree `7ac1`, épica local atualizada por fast-forward e arquivos anteriores
preservados. Assunto sem cargo e formato 4 são a base; V15 autorizada conforme
[ajuste específico](capacity-schema-amendment.md). [Verificação do grupo](practice-capacity-verification.md).

- [x] 2.1 Manter seleção aleatória e disponibilidade de até cinco por prática, considerando todo o acervo próprio por assunto sem cargo; testar pools0/1/5/24/25/>25, cinco bases distintas, sorteios com RNG controlado, replay estável e preservação da meta25 no acervo.
- [x] 2.2 Preservar formato4, codec, capturas e comandos anteriores, inclusive as práticas de25 já criadas; recusar criação nova acima de5, sem regravar V1–V15, truncar ou ressorteiar histórico. Verificar reinício, backup/restore e gravação após restaurar.
- [x] 2.3 Atualizar HTTP/OpenAPI para available real e maximum5, mantendo início por comando/assunto e compatibilidade de leitura/replay antigos; verificar segurança, posse, CSRF, insuficiência, conflitos e correção após conclusão.
- [x] 2.4 Corrigir cliente/textos para cinco sorteadas por nova prática; manter leitura/histórico/resultado anteriores até25. Verificar disponibilidade, início explícito, mudança de pool, falhas/retry, último item e ausência de seletor de quantidade.
- [x] 2.5 Executar Chromium/Edge, conta/visitante, teclado/celular e jornada completa de cinco sorteadas de um pool de pelo menos25; verificar fila, substituição, identidade, expiração, replay e histórico anterior.
- [x] 2.6 Atualizar spec/guias/issue e evidência da distinção acervo25/prática5; preservar relatórios anteriores como histórico, conferir builds e limites de recuperação. Teste do usuário precede publicação/revisões.

2.1–2.6: implementadas/verificadas localmente em 2026-11-11, sem aceite/revisão/integração presumidos. Backend final 339 aprovados/um ignorado histórico de symlink, frontend 238, navegador Chromium/Edge 30, Python de recuperação 11/expansão 12; V10–V15 backup/restore real e reinício em dois processos com 25 respostas passaram. O ensaio identificou e corrigiu preservação de arrays imutáveis no codec para formatos 3/4, com regressão de ordem oposta e validação de reinício. Ambiente sintético próprio em http://127.0.0.1:5393/#/aulas/P01 disponível para teste. [Evidências/limites](practice-capacity-verification.md). Revisão independente segue após teste do usuário; grupos de conteúdo e cota permanecem abertos.

**Verificação da correção atual:** tarefas2.1–2.6 revalidadas para práticas de até
cinco, sem reduzir o acervo25. Backend completo341 (340 aprovados/um ignorado
histórico), revalidação13/13 das suítes afetadas, frontend238, Chromium/Edge30 e
reinício real passaram. O banco sintético aberto pelo usuário foi preservado por
backup/restore com igualdade das25 tabelas de negócio; uma prática anterior de25
foi mantida. [Evidência atual](practice-sampling-five-verification.md). Aceite,
revisão independente, publicação e integração continuam pendentes.

**Teste local confirmado pelo usuário em 2026-11-11:** após a correção para até
cinco questões sorteadas, o usuário respondeu “Deu certo”. Esse aceite refere-se
ao comportamento da prática testada, não à completude editorial do acervo. O
usuário pediu concluir os grupos já autorizados sem novas pausas intermediárias.

## Entrega de conteúdo — 13/11/2026 (1.2 e grupos 3–7)

Lote autorizado pelo usuário em 13/11/2026: 1.2, 3–7 e 8 completos. Por decisão do usuário,
as branches publicadas levam código, scripts, docs, os 102 pacotes finais e o manifesto
SHA-256 dos arquivos locais, sem o acervo bruto (PDFs de terceiros). 1.2 e 3–7
implementadas/verificadas localmente, sem revisão/aceite/integração presumidos:
102/102 células com ≥25 itens e ≥5 originais (1.294 simulado, 1.293 prática, 530 originais
com identidades distintas); fingerprints recalculados, revisão individual atual 2.587/2.587,
SHA-256 de prova/gabarito 530/530, auditoria v10 138/138 CURRENT, leitor/prévia Java
102 pacotes/2.587 NEW sem escrita e Matemática 617/617 conferida às cegas. Revisões
assistidas por IA, sem revisão humana. [Relatório](../../../docs/question-bank-expansion-content.md).

## 3. Português — depende de 1

Cada tarefa de assunto dos grupos 3–7 prepara os dois acervos: pelo menos 25 bases válidas por modalidade, incluindo cinco originais diferentes por modalidade, contando as bases anteriores válidas uma vez. Conferir cada acréscimo e suas fontes/resoluções, registrar fingerprints e decisões de duplicidade, compilar os dois pacotes e validar a cobertura candidata. Preparação não é elegibilidade no banco; a habilitação integrada pertence ao grupo 8.

- [x] 3.1 Completar P01 — Interpretação de Textos; verificar as duas seleções e as cotas conforme os critérios do grupo.
- [x] 3.2 Completar P02 — Sinônimos, Antônimos e Relações de Sentido; verificar as duas seleções e as cotas conforme os critérios do grupo.
- [x] 3.3 Completar P03 — Sentido Próprio e Figurado; verificar as duas seleções e as cotas conforme os critérios do grupo.
- [x] 3.4 Completar P04 — Pontuação; verificar as duas seleções e as cotas conforme os critérios do grupo.
- [x] 3.5 Completar P05 — Ortografia e Acentuação; verificar as duas seleções e as cotas conforme os critérios do grupo.
- [x] 3.6 Completar P06 — Classes de Palavras; verificar as duas seleções e as cotas conforme os critérios do grupo.
- [x] 3.7 Completar P07 — Concordância Verbal e Nominal; verificar as duas seleções e as cotas conforme os critérios do grupo.
- [x] 3.8 Completar P08 — Regência Verbal e Nominal; verificar as duas seleções e as cotas conforme os critérios do grupo.
- [x] 3.9 Completar P09 — Colocação Pronominal; verificar as duas seleções e as cotas conforme os critérios do grupo.
- [x] 3.10 Completar P10 — Crase; verificar as duas seleções e as cotas conforme os critérios do grupo.
- [x] 3.11 Entregar testes editoriais e relatório/documentação de Português: verificar 250 candidatas válidas por modalidade e pelo menos 100 originais no conjunto, todas as resoluções/distratores/fontes, pares suspeitos e preservação do legado; exercitar os pacotes reais no leitor/prévia em destino isolado sem aprová-los por efeito do teste.

## 4. Matemática/Raciocínio — depende de 1

- [x] 4.1 Completar M01; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, com conferência independente dos cálculos.
- [x] 4.2 Completar M02; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, com conferência independente dos cálculos.
- [x] 4.3 Completar M03; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, sem contar variantes parametrizadas como bases.
- [x] 4.4 Completar M04; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, preservando M04-Q05 e sem contar variantes como bases.
- [x] 4.5 Completar M05; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, com conferência independente dos cálculos.
- [x] 4.6 Completar M06; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, com conferência independente dos cálculos.
- [x] 4.7 Completar M07; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, com conferência independente dos cálculos.
- [x] 4.8 Completar M08; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, com conferência independente dos cálculos.
- [x] 4.9 Completar M09; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, com conferência independente dos cálculos.
- [x] 4.10 Completar M10; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, com conferência independente dos cálculos.
- [x] 4.11 Completar M11; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, com conferência independente dos cálculos/raciocínio.
- [x] 4.12 Completar M12; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, com conferência independente dos cálculos/raciocínio.
- [x] 4.13 Entregar testes editoriais e relatório/documentação de Matemática: verificar 300 candidatas válidas por modalidade, pelo menos 120 originais no conjunto e oráculos independentes para os problemas calculáveis; conferir pares/identidades e pacotes reais no leitor/prévia isolados, sem novas famílias ou aprovação pelo teste.

## 5. Informática — depende de 1

- [x] 5.1 Completar I01; verificar as duas seleções e cotas pelo critério comum, incluindo versão/idioma/condições que determinam a resposta.
- [x] 5.2 Completar I02; verificar as duas seleções e cotas pelo critério comum, incluindo versão/idioma/condições que determinam a resposta.
- [x] 5.3 Completar I03; verificar as duas seleções e cotas pelo critério comum, incluindo versão/idioma/condições que determinam a resposta.
- [x] 5.4 Completar I04; verificar as duas seleções e cotas pelo critério comum, incluindo versão/idioma/condições que determinam a resposta.
- [x] 5.5 Completar I05; verificar as duas seleções e cotas pelo critério comum, incluindo versão/idioma/condições que determinam a resposta.
- [x] 5.6 Completar I06; verificar as duas seleções e cotas pelo critério comum, incluindo versão/idioma/condições que determinam a resposta.
- [x] 5.7 Completar I07; verificar as duas seleções e cotas pelo critério comum, incluindo versão/idioma/condições que determinam a resposta.
- [x] 5.8 Completar I08; verificar as duas seleções e cotas pelo critério comum, incluindo versão/idioma/condições que determinam a resposta.
- [x] 5.9 Completar I09; verificar as duas seleções e cotas pelo critério comum, incluindo versão/idioma/condições que determinam a resposta.
- [x] 5.10 Completar I10; verificar as duas seleções e cotas pelo critério comum, incluindo versão/idioma/condições que determinam a resposta.
- [x] 5.11 Entregar testes editoriais e relatório/documentação de Informática: verificar 250 candidatas válidas por modalidade e pelo menos 100 originais no conjunto, fontes primárias, versões/contextos e duplicatas; conferir pacotes reais no leitor/prévia isolados sem aprovar pelo teste.

## 6. Conhecimentos Administrativos — depende de 1

- [x] 6.1 Completar A01; verificar as duas seleções e cotas pelo critério comum, conferindo dispositivos/recorte normativo aplicáveis.
- [x] 6.2 Completar A02; verificar as duas seleções e cotas pelo critério comum, conferindo dispositivos/recorte normativo aplicáveis.
- [x] 6.3 Completar A03; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e condições da rotina administrativa.
- [x] 6.4 Completar A04; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e condições da rotina administrativa.
- [x] 6.5 Completar A05; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e condições da rotina administrativa.
- [x] 6.6 Completar A06; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e condições da rotina administrativa.
- [x] 6.7 Completar A07; verificar as duas seleções e cotas pelo critério comum, conferindo dispositivos/recorte normativo aplicáveis.
- [x] 6.8 Completar A08; verificar as duas seleções e cotas pelo critério comum, conferindo dispositivos/recorte normativo aplicáveis.
- [x] 6.9 Entregar testes editoriais e relatório/documentação de Administrativo: verificar 200 candidatas válidas por modalidade e pelo menos 80 originais no conjunto, pares com Escolar e fontes atuais sem inventar norma/prazo municipal; conferir pacotes reais no leitor/prévia isolados sem aprovação automática.

## 7. Conhecimentos Escolares — depende de 1

- [x] 7.1 Completar S01; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e o recorte normativo pertinente.
- [x] 7.2 Completar S02; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e o recorte normativo pertinente.
- [x] 7.3 Completar S03; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e o recorte normativo pertinente.
- [x] 7.4 Completar S04; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e o recorte normativo pertinente.
- [x] 7.5 Completar S05; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e o recorte normativo pertinente.
- [x] 7.6 Completar S06; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e o recorte normativo pertinente.
- [x] 7.7 Completar S07; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e o recorte normativo pertinente.
- [x] 7.8 Completar S08; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e o recorte normativo pertinente.
- [x] 7.9 Completar S09; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e o recorte normativo pertinente.
- [x] 7.10 Completar S10; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e o recorte normativo pertinente.
- [x] 7.11 Completar S11; verificar as duas seleções e cotas pelo critério comum, incluindo orientações oficiais do ciclo pertinente ao Censo Escolar.
- [x] 7.12 Entregar testes editoriais e relatório/documentação de Escolar: verificar 275 candidatas válidas por modalidade e pelo menos 110 originais no conjunto, legislação/fontes atuais e pares com Administrativo sem inventar norma/prazo local; conferir pacotes reais no leitor/prévia isolados sem aprovação automática.

## 8. Validação integrada e entrega local — depende de 2–7

- [x] 8.1 Consolidar manifestos e repetir a auditoria conjunta de fontes, fingerprints e pares entre disciplinas/modalidades/legado; verificar as 102 células candidatas, mínimo 510 originais e nenhuma justificativa desatualizada, preservando relatórios específicos já entregues em cada grupo.
- [x] 8.2 Ensaiar a composição completa em PostgreSQL isolado: prévia/aplicação dos pacotes, DRAFT, reaplicação, base desatualizada, seleção/digest alterados, concorrência e rollback. Conferir dados antes/depois e contratos de segurança sem aprovar o acervo real por fixtures ou CI.
- [x] 8.3 Preparar backup privado e reconciliar o destino local, importar somente a seleção/digests conferidos e registrar decisões editoriais pelos mecanismos existentes para o teste local autorizado; verificar recibos, ponteiros, sequências, preservação de dados e ausência de habilitação por upload/startup, sem publicar dados privados.
- [x] 8.4 Conferir pelo banco e contratos HTTP a cobertura elegível final: ≥25 e ≥5 originais em cada célula, ≥1.275 por modalidade e ≥2.550 no conjunto, sem multiplicar por cargo/variante/revisão. Entregar evidência com data/destino e déficits reais; qualquer célula pendente impede declarar a expansão completa.
- [x] 8.5 Executar testes editoriais/backend/frontend, build e Playwright Chromium/Edge pertinentes no conjunto: visitante/conta, aula → prática de até cinco sorteadas do acervo de pelo menos25 → resultado, simulado, histórico antigo, reinício, teclado/celular e proteção de filas/identidade. Registrar comandos, SHAs, resultados e limitações, sem tratar CI como revisão editorial/semântica.
- [x] 8.6 Entregar guia e relatório integrado para o teste local do usuário com seleção/digests, totais e recuperação por backup/imagem exatos; verificar o percurso documentado e separar habilitado/verificado de aceite, revisão, integração e publicação. Não adiar para este grupo os testes/docs devidos nos grupos anteriores.

Grupo 8 implementado/verificado localmente em 13/11/2026: auditoria reexecutada igual à v10
(138/138 CURRENT); ensaio completo em PostgreSQL isolado (prévia, DRAFT, replay, reaplicação, base
desatualizada, seleção/digest alterados, rollback, concorrência); habilitação local feita no ambiente
paralelo da épica combinada pelo fluxo em lote da #177 (decisão do usuário de 12/11), reconciliada com
os 102 digests versionados; banco e HTTP com 2.587 elegíveis, 102/102 células ≥25 e ≥5 originais;
backend 340/0 falhas (1 skip histórico), Python 22, frontend 238 + build, Playwright Chromium/Edge 30.
[Relatório integrado](../../../docs/question-bank-expansion-integrated.md). Aceite do usuário, revisão
final, integração e publicação seguem pendentes.

Correção de 13/11/2026 (achado A1 da revisão final, triagem aprovada pelo usuário): 27 referências
públicas corretivas substituídas (camadas r7/r8), revisões individuais refeitas, auditoria v11 138/138
CURRENT, pacotes e evidências regenerados e guarda automática `study_reference` adicionada. A
habilitação do ambiente paralelo (8.3/8.4) refere-se aos pacotes de 12/11 e o ambiente foi perdido;
a nova habilitação local com os pacotes corrigidos está pendente. [Detalhes](../../../docs/question-bank-expansion-content.md).

## Workflow follow-up

- Início autorizado/registrado em 2026-11-10 conforme nota acima; a restrição da publicação documental inicial é histórica, não nova pausa genérica de implementação.
- Grupos 3–7 dependem do contrato/ferramentas de 1 e podem trabalhar em paralelo com arquivos próprios. Grupo 2 possui os contratos compartilhados de prática; a central conserva manifestos agregados e ordem. Grupo 8 exige 2–7. Novas dependências devem ser registradas, sem ampliar escopo ou compartilhar runtime silenciosamente.
- Teste do usuário ocorre antes de envio/revisão de implementação conforme AGENTS.md. Publicação documental atual é autorizada; não autoriza pushes futuros de código/conteúdo. Por decisão do usuário de 2026-11-12, não há revisão por lote; a única revisão por modelo é a revisão final de código (Claude Opus 5.5, esforço medium, agente novo) antes do `[SPEC]` da épica combinada `epic/question-bank-expansion-curation` para `main`. A regra SOL anterior fica como histórico.
- Na issue, registrar implementação/verificação, revisão/aceite e integração como estados separados, com PRs/SHAs/checks/limites reais. OpenSpec strict/CI não provam originalidade, correção ou elegibilidade. Não inventar revisões, tokens ou aprovação humana/institucional.
- #161 integrada à main pelo PR #176; dependência satisfeita para grupo 2. Preservar assunto sem cargo e formato 4; não alterar a stack ou V1–V14. V15 foi autorizada especificamente para o CHECK de quantidade. Grupo 8 continua dependendo de 2–7. As #158/#160 foram encerradas após conferência dos merges e CI, sem arquivar seus OpenSpecs.
- Integração de tarefas e PR final `[SPEC]` → main exigem autorizações específicas e gates próprios; não usar auto-merge, fila ou bypass. Arquivamento, carga/deploy na VPS e publicação seguem autorização/entrega operacional próprias.
