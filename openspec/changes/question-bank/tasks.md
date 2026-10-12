# Tasks

**Proposta aprovada no #31; rodada 1.2, 1.3 e 2.1–2.3 autorizada pelo usuário**, sem pausas entre tarefas e sem merges. Issue #30; `[x]` registra implementação/verificação na branch, **não aprovação/integração à épica**. PRs pequenos em draft; duas revisões independentes do lote (Astra medium e SOL 6.1 xhigh) ao fim da implementação, antes da aprovação de merge. Registro do processo: #39, sem integração presumida. Fixtures não aprovam conteúdo real; demais tarefas não iniciam automaticamente.

## 1. Catálogo e domínio — grupo catalog-domain

- [x] 1.1 Definir IDs/mapeamento das cinco disciplinas, assuntos e dois cargos; testar associação assunto-disciplina, compartilhamento sem duplicação e rejeição de cargos externos; documentar matriz rastreável aos módulos.
- [x] 1.2 Implementar questão/revisão/alternativas e invariantes de conteúdo imutável, resposta única, fontes/acessibilidade; testar versões/classificação e ausência de frameworks no núcleo, com documentação do modelo.
- [x] 1.3 Implementar transições editoriais, evidência da versão exata, substituição/retirada de elegibilidade e UseCases/portas necessários; testes puros de evidência desatualizada, revisão pendente e inexistência de aprovação implícita.

## 2. Persistência e unidade atômica — grupo question-storage, após 1

- [x] 2.1 Criar migração Flyway aditiva apósV3 e modelosJPA separados; testar banco novo/upgradePG18.6, constraints e preservação de identidades/sessões; documentar schema/rollback não destrutivo.
- [x] 2.2 Implementar persistência/leitura e controle concorrente de revisões/ponteiro elegível; testar rollback integral, substituição atômica, duas aprovações concorrentes e base desatualizada, sem sobrescrita silenciosa.
- [x] 2.3 Implementar unicidade/idempotência por origem/conteúdo e tradução somente de conflitos conhecidos; testar reimportação concorrente, FK/falha inesperada propagadas e recovery apósrollback; proteger fronteiras com ArchUnit e documentar limites.

## 3. Extração e operações locais — grupos legacy-extraction e editorial-cli, após 1–2 conforme dependência

- [x] 3.1 Definir formatoJSON/canonização/fingerprint e extrator manifesto/CSV-BOM/HTML/gabaritos; testar joins porID, contagens510, Unicode/fórmulas, recursos e resolução correspondente; produzir inventário completo/pendências e documentar o formato.
- [x] 3.2 Validar markup/URLs/caminhos/recursos e suspeitas de duplicação; testes de scripts/eventos/traversal/symlinks/extração incompleta e ausência de fusão/perda silenciosa, com documentação da allowlist.
- [x] 3.3 Implementar CLI nonweb de prévia/aplicação local confirmada, seleção de pacote e versão-base esperada; testar prévia sem escrita, pacote inválido sempartial e reaplicação sem novosIDs/versões/reativação; verificar comandos documentados emDBisolado.
- [ ] 3.4 Implementar CLI de envio à revisão/aprovação/retirada e relatório editorial por disciplina/assunto; testar com fixtures a versão exata/checklist/destino/confirmação, bloquear ativação em startup normal e aprovação real em CI; documentar responsabilidade operacional e ausência de privilégios Google/API admin.

## 4. Consulta HTTP pública — grupo public-question-api, após 1–2

- [ ] 4.1 Implementar catálogo/lista/detalhe filtrados, paginação/ordem e DTOs/OpenAPI; testesHTTP reais de400/404/200/vazio, filtrosAND e somente versão elegível, sem gabarito/resolução/estado privado ou entidadesJPA.
- [ ] 4.2 Configurar fronteira de segurança/leitura semsessão para rotas exatas, incluindo negações e preflightCORS permitido; testar visitantes/autenticados/cookies conhecidos/desconhecidos, métodos/paths/gabaritos/escritas negados e ausência de efeitos; regressar ordem/registro/readiness/OIDC/CSRFlogout.
- [ ] 4.3 Testar projeções deJSON/markup/recursos contra campos/indicadores da resposta, leitura direta de rascunho/retirada e indisponibilidade sem acervo vazio fictício; documentar diferença entre proteção da API e gabaritos públicos legados, com negativo controlado de vazamento.

