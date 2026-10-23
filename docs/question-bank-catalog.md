# Catálogo inicial do banco de questões

Tarefa 1.1 de [question-bank](../openspec/changes/question-bank/tasks.md), issue #30.
Escopo fixo: **Limeira 01/2026**, ID `limeira-01-2026`; cargos de estudo
`assistente-administrativo` (AA) e `secretario-de-escola` (SE). Não são roles de
segurança: login Google não confere privilégios editoriais.

## IDs e responsabilidade

`questionbank.domain` usa Java puro: `Discipline`, `Topic`, `TargetRole` e
`QuestionClassification`. As cinco disciplinas têm os IDs da matriz abaixo. Cada
assunto tem exatamente uma disciplina e preserva o ID do módulo do legado; o nome
da constante de `Topic` é esse ID estável, **não deve ser renomeado** ao mudar o título.
IDs são exatos/sensíveis a maiúsculas, sem trim, aliases ou fallback. Não persistir
ordinais de enums. Os dois cargos também têm IDs explícitos, independentes de rótulos.

Uma classificação contém disciplina, assunto e conjunto imutável não vazio de cargos.
Ela rejeita assunto de outra disciplina, cargo externo/ausente e valores nulos;
repetições de um cargo na entrada resultam em uma associação. Uma revisão futura
usará **uma** classificação, inclusive quando aplicável aos dois cargos, não uma
cópia da questão por cargo. O teste desta tarefa cobre associação/imutabilidade;
identidade/versão e contagem por filtros serão testadas com questão e consulta nas
tarefas 1.2 e 4, ainda não implementadas. Não há UseCase/Service, endpoint ou schema
artificial para expor esses valores nesta tarefa. ArchUnit existente verifica o núcleo.

## Matriz rastreável

Cada ID abaixo aponta para o mesmo `id` de [manifesto_v3.json](../manifesto_v3.json),
que fornece título completo, caminho da aula, gabarito e referências originais.
O mapeamento é **explícito**, não derivado do prefixo, de seções de HTML ou da pasta
em produção. O teste compara todos os 51 IDs/títulos/pastas à matriz e ao enum,
sem modificar o manifesto. Há 10/12/10/8/11 assuntos nas cinco disciplinas.

AA + SE e os cargos específicos indicam **destino candidato do material legado**,
nunca aplicação automática nem checklist aprovado de cada questão. O pacote e a
revisão editorial precisam conferir a associação concreta; ambos os cargos do escopo
podem ser associados quando justificado, sem uma proibição artificial por disciplina.
Uma referência temática ou fundamento legal no legado não comprova vigência/correção.

