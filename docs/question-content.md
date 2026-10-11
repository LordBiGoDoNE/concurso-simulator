# Conteúdo e identidade das questões — tarefa 1.2

`Question` protege identidade/origem e sequência única de revisões. `QuestionRevision`
é imutável; `QuestionContent` copia listas e exige duas alternativas distintas com
UUIDs únicos, uma única resposta pertencente à revisão, resolução, classificação,
proveniência e referências de estudo nomeadas. Ordem da lista é ordem editorial,
nunca índice/letra de gabarito. Nenhuma variante parametrizada/tentativa nesta etapa.

`TextContent` é texto acessível, preservando Unicode/fórmulas sem trim ou normalização;
UTF-16 malformado é rejeitado antes da canonização UTF-8, sem substituir surrogates
isolados. Pares válidos de caracteres suplementares são preservados, inclusive em URLs.
recursos de cada bloco têm caminho relativo, tipo permitido, SHA-256 e descrição.
Não é HTML renderizável nem um sanitizador. A conversão/allowlist e inspeção de SVG,
raízes/symlinks do extrator pertencem a 3.1–3.2; não publicar recursos apenas porque
passaram nas validações estruturais. Fontes HTTP(S) não são buscadas automaticamente.
Proveniência distingue autoria de links temáticos e registra hashes de conteúdo/gabarito.
Fundamento legal pode registrar dispositivo/data; isso não aprova automaticamente normas.

Fingerprint `question-content/v1`: SHA-256 de representação binária determinística,
inteiros de 32 bits big-endian para comprimento UTF-8/quantidade, booleano para fundamento
opcional. Ordem: versão; enunciado; alternativas em ordem (ID/bloco); ID correto;
resolução; disciplina/assunto; cargos por ID ordenado; referências em ordem (nome/URL/
fundamento/data); autoria/caminhos/hashes de origem. Cada bloco inclui texto e recursos
em ordem (caminho/tipo/hash/descrição). IDs/número de revisão e estados não são conteúdo.
O pacote JSON futuro deverá declarar este formato e preservar integralmente seus campos.

Testes puros de conteúdo e ArchUnit; fixtures sintéticas identificadas. Não há estado
editorial/aprovação, persistência ou projeção pública nesta tarefa. Resposta/resolução
permanecem no modelo interno, nunca contratos públicos de questões. Não alterar o legado.
