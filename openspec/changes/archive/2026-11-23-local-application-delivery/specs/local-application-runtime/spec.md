# Spec Delta

## Purpose

Executar a aplicação empacotada e o material no computador do operador com Docker.

## ADDED Requirements

### Requirement: Artefatos executáveis e contexto restrito
O pacote SHALL executar JAR e SPA compilados, sem Gradle/Vite dev/fixtures de teste
no runtime. MUST fixar versões, registrar commit/IDs das imagens e excluir segredos,
backups e dados locais dos contextos/camadas/bundles. Web/API SHALL executar sem
privilégios de root; dados de teste MUST NOT entrar no startup normal.

#### Scenario: Build com arquivos privados presentes no workspace
- **WHEN** o operador constrói imagens com sentinelas sintéticas em caminhos privados/ignorados
- **THEN** a construção funciona e esses arquivos não aparecem nos contextos permitidos, camadas ou artefatos finais, com alcance da inspeção registrado.

### Requirement: Execução isolada e entrada loopback
Compose SHALL usar projeto/rede/volume próprios, sem adotar o banco existente.
Somente web SHALL publicar porta no host, vinculada a127.0.0.1. API/PG MUST NOT
publicar portas; start/stop MUST NOT remover volumes ou parar serviços alheios.

#### Scenario: Colisão com outra aplicação local
- **WHEN** a porta escolhida já está ocupada
- **THEN** a inicialização falha claramente, preserva serviços/dados existentes e exige escolha explícita de outra porta/configuração coerente.

### Requirement: Uma origem e contratos preservados
SPA/API/OAuth SHALL usar origem local coerente. O proxy MUST preservar método/query,
status e no-store; API/autenticação e caminhos rejeitados MUST NOT virar index.html.
O perfil local permite HTTP loopback, mantendo sessão/CSRF/posse; perfis padrão e
produção MUST conservar Secure/HTTPS. Google SHALL iniciar desligado e sem credenciais.

#### Scenario: Erro privado e tentativa de método não aceito
- **WHEN** o navegador recebe401/403/404/409/503 ou usa método negado em API/OAuth
- **THEN** observa o contrato real do backend, sem sucesso HTML, cache privado ou contorno de CSRF/posse.

### Requirement: Material independente e prontidão real
A cópia pública do material SHALL preservar51 aulas,paths/acentos/assets/offline e
ficar separada do bundle da SPA. Falha da API MUST NOT impedir estudar.
Prontidão SHALL refletir consulta real ao banco e falhar em prazo finito.

#### Scenario: Banco para depois do startup
- **WHEN** PostgreSQL fica indisponível após o ambiente estar pronto
- **THEN** API informa indisponibilidade sem dados privados, a interface não inventa dados e as aulas empacotadas continuam acessíveis.
