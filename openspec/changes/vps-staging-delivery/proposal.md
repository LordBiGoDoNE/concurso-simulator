# Proposal

## Why

A aplicação completa está na main em f61d57d, com execução local e prática de aula
entregues. O usuário dispõe de uma VPS Hostinger e quer preparar a publicação,
começando por homologação privada. Em 2026-10-25 confirmou este escopo e pediu
registrá-lo no backlog para continuar em outra máquina. Acompanhamento: #156.

O usuário executará todos os comandos na VPS. Chaves, login e credenciais ficam
exclusivamente sob seu controle; o assistente prepara arquivos e orientações.

## What Changes

- Especificar implantação guiada em Rocky Linux 10.2 x86_64, 4 vCPUs,
  aproximadamente16 GB de RAM e194 GB livres, conforme inventário do usuário.
- Preparar instalação/verificação de Docker Engine/Compose, preservando SELinux,
  firewall e acesso SSH. Validar compatibilidade no Rocky real antes de alegá-la.
- Criar runtime de homologação separado do local, com projeto/volumes/segredos
  próprios e imagens identificadas, construídas fora da VPS.
- Acessar http://localhost:18088 por túnel SSH operado pelo usuário, com porta
  remota exclusivamente em loopback. API/PostgreSQL não publicam portas.
- Levar somente o acervo atual:251 bases de simulado,255 práticas e conteúdo
  parametrizado aprovado, sem contas, tentativas ou sessões locais. Preservar
  perguntas/fingerprints; não iniciar nova redação ou auditoria semântica do acervo.
- Definir importação editorial deliberada, sem seed ou aprovação no startup;
  manifesto exato e destino identificados antes de qualquer escrita.
- Entregar atualização, diagnóstico, backup manual com cópia no computador,
  restauração em destino separado e rollback compatível com o schema.
- Concluir com homologação privada funcional e recuperação demonstrada pelo
  usuário; conservar execução local, aulas e publicação estática existentes.

## Capabilities

### New Capabilities

- `vps-staging-runtime`: runtime privado, releases identificadas e operação guiada.
- `vps-staging-data`: inicialização exclusiva do acervo e decisões rastreáveis.
- `vps-staging-recovery`: backup manual, transferência, restauração e rollback.

### Modified Capabilities

Nenhuma alteração normativa das capabilities existentes é proposta. Contratos de
negócio e limites arquiteturais permanecem os atuais; configurações e contratos
operacionais próprios serão detalhados na implementação autorizada.

## Impact

Futuros artefatos em infra/vps-staging, ferramentas de build/operação e roteiro
para Windows/PowerShell e Rocky Linux. Testes de configuração, isolamento, pacote,
recuperação e navegador complementam as regressões existentes. Sem nova migração
de negócio prevista; Flyway continua único dono do schema.

Implementação futura classificada como crítica por rede/segredos, elegibilidade,
persistência e restauração. Revisões SOL6.1xHigh orientarão essas fronteiras; não
foram executadas nesta preparação de backlog. Gates e merges mantêm suas regras.

Domínio, HTTPS público, Google real, backup externo automático e abertura pública
terão outra spec. Não instalar agente/runner na VPS nem dar acesso remoto ao
assistente. CI apenas verifica. Sem promessa de HA, SLA ou frequência automática
de backup. Proposta registrada não inicia nenhum dos grupos1–5 de implementação.
