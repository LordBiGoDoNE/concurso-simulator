# Grupos 6 e 7 — verificação em andamento

## Retomada após configuração do ambiente

Em 2026-10-14 o usuário informou GitHub autenticado e autorizou subir o PostgreSQL
necessário. Autenticação `LordBiGoDoNE` foi confirmada pelo GitHub CLI instalado;
o PATH da sessão antiga não o incluía, por isso foi usado seu caminho absoluto.
Foi preparado PostgreSQL 18.6 exclusivo, em volume próprio e bind 127.0.0.1,
com API local, Flyway aplicado e consulta pública vazia (zero elegíveis).
Essa preparação **não importou questões nem executou decisões editoriais**.

Uma nova tentativa do ensaio completo foi rejeitada pelo controle automático:
autorização de subir PostgreSQL foi aceita, mas as 500 decisões SUBMIT/APPROVE
continuaram exigindo autorização separada. Foi solicitada explicitamente ao usuário;
não será presumida. O restante deste documento registra a primeira execução e seus
bloqueios à época. GitHub está agora disponível para publicar PR em rascunho/CI.

Pedido: todos os 25 lotes editoriais (6.1–6.25), mais 7.1–7.3. Branch de tarefa
criada da épica `dcbe086af47a6fafca114290850a419f0886cc92`, com histórico preservado.
Não representa integração à épica, aprovação humana dos PRs ou gate `[SPEC]`.

## Cobertura e artefatos

- 250 intervenções editoriais explícitas, 50 por disciplina, distribuídas nos 51
  assuntos. O compilador offline gera 25 pacotes de dez, o inventário completo,
  fichas legíveis, registros por fingerprint e cobertura. Não se conecta ao banco.
- Os hashes de conteúdo/fontes e de cada registro de revisão ficam fixados em
  `editorial/question-bank/2026-10/manifest.json`. Mudanças exigem nova conferência.
- Questões continuam autorais. Responsável declarado: Codex, revisão assistida
  por IA; não há atribuição de revisão humana, fonte oficial de prova ou curadoria
  por fixtures. Dispositivos legais indicados foram consultados em 2026-10-14.
- Correções incluem a equivalência 7/7,0 em M05-Q04, referência ao item anterior em
  M07-Q02/M09-Q02/M12-Q02/A08-Q03/I10-Q05, condições de produtividade/proporção,
  domínio das regras escolares, faixa/base constitucional e localização dos atalhos.
- P05-Q01 permanece inválida no inventário original; não foi selecionada. Alertas
  textuais são preservados e não fornecem duas identidades ao acervo selecionado.
  Perguntas S07-Q03/Q04, S10-Q01 e equivalentes escolares dos alertas administrativos
  foram substituídas. Sobreposição de conceitos não é certificação de cobertura do edital.

## Verificações executadas pelo coordenador

Ambiente Windows, Java Temurin 25.0.4.1+1, Node 24.21.0, Docker Linux 29.8.0.

- `backend/gradlew.bat -p backend --no-daemon --console=plain clean build`: PASS,
  112 testes, zero falhas/erros/skips, com PostgreSQL 18.6 isolado. Inclui leitura
  dos 25 pacotes/250 fingerprints pelo leitor Java sem importar/aprovar acervo real.
- `npm ci`, `npm test`, `npm run build` em frontend: PASS, 73 testes e build.
- `npm run test:identity`: PASS, seis testes de navegador reais com API, PostgreSQL,
  provedor OIDC local e fixtures. Isso não testa nem aprova as 250 questões reais.
- `npm run test:e2e` com STUDY_ROOT construído: seis testes de interface/offline.
- `python -m unittest discover -s scripts/tests -v` em `python:3.14-bookworm`, com
  repositório montado somente leitura: PASS, 46 testes, zero skips. O digest de imagem
  observado foi `sha256:7ee7e4d4fb42c3ad45b8fdc473b64ec69c3cf6e80ae5d52f6fa55f77fac29027`.
  A primeira execução nativa Windows falhou por privilégio de symlink, codificação
  padrão e configuração Git dos repositórios temporários; não foi declarada PASS.
  A execução Linux preservou todos os testes, sem relaxar guardas ou inventar skips.
- `scripts/build_pages.py`: PASS, 108 HTML/1757 referências locais, nove módulos e
  90 resoluções, fontes originais preservadas. Pacotes editoriais não entram no site.
- `npx @fission-ai/openspec@1.14.1 validate --all --strict --no-interactive`: PASS,
  três changes estritos. CLI executado por versão fixada, sem instalação global.

Os resultados acima se referem às execuções informadas, não a testes rodados pelo
revisor independente. Delta editorial posterior será verificado novamente por
testes de pacote/fingerprint/cálculo, sem transformar teste de fixture em aprovação real.

## Bloqueios e limitações operacionais

O controle automático de aprovação rejeitou executar o ensaio que criaria um banco
isolado e persistiria as 500 decisões SUBMIT/APPROVE das 250 revisões. Motivo:
o pedido de grupos 6/7 não foi considerado autorização suficientemente explícita
para aprovação em massa e seu escopo operacional. **O comando não iniciou**;
nenhum volume/banco de curadoria ou aprovação real foi criado por esse ensaio.
Não houve tentativa indireta de contornar a rejeição.

Os pacotes/fichas concretos ficam em `backend/build/curation/`. Um harness temporário
nesse diretório chama a CLI real e usa as confirmações por hash de arquivo, sem
alterar os controles do produto. Não é distribuído no JAR nem executado em CI.
Sua execução depende de autorização operacional explícita. O estado elegível do
acervo real permanece **não comprovado**; `databaseEligible` e IDs de revisão ficam
nulos, em vez de inventar 250 aprovadas. 7.1 permanece incompleta, assim como a
aprovação persistida dos lotes 6.1–6.25; 7.3 e o conjunto não estão concluídos.

GitHub CLI portátil 2.97.0 foi preparado no diretório ignorado `backend/build/tools`,
com checksum conferido contra a distribuição oficial. Não há host autenticado
nesse ambiente; nenhum PR/issue foi criado ou atualizado, nem houve merge/push.
As ferramentas locais de CLI não substituem a autenticação do usuário no GitHub.

## Risco e revisão independente

Risco **crítico**: conteúdo/fingerprint/evidência podem determinar elegibilidade
editorial e respostas; a quantidade aumenta o alcance de um erro sistemático.
Não há alteração de produção em autenticação, SQL, schema, regras de CI ou deploy.
Somente SOL 6.1 xhigh revisará o lote completo em contexto novo. A ferramenta
`quota_status` não está disponível no harness atual: janelas 5h/semanal e tokens
não devem ser inventados, e não se atribui consumo exato ao revisor.
O gate cumulativo Astra medium permanece pendente e separado.
