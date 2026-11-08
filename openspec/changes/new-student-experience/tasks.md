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
- [x] 3.3 Preservar exploração livre e material integral; verificar links e atalhos nas regressões (leitor inicial substituído pelo grupo 7).
- [x] 3.4 Verificar teclado, foco e ausência de corte lateral no fluxo390/1440px e documentar limites.

## 4. Disponibilidade e validação integrada

- [x] 4.1 Consultar disponibilidade tipada; verificar contrato, zero, falha e retry nos testes unitários.
- [x] 4.2 Proteger início explícito e ignorar respostas obsoletas; conferir contagem de POST e troca de cargo nos testes.
- [x] 4.3 Concluir build, unitários e regressões Chromium/Edge.
- [x] 4.4 Registrar evidências e limites, sem dados privados ou publicação.

## 5. Prática integrada à aula — solicitado em 2026-11-08

- [x] 5.1 Colocar o painel recolhível após a aula, com botão persistente e carregamento somente por clique, na mesma rota.
- [x] 5.2 Preservar executor, filas, retry idempotente e isolamento por identidade/assunto ao recolher ou navegar.
- [x] 5.3 Ajustar a altura do material de mesma origem e verificar desktop/celular, teclado e fallback de outra origem (implementação histórica substituída pelo grupo 7).
- [x] 5.4 Validar build, unitários, navegadores e fluxo com acervo local; registrar evidências para teste do usuário.

## 6. Índice acima da aula — solicitado em 2026-11-08

- [x] 6.1 Reposicionar o índice antes do título da aula, identificar Nesta Disciplina - nome da disciplina e ampliar o leitor para a largura disponível.
- [x] 6.2 Conferir navegação, teclado, histórico e ausência de corte em celular/tablet/desktop; registrar evidências locais.

## 7. Aulas nativas — solicitado em 2026-11-08

- [x] 7.1 Converter as 51 aulas para dados nativos reproduzíveis, preservando teoria, exemplos, esquemas, fórmulas e fontes; exercícios somente na prática.
- [x] 7.2 Substituir iframe/cabeçalho/navegação legados pelo leitor React, com sumário local, retry e invalidação de respostas obsoletas.
- [x] 7.3 Direcionar atalhos à aula nativa e conferir responsividade, teclado, fontes, tabelas e fórmulas nas cinco disciplinas.
- [x] 7.4 Validar build, seleção/hashes, unitários, navegadores e regressões integradas; registrar evidências para teste local do usuário.

## Workflow follow-up

- Usuário testar e validar localmente antes de revisão por modelo. Envio para
  retomada no notebook autorizado explicitamente em 2026-11-08, sem declarar aceite local.
- Executar revisão independente de lote SOL6.1xHigh e corrigir/revalidar achados.
- Commits/envio autorizados em 2026-11-08; issue #158 registra spec e PR de rascunho.
- Revisar integração cumulativa e obter autorização própria de merge; não arquivar automaticamente.

Importação, Google, TLS do concurso e deploy não pertencem ao lote.
