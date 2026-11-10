# Regressões dos digests administrativos

`AdministrativeDigestTests` fixa vetores sintéticos das duas finalidades e da seleção.
A matriz altera texto, espaços/Unicode, IDs e ordem de alternativas, resposta, classificação,
referências/base legal, autoria, recursos e sua ordem/quantidade, origem/base esperada,
finalidade/namespace, caminhos, tipos, bytes/hashes e comprimento dos arquivos.

Cada mutação recebe fingerprint consistente; referências a arquivos e hashes são ajustadas
em conjunto para manter pacotes válidos. Campos presos a uma invariante não são alterados
isoladamente para fabricar um pacote inválido. O conteúdo selecionado e o não selecionado
são exercitados, com fontes exclusivas que não aparecem nas evidências da seleção.
Ordenação de itens/arquivos/cargos e seleção como conjunto preservam as equivalências
previstas; listas editoriais e conteúdo lexical permanecem sensíveis. Formato fixo,
fingerprint divergente e violações estruturais continuam nos testes negativos do leitor.
Esses vetores protegem regressões do contrato; não constituem aprovação editorial real.
