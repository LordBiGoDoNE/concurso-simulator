# Tasks

Backlog #156. Decisões de planejamento confirmadas em2026-10-25; registro e envio
autorizados para retomada em outra máquina. Nenhum grupo de implementação escolhido.
Caixas vazias não significam falta de planejamento: toda implementação está futura.

## 1. Preparação guiada do servidor

- [ ] 1.1 Consolidar inventário, pré-requisitos e verificações de acesso administrativo, sem acesso do assistente à VPS.
- [ ] 1.2 Preparar procedimento Rocky10.2 para Docker Engine/Compose, versões verificáveis e conta operacional.
- [ ] 1.3 Documentar SSH, firewall/IPv6, SELinux e permissões/mounts preservando o acesso existente.
- [ ] 1.4 Validar roteiro, riscos, recuperação de acesso e evidências executadas pelo usuário; incluir testes/docs/revisão do grupo.

## 2. Runtime e releases — depende de1

- [ ] 2.1 Preparar build/exportação por commit e manifesto de imagens linux/amd64 sem segredos.
- [ ] 2.2 Implementar Compose/estado/segredos próprios e comandos operacionais com verificação de destino/ownership/colisão.
- [ ] 2.3 Configurar localhost:18088, túnel manual, proxy, sessão/CSRF e coexistência com a aplicação local; sem portas API/PG públicas.
- [ ] 2.4 Concluir regressões de imagens/runtime/recusa e roteiro de transferência manual; incluir docs/revisão do grupo.

## 3. Acervo inicial — depende de2

- [ ] 3.1 Preparar pacote congelado com251 simulados/255 práticas/3 famílias/111 variantes e respectivas seleções/digests.
- [ ] 3.2 Implementar/verificar carga por operações editoriais existentes, destino explícito e ausência de seed/aprovação no startup.
- [ ] 3.3 Conferir idempotência, linhagem/finalidade e ausência de contas/sessões/tentativas/capturas/comandos do computador.
- [ ] 3.4 Validar disponibilidade por assunto/cargo e fluxos dos dois acervos; incluir testes/docs/revisão do grupo.

## 4. Operação e recuperação — depende de2; ensaio final usa3

- [ ] 4.1 Preparar backup manual consistente e manifesto, pausa de escritores e cópia privada no computador.
- [ ] 4.2 Preparar restauração em destino novo/vazio, conferência de conteúdo e invalidação de sessões restauradas.
- [ ] 4.3 Documentar atualização, exclusão mútua, falha e rollback por imagem/schema compatíveis, preservando origem e volumes.
- [ ] 4.4 Ensaiar backup/transferência/restauração/rollback com falhas e artefatos inválidos; incluir testes/docs/revisão do grupo.

## 5. Validação integrada e gate — depende de1–4

- [ ] 5.1 Consolidar regressões de aulas/simulado/prática, visitante, correção final, reinício e indisponibilidade do banco.
- [ ] 5.2 Guiar validação real do usuário no Rocky: túnel, portas IPv4/IPv6, SELinux/firewall, reboot, acervo e recuperação.
- [ ] 5.3 Consolidar evidências sanitizadas/limites, revisão de lote e gate cumulativo independente SOL6.1xHigh com triagem/cota quando observável.
- [ ] 5.4 Conferir SHAs/checks/relatório e autorizações próprias de merge; atualizar issue com verificado versus integrado e concluir handoff operacional.

Cada grupo inclui suas subtarefas inteiras, testes e documentação. O usuário
escolhe o lote antes de implementar; dependências reais seguem o fluxo do AGENTS.
Domínio/HTTPS público/Google real/backup externo automático/abertura pública são
de outra spec e não bloqueiam a conclusão desta homologação privada.
