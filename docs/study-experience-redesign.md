# Nova experiência de estudo

Lote autorizado: grupos 2–6 de study-experience-redesign. O protótipo aprovado está no PR #140. A implementação usa os contratos existentes; main, stable, Pages e o acervo permanecem fora desta mudança.

## Navegação e início — grupo 2

Início, Simulados, Aulas e Banco de questões são áreas separadas. O início consulta contagens públicas reais; falha de consulta não é apresentada como acervo vazio nem como progresso pessoal. Aulas e material continuam acessíveis sem API/login.

A realização permanece montada ao navegar, preservando escolha, fila e comando idempotente. Conteúdo privado continua condicionado à verificação de identidade; troca de conta/logout desmontam o contexto mesmo em outra área. A navegação move foco para o título da área e oferece atalho para o conteúdo. A revisão por assunto e o histórico existentes permanecem em Simulados neste lote inicial.

Verificação inicial: 62 testes de entrada, identidade, contexto e questões passaram, incluindo navegação com retry pendente e troca de conta em área distinta. Build de produção passou. Os três testes Chromium de entrada passaram, com teclado, foco e ausência de corte lateral em 390px/1280px; os fluxos reais foram atualizados para percorrer a navegação explícita e aguardam regressão integrada. Implementado não equivale a revisado/integrado; revisão independente e gate cumulativo ao fim do lote completo.

## Composição — grupo 3

Configuração organizada em cargo, distribuição/assuntos e variações opcionais. Resumo lateral com total e composição efetiva; em celular, total e início permanecem disponíveis em barra inferior. Sem alteração do contrato, critérios, operação idempotente ou tratamento de insuficiência/retry.

Build e 11 testes unitários de composição/prática dirigida passaram. Dois testes Chromium (390px/1440px) conferiram validação, total, seleção de assunto/variações, preservação ao navegar e ausência de corte lateral. Revisão independente ao fim do lote e integração permanecem pendentes.

## Realização — grupo 4

Enunciado e alternativas ficam no cartão principal, com progresso e mapa navegável de questões. Seleção/ausência de resposta são indicadas por texto e estado acessível; não representam correção ou confirmação do salvamento. O diálogo de conclusão informa respondidas/brancos e devolve o foco ao cancelar.

A máquina de fila/versão/idempotência não foi alterada. Estados de salvamento confirmado, pendente, falha, conflito e escolhas preservadas continuam distintos, com as mesmas ações de retry/recarga. Resultado e revisão individual continuam a depender do servidor após conclusão. Os testes existentes de realização/resultado/contexto passaram; regressão específica do mapa/contagens adicionada. Revisão do conjunto e gate cumulativo pendentes.

Mapa/contagens: 10 testes de realização passaram e dois testes Chromium (390px/1440px) conferiram escolha, salvamento, navegação por teclado, foco e cancelamento via Escape. Capturas locais conferidas. O resumo de sessão foi compactado, com orientações completas em expansão; controles e mensagens de acesso permanecem disponíveis.

## Aulas — grupo 5

Biblioteca local das 51 aulas com busca normalizada e filtros combináveis, independente da disponibilidade da API. Catálogo público gerado a partir da seleção aprovada, testado contra manifesto e mapa de teoria. Nenhum trecho parcial do protótipo substitui a aula: o leitor incorpora o HTML completo no endereço canônico conhecido, em iframe sem execução de scripts, com link ao original, navegação da disciplina, anterior/próxima e seleção de cargo elegível. Figuras/fórmulas/exercícios/links continuam na origem; não há importação do HTML para o bundle nem alteração das fontes.

A moldura do leitor segue o layout aprovado; a página incorporada conserva sua apresentação original e rolagem própria. Navegadores não permitem confirmar status HTTP de iframe remoto: há fallback explícito para abrir o original e retry se a carga demorar. Isso não certifica disponibilidade de hospedagem externa.

O atalho usa a fronteira de composição da realização existente. A camada depende da navegação/composição e do contexto de realização que preserva: cancelar troca mantém escolha/fila/comando; confirmar apenas preenche uma questão do assunto, sem POST automático e sem variações selecionadas. Operação em andamento ou verificação de identidade bloqueiam a substituição.

Verificação: 171 unitários passaram; 9 testes Chromium de início/composição/realização/aulas passaram (390px e 1280px/1440px), incluindo fontes completas locais, imagem original carregada, teclado/foco e ausência de corte lateral. Capturas do leitor/biblioteca conferidas. Não representa auditoria WCAG completa ou gate final.
