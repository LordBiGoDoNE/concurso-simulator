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
