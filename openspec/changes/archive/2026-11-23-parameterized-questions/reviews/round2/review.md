# Revisão do lote — grupos completos 3 e 4

**Concluída, sem achados abertos após duas revalidações.** Risco crítico: persistência,
compatibilidade, atomicidade/concorrência e integridade da aprovação/captura editorial.
Revisor `openai/gpt-6.1-sol#xhigh`, contexto inicial novo `/root/parameterized_round2_sol`;
mesma sessão nas revalidações, sem outro modelo. Somente leitura; testes e CI abaixo
foram executados/conferidos pelo coordenador. Não é gate cumulativo final nem aprovação de merge.

## Fontes efetivamente revisados

| PR | Base SHA | Head de fonte revisado | Digest canônico |
|---|---|---|---|
| [#94](https://github.com/LordBiGoDoNE/concurso-simulator/pull/94) | `6781e9cbd7e09b2831131259a5f84a10f8d259ce` | `b84d169874603697b162ae2cf0c619c0dc818f87` | `7db0e3435157e4b221442632832298dcd09826746bda028dc5a320bd3ab7bc7a` |
| [#95](https://github.com/LordBiGoDoNE/concurso-simulator/pull/95) | `b84d169874603697b162ae2cf0c619c0dc818f87` | `ddc37f1932d818a9ab94a001aaca2fb3b4d92588` | `9f0c1cd15575dfc326ec7bcbd61763a3a4ab25d8cb8f0dfabc092e950f52fefc` |
| [#97](https://github.com/LordBiGoDoNE/concurso-simulator/pull/97) | `6781e9cbd7e09b2831131259a5f84a10f8d259ce` | `062bf0f7a973552c0599ff0d0eed2af1eafd8f1c` | `d0eaa1982d7bb74c450e44bd1d70dd812927fe1b9fa55694cde37318cef411af` |
| [#98](https://github.com/LordBiGoDoNE/concurso-simulator/pull/98) | `ddc37f1932d818a9ab94a001aaca2fb3b4d92588` | `fd54499bfd885c89877e21f45a69eb2dc89842f3` | `c2110febb80672e86254faac6e856edbd8710113df71f3e07137e144134e7e4f` |

Combinação **somente local**: `6781e9cbd7e09b2831131259a5f84a10f8d259ce` →
`52abdfa00a6ad5a04d7b43da1989fb3db7ace1db`, digest `76e71dce4b1e4a5d847fa0d1deb933d21f3467d86d72c60d8bf26b434230ff63`.
Estes dois relatórios são evidência posterior aos heads examinados, sem mudança de
fonte; não se declaram incluídos no digest analisado. Bases, composição, heads e
histórico das três fases estão no [registro JSON](review.json).

## Achados e triagem

1. **P5-R2-001 — corrigido.** Conteúdo de uma tupla aceitava parâmetros/digest válidos
   de outra. Dois testes reproduziram a falha antes da correção. `a919597` vincula
   corpo e correção reais ao digest na construção/restauração e em V8, com rollback
   integral por SQL direto e recusa de upgrade divergente. Fechado na primeira
   revalidação e preservado na segunda.
2. **P5-R2-002 — corrigido.** Referências, autoria e cargos reais podiam divergir da
   definição aprovada com fingerprint recalculado e corpo intacto. Três regressões
   de construtor e uma SQL confirmaram a falha. `fd54499` reconstrói a definição dos
   dados capturados e V9 compara metadados com a versão imutável. Construção,
   restauração, commit SQL e upgrade adulterado são rejeitados. Fechado na segunda
   revalidação; nenhum achado novo.

O revisor conferiu os diffs individuais e a combinação, seleção/materialização,
projeções privadas/públicas, hashes e codecs V5, FKs/constraints, transações/rollback,
concorrência, imutabilidade, expiração e atualização de schema. Conferiu também as
111 tuplas (64/20/27), corretas/distratores/resoluções, três definições/manifestos,
seleção exata, fingerprints-base e os 104 arquivos-fonte. As revalidações preservam
a árvore editorial e os 111 hashes. A restauração não usa gerador/oráculo/catálogo.

## Verificação e conteúdo local

- Build Windows após `fd54499`: 204 casos, 203 executados sem falhas/erros, um symlink
  omitido pela permissão do sistema.
- [Backend Linux](https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/38831945208/job/116752473824):
  **204 PASSED**, nenhum omitido ou falho. Backend, frontend, material, identidade no
  navegador e `spec-review-tests` passaram nos quatro heads de fonte; links no JSON.
- [Guard editorial #97](https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/38828381344/job/116741121824):
  62 testes OK. Oito regressões editoriais passaram também localmente. Auditor: 111/111.
- Preview offline preservou conteúdo/digests das 111 variantes e três manifestos;
  ordem do conjunto de cargos desconsiderada. OpenSpec 5/5 e whitespace passaram.
- Aprovação editorial específica do usuário executada somente na cópia local:
  M04-Q05 e três famílias v1. Seleção SHA-256
  `879e9fa5d43d885a6125c9cbccc8590b373abc8d2321929d370159426ef1dba3`.
  O segundo clone de retirada não muda o destino aprovado; variantes não ampliam
  identidades-base. Registros e limitações em #97; não é aprovação concedida por IA/CI.

## Tamanho e limites

- #94: +247/−12; volume gerado identificado +0/−0, caminhos no JSON.
- #95: +589/−25; volume gerado identificado +151/−0, caminhos no JSON.
- #97: +14043/−4; volume gerado identificado +12920/−0, caminhos no JSON.
- #98: +358/−21; volume gerado identificado +0/−0, caminhos no JSON.

Os volumes gerados são identificados separadamente, sem fingir revisão apenas das
linhas manuais. O revisor percorreu também seu conteúdo. O pacote editorial coeso
reúne bases/seleção/decisões/auditor/docs, além das saídas serializadas.

Somente leitura pelo revisor, sem execução de testes/build/rede/DB e sem autenticar
diretamente as observações locais. O conjunto combinado não teve CI remoto próprio;
backend é o fonte de #98 e auditor/editorial o de #97. Metadados editoriais não mudam
Java/frontend. Cota/tokens/observações privadas ficam fora do Git e são informados
separadamente ao usuário, sem estimativa fictícia ou conversão em cobrança.

**16/24 implementadas/verificadas; 8/24 aprovadas/integradas.** Grupos 5/6 e gate final
continuam pendentes. Stack #96: #94 → #95 → #98, trunk da spec; #97 independente.
Nenhum merge, sync pós-merge, retargeting, fechamento, publicação de site, arquivamento ou mudança de main.
Mudanças futuras de base/diff exigem conferência e revalidação antes de integração.
