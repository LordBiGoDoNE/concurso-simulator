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
