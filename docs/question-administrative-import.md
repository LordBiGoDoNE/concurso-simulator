# Pacote administrativo — camada de leitor

Esta camada implementa leitor/prévia do núcleo. HTTP, persistência e autenticação são
camadas superiores do mesmo lote autorizado, ainda não integradas.

## Formato e validação

Envelope de prévia: `package` e `keys`. Aplicação acrescenta `confirmationToken`.
Pacote: `format=administrative-question-package/v1`, `purpose`, `items`, `files`.
Cada item contém `origin`, `content`, `fingerprint` e `expectedLatestRevision`
**obrigatório**, null para origem nova ou UUID da última revisão esperada para mudança.
Conteúdo segue os VOs existentes: texto/alternativas identificadas, resposta correta,
resolução, classificação, referências e proveniência. O fingerprint binário continua
`question-content/v1`; pode ser calculado por `scripts/question_package.py`.

Arquivos contêm `path`, `mediaType`, `sha256` e `base64` dos bytes originais, com
padding padrão e sem whitespace. Fontes usam `sources/` com UTF-8 text/plain ou
application/json; recursos usam `assets/` com PNG/JPEG/SVG inertes e descrição acessível.
Proveniência pode apontar duas vezes ao mesmo arquivo fonte. Todos os arquivos devem
ser referenciados; nomes são chaves lógicas, nunca caminhos lidos/escritos no host.
Nenhum link é buscado automaticamente. Hash comprova bytes, não autoria/correção.

Limites: 16 MiB no corpo integral (inclusive sem Content-Length); 1–100 itens e
1–100 chaves explícitas; até 100 arquivos, 1 MiB cada e 8 MiB decodificados somados;
profundidade JSON 32; 2–10 alternativas, 1–20 referências, até oito recursos por bloco
textual e demais limites dos VOs. Content-Encoding comprimido não é aceito. Uma
requisição de prévia/aplicação processa por instância; excesso retorna 429 com
Retry-After, sem fila de payloads. Não é proteção global entre instâncias.

O pacote inteiro é validado, inclusive itens não selecionados. Suspeitas de duplicação
comparam texto exato entre origens do próprio pacote; não são busca semântica global
nem provocam fusão. A classificação valida assunto/disciplina/cargos do catálogo atual.
Quando questões de prática forem aprovadas por operação editorial separada, entram
no conjunto do assunto/cargo; cada treino continua escolhendo até cinco questões.

## Digests e confirmação

SHA-256 de enquadramento binário: strings UTF-8 com comprimento int32 big-endian,
contagens int32, presença de UUID como byte 0/1, UUID textual canônico. Ordem: marcador
`administrative-question-package/v1`, finalidade, quantidade de itens ordenados por
chave (namespace único por pacote); origem namespace/chave, fingerprint recalculado,
presença/base esperada de cada item; quantidade de arquivos ordenados por caminho;
caminho, mediaType, SHA-256 recalculado e comprimento dos bytes por arquivo.
Seleção usa marcador `question-import-selection/v1`, quantidade e chaves ordenadas.

Espaços/propriedades JSON e ordem de itens/arquivos/chaves não mudam digest. Alterar
conteúdo, finalidade, base ou bytes de qualquer arquivo, inclusive não selecionado,
invalida a confirmação. Ordem editorial de alternativas/referências/recursos permanece
significativa. A confirmação HMAC vincula nonce, ator, destino, pacote, seleção e tempo;
não substitui sessão, autorização ou CSRF. Token tem até 4 KiB e nunca é persistido.
