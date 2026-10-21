# Entrega atual somente local

## Decisão aprovada

Em 2026-10-20 o usuário decidiu: “Vamos deixar a publicacao externa para o futuro
por hora sera somente local”. Em seguida aprovou o registro dessa decisão no
roadmap, com gate cumulativo final e aprovação de integração em `main` preservados.

## Motivação e mudança

A execução local com Docker já foi integrada pelo #130 na épica geral em
`da1c0a538c992fd9f638fb132e5cb46dd8e6740d`. O roadmap ainda condicionava a conclusão
da etapa 7 à preparação pública. Esta mudança documental alinha o escopo da entrega
à decisão explícita: concluir a etapa 7 pela execução local já entregue e adiar
hospedagem, domínio/TLS, operação pública, backups externos, smoke Google real e
publicação/rollback para uma proposta futura.

## Limites

- Não altera código, configuração, autenticação, dados, migrações ou workflows.
- Mantém loopback, Google desligado por padrão, proteção de dados e recuperação local.
- Mantém os requisitos públicos para eventual retomada, sem autorizar exposição.
- Preserva as propostas e evidências anteriores como snapshots históricos.
- Não muda o Pages do material estático nem seus previews ou regras de deploy.
- Exige revisão independente deste diff documental e, separadamente, gate cumulativo
  da épica geral. Nenhuma aprovação anterior é reaproveitada como gate final.
- Não autoriza merge, publicação ou arquivamento; integração continua sujeita à
  aprovação explícita dos PRs e commits revisados.

## Acompanhamento

Épica desta decisão: `epic/local-only-delivery-scope`, criada da épica geral no
commit acima. Objeto documental único: atualizar `docs/application-reformulation.md`
e registrar os requisitos desta decisão. Não há lote de implementação de produto.
Issue geral #15 e acompanhamento do incremento local #123 registram o novo escopo.
