# Organização do projeto

## Specs, épicas e tarefas

- Toda spec OpenSpec usa `epic/<change-name>` como branch de integração.
- Uma tarefa ou grupo pequeno e coeso usa `task/<change-name>/<assunto>` e abre PR para a épica, nunca diretamente para `main`.
- Inclua código, testes e documentação necessários à tarefa no mesmo PR. Evite separar testes da implementação.
- Prefira PRs revisáveis: cerca de 300–500 linhas autorais quando possível; identifique lockfiles e arquivos gerados à parte. Não divida artificialmente uma mudança inseparável.
- Branches novas partem da épica atualizada. Tarefas dependentes aguardam os PRs anteriores; PR antecipado deve ser rascunho com dependências explícitas. Não marque tarefas integradas à épica antes da revisão/merge.
- Quando autorizado pelo usuário, implemente grupos em PRs encadeados de rascunho para revisão ao final, sem pausas e sem merges no GitHub. Este modo está autorizado para `optional-google-identity`. Preserve o histórico ao atualizar dependências; merges locais entre branches de trabalho não representam aprovação/integração na épica. Retargetar os PRs na ordem após revisão autorizada.
- Use uma issue da spec com checklist e links dos PRs. Registre tarefas implementadas/verificadas separadamente das integradas/aprovadas.
- Durante a reformulação, a épica geral é `epic/application-reformulation`. Cada spec cria sua branch `epic/<change-name>` a partir da épica geral atualizada; tarefas continuam abrindo PRs para a branch da spec. Ao concluir a spec, seu PR é destinado à épica geral, **não a main**.
- Todas as specs da reformulação ficam fora de main até concluir o pacote e obter aprovação explícita da entrega final. Só então abra PR `epic/application-reformulation` → `main`. Merges de tarefas/specs não autorizam esse merge final. O diff final é cumulativo; os PRs menores são a evidência da revisão por partes.
- Não congele correções independentes do site atual: podem continuar em main por PR aprovado, sem trazer a aplicação incompleta. Atualize a épica geral com main quando necessário, preservando o trabalho em andamento.
- Nunca faça merge, feche PRs sem substituição documentada, arquive OpenSpec ou relaxe regras de deploy sem autorização.
- Preserve branches/commits anteriores ao reorganizar trabalho. Não apague implementação existente.
- Consulte `docs/application-reformulation.md` para o roadmap, critérios de entrega e decisões ainda abertas. Uma etapa do roadmap não autoriza implementar uma spec sem proposta e aprovação.

## Arquitetura

- Antes de implementar ou revisar backend, leia `docs/architecture/adr/0001-domain-and-application-boundaries.md`. Esta é a referência detalhada das decisões aprovadas em 2026-10-08; não dependa da memória da conversa.
- Use monólito modular por funcionalidade. UseCases coordenam ações relevantes; entidades/objetos de valor protegem invariantes. Domain Services somente para regras sem proprietário natural; não invente comportamento para preencher camadas.
- Domínio e núcleo da aplicação são Java puro, sem Spring/JPA/JDBC/HTTP/OAuth. Dependências apontam dos adaptadores para o núcleo, nunca no sentido contrário. Wiring e execução transacional ficam na infraestrutura.
- Persistência do domínio usa JPA/Hibernate por contratos de repository. Modelos JPA ficam na infraestrutura; SQL nativo é permitido ali quando justificado. Nunca acessar EntityManager/JdbcTemplate ou repositories JPA concretos em UseCases/controllers/handlers.
- Flyway é o único dono do schema; Hibernate usa `ddl-auto=validate` e `open-in-view=false`. Não alterar migrações já versionadas para adaptar ORM. Spring Session JDBC e prontidão SQL são mecanismos técnicos, não entidades de negócio.
- Prefira composição; interfaces somente em fronteiras úteis. Não criar Service intermediário que apenas repasse ao UseCase, classe abstrata especulativa, entidade de negócio para cookie/CSRF ou DTO que vaze entidade JPA.
- Teste domínio sem framework, aplicação com portas substituíveis, infraestrutura com PostgreSQL real isolado e limites com ArchUnit. Preserve rollback antes de recuperar conflitos; não tratar toda falha de integridade como identidade já vinculada.

## CI e preview

- CI verifica builds, testes e regressão; não publica previews.
- Produção estática continua automática em push para `main`.
- Preview é manual: Actions → Publish GitHub Pages → Run workflow, executar o workflow de `main` e informar a branch desejada no campo `branch`.
- A referência do workflow é `main`, não a branch da tarefa: preserva as regras do ambiente github-pages. Antes do merge dessa automação, use a branch autorizada `infra/spec-task-workflow` como referência do workflow.
- Previews atuais são do material estático; não executam Java/PostgreSQL nem publicam a aplicação React. Deployment da aplicação completa terá spec própria.
- Nunca publicar segredos, arquivos `.env` ou dados privados. Preserve as aulas e os previews já armazenados.
