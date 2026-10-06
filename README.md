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

Edite os arquivos HTML, CSS e JavaScript e envie as alterações para a branch `main`. O GitHub Actions verifica os caminhos internos e publica o site no GitHub Pages automaticamente.

Preserve os nomes de arquivos e os caminhos relativos. Ao alterar teoria ou questões, confira também os gabaritos e as referências correspondentes. Veja `CONTEXTO_PARA_CONTINUAR_V3.md` para as orientações editoriais do material original.

Para verificar os links locais:

```sh
python3 scripts/check_links.py
```

## Revisão por branch e previews

Mantenha `main` como versão aprovada. Crie uma branch para cada melhoria e abra um Pull Request; faça merge apenas após aprovação.

O workflow desta branch publica a versão de teste em `previews/<nome-da-branch>-<hash>/`, com um aviso de que o conteúdo ainda não foi aprovado. O link aparece no resumo da execução do GitHub Actions. Os previews são públicos e não devem conter dados privados.

Durante uma publicação de preview, a raiz do site é montada exclusivamente a partir de `main`. A branch `pages-store` guarda os arquivos publicados e preserva os outros previews entre publicações; não edite essa branch como código-fonte. Todas as publicações usam a mesma fila para evitar sobreposições.

Até a infraestrutura ser aprovada e mesclada, branches de melhorias precisam partir desta branch para utilizar o workflow novo. A automação antiga de `main` ainda não preserva previews; não envie mudanças para `main` antes dessa aprovação.

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
