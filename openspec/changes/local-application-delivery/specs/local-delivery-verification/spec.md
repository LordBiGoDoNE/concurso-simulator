# Spec Delta

## Purpose

Verificar as imagens locais completas e explicitar o que falta para publicação.

## ADDED Requirements

### Requirement: Navegador usa artefatos empacotados
A verificação SHALL exercitar servidor web compilado/proxy/JAR/PG reais, sem Vite
no runtime. SHALL cobrir visitante e conta com sessão sintética de harness separado
em banco descartável, sessão/CSRF, outra sessão, retry, conclusão/revisão/prática e falhas.

#### Scenario: Conta retoma e visitante permanece temporário
- **WHEN** a suíte navega pelas imagens construídas, conclui e retoma em outra sessão
- **THEN** preserva as regras aprovadas de posse, idempotência, histórico de conta e ausência de transferência do visitante.

### Requirement: Autenticação de teste separada
A suíte OIDC existente SHALL rodar separadamente. Provedor/fixtures MUST NOT entrar
nas imagens executáveis ou endpoints normais, nem alterar o issuer Google fixo.
Sessão sintética MUST NOT ser declarada como verificação do login Google.

#### Scenario: Imagem normal e ensaio de conta
- **WHEN** verifica sessões sintéticas e executa o OIDC de teste separado
- **THEN** a imagem normal conserva configuração Google original e não contém provider, fixture ou bypass de autenticação.

### Requirement: Regressão e operação reproduzíveis
A entrega SHALL registrar comandos/commit/imagens/checks e resultado de build,
start/stop, falhas, backup/restore e reinício em ambiente isolado/exportação limpa.
SHALL verificar51 aulas/111 variantes versionadas e privacidade dos artefatos.
CI MUST apenas verificar, sem publicação ou relaxamento de Pages/gates/rulesets.

#### Scenario: Execução sem arquivos privados do operador
- **WHEN** segue o procedimento em exportação limpa com segredos sintéticos externos
- **THEN** reproduz o ambiente e os ensaios, preservando o banco anterior e os previews/material existentes, com limitações da plataforma explícitas.

### Requirement: Revisão independente e aprovações distintas
Cada grupo SHALL incluir testes/docs/revisão SOL6.1xHigh. A spec SHALL passar por
gate cumulativo independente em contexto próprio e evidência atual/triagem antes
do pedido de merge na épica geral. MUST manter separados proposta, escolha de grupos,
aprovação de merges e futura entrega pública.

#### Scenario: Local passou mas hospedagem segue indefinida
- **WHEN** todos os ensaios locais e o gate desta spec passam
- **THEN** pode pedir aprovação própria da integração local, sem declarar etapa7 pública concluída, abrir/integrar main ou publicar com base nessa aprovação.

### Requirement: Pendências públicas explícitas
O relatório SHALL conservar pendentes hospedagem/domínio/TLS, orçamento,
disponibilidade/backups externos, conteúdo real autorizado, smoke Google real e
plano de publicação/rollback. Validação sintética MUST NOT ser declarada como login
Google real, teste de produção, garantia de disponibilidade ou autorização editorial.

#### Scenario: Resultado do OIDC de teste
- **WHEN** o teste de conta com provedor local é aprovado
- **THEN** registra a fronteira exercitada e mantém smoke Google real como pendência antes da publicação pública.
