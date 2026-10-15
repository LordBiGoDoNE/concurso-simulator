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

## Persistência e codecs

V7 adiciona `pq_instance`, ligada ao item e à versão real do template. Snapshot,
item, instância e comando confirmam na mesma transação REPEATABLE_READ. Constraints
deferidas impedem item gerado sem instância, vínculo divergente e instância em item
fixo; UPDATE e DELETE independente são proibidos. Cleanup em cascata remove apenas
dados dependentes do item. As tabelas, FKs e funções V1–V6 não são reescritas.

O adaptador JPA usa SQL nativo parametrizado apenas para inserir/consultar a cópia
JSON imutável da instância, sem entidade mutável adicional; o agregado e os itens
continuam mapeados por Hibernate. A restauração confere igualdade dessa cópia.

Novas tentativas usam `format: 2`, mesmo fixas. `AttemptCodec` reconhece o JSON V5
sem marcador, restaura fonte fixa/parâmetros vazios e mantém formato 1 no núcleo.
Ao atualizar, reutiliza os nós imutáveis originais e substitui somente respostas,
versão e conclusão. Novos outcomes de comandos de uma tentativa antiga também usam
o formato antigo; não há preenchimento retroativo em `me_item` ou `me_command`.
Formato desconhecido, composição nova sem versão e fonte nova em item antigo falham.

Regressão instala snapshot e hash produzidos pelo código V5 real (`0d0fc48`) em
PostgreSQL V5 e atualiza para V7; prova replay ausente/false e conflito true, preservação
das linhas durante upgrade e dos campos imutáveis durante resposta/conclusão.

## Rollback compatível

Depois da primeira escrita em formato 2, mesmo fixa, **não retornar ao JAR V5**.
Usar este artefato com `--parameterization.generation-enabled=false`: novas criações
parametrizadas falham, sem fallback, enquanto leitura/resposta/conclusão e replay
confirmado funcionam nos dois formatos. Fixas continuam disponíveis. Não apagar
snapshots, reescrever comandos ou executar downmigration. O teste de persistência
simula catálogo/gerador indisponíveis e comprova preservação de instância/correção.
O ensaio operacional de JARs e ponta a ponta permanece no grupo 6.
