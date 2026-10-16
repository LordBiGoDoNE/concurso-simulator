# Tasks

**Proposta aprovada pelo usuário em 2026-10-15**, #87 integrado em `05b9cb9` na
épica da spec; primeiro lote autorizado: grupos completos 1 e 2. **8/24 tarefas
implementadas/verificadas, 0/24 integradas/aprovadas**; revisão independente e CI do lote
registrados separadamente na issue #86. Grupos 3–6 pendentes. Pré-requisito #84 em `0d0fc48`.

Grupos completos são a unidade de autorização do lote. Cada tarefa inclui código,
testes e documentação pertinentes; PRs coesos podem dividir um grupo sem interrompê-lo.
`[x]` registra implementação/verificação, não aprovação/merge. Integração e decisões
editoriais são acompanhadas separadamente na issue. Sem conteúdo aprovado por CI.

## 1. Domínio e geração determinística

- [x] 1.1 Modelar template/versão/domínio/fonte/tupla e invariantes Java puras; testar campos/tipos, compatibilidade da base e imutabilidade; documentar fronteiras entre banco, parametrização e tentativa.
- [x] 1.2 Implementar as três famílias com aritmética exata, quatro alternativas e resolução concreta; testar casos de referência, unidades, distratores equivalentes e limites; documentar os domínios 64/20/27.
- [x] 1.3 Implementar enumeração/manifesto/digests versionados e validação exaustiva das 111 tuplas com oráculo independente; testar adulteração, cardinalidade e saídas divergentes; documentar canonização e limites da prova.
- [x] 1.4 Implementar UseCases/portas de prévia/registro/decisão/captura com aprovação exata e ponteiro único elegível por família; testar v2 substituindo v1, retirada sem reativação, replay e decisões concorrentes com portas substituíveis; documentar critérios de elegibilidade.

## 2. Persistência e operação editorial local — depende de 1

- [x] 2.1 Criar migração aditiva após V5/JPA/repositories de templates, versões, manifestos e decisões; testar PG18 novo/upgrade, FKs/constraints/imutabilidade preservando banco/contas/sessões; documentar schema.
- [x] 2.2 Implementar registro/decisão/ponteiro elegível atômicos, unicidade e recuperação após rollback; testar aprovação concorrente de versões, retirada atual/histórica, base desatualizada, repetição sem reativação e falhas desconhecidas; documentar transações.
- [x] 2.3 Implementar captura privada de templates na transação consumidora, vinculada à revisão-base elegível; testar retirada/troca de base antes/depois do snapshot e ausência de dependência reversa; documentar contrato de módulos.
- [x] 2.4 Implementar launcher local explícito com prévia sem escrita, limites de entrada e confirmação destino/digest; testar path traversal, seleção parcial, falhas/CI sem aprovação e ausência no startup web; documentar operação segura.

## 3. Integração de tentativas e compatibilidade — depende de 1 e 2

- [ ] 3.1 Estender composição e seleção por identidade-base com opção false por padrão; testar uniformidade estrutural/sem reposição, fixas misturadas, ausência/retirada de template e integridade falhando sem fallback; documentar política.
- [ ] 3.2 Materializar/capturar conteúdo, metadados/parâmetros e alternativas opacas numa transação, com vínculo imutável ao item; testar FKs, divergência do manifesto/fingerprint, rollback integral e cleanup visitante sem remover famílias; documentar fontes fixa/gerada.
- [ ] 3.3 Versionar snapshot e manter codecs de origem em tentativas/itens/comandos; testar alteração com triggers V5/JSON imutável, retry de hash produzido no V5 com opção ausente/false, 409 com true, corrida/timeout; documentar compatibilidade de codec e hash sem reescrever histórico.
- [ ] 3.4 Preservar correção e histórico após retirada/atualização/ausência de gerador; testar sem invocação de geração na leitura/conclusão, posse/expiração e rollback com leitor compatível; documentar limite do JAR antigo.

## 4. Conteúdo e revisão editorial — depende de 1 e 2

- [x] 4.1 Preparar definições e selecionar três bases compatíveis, origens/fontes e domínios em arquivos versionados; verificar elegibilidade real sem inventar IDs/aprovações; documentar lacunas que dependam de decisão editorial própria.
- [x] 4.2 Produzir relatório integral das 111 variantes com resultados independentes, alternativas, unidades e resoluções; revisar linguagem/distratores/acessibilidade/proveniência de todo o domínio; documentar pendências sem aprovação fictícia.
- [x] 4.3 Submeter versões/digests exatos para decisão editorial explícita e registrar resultado autorizado em PG local isolado; testar distinção entre fixtures e conteúdo real, reexecução/retirada e relatório por família; não declarar família liberada se aprovação estiver pendente.
- [x] 4.4 Demonstrar que variantes não aumentam contagem de identidades na meta do banco nem duplicam a mesma base no simulado; verificar material original/URLs/offline e registrar rastreabilidade de bases/manifestações.

Grupo 4: seleção editorial exata aprovada pelo usuário e executada somente na cópia
local, com replay/retirada em clone separado. Evidências em
`editorial/parameterized-questions/2026-10/eligibility-after.json` e `withdrawal-probe.json`.
Implementação/verificação não significa revisão do lote nem integração dos PRs.

## 5. Contrato HTTP e interface — depende de 3

- [ ] 5.1 Estender DTO/OpenAPI de criação com opção booleana opcional e false por padrão; testar cadeia real de segurança/CSRF, payload inválido, campos extras, clientes antigos e erro genérico; documentar proprietário de cada decisão pelo checklist HTTP.
- [ ] 5.2 Adicionar checkbox/explicação acessíveis e preservar escolha/commandId no retry; testar teclado/foco, opção desligada inicial, mistura fixa/gerada e troca de contexto; documentar comportamento e ausência de garantia de novidade.
- [ ] 5.3 Verificar projeções/recursos/DOM/bundle/erros sem metadados privados ou correção antecipada e resultado com valores/descrições corretos; testar inspeção e validação rigorosa dos contratos, sem solver no cliente; documentar limites do legado.
- [ ] 5.4 Verificar conta salva/retoma/resultado em outra sessão, visitante temporário e falhas de rede/expiração/concorrência com conteúdo gerado; adicionar regressões necessárias sem duplicar a suíte anterior; documentar evidências por ator.

## 6. Verificação integrada e gate — depende de 3, 4 e 5

- [ ] 6.1 Executar matriz de cenários desta spec, testes Java puro/ArchUnit/PG real/HTTP/browser e build frontend/backend; conferir requisito→código/teste, sem apresentar contagem como revisão semântica.
- [ ] 6.2 Ensaiar upgrade com banco V5/contas/sessões/tentativas/comandos antigos e rollback com geração desabilitada/leitor compatível, inclusive novas instâncias; documentar incompatibilidade do JAR anterior desde a primeira escrita no formato novo, mesmo fixa, sem apagar dados.
- [ ] 6.3 Verificar clone limpo/material/links/offline e operação editorial de ponta a ponta, registrando 111/111 validadas e decisões reais separadas de aprovação de código; manter pendências explícitas sem liberar versões não aprovadas.
- [ ] 6.4 Consolidar PRs/SHAs/checks/triagem/cobertura/cota/tokens e executar gate cumulativo SOL 6.1 xhigh em contexto novo após revisões dos lotes; abrir/manter `[SPEC]` para épica geral e solicitar aprovação própria, sem main/deploy/arquivamento.
