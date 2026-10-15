# Tentativas e variações numéricas

A opção interna `Composition.useParameterizedQuestions` é false por padrão. A API e
a interface só a expõem no grupo 5. A seleção continua por identidade-base, uniforme
sem reposição: o número de tuplas não aumenta a chance de selecionar uma questão.
Depois da seleção, cada base com template elegível recebe uma única tupla sorteada
uniformemente. Bases sem template permanecem fixas; divergência de uma captura
declarada elegível aborta a operação inteira, sem fallback.

`AttemptMaterializer` verifica base/revisão/fingerprint/classificação, gera e confere
o manifesto antes de atribuir UUIDs opacos e embaralhar alternativas. A fonte tipada
contém revisão/fingerprint da base, versão/digests/gerador/tupla e fingerprint do
conteúdo final. Não representa uma nova revisão do banco. O mapa legado de parâmetros
é apenas uma projeção validada dessa captura, nunca input livre.

A fronteira aprovada acrescenta dependência do núcleo de simulados nos VOs e funções
puros de `parameterization.domain`. ArchUnit permite somente esse módulo de domínio;
continua proibindo infraestrutura, aplicação de parametrização e dependências reversas.

Leitura, resposta, conclusão, replay e histórico usam somente o conteúdo congelado.
Não consultam templates, elegibilidade ou geradores. A política `uniform-v1`, posse,
expiração de visitante em 24h e acesso exclusivo da conta permanecem vigentes.
Testes percorrem os 111 índices do sorteio, mistura/fixas, ausência e divergência,
replay sem nova captura, correção/histórico e rejeição de fonte incompleta.

O hash fixo mantém exatamente a canonização V5. True usa o discriminador
`parameterized-create/v1:`; mudar a opção com o mesmo commandId causa conflito.