| Módulo / assunto ID | Disciplina ID | Assunto | Destino candidato |
| --- | --- | --- | --- |
| P01 | portugues | Interpretação de Textos | AA + SE |
| P02 | portugues | Sinônimos, Antônimos e Relações de Sentido | AA + SE |
| P03 | portugues | Sentido Próprio e Figurado | AA + SE |
| P04 | portugues | Pontuação | AA + SE |
| P05 | portugues | Ortografia e Acentuação | AA + SE |
| P06 | portugues | Classes de Palavras | AA + SE |
| P07 | portugues | Concordância Verbal e Nominal | AA + SE |
| P08 | portugues | Regência Verbal e Nominal | AA + SE |
| P09 | portugues | Colocação Pronominal | AA + SE |
| P10 | portugues | Crase | AA + SE |
| M01 | matematica-raciocinio | Operações com Números Reais | AA + SE |
| M02 | matematica-raciocinio | MMC e MDC | AA + SE |
| M03 | matematica-raciocinio | Razão, Proporção e Regra de Três | AA + SE |
| M04 | matematica-raciocinio | Porcentagem e Juros Simples | AA + SE |
| M05 | matematica-raciocinio | Média Aritmética e Ponderada | AA + SE |
| M06 | matematica-raciocinio | Equações e Sistemas do 1º Grau | AA + SE |
| M07 | matematica-raciocinio | Tabelas e Gráficos | AA + SE |
| M08 | matematica-raciocinio | Sistemas de Medidas | AA + SE |
| M09 | matematica-raciocinio | Geometria- Perímetro, Área, Volume, Ângulos e Pitágoras | AA + SE |
| M10 | matematica-raciocinio | Relações Lógicas, Dedução e Sequências | AA + SE |
| M11 | matematica-raciocinio | Estruturas e Argumentação Lógica | AA + SE |
| M12 | matematica-raciocinio | Diagramas Lógicos e Situações-Problema | AA + SE |
| I01 | informatica | Windows - Arquivos, Pastas e Diretórios | AA + SE |
| I02 | informatica | Windows - Atalhos, Área de Trabalho e Área de Transferência | AA + SE |
| I03 | informatica | Word - Edição e Formatação | AA + SE |
| I04 | informatica | Word - Tabelas, Páginas, Índices e Objetos | AA + SE |
| I05 | informatica | Excel - Células, Linhas, Colunas e Referências | AA + SE |
| I06 | informatica | Excel - Fórmulas e Funções | AA + SE |
| I07 | informatica | Excel - Tabelas, Gráficos, Classificação e Dados | AA + SE |
| I08 | informatica | Excel - Macros | AA + SE |
| I09 | informatica | PowerPoint - Apresentações, Edição, Animações e Transições | AA + SE |
| I10 | informatica | E-mail, Internet, URL, Links e Busca | AA + SE |
| A01 | conhecimentos-administrativos | Princípios da Administração Pública | AA |
| A02 | conhecimentos-administrativos | Rotinas Administrativas e Redação Técnica | AA |
| A03 | conhecimentos-administrativos | Processos Administrativos- Autuação, Registro e Encaminhamento | AA |
| A04 | conhecimentos-administrativos | Gestão de Materiais e Suprimentos | AA |
| A05 | conhecimentos-administrativos | Arquivística e Gestão Documental | AA |
| A06 | conhecimentos-administrativos | Atendimento ao Público e Comunicação | AA |
| A07 | conhecimentos-administrativos | Ética e Sigilo Profissional | AA |
| A08 | conhecimentos-administrativos | Orçamento, Cálculos e Prestação de Contas | AA |
| S01 | conhecimentos-escolares | LDB - Lei 9.394-1996 | SE |
| S02 | conhecimentos-escolares | Escrituração Escolar | SE |
| S03 | conhecimentos-escolares | Registros Individuais, Ficha e Histórico Escolar | SE |
| S04 | conhecimentos-escolares | Matrícula, Frequência e Transferência | SE |
| S05 | conhecimentos-escolares | Protocolo e Arquivo Escolar | SE |
| S06 | conhecimentos-escolares | Redação Oficial - Atas, Ofícios e Requerimentos | SE |
| S07 | conhecimentos-escolares | Constituição Federal - Educação - arts. 205 a 214 | SE |
| S08 | conhecimentos-escolares | ECA - arts. 1 a 6 e 53 a 69 | SE |
| S09 | conhecimentos-escolares | Atendimento ao Público e Comunicação Escolar | SE |
| S10 | conhecimentos-escolares | Relações Humanas, Ética, Sigilo e Assiduidade | SE |
| S11 | conhecimentos-escolares | Censo Escolar e Informações Educacionais | SE |

## Verificação e limites

No diretório `backend`, com JDK 25: `./gradlew test --tests '*QuestionCatalogTests'
--tests '*ArchitectureTests'`. Não requer banco para esses testes. O build completo
continua exigindo Docker/PostgreSQL isolado, conforme [README](../backend/README.md).
O catálogo não lê HTML em produção e não publica as questões nem gabaritos. Nenhum
item real foi importado/revisado/aprovado, nem a meta editorial declarada atendida.
50–100 por disciplina é meta de curadoria, não limite do enum ou do armazenamento.
