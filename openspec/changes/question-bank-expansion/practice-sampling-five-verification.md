# Esclarecimento atual — acervo25, prática até cinco

Em2026-11-11 o usuário esclareceu que deseja até cinco questões por prática,
sorteadas de um acervo com pelo menos25 por assunto/modalidade. A leitura anterior
de25 itens por prática foi superada. O [contrato atual](specs/lesson-practice-capacity/spec.md)
registra essa distinção. Cotas editoriais e cinco originais por célula permanecem.

## Implementação

- `AttemptUseCases` usa todo o pool elegível do assunto e seleciona `min(5,pool)`
  com o embaralhamento existente. Available informa o pool real; maximum é5.
- `Attempt.startPractice` recusa nova criação com mais de cinco. Restaurar captura
  anterior formato4 até25 continua permitido; comandos repetidos mantêm sua captura,
  sem ressorteio/truncamento. Novos commandIds sorteiam novamente e podem ter
  coincidências entre práticas. Não há cargo atribuído nem mistura com simulados.
- Cliente/textos usam cinco para novos treinos. Leitura, histórico, resultado e
  replay mantêm compatibilidade com as capturas anteriores maiores.
- V1–V15 não foram modificadas nem foi criada outra migração. A V15 já aplicada
  conserva a legibilidade do teste anterior. A correção do codec para preservar
  JSON imutável entre JVMs foi mantida.

## Preservação do ambiente local

Antes de reiniciar a API própria de teste, o banco Testcontainers foi identificado
pelo mapeamento de porta do log e rótulos/imagem. Backup privado customizado,
checksum e fingerprints das25 tabelas de negócio foram conferidos. A API anterior
foi encerrada somente após verificar PID/classe e existência do backup.

O harness novo aceita restauração explícita por arquivo/hash, antes do startup;
esse mecanismo existe somente em `src/test`, fora do JAR/HTTP de produção. Ao
restaurar, não reaplica seeds. Comparação após restauração preservou as25 tabelas
e a prática de25 já aberta. Sessões/configuração privadas não foram publicadas.
Nenhum banco do operador ou VPS foi consultado ou alterado.

Ambiente do usuário: [P01](http://127.0.0.1:5393/#/aulas/P01), API19370. Conferidos
UI/status200 e `available:30, maximum:5`; são30 bases sintéticas para testar amostra
de cinco, não o conteúdo editorial da expansão. Atualizar a página e iniciar nova
prática aplica o sorteio de cinco; uma captura antiga mantém seu tamanho original.

## Verificações

Regressão completa de backend:341 testes,340 aprovados e um ignorado histórico
de symlink no Windows. Frontend238/238 e build aprovados. O teste de domínio usa
pool25/RNG controlado para conferir amostras de cinco, seleção do pool inteiro e
replay estável. Reinício real em dois processos passou com novo treino de cinco
e leitura preservada da captura anterior de25.

A inicialização do teste de navegador expôs uma fixture histórica que escolhia
qualquer cargo editorial para uma captura formato3 de cargo administrativo. A
fixture agora escolhe uma base compatível com esse cargo histórico; a prática nova
continua sem filtro de cargo. Isso não relaxou invariantes ou assertions. O launcher
de navegador usa dependências locais em modo offline. Revalidação das suítes
afetadas:13/13 aprovados. Chromium/Edge:30/30, zero falhas, ignorados ou flaky.

Relatórios anteriores de25 por prática foram preservados como histórico. Aceite
do usuário, revisão independente SOL6.1xhigh, publicação, merge e gate cumulativo
continuam separados. Acervo externo:79 candidatas preservadas, ainda sem revisão
editorial integral/cotas completas ou importação/habilitação real.
