# Verificação da implementação — grupos 2–6

## Fonte e composição

Stack #143, trunk `epic/study-experience-redesign`. PRs #141 (navegação/início), #142 (composição), #144 (realização), #145 (aulas) e #146 (regressões/evidências), todos em rascunho. Proposta/protótipo #140 já integrados por autorização própria. Esta verificação não autoriza merges nem substitui revisão independente/gate cumulativo.

Risco crítico do conjunto: apresentação/ocultação do contexto privado e preservação de fila/comando idempotente ao navegar/praticar. Backend, contratos, migrações, acervo e workflows não foram alterados. Aulas completas usam o HTML público no endereço canônico, em iframe sem scripts; o leitor conserva a apresentação/rolagem do documento e oferece fallback ao original.

## Verificações do coordenador

| Verificação | Resultado |
| --- | --- |
| Frontend unitário | 171/171, incluindo navegação com resposta pendente, troca de conta oculta e cancelamento/aceite de prática da aula |
| Build TypeScript/Vite | Passou |
| Chromium geral | 11 passaram; cenário offline separado por exigir montagem do material |
| Material offline sem JavaScript | Passou: nove módulos/90 resoluções; empacotador conferiu108 HTMLs/1757 referências locais |
| Java/API/PostgreSQL/OIDC sintético | 24/24, incluindo posse, retomada/restart, expiração, CSRF, resultado e revisão |
| Variações numéricas reais | 5/5, incluindo retry, conflito, retomada e expiração |
| OpenSpec1.14.1 estrito | Passou |
| Pacote Docker: imagens/isolamento/runtime/recuperação | Fases1–7 da exportação limpa passaram no commit `cd2d8b332f1946544a7924ece429f8f4e6cea0df` |
| Navegador no pacote Docker | 6/6 após correção do teste em `5d6964e6b58e267b28ab4e45b5914037edee3dba`:51 URLs, leitura completa, MathML, figura/dez resoluções, prática sem POST, visitante/conta, retry/conflito/retomada e desktop/celular |

As duas primeiras execuções da nova verificação de leitura falharam no teste: âncora antiga `s5` incompatível com a versão didática construída; depois seletor global ambíguo entre aula oculta e checkbox. Corrigidos para verificar o final da aula/dez resoluções e restringir a asserção à composição ativa. Os quatro cenários existentes do pacote passaram nas duas execuções.

O reteste final repetiu somente o navegador. Todos os arquivos do contexto restrito e seu SHA256 foram conferidos iguais entre a imagem `cd2d8b3` e o teste corrigido `5d6964e`; contexto `2ce88f59000b4d56c5c9ed93576ce54326c8ab9ba634f994eb5e97e2dd1ce163`. Não se alega nova execução completa1–8 nesse reteste. CI do head atual deve ser conferido separadamente, sem transportar checks de outro commit.

Capturas reais de composição/realização/biblioteca/leitor em390px e1280/1440px foram conferidas e preservadas localmente para revisão; imagens sintéticas/ambientes descartáveis, sem dados do operador. Navegação por teclado, foco, Escape, estados vazios, critérios, figura real e ausência de corte lateral foram exercitados. Não constitui auditoria WCAG completa nem comprova Google real em produção.

## Preservações e limites

Aplicação do operador em8088 permaneceu UP com251 questões; seu PostgreSQL e suas sessões/tentativas não foram usados nos ensaios. Main/stable/Pages, publicação externa, arquivamento e integração não entram nesta verificação. Exportações, sessões sintéticas, diagnósticos e backups ficam ignorados/privados; nenhum dump é publicado.

Os erros iniciais da navegação e sua regressão positiva estão em `docs/study-navigation-regression.md`. Revisões SOL6.1xHigh, triagem, base/head/digests e limites de cota/tokens ficam nos relatórios próprios; gate cumulativo exige contexto independente do revisor do lote. Implementado/verificado, revisado e integrado são estados distintos.
