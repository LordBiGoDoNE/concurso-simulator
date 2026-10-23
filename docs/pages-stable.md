# Site estático em stable, aplicação completa em main

Decisão do usuário em 2026-10-23: preservar a main anterior em `stable`, mover o
GitHub Pages para essa branch e então integrar a reformulação em `main`. A stack
completa não usa Pages nesta entrega; sua execução continua local com Docker.

## Publicação

- `stable` parte da main anterior, commit `f5f30d64e0c968c4530504929c6d0d413f13eefc`.
  Alterações de infraestrutura preservam material/aulas e histórico desse commit.
- Pages mantém a URL existente e o build por Actions. O workflow publica em push
  de `stable`; dispatch só executa publicação quando a referência do workflow é
  `stable`. Push ou dispatch do workflow em `main` não publica.
- O ambiente `github-pages` deve autorizar somente a branch `stable`. As antigas
  permissões por referência `main`, `infra/*` e `melhoria/*` são substituídas por
  essa referência, sem bypass. Preview é solicitado a partir do workflow de stable.
- Em Run workflow, selecionar `stable`. O campo `branch` usa `stable` para produção
  e outra branch de material para preview. `main` e `pages-store` são recusadas.
  O material de produção vem sempre de stable; previews armazenados são preservados.
- O workflow permanece também na branch padrão para ser descoberto pelo Actions,
  mas seu job de publicação exige `refs/heads/stable` e não roda em main.

`build_pages.py --production-branch stable` expressa a origem de produção do Pages.
O default histórico `main` continua aceito por chamadas locais/CI que montam pacotes
estáticos sem publicar, inclusive o pacote de aulas da aplicação Docker.

## Sequência e verificação

1. Preservar main em stable sem sobrescrever branch existente; guardar configurações
   atuais antes de atualizar origem/políticas do Pages.
2. Revisar independentemente o ajuste de infraestrutura com SOL 6.1 xHigh e validar
   montagem, preservação dos previews, recusa de main e links do material.
3. Integrar a infraestrutura em stable e na main ainda estática por PRs próprios.
   Configurar Pages/ambiente para stable e confirmar publicação dessa referência,
   mantendo a URL, os previews e o material. Confirmar que push em main não publica.
4. Sincronizar a épica geral com a infraestrutura de main sem trazer a stack para
   stable. Atualizar/revalidar o gate cumulativo do #133 contra a nova base/head.
5. Com CI/gate atuais, integrar #133 em main conforme a ordem autorizada pelo usuário;
   conferir CI pós-merge e ausência de publicação Pages a partir da nova main.

Sem deleção de branches, força de push, arquivamento ou contratação de hospedagem.
Correções futuras do site estático usam stable; evolução da aplicação usa main.
