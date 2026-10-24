# Tasks

Proposta em preparação; nenhuma tarefa de implementação iniciada. Pedido do usuário:
separar prática de aula e simulado; decisão confirmada de correção somente ao encerrar.
Escolher explicitamente os grupos completos antes de implementar.

## 1. Núcleo e persistência — base dos demais grupos

- [ ] 1.1 Modelar a finalidade e o início da prática por assunto/cargo, política até cinco e invariantes de correção final.
- [ ] 1.2 Persistir a distinção por migração aditiva, preservando formatos/comandos/dados antigos e transações/replay.
- [ ] 1.3 Separar consultas de histórico e métricas antes de paginação/limite; manter conta/visitante/expiração.
- [ ] 1.4 Concluir testes de domínio/aplicação/PostgreSQL/compatibilidade/ArchUnit, docs e revisão independente do grupo.

## 2. HTTP e privacidade — depende de1

- [ ] 2.1 Expor DTOs e endpoints próprios de prática e histórico, com contratos estáveis e erros tipados.
- [ ] 2.2 Integrar a cadeia de segurança real: posse, visitante, sessão, CSRF, acesso por modalidade e respostas no-store.
- [ ] 2.3 Verificar criação/retry/conflito/salvamento/conclusão e bloqueio de correção antecipada e cruzamento de modos.
- [ ] 2.4 Concluir regressões HTTP/PG, checklist de responsabilidades, documentação e revisão independente do grupo.

## 3. Interface própria — depende de1–2

- [ ] 3.1 Iniciar prática pela aula com cargo e contexto claros, carregamento/retry/sem questões, sem passar pelo compositor.
- [ ] 3.2 Implementar realização/correção final/histórico próprios e retorno à aula, com salvamento confirmado.
- [ ] 3.3 Preservar simulado e prática independentes, filas/identidade/logout/respostas tardias e troca explícita de prática.
- [ ] 3.4 Concluir regressões React/Chromium, teclado/foco/desktop/celular, documentação e revisão independente do grupo.

## 4. Verificação integrada e gate — depende de1–3

- [ ] 4.1 Ensaiar migração e leitura dos dados antigos em PostgreSQL isolado, modos/contas/visitante e paginação de históricos/métricas.
- [ ] 4.2 Verificar no Docker o fluxo aula→prática→correção final, preservando um simulado e todas as51 aulas/acervo.
- [ ] 4.3 Consolidar regressões/limites/procedimento de atualização e executar gate cumulativo independente SOL6.1xHigh.
- [ ] 4.4 Conferir checks/SHAs/triagem/cota, solicitar merges específicos e atualizar a issue após cada mudança/integração.

Todos os grupos incluem código, testes e docs necessários. PRs pequenos dividem
entrega, não o compromisso do grupo. A aprovação da proposta não presume escolha
do lote nem autorização de merges; estes estados ficam separados na issue.
