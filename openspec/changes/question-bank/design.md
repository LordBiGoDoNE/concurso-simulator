# Design

## Context

Ver `proposal.md`. Base da spec: `f72fcc5`, após #25 na épica geral. Stack existente: Java25/Boot4.1.1/Gradle9.8.0, PostgreSQL18.6, JPA/Flyway, React/TypeScript/Vite; ADR0001 e checklistHTTP obrigatórios. Não há módulo de questões nem endpoints de acervo; a segurança permite explicitamente prontidão/identidade/OIDC e nega demais rotas.

O manifesto contém51módulos e o CSV510IDs únicos: Português100, Matemática/Raciocínio120, Informática100, Administrativo80, Escolar110. O CSV tem BOM UTF-8, referências temáticas e30fundamentos legais; não contém enunciados/alternativas/resoluções. Estes estão em HTML de conteúdo/gabarito; `scripts/math_content.py` é fonte editorial complementar das90resoluções enriquecidas, não do acervo completo. Inventário não comprova correção ou cobertura do edital.

## Goals / Non-Goals

**Goals:** conteúdo/editorial verificáveis, reimportação segura, consulta pública somente da revisão elegível, proteção do material e fronteiras compatíveis com tentativas futuras.

**Non-Goals:** não criar um motor de concursos/configurações, um serviço por endpoint, editor administrativo web, roles Google adicionais, busca semântica/GraphRAG, geradores ou schema de tentativas antecipado. Sem garantia de sigilo global: o legado já tem gabaritos públicos e será preservado.

## Decisions

### Fronteira funcional e invariantes

Novo módulo `questionbank`: domínio/aplicação puros, adaptadores HTTP/CLI e JPA/segurança/transações na infraestrutura. Questão tem identidade estável, revisões imutáveis e uma revisão aprovada elegível no máximo. Métodos do domínio protegem revisão, aprovação, substituição e retirada; UseCases coordenam importação, operações editoriais e seleção do acervo por portas necessárias. Evitar Service intermediário, entity técnica para arquivo/hash e interfaces sem fronteira real.

Conteúdo de cada revisão inclui classificação/associações de cargo; a consulta usa a classificação da revisão elegível, não a de um rascunho mais recente. Assunto pertence a uma disciplina; múltiplos cargos não duplicam questão. Usar as cinco disciplinas e os dois cargos atuais, com IDs estáveis e mapeamento explícito dos módulos; não transformar automaticamente cada seção de aula em novo assunto.

Revisão tem ID e número sequencial por questão, enunciado, alternativas de ID estável/ordem editorial, único ID correto, resolução didática, fontes e proveniência. Conteúdo inclui representação textual acessível de fórmulas e referências a recursos locais permitidos. Nada de embaralhamento ou parâmetros nesta etapa. Alternativas precisam de pelo menos duas opções distintas; gabarito não é índice transitório ou letra dependente de ordem.

Estado editorial é metadado mutável: `DRAFT` → `IN_REVIEW` → `APPROVED`; revisão com pendência volta a `DRAFT` com motivo, e correção de conteúdo cria outra revisão. Somente aprovação explícita substitui o ponteiro elegível, em transação; revisão anterior continua aprovada no histórico, não na consulta corrente. Retirada desativa elegibilidade com motivo sem apagar conteúdo. Mesmo reimportando pacote anterior, nunca regredir ponteiro/estado ou reativar retirada.

### JPA, migração e concorrência

Flyway cria migração aditiva apósV3 para catálogo, questão/revisão, alternativas, fontes/classificações e registros editoriais; definir tabelas/constraints na implementação, sem reescreverV1–V3. `ddl-auto=validate`/OSIVfalse mantidos, entidadesJPA separadas do núcleo. Não armazenar HTML bruto executável ou sessão/editor como entidade da questão.

Unicidade de chave de origem com namespace, número de revisão e IDs de alternativas; referência correta tem que pertencer à mesma revisão. Aplicação de pacote validado é atômica. Mudança de chave existente exige versão-base esperada e trava/controle otimista; concorrente não sobrescreve decisão editorial. Repetição de mesma chave/conteúdo devolve existentes; falhas desconhecidas/FK/indisponibilidade não viram sucesso. Se recuperar conflito conhecido, fazê-lo somente apósrollback e por constraint específica, conformeADR.

Questão/revisão recebem UUIDs persistidos; mesma origem resolve IDs existentes. Fingerprint é SHA-256 de payload canônico versionado, cobrindo todo conteúdo relevante, classificação e fontes, não uma comparação só de enunciado. Codificação UTF-8, ordem explícita dos campos/listas e recursos fazem parte do formato; preservar texto matemático/Unicode, sem normalização lossy. Hash de arquivo de origem também é registrado, mas sozinho não define equivalência editorial.

### Extração e pacote editorial local

Extrator lê somente arquivos manifestados nas raízes locais aprovadas, CSV com `utf-8-sig` e HTML de conteúdo/gabarito. Junta pela chave comoP01-Q01 e número da questão, nunca só por proximidade de parágrafos. Não buscar fontes na rede nem executar scripts/importar arbitrariamente Python do pacote. Conferir hashes/caminhos do snapshot original e impedir traversal/symlink para fora das raízes.

