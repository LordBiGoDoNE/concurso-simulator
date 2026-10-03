# Contexto para continuar o material — Concurso Público Limeira 2026

## Objetivo

Este pacote é a V3 do material de estudo do projeto **[Gaby] — Concurso Público Limeira 2026**, voltado aos cargos de **Assistente Administrativo** e **Secretário de Escola**. O objetivo é manter uma apostila HTML navegável, organizada por disciplina, com teoria, exemplos, exercícios, gabarito separado e recursos visuais que ajudem a estudar.

Esta versão foi concluída em **27/09/2026**. O ZIP é autocontido para estudo e edição em outro computador. Os links para sites de referência exigem internet.

## O que há no pacote

- **51 módulos de estudo**, cada um em dois arquivos: `Disciplina NN - Assunto.html` e `Disciplina NN - Assunto - Gabarito e Revisão.html`.
- **510 questões da versão aprofundada**, mantidas na V3 e identificadas por origem e tipo.
- **51 esquemas visuais SVG** originais em `assets/esquemas/`, um por módulo.
- **51 oficinas visuais adicionais**: proposta no arquivo de estudo e resolução no gabarito. Elas não fazem parte da contagem das 510 questões.
- Referências e atividades de aprofundamento ao fim de cada módulo, além de caixas padronizadas de **atenção, pegadinha, exemplo resolvido, resumo, lei importante, fonte da questão e referência complementar**. A caixa de lei aparece nos temas em que é pertinente.
- Índice com busca por assunto, páginas de plano/cronograma/fontes, `manifesto_v3.json` e `catalogo_fontes_questoes.csv`.

## Como abrir em casa

1. Extraia **todo** o ZIP para uma pasta curta, por exemplo `C:\Estudos\Limeira_V3`. Alguns nomes de módulos são longos; uma pasta curta evita problemas de caminho no Windows.
2. Abra `Limeira_V3/index.html` em um navegador. O HTML e os esquemas funcionam sem servidor e sem internet.
3. Procure um assunto no índice ou navegue pelas disciplinas. Leia o conteúdo, resolva a oficina e as dez questões, depois abra o arquivo de gabarito correspondente.
4. Para editar, mantenha a estrutura de pastas e os nomes de arquivos: os links entre páginas, esquemas e índice são relativos.

## Organização

```text
Limeira_V3/
  index.html
  00_Como_Usar_o_Material.html
  00_Plano_Mestre_e_Matriz.html
  00_Cronograma_Operacional.html
  00_Fontes_Oficiais.html
  00_Novidades_e_Criterios_V3.html
  01_Portugues/                  10 módulos × 2 arquivos
  02_Matematica_Logica/          12 módulos × 2 arquivos
  03_Informatica/                10 módulos × 2 arquivos
  04_Assistente_Administrativo/   8 módulos × 2 arquivos
  05_Secretario_Escola/          11 módulos × 2 arquivos
  assets/esquemas/               51 arquivos SVG
  assets/v3.css
  assets/v3.js
  manifesto_v3.json
  catalogo_fontes_questoes.csv
  CONTEXTO_PARA_CONTINUAR_V3.md
```

O índice lista os dois arquivos de cada módulo. O `manifesto_v3.json` traz os nomes completos e os caminhos correspondentes, o que facilita conferir a estrutura depois de qualquer edição. O CSV lista as 510 questões, sua classificação e as referências temáticas ou legais indicadas.

## Critérios de fontes e autoria

As questões foram herdadas do material aprofundado anterior como **questões autorais do projeto**. Nenhuma delas foi rotulada como reprodução de prova anterior ou atribuída a uma banca específica sem prova dessa origem. A referência ao fim de uma questão serve para **estudar ou conferir o conceito**; não significa que o órgão indicado escreveu ou validou o exercício.

Os SVGs são desenhos didáticos originais. Os esquemas de Word, Excel, Windows e PowerPoint são representações simplificadas, sem reprodução de telas oficiais. As referências externas incluem, conforme o assunto, textos legais compilados no Planalto, orientações do Inep, Conarq, Enap, OBMEP/IMPA, Academia Brasileira de Letras, Microsoft e CERT.br. Consulte o texto oficial vigente quando estudar legislação e as instruções da edição aplicável no caso do Censo Escolar.

## Alterações editoriais pontuais já feitas

- **Português 01:** o exemplo sobre aumento de usuários após vídeos de orientação foi corrigido para não tratar mera sequência temporal como prova de causalidade.
- **Secretário de Escola 01:** a explicação de frequência distingue a regra de 75% para aprovação no ensino fundamental e médio da frequência mínima de 60% na pré-escola, conforme os arts. 24 e 31 da LDB.
- **Secretário de Escola 08:** o resumo do art. 56 do ECA passou a mencionar a hipótese acrescentada em 2025 e a vincular a exigência de esgotar recursos escolares ao inciso II.
- A página `00_Novidades_e_Criterios_V3.html` explica os limites e o uso de cada tipo de caixa.

## Pontos para a próxima revisão

1. Fazer uma **revisão pedagógica individual** dos 51 módulos e 510 questões: conferir clareza, dificuldade, gabarito e explicação de cada alternativa. A checagem desta entrega confirmou a estrutura e a correspondência dos gabaritos rápidos, mas não substitui uma revisão humana de cada enunciado.
2. Conferir o edital, eventuais retificações e a data de corte de legislação antes de usar o material como guia definitivo de cobrança. A matriz e o cronograma foram preservados da versão aprofundada; a V3 acrescentou recursos didáticos e referências, sem nova auditoria integral do edital.
3. Se acrescentar questões reais de provas anteriores, registrar **banca, concurso, ano, cargo, número da questão e link da fonte**; marcar adaptações quando houver mudança no enunciado. Não trocar a atribuição das 510 questões autorais sem documentação.
4. Se acrescentar fotos, capturas ou diagramas de terceiros, verificar licença, legibilidade, utilidade didática e crédito. Manter imagens locais no pacote para leitura sem internet e sempre incluir texto alternativo.
5. Ao alterar a teoria, atualizar também exemplos, exercícios afetados, gabarito, oficina visual e referências. Manter o par de arquivos e o padrão `Disciplina NN - Assunto`.
6. Conferir os links do índice e entre módulos depois de renomear arquivos. Testar a leitura em tela pequena e a impressão pelo navegador.

## Verificação desta entrega

A V3 foi aberta em navegador em telas larga e estreita. A busca do índice, inclusive sem acentos, funcionou. Foram verificados **3.119 links e referências internas**, 51 imagens com descrição alternativa, os 51 pares de módulos e a correspondência dos 510 enunciados com a versão aprofundada. O ZIP foi testado depois de criado. Veja `CONFERENCIA_V3.txt` para o registro da checagem.

## Pedido para continuar o trabalho em outro computador

Se quiser retomar com um assistente, envie o ZIP inteiro e diga, por exemplo:

> Continue a revisão da V3 do material [Gaby] — Concurso Público Limeira 2026. Leia `CONTEXTO_PARA_CONTINUAR_V3.md` e `manifesto_v3.json`. Preserve os dois arquivos por módulo e os nomes com disciplina, número e assunto. Antes de alterar muitos módulos, revise conteúdo e gabaritos para identificar erros; depois melhore os capítulos prioritários, atualize referências oficiais e teste os links internos. Entregue novamente um ZIP navegável.

