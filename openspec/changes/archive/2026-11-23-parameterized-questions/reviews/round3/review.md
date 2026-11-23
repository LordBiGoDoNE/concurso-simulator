# Revisão do lote final — grupos 5 e 6

Concluída e revalidada, sem achados abertos. Risco crítico: HTTP/CSRF/sessão,
idempotência, privacidade, compatibilidade de dados/rollback e CI. Revisor independente
`openai/gpt-6.1-sol#xhigh`, contexto novo `/root/parameterized_round3_sol`; mesma
sessão na revalidação. Não substitui o gate cumulativo 6.4 nem aprova merges.

## Fontes revisados

| Camada | Base | Head | Digest canônico |
|---|---|---|---|
| #99 | `542f9ac1733f1338048b4851fbd2e9a07a5c038d` | `f413165e761dccf42e9840ef6588988f879986b9` | `34cfabf6ed6c0ae51c2c5c337489448f42a815a482e50538fbc50f6e13979c92` |
| #100 | `f413165e761dccf42e9840ef6588988f879986b9` | `700603ed837469889d130a015d95cd4b691c0d72` | `b7fb0aff53133bc0f81eef49bdf0c6e0010461bd76e7123e5d8fbad2dc1b12da` |
| Verificação integrada, PR pendente | `700603ed837469889d130a015d95cd4b691c0d72` | `61b6d736ec374895377c05217fa2b7fb1e8bd533` | `171230b54a0382ef5d776d3d01672413cf5c1633a506923d4ff4ed61ca5e847b` |

Combinação `542f9ac1733f1338048b4851fbd2e9a07a5c038d` →
`61b6d736ec374895377c05217fa2b7fb1e8bd533`, SHA-256
`852af3bf84c46377f30c901ab8abf29d227a45c7fc2f00a7cc9fd0fbbbbd023d`.
O revisor recalculou os quatro hashes com a renderização canônica do Git e conferiu
ancestralidade. Relatórios são evidência posterior, sem mudança dos fontes revisados.
Composição inicial e resultado estruturado estão em [review.json](review.json).

## Achado e triagem

**PQ-R3-001, blocking nos prefixos iniciais, corrigido.** A tradução de
IllegalStateException pelo repository caía no handler 503 de DataAccessException;
a correção 500 existia somente na última camada. `f413165` coloca advice, teste e
documentação na camada HTTP. Revalidação confirmou o contrato 500 internal_error,
rollback das quatro tabelas e ausência de dependência da camada superior.
Nenhum novo achado blocking/recommended/optional.

## Cobertura e verificação

Revisados cada diff e combinação, DTO/OpenAPI/parsing estrito e compatibilidade,
CSRF/posse, fronteiras do ADR 0001, projeções e artefatos privados, checkbox/acessibilidade,
retry/commandId/troca de contexto, browser/fixtures, V5 → V9 e rollback compatível,
CI sem deploy. A matriz e as execuções do coordenador estão em
[verification.md](../../verification.md), que não foi fornecido ao revisor.

Coordenador executou: backend 207 casos/206 sem falhas e 1 symlink omitido Windows;
frontend 110; navegador 5 parametrizados + 14 regressões + 6 aplicação/material offline;
ensaio operacional com JAR V5 real, PostgreSQL isolado e geração desligada;
exportação limpa 7a55651 com builds/scanner/material/111 variantes; nova consulta
editorial somente leitura, sem alteração das três famílias e111 saídas aprovadas.

O revisor realizou somente consultas locais; não executou testes/build/rede/DB,
não atestou checks remotos ou decisões editoriais, nem leu relatórios anteriores.
Sentinelas/contratos não provam ausência de toda dica semântica; material legado
continua fora da promessa de sigilo da aplicação. Cota e tokens indisponíveis;
observações mínimas privadas permanecem fora do Git e não são consumo zero.

## Estado de integração

23/24 tarefas implementadas/verificadas,16/24 integradas. Terceira camada ainda
aguarda publicação com escopo workflow; checks e composição remota serão conferidos
antes da aprovação explícita de cada PR. Após integração autorizada das tarefas,
6.4 exige novo contexto independente para gate da spec real contra a épica geral.
Sem autorização de main, deploy ou arquivamento.

## Checks remotos conferidos pelo coordenador

Após revalidação, todos os checks reportados passaram nos heads de #99 `f413165`
e #100 `700603e`: backend, frontend, study-material, identity-browser e
spec-review-tests. Referências: [#99](https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/38845178979),
[#100](https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/38845177113).
Os checks da terceira camada continuam pendentes de publicação; nenhum merge autorizado.
