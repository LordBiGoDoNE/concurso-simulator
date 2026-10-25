# Retomada — backlog #156

## Status

Planejamento confirmado e registro/envio autorizados em2026-10-25.
Somente documentos: sem implementação, acesso remoto, alterações na VPS ou carga
de dados. Revisão documental e gate cumulativo ainda não executados. Nenhum grupo
de implementação escolhido; não iniciar lote automaticamente.

Issue: https://github.com/LordBiGoDoNE/concurso-simulator/issues/156
Ela reúne o PR em rascunho, commit e próximos passos. Main de origem:f61d57d.

## Decisões que continuam válidas

- Usuário opera a VPS e conserva acesso exclusivo; assistente não recebe chaves/login.
- Rocky10.2 x86_64,4 vCPUs,15Gi RAM,194G livres; VPS nova, ainda sem domínio.
- Concluir na homologação privada via túnel SSH, usando localhost:18088.
- Somente acervo atual; não migrar contas/histórico/sessões do computador.
- Backup manual com cópia no computador; sem frequência automática prometida.
- Questões congeladas. Ler manifests/fingerprints necessários à carga não reabre
  rodadas de redação ou auditoria semântica do conteúdo já entregue.
- Local127.0.0.1:8088 e stable/pages-store permanecem preservados.
- Domínio/HTTPS público/Google real/backup automático/abertura serão outra spec.

## Como continuar em outra máquina

No clone do repositório, buscar e selecionar a branch de proposta:

```sh
git fetch origin
git switch --track origin/task/vps-staging-delivery/proposal
```

Se a branch já existir localmente, usar git switch com o nome local e conferir
status/commit antes de atualizá-la. Não sobrescrever alterações existentes.
Ler AGENTS.md, proposal.md, design.md, tasks.md e os três deltas desta pasta.
Consultar a issue#156 para o estado mais recente dos PRs/checks.

Próxima decisão do usuário: quais grupos completos1–5 entram na implementação.
Proposta usa task/vps-staging-delivery/proposal → epic/vps-staging-delivery;
integração final futura [SPEC] da épica → main. Dependências reais seguem Stacks
conforme AGENTS, com aprovação própria dos merges. Rascunho não é revisão concluída.

Os comandos da VPS serão fornecidos em passos curtos para o usuário executar.
Docker/Compose, SELinux/firewall, conta operacional e conectividade ainda precisam
ser levantados por ele. IP, usuário SSH e segredos são parâmetros locais do roteiro,
nunca requisitos para dar acesso ao assistente ou conteúdo a publicar no GitHub.
