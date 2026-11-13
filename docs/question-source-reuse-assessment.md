# Avaliação de reutilização de questões oficiais — #173

Data: 2026-11-10. Responsável: Codex, pesquisa/avaliação assistida por IA; sem
parecer jurídico independente ou autorização da banca presumidos.

## Escopo esclarecido pelo usuário

Software genérico separado do acervo; JSONs externos são importados deliberadamente.
Eventual comercialização se refere à plataforma e suas funcionalidades. Acesso
público sozinho não foi tratado como licença, nem sua ausência como proibição.
O usuário pediu avaliar esse enquadramento e continuar os grupos autorizados.

## Fontes e conclusão operacional

1. [Lei 9.610/1998, art. 8º, IV](https://www2.camara.leg.br/legin/fed/lei/1998/lei-9610-19-fevereiro-1998-365399-normaatualizada-pl.html)
   exclui atos oficiais da proteção. Arts. 7º/29/46/31 orientam separar elementos
   de terceiros, cópia/armazenamento, hipóteses legais de uso e modalidades.
2. [TJPR, 5ª Câmara Cível, Apelação 1437332-3](https://portal.tjpr.jus.br/e-dj/publico/carregarDocumento.do?tjpr.url.crypto=8a6c53f8698c7ff760a943e5f78e4955549b94d0e63466658a221c700814ffe23f5e0a9d3d8bbc03e9dd0b0b975d50f7),
   relator Leonel Cunha, julgamento 22/03/2016, processo de origem
   0084775-31.2013.8.16.0014: a publicação oficial aplica esse dispositivo à cópia
   de questões entre concursos. Trata-se de ação de improbidade; não é autorização
   da Avança SP, tese vinculante geral ou julgamento de uso em plataforma paga.
3. [Requerimento da Câmara de Americana de 2010](https://siscam.camara-americana.sp.gov.br/arquivo?Id=23360)
   defende publicidade/acesso às provas com o mesmo fundamento. É manifestação
   parlamentar sobre acesso por candidatos, não decisão judicial/licença.
4. A pesquisa também encontrou editais de outras bancas que alegam direitos sobre
   provas. Isso impede generalizar uma política de todas as bancas a partir de um
   documento; não demonstra restrição específica da Avança SP.

**Avaliação adotada para continuar localmente:** admitir análise de reutilização
fundamentada no art. 8º, IV, para questões oficiais autossuficientes de concursos
públicos já aplicados e publicadas pela organização, preservando origem e resposta
definitiva. A evidência `reuse` pode ser esta avaliação documentada, aplicada por
item, em vez de exigir exclusivamente uma licença expressa. É uma interpretação
documentada com limites, não uma garantia jurídica nem licença inventada.

Textos literários, charges, fotografias, diagramas ou passagens de terceiros não
herdam automaticamente esse enquadramento. Questões que dependem desses elementos
permanecem pendentes de avaliação própria; não serão mutiladas/adaptadas e contadas
como originais. Comentários de editoras/agregadores não serão copiados: resoluções
e explicações novas são identificadas como elaboração assistida por IA.

## Evidências e separação técnica

Cada item deve manter prova/gabarito/identidade/hash, correspondência e análise de
elementos de terceiros. O campo `reuse.note` identifica explicitamente a base
adotada e seus limites; não deve afirmar consentimento da banca. Análise estrutural
do arquivo/hash não decide a questão jurídica ou editorial.

Acervo e pacotes reais desta retomada ficam no diretório externo explícito
`backend/build/question-expansion/external-2026-11/`, ignorado pelo Git e excluído
do contexto Docker/JAR/frontend/material. Compilador/auditor recebem `--edition-root`;
esse parâmetro não lê rede/DB nem altera importador HTTP ou startup. Metadados e
relatórios públicos podem ser versionados separadamente, sem embutir o acervo.

A meta de 510 originais e os critérios editoriais permanecem. Somente revisão
individual sobre o fingerprint e triagem resolvida permitem contar uma candidata
como verificada; importação segue DRAFT e elegibilidade é decisão separada. Grupos
2/8 aguardavam #161 na etapa inicial; ela foi integrada pelo PR #176 e o grupo 2
retomou em 2026-11-11. Esta avaliação não autoriza merge, publicação ou VPS.
O dossiê externo conserva a cópia original datada e seu SHA-256; esta atualização
de coordenação não reescreve as fontes/fingerprints dos pacotes já preparados.
