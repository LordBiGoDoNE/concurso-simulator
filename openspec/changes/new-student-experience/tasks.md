# Tasks

Acompanhamento: [issue #158](https://github.com/LordBiGoDoNE/concurso-simulator/issues/158).

Lote autorizado: quatro grupos completos do plano de2026-11-05.
Checkboxes registram entregas locais verificadas; não significam integração.

## 1. Proposta e protótipo

- [x] 1.1 Registrar proposta, decisões e requisitos; conferir com OpenSpec validate strict.
- [x] 1.2 Preparar protótipo navegável; conferir presença de HTML e instruções de abertura.
- [x] 1.3 Conferir capturas desktop/celular e disponibilizar preview observável para validação local.

## 2. Navegação pública

- [x] 2.1 Centralizar rotas hash; verificar round-trip nos testes de navegação.
- [x] 2.2 Validar parâmetros sem comandos automáticos; verificar casos inválidos nos testes e documentar rotas no design.
- [x] 2.3 Verificar atualização, Voltar/Avançar, foco e filas nos testes Chromium e Edge.

## 3. Entrada e biblioteca

- [x] 3.1 Orientar escolha dos cargos e aviso independente; verificar entrada sem API nos testes.
- [x] 3.2 Usar catálogo local e filtros por cargo; verificar contagens40/43 e aula restaurada nos testes.
- [x] 3.3 Preservar exploração livre e material integral; verificar iframe, links e atalhos nas regressões.
- [x] 3.4 Verificar teclado, foco e ausência de corte lateral no fluxo390/1440px e documentar limites.

## 4. Disponibilidade e validação integrada

- [x] 4.1 Consultar disponibilidade tipada; verificar contrato, zero, falha e retry nos testes unitários.
- [x] 4.2 Proteger início explícito e ignorar respostas obsoletas; conferir contagem de POST e troca de cargo nos testes.
- [x] 4.3 Concluir build, unitários e regressões Chromium/Edge.
- [x] 4.4 Registrar evidências e limites, sem dados privados ou publicação.

## Workflow follow-up

- Usuário testar e validar localmente antes de revisão por modelo. Envio para
  retomada no notebook autorizado explicitamente em 2026-11-08, sem declarar aceite local.
- Executar revisão independente de lote SOL6.1xHigh e corrigir/revalidar achados.
- Commits/envio autorizados em 2026-11-08; issue #158 registra spec e PR de rascunho.
- Revisar integração cumulativa e obter autorização própria de merge; não arquivar automaticamente.

Importação, Google, TLS do concurso e deploy não pertencem ao lote.
