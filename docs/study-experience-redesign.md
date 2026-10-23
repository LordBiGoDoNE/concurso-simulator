# Nova experiência de estudo

Lote autorizado: grupos 2–6 de study-experience-redesign. O protótipo aprovado está no PR #140. A implementação usa os contratos existentes; main, stable, Pages e o acervo permanecem fora desta mudança.

## Navegação e início — grupo 2

Início, Simulados, Aulas e Banco de questões são áreas separadas. O início consulta contagens públicas reais; falha de consulta não é apresentada como acervo vazio nem como progresso pessoal. Aulas e material continuam acessíveis sem API/login.

A realização permanece montada ao navegar, preservando escolha, fila e comando idempotente. Conteúdo privado continua condicionado à verificação de identidade; troca de conta/logout desmontam o contexto mesmo em outra área. A navegação move foco para o título da área e oferece atalho para o conteúdo. A revisão por assunto e o histórico existentes permanecem em Simulados neste lote inicial.

Verificação inicial: 62 testes de entrada, identidade, contexto e questões passaram, incluindo navegação com retry pendente e troca de conta em área distinta. Build de produção passou. Os três testes Chromium de entrada passaram, com teclado, foco e ausência de corte lateral em 390px/1280px; os fluxos reais foram atualizados para percorrer a navegação explícita e aguardam regressão integrada. Implementado não equivale a revisado/integrado; revisão independente e gate cumulativo ao fim do lote completo.

## Composição — grupo 3

Configuração organizada em cargo, distribuição/assuntos e variações opcionais. Resumo lateral com total e composição efetiva; em celular, total e início permanecem disponíveis em barra inferior. Sem alteração do contrato, critérios, operação idempotente ou tratamento de insuficiência/retry.

Build e 11 testes unitários de composição/prática dirigida passaram. Dois testes Chromium (390px/1440px) conferiram validação, total, seleção de assunto/variações, preservação ao navegar e ausência de corte lateral. Revisão independente ao fim do lote e integração permanecem pendentes.
