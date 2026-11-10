# Tasks

Acompanhamento: [issue #161](https://github.com/LordBiGoDoNE/concurso-simulator/issues/161).

Estado em 2026-11-10: o usuário autorizou explicitamente a implementação local dos
grupos completos 1–4 pela conversa central “Automatizar projetos na VPS”.
A autorização anterior apenas documental fica preservada no histórico #162.
Checkboxes de implementação/verificação não representam aprovação ou integração.

## 1. Núcleo e compatibilidade persistida

- [x] 1.1 Selecionar prática por assunto/destinação/aprovação/fonte fixa, sem cargo, com 1–5 bases distintas e insuficiência real para zero.
- [x] 1.2 Introduzir snapshot formato 4 sem Composition; preservar Composition obrigatória dos simulados e invariantes dos formatos anteriores.
- [x] 1.3 Adicionar V14/codec/JPA e replay de criação v2, preservando snapshots, comandos, itens e resultados existentes sem reset ou regravação.
- [x] 1.4 Concluir testes de domínio/aplicação/PG/concorrência/legado/ArchUnit e documentar evidências e limites do grupo.

## 2. Contratos HTTP de prática — depende de 1

- [x] 2.1 Expor início, disponibilidade, visualização, encerramento e histórico tipados por assunto, sem roleId de realização; preservar contratos dos simulados.
- [x] 2.2 Recusar cargo nos novos pedidos e manter erros, no-store, propriedade/modalidade, sessão, CSRF e correção somente ao encerrar.
- [x] 2.3 Projetar práticas antigas pelo contrato novo e verificar replay/conflitos/expiração/cruzamento de modos com HTTP e PostgreSQL reais isolados.
- [x] 2.4 Aplicar checklist de responsabilidades dos endpoints, registrar proprietários das decisões, regressões e limites.

## 3. Interface por assunto — depende de 1–2 e da jornada #158

- [x] 3.1 Conferir disponibilidade da dependência new-student-experience e refinamentos de aulas nativas/painel; não integrar outra spec implicitamente.
- [x] 3.2 Remover cargo da entrada/contexto/histórico e adotar tipos próprios de prática sobre contrato mínimo compartilhado da fila/executor.
- [x] 3.3 Identificar painel/prática/início pendente por assunto; preservar execução ao mudar cargo público, recolher, navegar e retomar histórico/legado.
- [x] 3.4 Manter início explícito, questões abaixo do botão, correção final, retorno à aula e isolamento por identidade; concluir testes React de falhas/retry/respostas tardias.

## 4. Verificação integrada e entrega local — depende de 1–3

- [x] 4.1 Verificar Chromium/Edge em 390/1440px: link direto, exploração livre, prática sem cargo, histórico antigo e retorno à aula.
- [x] 4.2 Verificar fila pendente, conflito/retry, criação com confirmação perdida, encerramento, logout/expiração e simulado independente em backend/PG reais isolados.
- [x] 4.3 Revalidar build, seleção editorial preservada, contratos e métricas de simulados, OpenSpec e migração com dados anteriores; registrar risco crítico e evidências locais.
- [ ] 4.4 Aceite local do usuário pendente. Entrega preparada e disponibilizada para teste; somente após o aceite seguir revisões SOL/gates e envios de código conforme autorizações próprias.

## Dependências e fluxo

Main f61d57d já contém lesson-practice pelo #154. Grupos 1–2 não dependem do leitor
nativo. Grupo 3 depende da jornada #158/PR #159 e refinamentos, já presentes na main
16ca8aef usada nesta implementação. A dependência não autoriza carregar sua stack ou modificar branches.

Futuras tarefas usarão task/lesson-practice-by-topic/<assunto>, com PRs à épica ou
Stacks apenas para dependências reais da mesma spec, conforme AGENTS. A proposta
documental não é uma camada de implementação; nenhum método de stack será testado
por merges reais. Todas as integrações/aprovações permanecem pendentes.

Aprofundamento por matéria, novas questões, cargas reais, VPS, deploy, arquivamento,
push de implementação e merges continuam fora desta entrega local. O teto fica em
cinco; ampliação para 25 pertence ao grupo 2 da #173, após integração autorizada.

Evidências e conciliações: [entrega local](local-delivery.md). Aceite do usuário,
revisões independentes e integração são estados separados e permanecem pendentes.
