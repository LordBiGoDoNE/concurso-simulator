# Extração local e formato question-package/v1

`python3 scripts/extract_questions.py --root . > /tmp/opencode/questions.json`
produz candidatos, inventário/pendências e suspeitas; não escreve nas aulas, consulta
a rede, executa Python do pacote, abre banco ou aprova conteúdo. Saída fica fora do Git.

## Contrato e identidade

Formato JSON `question-package/v1`: `catalog`, `sourceFiles` (path/SHA-256),
`inventory` (todos os 510 IDs/status/pendência legal), `items`, `issues` e
`possibleDuplicates`. Cada item tem `origin` (namespace `legacy-v3` + chave),
`content`, `fingerprint` e `expectedLatestRevision` (null para novo item; alterações
exigem UUID da base esperada). Catálogo contém exatamente os 51 módulos aprovados.
CSV usa UTF-8 com BOM; joins conferem ID, arquivo e número. UUIDs de alternativas
usam UUIDv5/NAMESPACE_URL de `concurso-simulator:legacy-v3:<chave>:option:<índice>`.
Questão/revisão recebem UUIDs persistidos no backend, não letras de gabarito.

Fingerprint usa a canonização binária UTF-8 v1 de `QuestionContent`, incluindo listas,
classificação, fontes, autoria, caminhos/hashes e recursos; JSON não é a canonização.
Sem normalização Unicode ou equivalência apenas por enunciado. Referências temáticas
são nomeadas, não autoria/validação oficial; fundamento legal do CSV fica como pendência,
sem fabricar data de conferência. O backend deve recomputar o fingerprint ao ler o pacote.

## Conversão e allowlist

Ler somente manifesto/CSV e arquivos manifestados nas cinco pastas de disciplinas;
recursos somente em `assets/`. Caminhos canônicos relativos, tamanho finito e sem
symlinks. O snapshot inclui hashes dos bytes lidos. Raiz é uma árvore local confiável
e deve permanecer imóvel durante extração/aplicação; essas verificações não protegem
contra um processo privilegiado trocando diretórios simultaneamente.

Selecionar blocos por `qN`, não copiar aula/esquemas/soluções inline adjacentes.
Gabarito corresponde ao mesmo ID e cabeçalho inequívoco. Excluir somente rótulo numérico,
aside de proveniência e navegação conhecida. Markup aceito vira **texto**, nunca HTML
executável: parágrafos/listas, marcadores de ênfase, Unicode e fórmulas MathML restritas
(fração/expoente/raiz representados textualmente). Scripts/eventos/atributos desconhecidos,
marcações de correção em enunciado/opções e conversão incompleta geram pendência.
Imagens PNG/JPEG/SVG têm hash e descrição; SVG restrito sem scripts/entidades/referências
ativas. Recursos não são publicados automaticamente por essa ferramenta.

O inventário mantém inclusive inválidos; seleção futura não implica apagar os demais.
Suspeitas por texto equivalente são apenas alertas: nenhuma fusão/exclusão automática.
Validação estrutural é distinta de revisão editorial de resposta/distratores/resolução.
Fixtures e testes não aprovam acervo real; grupo 6 permanece pendente.

Snapshot observado nesta implementação: 510 itens inventariados, 509 candidatos e
uma pendência estrutural em `P05-Q01` (alternativas repetidas), mantida no relatório.
Há seis grupos de possível duplicação textual e 30 fundamentos legais a conferir.
Não corrigimos/aprovamos esses itens para completar contagens; aplicação integral
de seleção inválida deve ser recusada. Curadoria permanece no grupo 6.