## 5. Interface acessível — grupo question-browser, após 4

- [ ] 5.1 Criar catálogo/filtros/paginação/lista/detalhe e estados carregando/vazio/erro, sem seleção de resposta/correção/histórico; testar consultas por visitante e mesmos filtros com usuário autenticado, mantendo LoginPanel e link das aulas.
- [ ] 5.2 Aplicar prazo finito atéJSON e invalidação/cancelamento entre filtros; regressões determinísticas de resposta antiga, timeout/retry/abort, sem alterar os comportamentos corrigidos da identidade; documentar limites de consulta.
- [ ] 5.3 Verificar teclado/foco/rótulos/fórmulas/fontes e layouts390/1280px comPlaywright; inspecionarDOM/bundle/recursos sem dados corretivos e APIindisponível com aulas acessíveis; atualizarREADME da interface.

## 6. Curadoria inicial — lotes pequenos após 3, paralelos a 4–5

Selecionar50questões distintas por disciplina, distribuídas por assuntos, em5lotes de10; pode ampliar até100 com lotes adicionais explicitamente revisados. Cada tarefa abaixo entrega pacote/registro/checklist da revisão exata, conferência de resposta/distratores/resolução acessível/referências e testes aplicáveis. Não escolher itens só para preencher número. Pendências bloqueiam sua aprovação; registrar lacuna sem fingir tarefa concluída. Cada lote é unidade revisável, não uma aprovação automática de toda a disciplina.

- [ ] 6.1 Revisar lote1 dePortuguês(10), com evidências/editorial e validação do pacote; conferir identidade/classificação/fontes/resolução antes da aprovação explícita.
- [ ] 6.2 Revisar lote2 dePortuguês(10), incluindo duplicações/cobertura contra lote1 e validação/evidências da versão exata.
- [ ] 6.3 Revisar lote3 dePortuguês(10), incluindo duplicações/cobertura acumulada e validação/evidências da versão exata.
- [ ] 6.4 Revisar lote4 dePortuguês(10), incluindo duplicações/cobertura acumulada e validação/evidências da versão exata.
- [ ] 6.5 Revisar lote 5 de Português (10), conferir total de 50 IDs elegíveis distintos/cobertura e entregar relatório/pendências com validação/evidências.
- [ ] 6.6 Revisar lote1 deMatemática/Raciocínio(10), com conferência de cálculo/resultado/fórmula e evidências/editorial da versão exata.
- [ ] 6.7 Revisar lote2 deMatemática/Raciocínio(10), conferir cálculo/duplicações/cobertura acumulada e validar pacote/evidências.
- [ ] 6.8 Revisar lote3 deMatemática/Raciocínio(10), conferir cálculo/duplicações/cobertura acumulada e validar pacote/evidências.
- [ ] 6.9 Revisar lote4 deMatemática/Raciocínio(10), conferir cálculo/duplicações/cobertura acumulada e validar pacote/evidências.
- [ ] 6.10 Revisar lote 5 de Matemática/Raciocínio (10), conferir 50 IDs distintos/cobertura, cálculos e relatório/pendências com validação/evidências.
- [ ] 6.11 Revisar lote1 deInformática(10), conferir condições/versões de ferramentas citadas, fontes/resolução e validar pacote/evidências.
- [ ] 6.12 Revisar lote2 deInformática(10), conferir duplicações/cobertura/condições de ferramentas e validar pacote/evidências da versão exata.
- [ ] 6.13 Revisar lote3 deInformática(10), conferir duplicações/cobertura/condições de ferramentas e validar pacote/evidências da versão exata.
- [ ] 6.14 Revisar lote4 deInformática(10), conferir duplicações/cobertura/condições de ferramentas e validar pacote/evidências da versão exata.
- [ ] 6.15 Revisar lote 5 de Informática (10), conferir 50 IDs distintos/cobertura e relatório/pendências com validação/evidências.
- [ ] 6.16 Revisar lote1 deconteúdo administrativo(10), conferir rotina/associação ao cargo/fontes/normas aplicáveis e validar pacote/evidências.
- [ ] 6.17 Revisar lote2 deconteúdo administrativo(10), conferir duplicações/cobertura e fontes/normas aplicáveis com validação/evidências.
- [ ] 6.18 Revisar lote3 deconteúdo administrativo(10), conferir duplicações/cobertura e fontes/normas aplicáveis com validação/evidências.
- [ ] 6.19 Revisar lote4 deconteúdo administrativo(10), conferir duplicações/cobertura e fontes/normas aplicáveis com validação/evidências.
- [ ] 6.20 Revisar lote 5 de conteúdo administrativo (10), conferir 50 IDs distintos/cobertura e relatório/pendências com validação/evidências.
- [ ] 6.21 Revisar lote 1 de conteúdo escolar (10), conferir rotina/dispositivo/vigência das normas aplicáveis e registrar fonte/data/pendências; validar pacote/evidências.
- [ ] 6.22 Revisar lote2 deconteúdo escolar(10), conferir duplicações/cobertura/normas e validar pacote/evidências da versão exata.
- [ ] 6.23 Revisar lote3 deconteúdo escolar(10), conferir duplicações/cobertura/normas e validar pacote/evidências da versão exata.
- [ ] 6.24 Revisar lote4 deconteúdo escolar(10), conferir duplicações/cobertura/normas e validar pacote/evidências da versão exata.
- [ ] 6.25 Revisar lote 5 de conteúdo escolar (10), conferir 50 IDs distintos/cobertura e relatório/pendências com validação/evidências.

