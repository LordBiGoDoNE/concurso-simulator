# Revisão independente da infraestrutura Pages — #135/#136

## Escopo e cobertura

Mudança completa de infraestrutura em dois PRs idênticos, não revisão parcial de
produto nem gate final da reformulação. Base `f5f30d64e0c968c4530504929c6d0d413f13eefc`, head
`bc22cb94ac57c812525360e5df17e4a216dba0f4`, digest `c33510cbec5ef09716edc2bd0a95385cce70254efeaa1e31cb78487b16264c71`.
SOL6.1xHigh em contexto novo `/root/pages_stable_infra_review`; risco crítico por
controlar publicação, permissões, credenciais e fronteira de material público.

Sete arquivos examinados integralmente: workflow de publicação e CI, builder,
testes, README, AGENTS e procedimento stable. Requisitos, integração, segurança,
manutenção e compatibilidade local/CI/Docker confrontados. Backend/DB não alterados.

## Achados e triagem

- PAGES-B1 bloqueador: scripts antigos de previews incompatíveis com novo argumento.
  Corrigido em4cb37c5: material separado, ferramentas aprovadas de stable.
- PAGES-R1 recomendado: instruções antigas do README. Corrigido em4cb37c5.
- PAGES-B2 bloqueador: links podiam copiar dados externos ao checkout público.
  Corrigido embc22cb9: lstat recusa links/reparse/arquivos especiais antes da cópia,
  incluindo previews armazenados; regressões com sentinelas e FIFO.

Os três foram confirmados corrigidos na segunda revalidação integral. Nenhum novo
achado ou bloqueador pendente identificado.

## Evidências e limitações

Revisor apenas leu código/Git e calculou hashes em memória: sem execução de testes,
build, scripts, rede, DB, escrita ou publicação. Não consultou estados privados ou
relatórios anteriores. Configuração efetiva de ambiente/Pages e operação de gh
serão conferidas pelo coordenador, sem inventar evidência remota nesta revisão.

Coordenador: Linux11/11 testes aprovados; Windows6 passaram/5 skips de plataforma
(symlink sem privilégio e FIFO POSIX). Build de produção108páginas/1757refs e ensaio
com material antigo eecda47:216páginas/3622refs aprovados. Arquivos públicos comparados
por Git NUL-delimited com nomes Unicode, sem mudança no material. CI no headbc22cb9:
[39895766901](https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/39895766901),
[39895767742](https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/39895767742).

Proteção de arquivos pressupõe fontes imutáveis durante o job, não isolamento contra
processo local concorrente. Nenhuma contratação ou publicação da stack incluída.
Cota5h/semanal indisponível antes/depois nas três fases; tokens não fornecidos;
sem isolamento ou atribuição de consumo. Observações mínimas privadas validadas e
comparadas. Houve atualizações operacionais do coordenador; observação após a
fase inicial inclui atraso de triagem. Intervalos observados, não custo:
initial: 403 s, revalidation-1: 238 s, revalidation-2: 178 s.

## Revalidação

Inicial89b493c: B1 eR1. Revalidação4cb37c5: anteriores corrigidos, B2 encontrado.
Segunda revalidaçãobc22cb9: conjunto completo e delta relidos, três achados corrigidos,
digest recalculado. Após o head revisado, somente estes dois arquivos de evidência
são acrescentados e excluídos do digest. Status operacional posterior fica na issue#134.
A migração será executada na ordem solicitada pelo usuário; gate cumulativo da
aplicação será revalidado separadamente após a sincronização de main/épica.