Gerar pacoteJSON com versão de formato, catálogo/mapeamento, origem/IDs e revisões candidatas. Copiar somente os blocos da questão/alternativas e resolução correspondente; não anexar aula/esquema com resposta por conveniência. Texto/markup restrito e sanitizado preserva ênfase, listas e fórmulas acessíveis; bloquear scripts/eventos/URLs perigosas e apontar perda/incompatibilidade, em vez de removê-la silenciosamente. Recursos locais têm tipo/hash/descrição permitidos; não copiar links de gabarito para projeção pública.

Prévia não abre transação de escrita nem altera arquivos. Relatório por chave: nova, idêntica, conteúdo alterado, inválida, possível duplicação entre chaves. Relatar todo o inventário510, incluindo itens não convertidos; aplicar somente pacote explicitamente selecionado e integralmente válido. Itens recusados permanecem no inventário/relatório, nunca somem da contagem original. Similaridade só sinaliza, sem fusão/exclusão automática. Reaplicar seleção não remove questões ausentes.

Adaptador CLI local do backend invoca UseCases, com perfil explícito **não web**, destino do banco/configuração e ação apresentados antes da confirmação de escrita; recusa aprovação por importação e execução editorial durante startup normal. CI não aprova acervo real; testes isolados exercitam operações somente sobre fixtures identificadas, sem produzir evidência editorial do acervo. Segredos vêm de configuração externa, não de argumentos/logs. Não criar endpoints/editor admin nem permitir que sessão Google aprove conteúdo. Preferir essa operação restrita a introduzir agora RBAC/painel fora do escopo.

### Revisão real, evidência e meta

Registro de revisão/decisão referencia questão, revisão e fingerprint, responsável editorial identificado no processo, data, checklist e justificativa/link de revisão. Isso é rastreabilidade, não autenticação Google nem prova automática de competência/independência. `IN_REVIEW` permite checklist de correção, opções/distratores, clareza, resolução passo a passo, acessibilidade, fontes e aplicação ao cargo; aprovação exige todos conferidos. Registro de outro conteúdo é rejeitado.

Questões autorais permanecem autorais. Referências temáticas do CSV não viram fonte específica garantida de cada resposta. Revisão exige conferir associação real e, em normas, dispositivo/vigência e data; fonte indisponível/ambígua gera pendência documentada. Ampliar resoluções insuficientes no pacote convertido, sem reescrever HTML-fonte; testes de cálculo quando viáveis complementam, não substituem revisão pedagógica.

Curadoria é trabalho desta spec, com lotes revisáveis por disciplina e seleção representativa dos assuntos; meta50–100 questões aprovadas distintas por disciplina, não50porassunto nem250versões da mesma questão. Relatório distingue inventário/importadas/pendentes/aprovadas/retiradas/cobertura. Lacuna requer curadoria ou decisão explícita de escopo antes de declarar meta atendida; não inflar contador por cargo, versão ou futura variação parametrizada. Não fazer aprovação fictícia para concluir tarefas.

### Contratos de leitura e segurança

- GET `/api/v1/question-bank/catalog`: disciplinas, assuntos e cargos; catálogo sem enunciados/soluções ou campos editoriais privados.
- GET `/api/v1/question-bank/questions`: `disciplineId`, `topicId`, `roleId` opcionais e combinados porAND; `page` default0 e `size` default20, máximo100. Ordenação fixa porID da questão; resposta `{items,page,size,total}` com resumo/IDs de questão e revisão, classificação e início do enunciado, sem alternativas/respostas/resoluções/fontes de gabarito.
- GET `/api/v1/question-bank/questions/{questionId}`: apenas revisão corrente elegível, com enunciado, alternativas identificadas em ordem editorial, referências de estudo seguras e IDs. Não aceitar parâmetros para ler rascunhos ou revisões antigas.
- Sintaxe UUID inválida, filtro desconhecido/incompatível ou paginação inválida: 400 JSON tipado; detalhe com ID válido desconhecido/não elegível: 404 indistinguível; resultado vazio: 200. Erros conhecidos de indisponibilidade não podem fingir acervo vazio. Contratos/OpenAPI explícitos, DTOs concretos, sem entidades JPA/`Map`/curingas.

Configurar matchers limitados a essas rotas na segurança, GET público e demais operações negadas; preservar preflightCORS técnico de origens explícitas sem operação de conteúdo. Isolar essa fronteira de materialização/leitura/commit de sessão usando configuração stateless e adaptação do limite de sessão existente, sem copiar regra no controller nem abrir `/api/**`. Somente cadeia estritamente de leitura/negação pode dispensarCSRF; logout e cadeias existentes não mudam sua proteção. Cookies válidos/desconhecidos não transformam consulta pública em login; rejeições não devem invalidar/renovar sessão ou alterar acervo. Preservar ordem/registro dos filtros, prontidão independente e OAuthGET/callbackexato.

