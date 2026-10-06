# Spec Delta

## Purpose

Garantir acesso ao material de estudo existente durante a evolução para uma aplicação com backend, evitando perda de aulas, URLs e uso offline.

## ADDED Requirements

### Requirement: Compatibilidade do material publicado
O projeto SHALL preservar as URLs e o comportamento das aulas estáticas existentes, incluindo fórmulas, fontes nomeadas, gabaritos separados e as 90 resoluções recolhidas de Matemática.

#### Scenario: Publicação após introduzir a stack
- **WHEN** o material estático é montado e publicado após a introdução da aplicação
- **THEN** seus links internos e âncoras continuam válidos, e as 90 resoluções continuam abrindo e recolhendo.

### Requirement: Leitura independente do backend
O material estático SHALL permanecer legível offline a partir do pacote montado, sem depender da API, do banco ou de JavaScript.

#### Scenario: API indisponível
- **WHEN** o usuário abre uma aula do pacote montado sem internet e sem backend
- **THEN** consegue ler teoria, fórmulas, questões e resoluções; apenas os links externos exigem conexão.

### Requirement: Entrada inicial da nova aplicação
O novo frontend SHALL oferecer uma página inicial com acesso explícito ao material estático e indicação compreensível de disponibilidade da API, sem anunciar funcionalidades ainda não implementadas.

#### Scenario: Backend disponível
- **WHEN** o usuário abre a página inicial e a consulta de prontidão é bem-sucedida
- **THEN** vê a disponibilidade da API e o link para o material de estudo.

#### Scenario: Backend indisponível
- **WHEN** a consulta falha ou informa indisponibilidade
- **THEN** o frontend informa a situação, oferece nova tentativa e mantém o acesso ao material estático.

### Requirement: Publicação estática independente
O fluxo de publicação do material SHALL continuar independente da execução do backend e MUST NOT publicar segredos, código do servidor ou dados privados de tentativas nos artefatos estáticos.

#### Scenario: Artefato de publicação
- **WHEN** o workflow do site monta o artefato do GitHub Pages
- **THEN** inclui apenas conteúdo público destinado ao site, preserva os previews e não exige PostgreSQL nem Java em execução.
