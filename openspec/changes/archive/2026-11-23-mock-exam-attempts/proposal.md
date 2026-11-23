# Proposal

**Aprovada pelo usuário em 2026-10-14, issue #64.** Question-bank integrado na épica geral pelo #63 (`d65006e`); primeiro lote autorizado: grupos completos 1 e 2.

## Why

O banco versionado permite consultar questões, mas ainda não compõe simulados nem
registra respostas e correção de uma tentativa reproduzível. Esta etapa transforma
o acervo elegível em prática: visitante sem histórico permanente e usuário conectado
com tentativas salvas e retomáveis pela conta, conforme decisão de 2026-10-14.

## What Changes

- Compor simulados de Limeira 01/2026 para um dos dois cargos atuais, com quantidade
  escolhida por disciplina e filtro opcional de assuntos, entre 1 e 100 questões no
  total. Não presumir pesos/distribuição oficiais do edital.
- Sortear identidades distintas e versões elegíveis no backend; preservar seleção,
  conteúdo exato, fingerprint, parâmetros, versão da política e ordem das questões
  e alternativas por tentativa. Nesta etapa não há questões parametrizadas.
- Permitir responder, alterar/limpar respostas, navegar e concluir explicitamente;
  não revelar gabarito, resolução ou indicadores corretivos durante a realização.
- Corrigir no backend após conclusão, com um ponto por acerto, zero por erro/em branco,
  total/percentual e revisão das respostas/resoluções/fontes das versões utilizadas.
- Para visitante, manter somente tentativa temporária vinculada à sessão, com expiração
  e limpeza; para conectado, salvar respostas confirmadas, retomar e consultar histórico
  privado na conta, inclusive em outro navegador após autenticação.
- Proteger posse, CSRF, idempotência, concorrência e transições; indisponibilidade
  não pode produzir falsa confirmação de salvamento ou correção calculada no cliente.
- Integrar interface acessível sem alterar aulas, consulta pública ou login opcional.
- Fora: parametrização/IA ao vivo, revisão por conceito, rankings, pesos oficiais,
  cronômetro eliminatório, mescla de visitante com conta, sincronização offline,
  novos cargos/concursos, administração web e deployment.

## Capabilities

### New Capabilities

- `mock-exams`: composição aleatória, realização e correção de simulados com versões
  e ordens preservadas, sem gabarito antecipado.
- `attempt-history`: posse, duração temporária de visitante e salvamento, retomada e
  histórico privado de tentativas da conta.

### Modified Capabilities

Nenhuma consolidada em `openspec/specs/`. Os contratos existentes de identidade e
consulta do banco permanecem; respostas/resoluções só aparecem no resultado da
tentativa concluída, nunca nos endpoints públicos de questões.

## Impact

Novo módulo funcional `mockexam` no backend, núcleo Java puro, portas internas de
leitura do banco e persistência JPA/Flyway aditiva após V4; contratos OpenAPI próprios
e interface React. PostgreSQL isolado, ArchUnit e testes HTTP/browser com fixtures.
Pré-requisito cumprido pelo gate e integração do #63; ainda sem código de produto.
`epic/mock-exam-attempts` criada da épica geral atualizada em `d65006e`; planejamento
e tarefas por PRs próprios e `[SPEC]` final para a épica geral.
