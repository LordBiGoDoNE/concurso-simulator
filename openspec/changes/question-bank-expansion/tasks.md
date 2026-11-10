# Tasks

Spec da [issue #173](https://github.com/LordBiGoDoNE/concurso-simulator/issues/173). Base: `main` `16ca8aef382d64e7652b280b413122c357775ead`; integração em `epic/question-bank-expansion`. [Proposta](proposal.md), [design](design.md), [cobertura](specs/question-bank-coverage/spec.md) e [prática](specs/lesson-practice-capacity/spec.md).

**Entrega atual somente documental. Todos os grupos abaixo estão pendentes de confirmação explícita do usuário.** Aprovar ou publicar a spec não autoriza iniciar código, conteúdo, migração ou carga. Os grupos escolhidos devem ser concluídos inteiros; revisão/aprovação/integração serão registradas separadamente na issue.

## 1. Inventário, fontes e ferramentas

- [ ] 1.1 Reconciliar catálogo, curadoria, base M04-Q05, práticas e candidatos legados com o estado observado do destino local; entregar matriz das 102 combinações com contagens, data/destino, déficit e limites, verificando que desconhecido não vira zero nem evidência versionada vira carga atual.
- [ ] 1.2 Mapear fontes de originais por assunto e possibilidade de reutilização, priorizando Avança SP; entregar inventário com prova/gabarito definitivo, identidade da questão e pendências. Verificar disponibilidade de dez originais distintas por assunto ou déficit explícito, sem declarar a cota cumprida antes da conferência por item.
- [ ] 1.3 Definir edição 2026-11, arquivos isolados por disciplina/finalidade e registro de autoria/origem/fontes; implementar compilação incremental determinística para o contrato administrativo existente, reutilizando fingerprint e chaves estáveis. Testar determinismo, permutação de alternativas, namespace, identidade, separação de finalidade e rejeição de fonte/hash/catálogo/seleção inválidos sem mudar os lotes históricos.
- [ ] 1.4 Implementar auditoria incremental de contagem e duplicatas contra os dois acervos/legado/revisões curadas, com decisões por par/fingerprint; testar duplicata exata, paráfrase sinalizada, equivalência numérica, base/variante/cargo contados uma vez e invalidade de justificativa antiga após edição. Documentar limites da triagem e como conferir pendências.
- [ ] 1.5 Documentar preparação, revisão, pacotes/seleções, dossiê e relatórios públicos/privados; testar um pacote sintético completo pela prévia/aplicação existente em PG isolado, incluindo teto 100, DRAFT e replay. Conferir comandos documentados, fontes fora dos builds e contrato entregue aos grupos 3–7, sem novas aprovações/endpoints ou carga real.

## 2. Práticas com até 25 questões

- [ ] 2.1 Ampliar seleção/invariantes e disponibilidade do núcleo para 25 preservando pools próprios por cargo/assunto; testar 0, 1, 5, 24, 25 e >25, bases únicas, ausência de complemento por simulado/template e preservação da composição de simulados.
- [ ] 2.2 Preservar formato 3, codec/capturas e comandos originais, sem migração/regravação; testar replay de comando que criou cinco, novas operações com 25, leitura de formatos 1/2/3, retomada após reinício, resposta/conclusão/histórico e rejeição de capturas inválidas, usando PG isolado nas fronteiras persistentes.
- [ ] 2.3 Atualizar DTOs/OpenAPI/contratos HTTP de disponibilidade maximum=25 e histórico até 25 mantendo início atual; verificar cadeia real de segurança, visitante/conta, posse, CSRF, insuficiência, conflitos e ausência de dados corretivos antes de encerrar, registrando responsáveis conforme ADR 0001/checklist HTTP.
- [ ] 2.4 Atualizar cliente, validadores, textos e apresentação para 1–25 itens; testar disponibilidade, pool que muda após consulta, histórico antigo/novo, último item e estados vazio/falha/retry, mantendo as aulas e início explícito sem seletor de quantidade.
- [ ] 2.5 Executar regressões próprias do grupo em Chromium/Edge com fixtures isoladas: prática completa de 25, teclado/foco/celular, fila pendente, substituição, navegação, troca de identidade/logout e resultado somente após concluir; registrar resultados sem aprovar conteúdo real.
- [ ] 2.6 Atualizar guias de prática/contratos/atualização conjunta e limites de recuperação com imagem exata; verificar exemplos, testes de backend/frontend e build, sem mudar a #161 ou afirmar compatibilidade do binário antigo com novas capturas de 25.

## 3. Português — depende de 1

Cada tarefa de assunto dos grupos 3–7 prepara os dois acervos: pelo menos 25 bases válidas por modalidade, incluindo cinco originais diferentes por modalidade, contando as bases anteriores válidas uma vez. Conferir cada acréscimo e suas fontes/resoluções, registrar fingerprints e decisões de duplicidade, compilar os dois pacotes e validar a cobertura candidata. Preparação não é elegibilidade no banco; a habilitação integrada pertence ao grupo 8.

- [ ] 3.1 Completar P01 — Interpretação de Textos; verificar as duas seleções e as cotas conforme os critérios do grupo.
- [ ] 3.2 Completar P02 — Sinônimos, Antônimos e Relações de Sentido; verificar as duas seleções e as cotas conforme os critérios do grupo.
- [ ] 3.3 Completar P03 — Sentido Próprio e Figurado; verificar as duas seleções e as cotas conforme os critérios do grupo.
- [ ] 3.4 Completar P04 — Pontuação; verificar as duas seleções e as cotas conforme os critérios do grupo.
- [ ] 3.5 Completar P05 — Ortografia e Acentuação; verificar as duas seleções e as cotas conforme os critérios do grupo.
- [ ] 3.6 Completar P06 — Classes de Palavras; verificar as duas seleções e as cotas conforme os critérios do grupo.
- [ ] 3.7 Completar P07 — Concordância Verbal e Nominal; verificar as duas seleções e as cotas conforme os critérios do grupo.
- [ ] 3.8 Completar P08 — Regência Verbal e Nominal; verificar as duas seleções e as cotas conforme os critérios do grupo.
- [ ] 3.9 Completar P09 — Colocação Pronominal; verificar as duas seleções e as cotas conforme os critérios do grupo.
- [ ] 3.10 Completar P10 — Crase; verificar as duas seleções e as cotas conforme os critérios do grupo.
- [ ] 3.11 Entregar testes editoriais e relatório/documentação de Português: verificar 250 candidatas válidas por modalidade e pelo menos 100 originais no conjunto, todas as resoluções/distratores/fontes, pares suspeitos e preservação do legado; exercitar os pacotes reais no leitor/prévia em destino isolado sem aprová-los por efeito do teste.

## 4. Matemática/Raciocínio — depende de 1

- [ ] 4.1 Completar M01; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, com conferência independente dos cálculos.
- [ ] 4.2 Completar M02; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, com conferência independente dos cálculos.
- [ ] 4.3 Completar M03; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, sem contar variantes parametrizadas como bases.
- [ ] 4.4 Completar M04; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, preservando M04-Q05 e sem contar variantes como bases.
- [ ] 4.5 Completar M05; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, com conferência independente dos cálculos.
- [ ] 4.6 Completar M06; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, com conferência independente dos cálculos.
- [ ] 4.7 Completar M07; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, com conferência independente dos cálculos.
- [ ] 4.8 Completar M08; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, com conferência independente dos cálculos.
- [ ] 4.9 Completar M09; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, com conferência independente dos cálculos.
- [ ] 4.10 Completar M10; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, com conferência independente dos cálculos.
- [ ] 4.11 Completar M11; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, com conferência independente dos cálculos/raciocínio.
- [ ] 4.12 Completar M12; verificar as duas seleções e cotas pelo critério comum dos grupos de conteúdo, com conferência independente dos cálculos/raciocínio.
- [ ] 4.13 Entregar testes editoriais e relatório/documentação de Matemática: verificar 300 candidatas válidas por modalidade, pelo menos 120 originais no conjunto e oráculos independentes para os problemas calculáveis; conferir pares/identidades e pacotes reais no leitor/prévia isolados, sem novas famílias ou aprovação pelo teste.

## 5. Informática — depende de 1

- [ ] 5.1 Completar I01; verificar as duas seleções e cotas pelo critério comum, incluindo versão/idioma/condições que determinam a resposta.
- [ ] 5.2 Completar I02; verificar as duas seleções e cotas pelo critério comum, incluindo versão/idioma/condições que determinam a resposta.
- [ ] 5.3 Completar I03; verificar as duas seleções e cotas pelo critério comum, incluindo versão/idioma/condições que determinam a resposta.
- [ ] 5.4 Completar I04; verificar as duas seleções e cotas pelo critério comum, incluindo versão/idioma/condições que determinam a resposta.
- [ ] 5.5 Completar I05; verificar as duas seleções e cotas pelo critério comum, incluindo versão/idioma/condições que determinam a resposta.
- [ ] 5.6 Completar I06; verificar as duas seleções e cotas pelo critério comum, incluindo versão/idioma/condições que determinam a resposta.
- [ ] 5.7 Completar I07; verificar as duas seleções e cotas pelo critério comum, incluindo versão/idioma/condições que determinam a resposta.
- [ ] 5.8 Completar I08; verificar as duas seleções e cotas pelo critério comum, incluindo versão/idioma/condições que determinam a resposta.
- [ ] 5.9 Completar I09; verificar as duas seleções e cotas pelo critério comum, incluindo versão/idioma/condições que determinam a resposta.
- [ ] 5.10 Completar I10; verificar as duas seleções e cotas pelo critério comum, incluindo versão/idioma/condições que determinam a resposta.
- [ ] 5.11 Entregar testes editoriais e relatório/documentação de Informática: verificar 250 candidatas válidas por modalidade e pelo menos 100 originais no conjunto, fontes primárias, versões/contextos e duplicatas; conferir pacotes reais no leitor/prévia isolados sem aprovar pelo teste.

## 6. Conhecimentos Administrativos — depende de 1

- [ ] 6.1 Completar A01; verificar as duas seleções e cotas pelo critério comum, conferindo dispositivos/recorte normativo aplicáveis.
- [ ] 6.2 Completar A02; verificar as duas seleções e cotas pelo critério comum, conferindo dispositivos/recorte normativo aplicáveis.
- [ ] 6.3 Completar A03; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e condições da rotina administrativa.
- [ ] 6.4 Completar A04; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e condições da rotina administrativa.
- [ ] 6.5 Completar A05; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e condições da rotina administrativa.
- [ ] 6.6 Completar A06; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e condições da rotina administrativa.
- [ ] 6.7 Completar A07; verificar as duas seleções e cotas pelo critério comum, conferindo dispositivos/recorte normativo aplicáveis.
- [ ] 6.8 Completar A08; verificar as duas seleções e cotas pelo critério comum, conferindo dispositivos/recorte normativo aplicáveis.
- [ ] 6.9 Entregar testes editoriais e relatório/documentação de Administrativo: verificar 200 candidatas válidas por modalidade e pelo menos 80 originais no conjunto, pares com Escolar e fontes atuais sem inventar norma/prazo municipal; conferir pacotes reais no leitor/prévia isolados sem aprovação automática.

## 7. Conhecimentos Escolares — depende de 1

- [ ] 7.1 Completar S01; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e o recorte normativo pertinente.
- [ ] 7.2 Completar S02; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e o recorte normativo pertinente.
- [ ] 7.3 Completar S03; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e o recorte normativo pertinente.
- [ ] 7.4 Completar S04; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e o recorte normativo pertinente.
- [ ] 7.5 Completar S05; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e o recorte normativo pertinente.
- [ ] 7.6 Completar S06; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e o recorte normativo pertinente.
- [ ] 7.7 Completar S07; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e o recorte normativo pertinente.
- [ ] 7.8 Completar S08; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e o recorte normativo pertinente.
- [ ] 7.9 Completar S09; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e o recorte normativo pertinente.
- [ ] 7.10 Completar S10; verificar as duas seleções e cotas pelo critério comum, conferindo fontes e o recorte normativo pertinente.
- [ ] 7.11 Completar S11; verificar as duas seleções e cotas pelo critério comum, incluindo orientações oficiais do ciclo pertinente ao Censo Escolar.
- [ ] 7.12 Entregar testes editoriais e relatório/documentação de Escolar: verificar 275 candidatas válidas por modalidade e pelo menos 110 originais no conjunto, legislação/fontes atuais e pares com Administrativo sem inventar norma/prazo local; conferir pacotes reais no leitor/prévia isolados sem aprovação automática.

## 8. Validação integrada e entrega local — depende de 2–7

- [ ] 8.1 Consolidar manifestos e repetir a auditoria conjunta de fontes, fingerprints e pares entre disciplinas/modalidades/legado; verificar as 102 células candidatas, mínimo 510 originais e nenhuma justificativa desatualizada, preservando relatórios específicos já entregues em cada grupo.
- [ ] 8.2 Ensaiar a composição completa em PostgreSQL isolado: prévia/aplicação dos pacotes, DRAFT, reaplicação, base desatualizada, seleção/digest alterados, concorrência e rollback. Conferir dados antes/depois e contratos de segurança sem aprovar o acervo real por fixtures ou CI.
- [ ] 8.3 Preparar backup privado e reconciliar o destino local, importar somente a seleção/digests conferidos e registrar decisões editoriais pelos mecanismos existentes para o teste local autorizado; verificar recibos, ponteiros, sequências, preservação de dados e ausência de habilitação por upload/startup, sem publicar dados privados.
- [ ] 8.4 Conferir pelo banco e contratos HTTP a cobertura elegível final: ≥25 e ≥5 originais em cada célula, ≥1.275 por modalidade e ≥2.550 no conjunto, sem multiplicar por cargo/variante/revisão. Entregar evidência com data/destino e déficits reais; qualquer célula pendente impede declarar a expansão completa.
- [ ] 8.5 Executar testes editoriais/backend/frontend, build e Playwright Chromium/Edge pertinentes no conjunto: visitante/conta, aula → prática de 25 → resultado, simulado, histórico antigo, reinício, teclado/celular e proteção de filas/identidade. Registrar comandos, SHAs, resultados e limitações, sem tratar CI como revisão editorial/semântica.
- [ ] 8.6 Entregar guia e relatório integrado para o teste local do usuário com seleção/digests, totais e recuperação por backup/imagem exatos; verificar o percurso documentado e separar habilitado/verificado de aceite, revisão, integração e publicação. Não adiar para este grupo os testes/docs devidos nos grupos anteriores.

## Workflow follow-up

- Antes de iniciar: solicitar confirmação explícita dos grupos completos e registrar na #173 responsável/conversa, branch/worktree, base, módulos/contratos e recursos de teste. Esta publicação documental não autoriza código/conteúdo/carga nem disparo de novas conversas de implementação.
- Grupos 3–7 dependem do contrato/ferramentas de 1 e podem trabalhar em paralelo com arquivos próprios. Grupo 2 possui os contratos compartilhados de prática; a central conserva manifestos agregados e ordem. Grupo 8 exige 2–7. Novas dependências devem ser registradas, sem ampliar escopo ou compartilhar runtime silenciosamente.
- Teste do usuário ocorre antes de envio/revisão de implementação conforme AGENTS.md. Publicação documental atual é autorizada; não autoriza pushes futuros de código/conteúdo. Revisões independentes de lote e gate cumulativo final são distintos, em contexto novo somente leitura, modelo confirmado `openai/gpt-6.1-sol#xhigh`, sem fallback ou medição de cota.
- Na issue, registrar implementação/verificação, revisão/aceite e integração como estados separados, com PRs/SHAs/checks/limites reais. OpenSpec strict/CI não provam originalidade, correção ou elegibilidade. Não inventar revisões, tokens ou aprovação humana/institucional.
- Manter #161 aberta como proposta independente; a futura remoção de cargo deve considerar o teto 25 e a main atual. Não alterar seu contrato/stack/migrações nesta spec. As #158/#160 foram encerradas após conferência dos merges e CI, sem arquivar seus OpenSpecs.
- Integração de tarefas e PR final `[SPEC]` → main exigem autorizações específicas e gates próprios; não usar auto-merge, fila ou bypass. Arquivamento, carga/deploy na VPS e publicação seguem autorização/entrega operacional próprias.
