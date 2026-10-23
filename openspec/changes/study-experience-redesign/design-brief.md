# Direção visual — estudo de experiência

## Objetivo

Tornar evidente onde estudar, como compor um simulado e como responder uma questão.
As três telas iniciais formam um percurso com uma ação principal em cada etapa.
A direção foi aprovada pelo usuário, que pediu a inclusão das aulas nesta rodada.

## Direção proposta

Referência do OpenDesign: `modern-minimal`, consultada na instalação local 0.21.1.
Interface clara, azul para ações, verde como sinal secundário e superfícies neutras.
Tipografia de sistema, contornos discretos e espaçamento maior entre decisões.
Ícones são SVG próprios no HTML; não há imagens, fontes ou scripts externos.
Contrato visual em `design/prototypes/study-experience/DESIGN.md`.

## As três telas

1. **Início:** apresentar o próximo passo, entrada principal para montar simulado,
   acesso às aulas e disciplinas. Visitante não recebe métricas pessoais inventadas.
2. **Composição:** cargo, distribuição por disciplina, assuntos opcionais recolhidos
   e variações numéricas. Resumo à direita no desktop; total e início fixos no celular.
3. **Realização:** enunciado e alternativas em primeiro plano; anterior/próxima,
   mapa de questões, progresso e confirmação de conclusão. Navegação geral é reduzida.

## Interação demonstrada

- Início → composição → realização, com atalhos de tela na barra de demonstração.
- Quantidades inteiras de 1 a 100 no total; zero permite excluir uma disciplina.
- Cargo, seleção de assuntos e preferência por variações mantidos na memória local.
- Alternativas, limpeza, mapa e confirmação de sair/concluir funcionam no protótipo.
- A conclusão mostra somente um encerramento demonstrativo, sem pontuação inventada.

## Extensão solicitada: aulas

- **Biblioteca:** visão das cinco disciplinas e51 aulas, busca sem distinção de acentos,
  filtro por disciplina, resultados e estado vazio, sem progresso pessoal inventado.
- **Leitura:** títulos e trechos públicos existentes em um leitor com índice da disciplina,
  anterior/próxima e abertura do material completo. A prévia é identificada e não promete
  reproduzir figuras, tabelas, fórmulas ou exercícios omitidos da extração textual.
- **Teoria → prática:** preenche cargo compatível quando específico, um assunto e uma
  questão na composição demonstrativa; não cria tentativa ou resposta no servidor.
- Menu Aulas e ação da página inicial agora abrem a biblioteca do próprio protótipo.
  Links para conteúdo completo usam somente o mapa versionado de páginas de teoria.

`lessons.json` mantém caminho e SHA-256 de cada uma das51 fontes públicas. Os trechos
são derivados para desenho da leitura, não nova aprovação editorial ou reescrita do acervo.

O protótipo não compõe questões reais segundo os filtros. Repete três exemplos para
avaliar interação, não persiste nem chama serviços. A seleção inicial da tela isolada
de realização contém três escolhas fictícias para mostrar estados do mapa.

## Preservações para a futura implementação

- Visitante temporário; conta mantém histórico e retomada; sem transferência automática.
- Google continua opcional; nenhum botão promete login disponível nesta instalação.
- Manter validação de acervo insuficiente, idempotência, conflitos e recuperação.
- Somente o servidor confirma salvamento/conclusão; não exibir sucesso antecipado.
- Sem gabaritos antes de concluir, migrações ou mudanças nas regras de composição.
- Sem metas, séries, cronômetro ou resultados pessoais inventados pelo layout.

## Avaliação visual

Rever as dez capturas reais exportadas pelo Chromium do OpenDesign: cinco estados em
1440px e em 390px. O raster acompanha o DPI do Windows e pode ser maior que a largura
CSS solicitada. Conferir leitura, hierarquia, quebra de linhas, opções de resposta,
controles de toque e comportamento do resumo. Lint visual e captura não equivalem
a auditoria WCAG, teste integrado, revisão independente de lote ou aceite do usuário.

## Decisões aguardando o usuário

- Direção completa aprovada em 2026-10-23, incluindo biblioteca e leitura.
- Após aprovação, quais grupos completos de implementação entram no primeiro lote?
