# Design

## Context

Base: main f61d57d5d55d6d61609815a2faa4b415ed8dca4b. Stack Java25/Spring Boot,
PostgreSQL18/FlywayV11, React e material estático; imagens locais fixadas por digest.
Os scripts locais deliberadamente restringem projeto, engine, origem e backups.
Web atual aceita127.0.0.1 e embute a origem absoluta do material. O cookie SESSION
tem caminho /, HttpOnly/SameSite=Lax e Secure fora do perfil local.

VPS nova Hostinger, Rocky Linux10.2 x86_64,4 vCPUs,15Gi RAM e194G livres. Dados
informados pelo usuário; Docker, firewall e SELinux ainda não foram inspecionados
no servidor. Ausência de swap não autoriza reparticionar ou reconfigurar o host.

## Goals / Non-Goals

Entregar homologação privada reproduzível, operada pelo usuário, com acervo e
recuperação conferidos. Não abrir o site publicamente nem automatizar acesso do
assistente à VPS. Nenhuma mudança de conteúdo editorial ou arquitetura de negócio.

## Decisions

### 1. Operação assistida e acesso exclusivo do usuário

O usuário conecta, instala, transfere e executa na VPS; o assistente fornece
comandos em passos curtos, efeito esperado e verificação antes do passo seguinte.
Não ler/solicitar chaves privadas, login, senha ou tokens; usar placeholders no
roteiro. Resultados compartilhados são mínimos e sem segredos/dumps/logs brutos.
Não configurar SSH remoto, runner ou agente para executar pelo usuário.

Levantar Docker/Compose, SELinux, regras/portas e acesso administrativo antes de
alterar o host. Preservar a conexão SSH existente e testar uma segunda sessão
administrativa antes de restringir acessos. Usar conta operacional com sudo;
não tratar acesso ao grupo docker como isolamento de privilégios.
Manter SELinux/firewall ativos; corrigir rótulos/permissões dos mounts necessários,
sem desabilitar proteções ou limpar regras Docker/serviços alheios.

### 2. Runtime e releases próprios

Propor infra/vps-staging e projeto concurso-simulator-staging, com estado sob
/opt/concurso-simulator/staging, diretórios privados, volume e ownership próprios.
Não reaproveitar volume/estado/segredos do operador local. Reutilizar componentes
de build quando útil, preservando os guardas dos comandos exclusivamente locais.

Construir a partir de commit identificado fora da VPS; transferir arquivos de
imagem e manifesto por SSH/SCP executados pelo usuário. Manifesto registra commit,
plataforma linux/amd64, IDs das imagens próprias, digests das bases/PG e versão de
schema suportada. Verificar origem/identificadores e configuração antes de iniciar;
checksum detecta corrupção, não autentica artefato desconhecido.

Comandos operacionais previstos: verificar, carregar imagens, iniciar, status,
parar, importar acervo, backup e restaurar, sempre explicitando destino/release.
Nenhum comando faz login/transferência remota automaticamente. Rotina não remove
volumes; colisão de porta ou ownership falha sem adotar recursos existentes.

### 3. Origem privada, proxy e sessão

Origem do navegador: http://localhost:18088. Usuário abre túnel com bind local
em loopback para127.0.0.1:18088 da VPS; a web remota publica somente nesse loopback.
API e PG ficam nas redes internas necessárias, sem portas no host. Conferir também
IPv6 e negar acesso externo à porta da web; não abrir80/443 nesta homologação.

Web/SPA/material usam essa origem e allowlist de Host explícita, API relativa e
proxy sem confiança em Forwarded/X-Forwarded fornecidos pelo cliente. Sessão/CSRF
mantêm fronteiras atuais; usar perfil local somente dentro deste transporte
privado, sem ampliar o modo HTTP para origens públicas. Google desligado.
localhost isola o cookie da execução local existente em127.0.0.1:8088: portas
diferentes, sozinhas, não isolariam cookies. Validar coexistência dos dois ambientes.

Segredos por arquivos privados montados apenas no serviço consumidor; nenhum em
ARG, imagens, manifesto público ou Git. Verificar leitura pelos usuários dos
containers e rótulos SELinux no Rocky, sem executar API/web como root para contornar.
Logs não registram cookies, credenciais ou query de callback. Material permanece
acessível quando a API/PG falhar; erros API não viram HTML de sucesso.

### 4. Acervo sem dados pessoais

Gerar pacote a partir das fontes versionadas e manifestos atuais:251 bases de
simulado,255 práticas,3 famílias parametrizadas/111 variantes e51 aulas. Digest
vigente da prática:1c7ff1ad0a916d5edbfb9a58f1a6302484667ca86cc0748a8ec66cbce7125ae1.
Congelar conteúdo; conferir contagens, fingerprints, finalidade e dependências,
sem reabrir revisão de originalidade/redação nesta entrega.

Banco inicial novo: Flyway/validação antes da carga. Não copiar dump completo do
operador nem contas, external identities, sessões, tentativas, comandos ou capturas.
Usar importação/UseCases existentes e operação editorial explícita no destino,
sem relaxar restrição loopback das CLIs ou publicar5432 para carregamento.
Apresentar manifesto/digests e projeto de destino antes de aplicar decisões;
aprovação local anterior não equivale a escrita remota já autorizada/executada.
Reexecução preserva IDs/linhagem sem duplicações; divergência falha, não sobrescreve.

### 5. Backup, atualização e recuperação

Backup manual antes de atualizar/importar e após validar a carga inicial; manter
cópia completa no computador do usuário por transferência SSH/SCP deliberada.
Identificar pausa necessária de escritores, inclusive API, no roteiro. Reutilizar
semântica do backup consistente atual; não apresentar hashes de banco em movimento
como prova de igualdade. Falha gera artefato incompleto que não pode ser restaurado.

Manifesto registra release/imagem, schema, PG, checksum e verificações de conteúdo.
Backups e configuração/segredos são preservados separadamente em locais privados.
Restauração usa backup confiável e destino novo/vazio, mantém origem e invalida
sessões restauradas antes de liberar a API. Demonstrar conteúdo e retomada após
novo acesso; backup no próprio host, sozinho, não cobre perda da VPS.

Atualizações e restauração não se sobrepõem. Rollback exige imagem/schema
compatíveis ou restauração separada; não desfaz Flyway nem rebaixa binário sobre
capturas incompatíveis. Sem retenção/exclusão automática, RPO ou RTO prometidos;
o usuário controla a frequência e preservação das cópias nesta homologação.

## Verification and Rollout

Testar configuração renderizada, imagens/exportação sem segredos, pacotes e casos
de recusa. Ensaiar Docker/navegador/PG e recuperação isolada antes das instruções
ao usuário. Usuário executa o roteiro no Rocky e registra resultados sanitizados,
inclusive reboot, portas externas fechadas, persistência e restauração.
CI de outra distribuição não certifica SELinux/firewall do Rocky.
Consolidar resultados antes de revisão de lote SOL6.1xHigh e gate cumulativo
independente. Nenhuma revisão foi disparada para este registro de backlog.

## References

- ADR0001, checklist HTTP, operação local e regras editoriais do repositório.
- [Instalação RPM Docker/RHEL](https://docs.docker.com/engine/install/rhel/):
  referência a validar em Rocky10.2, não certificação automática dessa distribuição.
- [Compose em servidor único](https://docs.docker.com/compose/how-tos/production/).
- [Rede/firewall Docker](https://docs.docker.com/engine/network/packet-filtering-firewalls/).
- [Backup lógico PostgreSQL18](https://www.postgresql.org/docs/18/backup-dump.html).
