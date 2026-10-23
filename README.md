# concurso-simulator

Material de estudo para o Concurso Público de Limeira 01/2026, voltado aos cargos de Assistente Administrativo e Secretário de Escola.

**Acesse o site:** https://lordbigodone.github.io/concurso-simulator/

## Conteúdo

- 51 módulos organizados em cinco disciplinas.
- Teoria, exercícios e gabaritos em páginas separadas.
- 510 questões autorais, esquemas visuais e oficinas.
- Busca por assunto, plano de estudo, cronograma e fontes de referência.

Esta publicação preserva o conteúdo e o visual do material V3 original. Não é um site oficial do concurso. Confira sempre o edital, as retificações e a legislação vigente. As referências das questões não significam que elas foram elaboradas ou validadas pelos órgãos citados.

## Usar localmente

Abra `index.html` no navegador, mantendo as pastas de disciplinas e `assets` junto dele. O material funciona sem internet; referências externas precisam de conexão.

Para servir o site localmente, opcionalmente execute:

```sh
python3 -m http.server 8000
```

Depois acesse http://localhost:8000.

## Atualizar e publicar

Edite o material HTML, CSS e JavaScript por branches/PRs para `stable`. Push nessa branch publica o site estático no GitHub Pages. `main` recebe a aplicação completa e não publica no Pages; veja [a separação de publicação](docs/pages-stable.md).

Preserve os nomes de arquivos e os caminhos relativos. Ao alterar teoria ou questões, confira também os gabaritos e as referências correspondentes. Veja `CONTEXTO_PARA_CONTINUAR_V3.md` para as orientações editoriais do material original.

Para verificar os links locais:

```sh
python3 scripts/check_links.py
```

## Revisão por branch e previews

**Padrão do projeto:** uma spec usa `epic/<nome-da-spec>`; tarefas ou grupos coesos usam `task/<nome-da-spec>/<assunto>` e abrem PRs para a épica. Após revisão dos PRs menores, a épica abre PR para `main`. Regras e dependências estão em `AGENTS.md`.

Mantenha `stable` como versão aprovada do site estático. Crie uma branch para cada melhoria e abra um Pull Request; faça merge apenas após aprovação. A aplicação completa evolui em `main`.

Previews são **exclusivamente manuais**, não fazem parte do CI: Actions → Publish GitHub Pages → Run workflow. Execute o workflow de `stable` e informe a branch de material no campo `branch`. `main` e `pages-store` são recusadas. O preview fica em `previews/<nome-da-branch>-<hash>/`, com aviso de versão não aprovada; o link aparece no resumo do Actions. São públicos e não devem conter dados privados. Apenas push em `stable` publica produção automaticamente. O preview serve o material estático, não a aplicação Java/React.

Durante uma publicação de preview, a raiz do site e as ferramentas de montagem vêm exclusivamente de `stable`; a branch escolhida fornece somente o material. Isso mantém previews de revisões anteriores sem exigir atualizar seus scripts. A branch `pages-store` guarda os arquivos publicados e preserva os outros previews; não edite essa branch como código-fonte. Todas as publicações usam a mesma fila para evitar sobreposições.

O ambiente `github-pages` autoriza somente a referência `stable`. A cópia do workflow em `main` permite sua descoberta no Actions, mas não publica a partir dessa referência.

Previews ficam disponíveis após o merge ou fechamento do PR até serem removidos explicitamente. A limpeza automática pode ser adicionada depois; o fechamento de PRs não executa código de forks nesta configuração.

## Edição didática de Matemática

Os nove módulos de Matemática recebem aulas guiadas, desenhos/fluxos, fórmulas com equivalente em linha e dez resoluções recolhidas por módulo. Os três módulos de Raciocínio Lógico não fazem parte desta revisão.

O módulo M04 é escrito diretamente em HTML. Nos demais, `scripts/math_content.py` centraliza aulas e resoluções; `scripts/enrich_math.py` gera HTML estático completo na publicação, preservando os arquivos V3 como base. A mesma resolução alimenta o botão na questão e o gabarito separado, evitando divergências. O site publicado funciona sem JavaScript, CDN ou conexão para ler o material.

**Para avaliar a edição enriquecida localmente**, gere uma cópia em uma pasta nova (os HTML-base desses oito módulos não incluem a revisão quando abertos diretamente):

```sh
python3 scripts/build_pages.py --branch main --stored /tmp/opencode/sem-previews --output /tmp/opencode/concurso-estudo
python3 -m http.server 8000 --directory /tmp/opencode/concurso-estudo
```

Abra http://localhost:8000. Também é possível abrir o `index.html` dessa pasta offline. Use uma pasta de saída vazia; não use a raiz do repositório como saída.

### Correções editoriais

- **M05-Q04:** a alternativa D era `7,0`, igual à correta B (`7`). Passou a `8`, mantendo B como única correta.
- **M07-Q06:** a alternativa D dizia `ambas A e B`, mas a queda de 50 para 40 é de 10 unidades (B) e 20% (C). Agora D diz `ambas B e C`, mantendo o gabarito D.

Além dos caminhos internos, a publicação confere as 90 respostas recolhidas, a correspondência com os gabaritos, a preservação das outras alternativas e os links com âncoras. Esta revisão não constitui auditoria integral do edital.

### Fontes sem caixas repetitivas

`scripts/compact_sources.py` troca as caixas “Fonte da questão” por **Fonte: nome real do material**, com link direto para o artigo, documento ou portal já listado nas referências do módulo. Se houver várias referências, mostra **Fontes:** e os nomes correspondentes, sem inventar uma associação específica por questão. Aplica-se às disciplinas e aos gabaritos na publicação. Os links são de apoio conceitual, não uma atribuição de autoria da questão à instituição referenciada. A origem autoral continua registrada nos arquivos-base e em `catalogo_fontes_questoes.csv`.
