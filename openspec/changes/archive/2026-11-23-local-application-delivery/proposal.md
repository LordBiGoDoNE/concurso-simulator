# Proposal

**Proposta aprovada pelo usuário e integrada pelo #124 em6283c4d, acompanhamento#123.** Em 2026-10-18 o usuário escolheu começar a
etapa 7 pela execução local com Docker; hospedagem e domínio ainda não definidos.
Etapa 6 integrada na épica geral pelo #121 em `3715219a02dfe77262ce0af4bcbb858fbc08fca7`.
Em2026-10-18 o usuário escolheu todos os grupos1–4 para implementação. Publicação e merges de tarefas continuam exigindo autorização própria.

## Why

A aplicação está integrada, mas o Compose atual executa somente PostgreSQL.
Backend e frontend dependem de processos de desenvolvimento separados. Precisamos
validar os artefatos empacotados, o acesso pelo navegador e a recuperação de dados
antes de escolher infraestrutura pública.

## What Changes

- Construir imagens locais do JAR Java 25 e do frontend compilado, com dependências
  fixadas, contexto de build limitado e sem ferramentas/fixtures de teste no runtime.
- Executar web, API e PostgreSQL 18.6 em projeto Compose próprio, separado do banco
  de desenvolvimento existente, com volume novo e somente a entrada web em loopback.
- Usar uma única origem local, inicialmente `http://127.0.0.1:8088`, para SPA,
  API e rotas de autenticação. Servir uma cópia do material em `/material/`, com
  links e pacote offline preservados. Porta diferente exige configuração coerente.
- Manter cookie/CSRF/posse e contratos existentes, HTTP apenas no perfil local,
  Google desligado por padrão e prontidão real dependente do PostgreSQL.
- Documentar e ensaiar backup, restauração em destino vazio e reinício/retorno a
  artefato compatível, sem apagar volume nem reescrever migrações ou capturas.
- Verificar o conjunto por navegador usando imagens reais e dados sintéticos
  isolados; produzir um procedimento local e evidência independente SOL 6.1 xhigh.
- Fora: contratação de serviços, domínio, exposição pública, TLS de produção,
  HA, backups remotos, importação automática da cópia editorial do operador,
  novas aprovações editoriais, painel administrativo, mudanças de produto e deploy.

## Capabilities

### New Capabilities

- `local-application-runtime`: execução empacotada e isolada de web/API/banco/material.
- `local-data-recovery`: persistência, backup/restauração e retorno compatível local.
- `local-delivery-verification`: verificação das imagens e registro dos limites
  para o incremento posterior de hospedagem/publicação.

### Modified Capabilities

Nenhuma consolidada em `openspec/specs/`. Identidade, banco, simulados, variações e
revisão conservam os requisitos aprovados. A orquestração não altera suas regras.

## Impact

Novos artefatos de empacotamento e Compose local, documentação e verificações;
ajustes de configuração pública/roteamento somente quando necessários à execução.
CI verifica e não publica imagens ou ambientes. O Compose de desenvolvimento,
GitHub Pages e os previews armazenados continuam preservados.

A spec parte da épica geral em `3715219`; proposta e tarefas destinam-se a
`epic/local-application-delivery`, e o futuro `[SPEC]` à épica geral. Os quatro grupos completos foram autorizados; sua implementação está em execução. O incremento local não
conclui a etapa 7: hospedagem, acesso público, smoke Google real e plano de publicação
continuam dependentes de decisões/proposta posteriores. Merge final para main e
qualquer publicação continuam sujeitos à aprovação específica.