## 7. Verificação integrada — após 1–6

- [ ] 7.1 Executar fluxoCLI/backend/PG/browser reais isolados:importar/reaplicar/revisar/aprovar/retirar/filtrar/consultar, incluindo concorrência/erros/migração; conferir250IDs aprovados no mínimo(50pordisciplina) ou registrar decisão de escopo explícita, sem misturarfixtures com acervo real.
- [ ] 7.2 Regressar Gradlecleanbuild, frontendtest/build, guard/OpenSpec, identidade/OIDC/readiness e UI/offline comSTUDY_ROOT construído; conferir108páginas/1757referências/nove módulos90resoluções e hashes de fontes intactos, registrando quem executou e nenhumskip silencioso.
- [ ] 7.3 Verificar pacotes/JAR/bundle semfixtures/editorial real automático/dados corretivos públicos, comandoslocais/rollback eCIsempublicação; registrar evidência de integração e tokens por sessão, sem confundir testes/aprovaçãoeditorial/revisãoindependente.

## Workflow follow-up

- Planejamento e proposta exigem aprovação antes de qualquer tarefa. PRs coesos podem dividir grupos maiores; lotes editoriais não devem gerar PRs gigantes ou contagens de aprovação fictícias.
- PilotoStacks somente após ensaio/autorização e ajuste de processo aprovado se necessário; trunk da spec, PR[SPEC] separado. Não rodar rebase/sync emworktree ocupado nem merge do topo sem aprovação de todas as camadas incluídas. Sem piloto validado, seguir sequencial normal.
- Atualizar issue #30 distinguindo implementado/verificado de aprovado/integrado; PRfinal[SPEC] exige revisão cumulativa nova Astra medium, relatório completed/gateverde e aprovação própria.
- Não arquivar, integrar main, publicar ou implementar as specs seguintes automaticamente. Google smoke real e infraestrutura permanecem na preparação da entrega.
