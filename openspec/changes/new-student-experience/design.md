# Design

## Context

A aplicação já mantém realizações montadas ao navegar e concentra gravações em
`useAttemptRun`. A identidade invalida conteúdo privado mesmo em outra área.
A nova navegação altera apenas o contexto público; não remonta esses executores.

## Goals / Non-Goals

**Goals:** manter contexto público restaurável sem reinicializar a realização privada.
**Non-Goals:** persistir respostas na URL ou criar uma segunda máquina de execução.

## Decisions

- Catálogo local existente fornece cargos, disciplinas e51 aulas. A associação de
  aulas segue a regra existente: comuns aos dois cargos e disciplinas específicas.
- `StudyRoute` centraliza área, cargo, disciplina, busca e aula. Nenhum desses
  valores autoriza acesso privado. URLs nunca recebem IDs de tentativas, respostas,
  credenciais ou conteúdo de operações.
- Rotas: `#/inicio`, `#/aulas`, `#/aulas/P01`, `#/simulados`, `#/praticas`,
  `#/questoes`. Parâmetros: `cargo`, `disciplina`, `busca`; fora de Aulas, `aula`
  conserva a seleção pública ao visitar outra área. Busca limitada a160 caracteres.
- `pushState` registra transições; busca substitui a entrada atual para não criar
  um passo por tecla. `popstate` e `hashchange` restauram a mesma representação.
  Canonicalização preserva pathname/query, inclusive o retorno de autenticação.
- Valores desconhecidos/duplicados/incompatíveis são descartados com aviso. Cargo
  conhecido não muda silenciosamente para acomodar aula de outro cargo.
- Área pública não causa POST. Somente o clique explícito de prática chama o
  mecanismo já existente, com comando idempotente e confirmação de substituição.
- Disponibilidade usa GET existente por assunto/cargo. O cliente valida os quatro
  campos exatos, correspondência dos IDs, quantidade inteira não negativa e máximo5.
  Quantidade disponível pode superar5; o início continua limitado a5 pelo contrato.
- Consulta é abortada ao trocar contexto/sair da área. Resposta obsoleta não libera
  botão de outro cargo/assunto. Falha mantém retry; zero confirmado mantém aula.
- Consulta de disponibilidade independe da consulta de identidade; iniciar depende
  da identidade verificada. Isso evita carregamento eterno quando a API está fora.
- Atualização restaura somente contexto público. Realizações continuam pelos
  mecanismos atuais de histórico; não prometer persistência de escolhas pendentes.
- Refinamento solicitado pelo usuário em 2026-11-08: prática no final da aula,
  em painel nativo recolhível. Abrir/recolher não inicia nem encerra a atividade;
  Começar prática faz o POST explícito, com carregamento e questões abaixo do
  botão, sem navegar para outra área. O resultado permanece nesse painel.
- A única instância de `LessonPracticeWorkspace` continua montada no App, depois
  do leitor e antes da navegação entre aulas. O contexto inline controla a
  apresentação; fechar o painel ou mudar aula/cargo apenas oculta a realização
  incompatível, sem remontar o executor ou cancelar sua fila. A área Práticas
  continua disponível para a realização atual e o histórico da conta.
- Disponibilidade pública continua por GET; o payload das questões só vem após
  início explícito. Criação pendente bloqueia novos comandos; falha mantém o retry
  idempotente existente. Troca de identidade remonta o contexto privado pela chave
  da identidade, inclusive quando a aula pública está sendo exibida.
- Refinamento de leitura solicitado em 2026-11-08: o índice fica antes do título
  da aula, com cabeçalho Nesta Disciplina - nome da disciplina e opções
  distribuídas em grade responsiva. O leitor usa
  uma única coluna em toda a largura disponível; a biblioteca mantém seu layout
  próprio. Identificação da aula atual, seleção e histórico usam as mesmas rotas.
- Refinamento nativo autorizado em 2026-11-08 substitui o iframe e sua medição.
  As 51 aulas são dados públicos compilados, carregados individualmente do próprio
  bundle e renderizados por elementos React conhecidos. Não há HTML bruto, CSS
  legado ou busca de documentos antigos em runtime. O título pertence ao layout.
- `compile_native_lessons.py` seleciona teoria, exemplos resolvidos, tabelas,
  esquemas e fontes. Reutiliza o enriquecimento didático existente de Matemática;
  exclui navegação/cabeçalhos/rodapés e exercícios/resoluções do banco antigo.
  A seleção não altera fontes estáticas ou elegibilidade/correção das questões.
- Fontes e documentos têm hashes SHA-256 normalizados em LF no manifesto gerado.
  `--check` e a regressão Python detectam divergências; geração é uma etapa de
  autoria, sem Python necessário para servir ou compilar o frontend já preparado.
- A compilação cria seu diretório temporário mesmo quando chamada diretamente,
  sem exigir um build anterior do backend ou a execução prévia da CLI. A regressão
  reproduz a seleção numa cópia das fontes sem diretório de build.
- Orientações legadas sobre gabarito em arquivo separado são adaptadas somente
  na compilação nativa: apontam para Pratique esta aula ao final da leitura e
  explicam que a correção aparece após encerrar. Os HTMLs estáticos permanecem
  preservados; a orientação não promete dez questões nem disponibilidade de acervo.
- SVG e MathML são árvores de elementos permitidos, com IDs locais por aula e
  figura. Marcas de edição são removidas dos esquemas; geometria e explicação
  didática permanecem. Esquemas largos e tabelas oferecem rolagem própria com
  teclado; a página preserva uma única rolagem vertical e largura responsiva.
- Âncoras do sumário focam/rolam a seção sem alterar o hash da navegação.
  Fontes externas abrem por ação explícita. Atalhos de revisão abrem a aula nativa
  com cargo compatível. Na revisão dos 51 assuntos, aulas de outro cargo abrem
  em exploração livre, sem mudar o cargo da tentativa atual. Falha do documento oferece retry; trocar aula cancela e
  invalida respostas anteriores. O material local continua independente da API.

### Alternatives considered

Rotas por pathname exigiriam configuração adicional do proxy; hash mantém o contrato
de implantação. Uma biblioteca de roteamento adicionaria dependência sem necessidade
para cinco áreas. Estado apenas local não atenderia atualização e compartilhamento.
Recriar as realizações a cada rota simplificaria renderização, mas descartaria filas.
Consultar disponibilidade somente após POST manteria o primeiro contato confuso com
acervo vazio; a consulta antecipada melhora a orientação sem substituir a validação do servidor.

## Risks / Trade-offs

Risco de implementação: crítico, conservadoramente, porque navegação toca contexto
que convive com respostas privadas em andamento. Cobrir troca de identidade, filas,
respostas tardias e navegação por histórico. Nenhuma regra de posse, transação,
correção ou separação entre simulados e práticas será alterada.

## Migration Plan

Nenhuma migração de banco ou dependência. A futura publicação troca o bundle do
frontend, preservando API e material. Rollback usa o bundle anterior; links hash
novos serão ignorados pelo bundle antigo, sem alterar tentativas ou dados.

## Delivery

Protótipo navegável documental em `design/prototypes/new-student-experience/`.
Validar implementação local antes de revisão independente SOL6.1xHigh, conforme
AGENTS.md. Revisão de lote e gate cumulativo final são distintos; nenhum está
certificado por esta documentação ou pelos testes. Integração/publicação dependem
das respectivas autorizações.
