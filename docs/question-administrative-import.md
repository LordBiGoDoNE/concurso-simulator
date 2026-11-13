# Importação administrativa — #160

Operação síncrona para ampliar os acervos **SIMULADO** e **PRATICA_AULA**, por JSON
autossuficiente. Até 100 questões por pacote, sem teto acumulado de armazenamento.
Tudo novo entra em DRAFT. A interface administrativa limita-se a login, senha e vínculo Google;
não há upload/editorial na interface nem publicação de recursos; decisões editoriais HTTP existem apenas em lote para o ADM ([#177](#decisões-editoriais-em-lote--177)), carga no startup ou suporte a templates. Os leitores locais antigos
continuam aceitando seus próprios formatos sem conversão automática.

## Acesso e operação local

O primeiro ADM é criado uma única vez, atomicamente, após Flyway. `ADMIN_USERNAME`
(opcional, padrão `admin`) e `ADMIN_PASSWORD` configuram apenas esse primeiro cadastro.
Nome aceita 1–64 caracteres ASCII de letras, números, ponto, underscore e hífen.
A senha escolhida deve ter de 15 a 128 caracteres Unicode, sem trim, normalização ou truncamento; UTF-16 malformado é recusado.
Se a senha não for fornecida, são gerados 32 bytes aleatórios codificados em Base64url;
a senha temporária aparece **uma vez no console privado do operador, após commit**.
Essa saída contém um segredo: não a publicar em issue, relatório, captura ou logs de acesso.
Reiniciar, mudar variáveis ou desabilitar o ADM não recria conta nem redefine sua senha.

Na interface, abrir **Acesso administrativo**, entrar e trocar eventual senha temporária.
Login por senha funciona com Google desativado e continua disponível após o vínculo.
Senha temporária permite consultar identidade/conta, trocar senha e sair; não permite
importar, vincular ou gravar tentativas autenticadas. Credenciais usam PBKDF2-HMAC-SHA256
com sal aleatório de 16 bytes, 600.000 iterações e saída de 256 bits, em formato versionado.
Senha/hash e tokens Google não são colocados no principal ou na sessão.

Para vincular, confirmar a senha atual e escolher a conta no Google. A intenção privada
expira em cinco minutos e é vinculada à sessão, state OIDC e versão da credencial.
Não há associação por e-mail ou concessão pelo primeiro visitante. Se o Google ainda
não possui conta, mantém-se o UUID ADM. Se já possui conta, seu UUID e histórico são
preservados; somente credencial/permissão são transferidas, e apenas se o ADM local
não tiver tentativas, comandos ou recibos. Nesse caso a fonte permanece cadastrada,
inativa, e rejeita gravações atrasadas. Se houver histórico local, o vínculo retorna
`LOCAL_ACCOUNT_HAS_HISTORY`, sem mesclagem. Concluir o vínculo antes de importar evita
esse conflito quando já existe uma conta Google de estudo. Trocar/desvincular Google e
gestão de vários administradores ficam para outra entrega.

A autorização lê a permissão e a versão persistentes a cada requisição administrativa.
Revogar via manutenção externa do cadastro (`enabled=false`) tem efeito sem reinício;
reativar é uma operação deliberada do operador, não um endpoint público. Troca/recuperação
de senha e vínculo incrementam a versão: acessos administrativos antigos são recusados,
mesmo com cookie ainda válido para estudo. Sessões Google anteriores à concessão
precisam autenticar novamente para obter acesso ADM. Não há fallback por UUID externo:
remover `app.question-import.allowed-user-ids`; valor legado não vazio impede startup com
diagnóstico genérico. A senha ADM não é a chave de confirmação de importação.

Definir também `app.question-import.destination-id` (identificador lógico ASCII de
até 100 caracteres, sem DSN) e `app.question-import.confirmation-key` (Base64 de 32–128
bytes aleatórios externos). Configuração parcial/inválida falha sem imprimir valores. Se destino e chave estiverem
ambos ausentes, cadastro/login ADM continuam disponíveis, e a importação retorna
503 `IMPORT_NOT_CONFIGURED`, antes de ler o pacote.
Usar configuração externa Spring, por exemplo `SPRING_APPLICATION_JSON` fornecido
pelo ambiente, nunca segredo em argumentos, repositório ou `.env` versionado.
Não reutilizar destino/chave entre bancos. Troca/restauração do banco lógico exige
trocar o destino; rotação da chave invalida confirmações pendentes.

1. Obter `/api/v1/csrf` na mesma sessão autenticada. Cookie continua HttpOnly.
2. Enviar `POST /api/v1/admin/question-imports/preview` com `Content-Type:
   application/json`, cookie e header CSRF informados pelo framework.
3. Conferir coleção, destino, seleção, fingerprints, bases e relatório. A prévia
   não persiste bytes, recibo, questão ou decisão, nem reserva IDs. STALE não emite token.
4. Enviar a mesma estrutura acrescida de `confirmationToken` para
   `POST /api/v1/admin/question-imports/apply` em até 15 minutos. Não renovar
   confirmação automaticamente após conflito; revisar pacote/base e gerar nova prévia.
5. Guardar o recibo e o pacote original em ambiente privado. Repetir a confirmação
   válida após resposta perdida retorna o recibo original sem duplicação.

O [exemplo de prévia](examples/administrative-question-import.json) é totalmente
sintético e tem fingerprint verificável. Não o carregar em acervo real por este guia.
Para simulado, usar `purpose=SIMULADO` e namespace `admin-question-v1`; para prática,
`purpose=PRATICA_AULA`, namespace `lesson-practice-v1` e chave `admin.<identificador>`.
Chaves têm até 100 caracteres ASCII alfanuméricos/ponto/hífen/underscore. As origens
históricas de prática permanecem protegidas: a API não aceita suas chaves antigas.
Cada pacote tem uma finalidade; importar ambos requer pacotes separados.

## Sessão, contratos e recuperação

Os novos contratos tipados estão em `backend/openapi.yaml`:

| Rota | Operação |
| --- | --- |
| `POST /api/v1/admin/auth/login` | JSON `username`/`password`; 204 após rotação de sessão/CSRF |
| `GET /api/v1/admin/account` | Estado privado: usuário, troca pendente, Google e disponibilidade da importação |
| `POST /api/v1/admin/account/password` | JSON `currentPassword`/`newPassword`; 204, nova versão e sessão |
| `POST /api/v1/admin/account/google-link` | JSON `currentPassword`; URL fixa para iniciar OIDC |

Todos usam no-store; POST exige CSRF do framework, inclusive login. Buscar `/api/v1/csrf`
na mesma sessão antes de cada mutação e novamente após login/troca. Corpos administrativos
aceitam somente JSON UTF-8 estrito, até 8 KiB, sem compressão, campos extras, duplicados ou
conteúdo depois do JSON. As rotas de importação mantêm o limite próprio de 16 MiB.
`/me`, configuração pública Google, `/csrf` e logout mantêm os contratos anteriores.

Cinco falhas de credencial em quinze minutos bloqueiam por quinze minutos no banco,
inclusive entre instâncias. A origem de rede real recebe também um orçamento de trinta
logins por quinze minutos/instância, incluindo tentativas bem-sucedidas; não se confia em
X-Forwarded-For arbitrário. 429 inclui `Retry-After: 900`. A UI limpa senhas após uso,
respeita o aviso de atividade visitante e não grava segredos em storage do navegador.

Se perder a senha inicial ou a senha permanente, usar recuperação **local e deliberada**,
com o schema V13 já aplicado. Fornecer `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` externamente,
sem argumentos com credenciais ou `.env` versionado. `DB_URL` deve apontar explicitamente
para PostgreSQL no loopback, sem credenciais na URL; schema/timeouts admitidos seguem o
validador do comando. Para banco remoto, usar o acesso local/túnel do operador, não
relaxar a restrição do executável.

```powershell
./backend/gradlew.bat -p backend --offline --no-daemon administrativeRecovery
```

O comando apresenta destino e exige digitar `RESET ADM <DB_URL>` exatamente. Cancelar
não abre o banco. Ele não executa Flyway/bootstrap, não cria conta ausente nem concede
permissão a ADM desabilitado. Após commit, mostra nova senha temporária no console
privado. Preserva Google e todo o histórico, incrementa a versão e exige nova troca;
não existe reset por HTTP/e-mail. Uma falha depois do commit pode perder a saída: executar
novamente o comando deliberado, sem interpretar ausência da mensagem como rollback.

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

## Commit, falhas e dados privados

Questões, revisões, evidências selecionadas e recibo compartilham um único commit.
Erro de banco, evidência ou base provoca rollback integral. Uma resposta perdida pode
ter ocorrido após commit; repetir resolve pelo nonce. Após expiração, nova prévia e
reaplicação reutilizam conteúdo existente. Conteúdo histórico idêntico mantém sua revisão
e não altera a última revisão, decisões ou elegibilidade, inclusive após retirada.

Recibo contém ator, tempo do servidor, destino, digests e IDs/reutilização reais.
Bytes privados são deduplicados por hash com verificação de igualdade e manifesto por
recibo; não existe rota de download público. Não armazenar itens não selecionados ou
token bruto. Evidência é imutável e sem limpeza automática; dimensionar backup restrito
e crescimento de disco. Aprovação local posterior não distribui automaticamente assets.

Erros: 400 envelope/JSON malformado; 401 sessão/principal inválido; 403 permissão/CSRF/
origem; 409 confirmação/base; 413 corpo; 415 mediaType/encoding; 422 pacote inválido;
429 capacidade; 503 indisponibilidade transitória conhecida; 500 erro técnico inesperado.
Respostas são no-store e sanitizadas, sem gabaritos, bytes, stacktrace, segredo ou DSN.
Falhas de entrada usam diagnóstico fixo INVALID_PACKAGE; conferir o pacote original
privadamente. Não registrar body, cookies ou confirmationToken em proxy/log de acesso.

Desabilitação operacional: revogar a permissão persistida (`enabled=false`), sem reinício; preservar tabelas,
recibos e evidências. Não executar down migration ou apagar dados para rollback.

## Responsabilidades e limites da verificação

Spring Security decide autenticação, permissão técnica, CSRF e CORS antes do controller. A indisponibilidade de sessão é traduzida por filtro de infraestrutura anterior ao Spring Session, com 503/no-store e CORS para origens conhecidas.
Adaptadores validam JSON/bytes/assinatura e traduzem HTTP; aplicação coordena seleção,
confirmação e unidade atômica; domínio protege conteúdo/revisões; infraestrutura JPA
persiste e conclui rollback/commit. SQL de recibos/bytea fica no repository e compartilha
a transação de questões; native SQL evita mapeamento artificial de blobs como entidades.
O controller não lê sessão/JPA, não cria token CSRF e não promove decisões editoriais.

Testes usam fixtures e PostgreSQL isolado: não demonstram curadoria real, desempenho de
produção ou revisão independente. Carga real, publicação e revisão por modelo exigem
autorizações próprias. A proposta e a implementação local pertencem à issue #160.

## Consulta administrativa do acervo — #177

Para o ADM (ou a IA que o auxilia) conferir o acervo antes de importar ou decidir, há
rotas **somente leitura** sob `/api/v1/admin/questions` ([#177](https://github.com/LordBiGoDoNE/concurso-simulator/issues/177)).
Mesma exigência de acesso da importação: sessão ADM, permissão e versão persistentes,
troca inicial concluída; CSRF em POST; respostas `no-store`. Não dependem de
`destination-id`/`confirmation-key`. Nada é gravado nem reservado. As rotas públicas
`/api/v1/question-bank/questions` continuam sem gabarito, resolução, estado e fingerprint.

- `GET /api/v1/admin/questions`: página (`page`, `size` até 100, padrão 50) ordenada por
  namespace, chave e ID, com total. Filtros por AND: `purpose`, `namespace`, `keyPrefix`
  (prefixo literal da chave), `disciplineId`/`topicId`/`latestStatus` (da última revisão),
  `hasEligible`, `reviewKind` (`HUMAN_PEER`/`AI_ONLY` da aprovação elegível) e
  `activityFrom`/`activityTo` (última decisão ou importação registrada). Aprovações
  históricas sem origem aparecem com `reviewKind: null` e não entram no filtro.
- `GET /api/v1/admin/questions/{id}`: todas as revisões com conteúdo integral, inclusive
  gabarito e resolução, no formato de conteúdo do pacote; decisões com sequência, responsável,
  checklist, justificativa e origem da revisão; recibos de importação relacionados.
- `POST /api/v1/admin/questions/lookup`: até 1.000 pares `{origin, fingerprint}` de um pacote
  local. Resultado por par, na ordem enviada: `ABSENT`, `PRESENT` (sem fingerprint),
  `SAME_AS_LATEST`, `SAME_AS_ELIGIBLE` ou `DIFFERENT`, com IDs/fingerprints atuais e
  `matchingRevisionId` quando alguma revisão tem o fingerprint informado. POST apenas
  pelo tamanho do corpo; sem efeitos.
- `POST /api/v1/admin/questions/export`: `questionIds` (última revisão de cada, até 100) ou
  `importReceiptId` (revisões do recibo), nunca ambos. Devolve `administrative-question-package/v1`
  com `expectedLatestRevision: null`, digests e chaves. O pacote é conferido pelo próprio
  leitor de importação antes da resposta; reimportá-lo resulta em `IDENTICAL`. Recusa com 422
  `EXPORT_REFUSED` e motivo (`NOT_FOUND`, `MIXED_PURPOSE`, `ORIGIN_NOT_EXPORTABLE` para origens
  fora do formato administrativo, `MISSING_EVIDENCE` quando fontes/recursos não foram
  armazenados por importação administrativa, `LIMIT_EXCEEDED`).

Corpos POST: JSON UTF-8 sem compressão, até 2 MiB, sem chaves duplicadas, campos
desconhecidos ou coerção de tipos (400 `INVALID_REQUEST`); filtros/limites inválidos
retornam 400 `INVALID_QUERY`. A plataforma não julga conteúdo nem semelhança entre questões:
a conferência de concorrência e de curadoria é do operador.

## Decisões editoriais em lote — #177

**Decisão alterada:** a #160 registrava "sem decisões editoriais HTTP". Por decisão do
usuário de 2026-11-12 ([#177](https://github.com/LordBiGoDoNE/concurso-simulator/issues/177)),
o ADM autenticado pode decidir em lote pela API. A CLI local `decide` continua disponível,
inclusive para RETURN_TO_DRAFT e WITHDRAW, que ficaram fora do lote nesta entrega.

Mesmo modelo da importação: prévia sem escrita, token de confirmação e aplicação atômica com
recibo. Exige a mesma configuração (`destination-id` e `confirmation-key`); sem ela, as rotas
respondem 503 `DECISIONS_NOT_CONFIGURED` antes de ler o corpo. O token é HMAC com separação de
domínio própria: token de importação não confirma lote, nem o inverso.

1. `POST /api/v1/admin/editorial-decisions/preview` com `action` (`SUBMIT` ou `APPROVE`),
   `reviewKind` (`HUMAN_PEER`/`AI_ONLY`, obrigatório em APPROVE), `checklist`, `rationale`
   (até 3.500 caracteres) e a seleção: `items` (`questionId`, `revisionId`, `fingerprint`) ou
   `importReceiptId` (as revisões gravadas por aquele recibo), nunca ambos. Até 100 questões,
   sem repetição — um recibo de importação sempre cabe.
2. A prévia devolve, por item, estado atual, estado resultante ou motivo de recusa
   (`QUESTION_NOT_FOUND`, `REVISION_NOT_FOUND`, `FINGERPRINT_MISMATCH`, `INVALID_TRANSITION`,
   `CHECKLIST_INCOMPLETE`), calculados aplicando a decisão a uma cópia do agregado pelas
   próprias regras do domínio. Se todos os itens forem válidos, devolve `expected` (ponteiro
   elegível e sequência observados por questão), `digest` e `confirmationToken` válido por
   15 minutos. Nada é gravado.
3. `POST /api/v1/admin/editorial-decisions/apply` com o mesmo envelope, `items` = `expected`
   da prévia e o token. O token vincula ator, destino e o digest do envelope e do estado
   confirmado; qualquer alteração retorna 409 `CONFIRMATION_MISMATCH` (ou
   `CONFIRMATION_EXPIRED`).
4. A aplicação trava as questões em ordem fixa e confere que o estado de **todas** é igual ao
   confirmado. Divergência recusa o lote inteiro com 409 `BATCH_DIVERGED` e a lista de
   questões, sem efeito parcial; refaça a prévia. Depois grava cada decisão pelo domínio
   (mesma regra do `DecideQuestionUseCase`), com `responsible = admin:<UUID do ADM>`, horário
   do servidor, checklist, origem da revisão e justificativa acrescida de `[lote <nonce>]`,
   e o recibo (`qb_decision_batch`, `qb_decision_batch_item`, V17), tudo no mesmo commit.
5. Resposta perdida: reenviar a mesma aplicação devolve o recibo original com
   `replayed: true`, sem decisões duplicadas.

A plataforma não procura conflitos editoriais nem julga conteúdo ou semelhança: a única
recusa automática além de forma, acesso e transições do domínio é a divergência entre a
prévia confirmada e o estado atual. Conferir revisão esperada e mudanças concorrentes é
responsabilidade do operador (ADM ou IA que o auxilia), apoiado pela consulta administrativa.
