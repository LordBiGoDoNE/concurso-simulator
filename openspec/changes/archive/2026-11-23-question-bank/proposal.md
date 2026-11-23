# Proposal

## Why

A base e a identidade opcional estão integradas na épica geral. As 510 questões do material estático ainda não formam um acervo consultável, versionado e editorialmente aprovado; precisamos dessa fronteira antes de compor simulados confiáveis.

## What Changes

- Criar catálogo de disciplinas/assuntos e associação aos dois cargos de Limeira 01/2026: Assistente Administrativo e Secretário de Escola, sem ampliar concursos/cargos.
- Adotar meta inicial de 50–100 questões aprovadas por disciplina, distribuídas entre assuntos. É uma meta editorial, não limite de armazenamento nem motivo para excluir o material existente.
- Persistir questões com identidade estável, versões, alternativas identificadas, resposta e resolução didática separadas dos contratos públicos, referências nomeadas e origem rastreável.
- Introduzir rascunho, revisão e aprovação explícitos; nova importação/edição não herda aprovação da versão anterior.
- Extrair o acervo local para pacote revisável e importar de forma idempotente, com prévia sem escrita e relatório de erros/conflitos. Escritas editoriais serão operação local controlada, não painel/API administrativa pública.
- Oferecer catálogo e consulta pública paginada/filtrável das versões aprovadas, com interface acessível e aulas sempre disponíveis. Não incluir resposta, resolução ou indicadores de alternativa correta nesses contratos.
- Preservar arquivos/URLs, gabaritos separados, fórmulas acessíveis e leitura offline do site; distinguir autoria das questões de referências de estudo.
- Fora de escopo: realização/correção de simulados, tentativas/progresso/sincronização, parametrização/IA ao vivo, novos cargos, permissões administrativas web e deployment. Não copiar antecipadamente regras dessas specs.

## Capabilities

### New Capabilities

- `question-bank`: catálogo de questões versionadas, elegibilidade editorial, proveniência/importação controlada e consulta pública sem gabaritos.

### Modified Capabilities

Nenhuma consolidada em `openspec/specs/`; mudanças anteriores permanecem não arquivadas. Preservar contratos de identidade/prontidão e leitura da foundation, adicionando apenas as exceções públicas de consulta descritas no desenho.

## Impact

Novo módulo funcional no backend, domínio/aplicação Java puro, persistência JPA e migração Flyway aditiva, ferramenta local de importação/editorial e DTOs/OpenAPI de leitura. Frontend ganha consulta do banco sem exigir login nem alegar histórico. Documentação/inventário/curadoria e testes passam a distinguir revisão editorial de validação estrutural. Planejamento vinculado à issue #30; `task/question-bank/planning` → `epic/question-bank`, criada de `f72fcc5`. Implementação depende de aprovação própria da proposta; integração final será `[SPEC]` para `epic/application-reformulation`, nunca main. Piloto de Stacks é condicionado a ensaio de compatibilidade, sem alterar proteções ou autorizar merges em lote.
