# Grupos 6 e 7 — execução autorizada e verificação integrada

## Escopo e autorização

O usuário autorizou explicitamente a importação dos 25 pacotes, as 500 decisões
SUBMIT/APPROVE das 250 revisões examinadas e a publicação da branch/PR draft/issue.
Esses passos foram executados depois da autorização, em 2026-10-14. As rejeições
automáticas anteriores permanecem histórico em `verification.md`, sem representar
o estado atual. Não houve contorno da aprovação, merge ou alteração do deploy.

PR **#62**, `task/question-bank/initial-curation` → `epic/question-bank`, draft.
Trunk/base permanece `dcbe086af47a6fafca114290850a419f0886cc92`.
As tarefas são um único incremento coeso de curadoria e comprovação; não há camada
dependente de outra branch nem necessidade de criar stack artificial. Os 25 lotes
de dez têm seleção/evidência individual. Manifesto/elegibilidade são dados gerados,
identificados separadamente das intervenções/editorial e código autorais.

## Acervo real e decisões persistidas

- PostgreSQL **18.6**, exclusivo, volume próprio e bind **127.0.0.1**. Flyway V1–V4
  aplicadas pelo aplicativo; JPA validate, sem alteração de migrações/schema.
- CLI real: prévia, importação e reaplicação de cada um dos 25 pacotes. A repetição
  preservou 250 questões e 250 revisões, sem duplicar identidades ou aprovar na importação.
- Para cada revisão exata, arquivo SUBMIT e arquivo APPROVE separados, com confirmação
  de destino/hash, fingerprint, checklist e sequência esperada. **250 + 250 decisões**.
- Relatório privado da CLI e SQL confirmaram **250 elegíveis, 50 por disciplina**,
  51 assuntos, zero rascunhos/pendências importados e zero sessões após as leituras.
  Inventário original: 510 IDs; P05-Q01 continua inválida/não selecionada.
- As 500 linhas `qb_decision` foram cruzadas com os arquivos de comandos: UUIDs,
  revisão, fingerprint, responsável, sequência, checklist e datas. A coluna temporal
  tem precisão 6; diferença observada de até um microssegundo entre Instant e banco.
  Não se declara igualdade de nanossegundos. SHA-256 dos bytes de comando é exato.
- Evidência sanitizada: `editorial/question-bank/2026-10/eligibility.jsonl` (250 linhas
  geradas) e `eligibility-summary.json`. Não incluem conexão, senha ou logs privados.
  Responsável: Codex, revisão editorial assistida; não se inventa revisão humana.

## HTTP e navegador com dados reais

`scripts/verify_curated_bank.py` executado contra a API local:

- três páginas/250 IDs ordenados e distintos, ligados aos UUIDs de revisão do relatório;
- **250 detalhes** comparados ao conteúdo revisado e sua projeção permitida;
- cinco disciplinas com 50 elegíveis e **102 combinações AND** de assunto/disciplina/cargo;
- ausência de campos extras/privados/corretivos, filtro desconhecido 400 e UUID ausente 404.

`scripts/verify_curated_browser.cjs` executado contra frontend/API reais:

- 390/1280 px, cinco disciplinas por largura, dez detalhes e 44 corpos públicos;
- paginação/filtros, entrada/saída do detalhe por teclado, foco e fontes visíveis;
- zero overflow horizontal/storage, nenhum campo corretivo e aulas acessíveis durante
  falha de consulta, com recuperação pelo retry. Capturas locais inspecionadas.

São ensaios manuais somente leitura, fora do CI de fixtures. As três regressões novas
do verificador usam relatórios **sintéticos** e simulam dados adulterados/campo de
resposta exposto; não aprovam acervo real nem entram na contagem das 250 identidades.

## Retirada, idempotência e isolamento

Uma cópia lógica completa do banco local foi criada para testar retirada de uma
revisão real e reaplicação do mesmo item pelo launcher. Nela: 250 → **249** elegíveis,
detalhe 404; reaplicação mantém 249/404, mesmos UUIDs/uma revisão, estado aprovado no
histórico e sequência editorial 3. O banco original permanece com **250** elegíveis.
A cópia e suas decisões herdadas não são somadas ao acervo/evidência de aprovação.

A primeira coleta de relatório desse ensaio falhou por stdout nativo Windows em
cp1252, embora as verificações HTTP de retirada/reaplicação já tivessem passado.
O harness temporário foi ajustado para stdout/stderr UTF-8; retomou a mesma cópia sem
duplicar a retirada e confirmou o relatório completo. Não se alterou o produto nem
se declarou PASS da coleta inicial. Cópia e dados originais foram preservados.

Concorrência, rollback integral, base/ABA, indisponibilidade, conflitos conhecidos e
upgrade preservando identidades/sessões são cobertos pela suíte real PG18.6: 112 testes
backend no clean build e também CI remoto. Fixtures ficam isoladas do acervo real.

## Regressões e empacotamento

- Backend: clean build **112 testes**, zero falhas/erros/skips; delta editorial final
  conferido novamente pelo teste de pacotes. CI do primeiro head publicado também PASS.
- Frontend: **73 testes/build**, seis browser reais OIDC/PG com fixtures e seis
  interface/offline PASS. Login desativado no ensaio de curadoria; regressão OIDC permanece
  a do provedor local, não smoke Google com credenciais externas.
- Python: **49 testes** PASS, zero skips, em Linux com repo somente leitura; inclui
  três negativos novos do verificador. Limitações da tentativa Windows anterior preservadas.
- OpenSpec: três changes estritos PASS; guard `spec-review-tests` remoto SUCCESS,
  sem certificar o gate cumulativo final.
- Aulas/assets/manifesto/CSV não alterados: 108 HTML/1757 referências/nove módulos/90
  resoluções. JAR sem fixtures/pacotes editoriais, bundle sem conteúdo corretivo identificado.
  Nenhum workflow, ruleset, permissão ou deploy foi relaxado.

CI inicial do PR: Application checks `38628302946`, guard `38628302941`; push
`38628295865`. Esses runs se referem ao head `96c663b`, não a futuros heads.
Conferir novamente o CI do head final antes de solicitar aprovação de merge.

Rollback operacional: desabilitar a consulta do banco de questões na configuração
aplicável e preservar os dados/versões; não executar downmigration nem apagar volume.
Os comandos existentes e suas confirmações permanecem em `docs/question-local-operations.md`.
Banco local não implica implantação da aplicação nem aprovação automática em outro destino.

## Revisão independente e limites

A revisão inicial SOL do head `3dc4389` não certifica automaticamente este delta.
Risco crítico mantido; o parecer SOL 6.1 xhigh e os snapshots efetivamente revalidados
estão em `final-review.md`. Este documento registra as verificações operacionais.
Quadro de tarefas registra implementação/verificação, não aprovação/integração de PR.
Gate cumulativo Astra medium, integração na épica geral, main e publicação da aplicação
permanecem separados. Cota/tokens indisponíveis no harness; não inventar medição.
