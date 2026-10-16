# Proposal

**Proposta em revisão, ainda não aprovada, issue #104.** Etapa 6 do roadmap. O usuário escolheu
em 2026-10-16 começar pelos 51 assuntos existentes, sem nova taxonomia de conceitos.
Etapa 5 integrada na épica geral pelo #103, commit `119ec25`.

## Why

O histórico conserva tentativas e resultados, mas ainda não reúne evidências por
assunto nem orienta o próximo estudo. Esta etapa liga desempenho observado,
revisão das aulas e nova prática, preservando a privacidade e as capturas históricas.

## What Changes

- Mostrar à conta um painel por cargo e assunto, usando as 100 tentativas concluídas
  mais recentes daquele cargo, com janela e eventual corte visíveis. Tentativas em
  andamento, visitantes e outras contas não entram no painel.
- Contar a observação mais recente de cada identidade-base nessa janela. Repetir
  uma questão, revisão ou variante numérica não cria evidência independente extra.
  Usar classificação e correção capturadas, sem consultar a elegibilidade atual.
- Exibir acertos, erros, em branco, questões-base distintas, exposições e percentual;
  sem dados resulta em percentual ausente, não zero. A prioridade de revisão exige
  ao menos três bases distintas e algum erro/branco; é uma heurística transparente.
- Oferecer também a decomposição por assunto de uma tentativa concluída própria.
  Visitantes usam somente o resultado temporário existente, sem novo histórico.
- Abrir as aulas do assunto por links controlados e preencher o compositor com
  cargo/assunto, uma questão e variações desligadas. O usuário confirma a criação
  e pode alterar critérios; insuficiência e repetições permanecem explícitas.
- Integrar interface acessível, contratos tipados e testes de posse, consistência,
  compatibilidade V5/formato 2, privacidade e fluxos reais.
- Fora: conceitos menores, nova curadoria de questões, diagnóstico de domínio,
  agenda espaçada/notificações, IA, rankings, pesos oficiais, mescla de visitante,
  sincronização offline, novos cargos, administração e deployment.

## Capabilities

### New Capabilities

- `subject-performance`: métricas e prioridade por assunto a partir de tentativas
  concluídas próprias, com amostra e classificação congeladas.
- `guided-practice`: acesso às aulas e preenchimento explícito da composição por
  assunto, reutilizando a realização e correção existentes.

### Modified Capabilities

Nenhuma consolidada em `openspec/specs/`. Simulados, histórico, identidade, banco e
parametrização mantêm seus requisitos; esta proposta acrescenta consultas e entrada
orientada no compositor, sem mudar seus contratos de comandos ou fontes históricas.

## Impact

Módulo funcional de revisão com núcleo Java puro e portas de evidência concluída;
leitura/validação dos snapshots permanece sob responsabilidade de `mockexam`.
JPA/SQL apenas na infraestrutura, sem tabela de progresso derivado ou backfill de
resultados. Eventual índice exige plano de consulta e Flyway aditivo após V9.
Novos contratos OpenAPI e interface React, mapeamento controlado de 51 aulas a partir
de `manifesto_v3.json`, sem alterar HTML/material. A épica da spec parte de
`119ec25`; tarefas vão para ela e o futuro `[SPEC]` para a épica geral.
Nenhum grupo de implementação está autorizado por esta proposta em revisão.