Projeções públicas não serializam ID correto, resolução, estado/checklist, localização do gabarito nem marcaçãoHTML/CSS/alt/data que indique resposta. Separação deve ser comprovada por contrato e regressão negativa, não só esconder CSS. Não disponibilizar endpoint público de revisão completa; tentativas futuras usarão porta interna e pin da versão. O legado não é um segredo: resolver prevenção de consulta indevida na API futura não significa eliminar todo acesso às aulas/gabaritos.

### Interface e regressões

Tela de catálogo/filtros/lista/detalhe, sem responder/corrigir/salvar tentativa. Consulta por visitante ou autenticado tem mesmo conteúdo, com navegação por teclado, rótulos, fórmula acessível, estadoscarregando/vazio/erro/retry. Prazo finito cobre headers+JSON e mudança de filtro cancela/invalida resultados antigos. Manter LoginPanel e link das aulas disponíveis durante falhas, sem prometer progresso salvo. Sem copiar dados corretivos para storage/bundle do cliente.

Testar domínio semframework, UseCases com portas, persistência/concorrência/migração emPG18.6isolado, CLI com prévia/aplicação idempotente e conteúdo malicioso, HTTP real/contratos/ausência de gabarito/negativos de rota/método/cookies. Browser com backend/PG reais: importarfixture, revisaraprovar, filtrar/ler, rejeitarpendente, recuperar falha; identidade/prontidão/OIDC e UI/offline antigos também regressam. CI só verifica, sem credenciaisGoogle reais/publicação/aprovaçãoeditorial automática.

### Piloto de Stacks e aprovação

Extensão oficial `github/gh-stack` não está instalada no ambiente verificado (`gh`2.97.0). Planejar piloto nas tarefas dependentes, trunk=`epic/question-bank`; tarefas independentes separadas e PR[SPEC] fora da stack. Não converter histórico já integrado, instalar ferramenta ou retargetar PRs nesta proposta.

Referências de comportamento: [GitHub Stacks](https://docs.github.com/en/pull-requests/reference/stacked-pull-requests) e [CLI/worktrees](https://docs.github.com/en/pull-requests/reference/stacked-prs-cli-commands). Disponibilidade e comportamento efetivos neste repositório ainda não foram ensaiados; documentação não é evidência de piloto executado.

Antes do uso real, ensaiar branches descartáveis/repoisolado com worktrees, pushes/rebases, metadados do eventoPR, proteções e `spec-review.yml`: tarefas precisam continuar emitindo `spec-review-tests`, enquanto somente integração na épica geral exige `spec-review`. Stacks avalia proteções contra trunk e exige linearidade; não relaxar regras. Se precisar mudar workflow/AGENTS para bases encadeadas, obter aprovação de ajuste de processo separado. Sem ensaio/autorização, usar PRs sequenciais normais.

Merge do topo inclui inferiores: só integrar camadas explicitamente aprovadas. Rebase mudaSHAs e pode quebrar ancestralidade de evidência mesmo semdiffvisualnovo; conferir base/head/digest atual e revalidar quando necessário. Não rodar sync/rebase em worktree de agente ativo. Sessão nova por tarefa, leituras focadas e tokens reais por sessão continuam; Stacks não garante economia nem substitui revisão independente cumulativa Astra medium.

## Risks / Trade-offs

- [HTML irregular/BOM/resoluções insuficientes] → inventário510, conversão revisável, prévia e erros por chave; nunca aprovação automática ou perda silenciosa.
- [Meta quantitativa vira aprovação superficial] → checklist/evidência da versão, contagem de identidades/cobertura e lacunas explícitas; qualidade acima de preencher quota.
- [Fonte genérica/legislação alterada] → distinguir autoria/apoio, conferir dispositivo/vigência, registrar pendências; links não provam correção.
- [Resposta em projeção/recursos ou HTMLativo] → sanitização restrita e contratos negativos de JSON/DOM/assets; preservar legado sem prometer sigilo global.
- [CLI com credenciais de escrita] → execução deliberada local/nonweb, prévia e confirmação de destino, sem logs sensíveis/APIadmin; controle de acesso operacional fora do loginGoogle.
- [Rebase automático invalida revisão] → ensaio isolado, autorização explícita, preservação do histórico e revalidação de snapshot; fallbacksequencial.

## Migration Plan

Somente após aprovação da proposta: migração aditiva/schema vazio sem popular aprovações emFlyway; testar banco novo e upgrade desdeV3 com identidade/sessões preservadas. Extrair para pacote/relatório, prévia, importar rascunhos deliberadamente em ambiente local, revisar/aprovar lotes, validar consulta/UI e regressões. Sem conexão remota/provisionamento/publicação automática. Rollback desabilita acesso ao banco de questões na aplicação e conserva dados/versões, sem apagar tabelas ou executar downmigration destrutiva.

Integração por tarefa aprovada na épica da spec; depois revisão independente do diff completo com evidência atual/triagem/gate e PR `[SPEC] Banco de questões...` → épica geral com aprovação própria. Não arquivar nem integrar main. Hospedagem e smokeGoogle continuam na preparação da entrega.
