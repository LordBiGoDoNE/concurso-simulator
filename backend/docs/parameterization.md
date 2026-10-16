# Parametrização — núcleo e operação local

Implementação por grupos autorizados, sem liberação automática de conteúdo. A primeira
camada entrega definições imutáveis e os geradores Java puros numeric-v1: regra de três
direta (64 tuplas), porcentagem (20) e juros simples (27). Parâmetros são um mapa interno
de chaves/valores fechados, nunca input público; classificação usa M03/M04 e dois cargos.

Saída privada não contém UUIDs de alternativas: atribuição opaca/ordem e captura da
tentativa pertencem ao grupo 3. Fingerprint do banco não identifica a instância; digest
da definição inclui revisão-base, domínio, classificação, fontes e proveniência. Digest
de saída inclui conteúdo, parâmetros, correta e resolução. UTF-8 com comprimentos e
ordem explícita; não depende da serialização JSON.

Alternativas são reais inteiros positivos. Distratores cobrem: usar total informado,
somar quantidades/omitir divisão; esquecer percentual/retornar aumento ou restante;
confundir juros e montante/ignorar prazo/omitir divisão por 100. Não há arredondamento
da resposta, DSL, IA em tempo real, rede ou acesso a banco pelo núcleo.

Testes sintéticos não aprovam conteúdo. Famílias reais e bases serão preparadas e
submetidas editorialmente no grupo 4. Grupos 1/2 entregam manifesto/validação,
transições, persistência e operação local. Aplicativo público e snapshots V5 intactos.

## Manifesto e validação

`VariantManifest.create` enumera o domínio inteiro na ordem de chaves/valores fixada,
compara saída com o gerador conhecido e verifica a resposta por relações aritméticas
independentes (`NumericOracle`, sem chamar o gerador). Testes também rejeitam cada uma
das 333 respostas distratoras no domínio completo, além dos problemas de referência.
Manifesto registra definição/domínio/gerador e parâmetros/digest de cada saída; ausência,
duplicação, troca de ordem ou adulteração invalida o conjunto. Nenhuma tupla é descartada.

Digest é detecção de mudança, não prova matemática ou aprovação pedagógica. A decisão
editorial continua necessária para linguagem, distratores, passos, unidades e referências.
Não publicar manifesto/relatório privado nos contratos de realização da tentativa.

## Elegibilidade e decisões

`TemplateFamily` protege versões sequenciais, manifesto exato, estados e ponteiro único.
SUBMIT/RETURN_TO_DRAFT/APPROVE preservam a revisão editorial; WITHDRAW da versão atual
limpa o ponteiro e da histórica não altera a atual. ACTIVATE é decisão explícita com
checklist completo, não reativação por importação/status. Aprovar uma versão inicial
não regrede uma sucessora já aprovada. Todas as decisões preservam digests e base exatos.

UseCases exigem base elegível/classificação/fingerprint exatos ao registrar conteúdo
novo e aprovar/ativar. Registro idêntico e operação editorial idêntica recuperam o
resultado sem reativação; comando com outro conteúdo falha. Versão esperada do ponteiro
e sequência editorial protegem contra decisões concorrentes/ABA. Somente conflito de
registro conhecido é recuperado em nova transação após rollback; outras falhas propagam.

`EligibleTemplates` é uma porta privada para captura no snapshot do consumidor; ainda
não altera a composição nem expõe HTTP. Aprovação editorial real permanece no grupo 4.

## Persistência V6

V6 adiciona apenas `pq_family`, `pq_version` e `pq_decision`. Família tem uma base única,
ponteiro elegível e versão técnica; definições/manifestações/decisões são imutáveis com
FKs para a revisão real e aprovação obrigatória para o ponteiro. Não altera V1–V5 ou
seus triggers. Metadados da instância do simulado serão adicionados no grupo 3, ainda
não autorizado neste lote. Nenhuma família é cadastrada/aprovada por migração/startup.

Escrita usa JPA, REQUIRES_NEW/REPEATABLE_READ, lock por agregado e limite de 15s.
Somente `pq_family_pkey`/`pq_family_base_key` permitem recuperação de registro idêntico
em transação nova após rollback. SQLSTATE 40001 resulta em estado desatualizado; FK,
indisponibilidade e falhas desconhecidas não viram sucesso. Repositório confere colunas,
payloads, digests, histórico e ponteiro ao restaurar. Sessão não escapa da transação.

Captura privada `JpaEligibleTemplates` exige transação do consumidor (que deve ser
REPEATABLE_READ como a de tentativas) e faz um join de ponteiro, aprovação e revisão-base
elegível/fingerprint em uma consulta. Retirada/substituição da base remove a geração
futura; um snapshot transacional anterior mantém seu conjunto. Não modifica a consulta
pública do banco nem cria dependência do núcleo de questões para parametrização.

## Operação editorial local explícita

Comandos são individuais e processam integralmente um arquivo de até 64 KiB numa raiz
escolhida pelo operador. Caminho do arquivo é relativo com `/`, sem traversal/symlinks;
a raiz/diretórios devem permanecer estáveis durante confirmação. JSON com campos
extras, duplicados, ausentes, objetos adicionais ou seleção em array é recusado antes
de conectar. Arquivo é relido/conferido após confirmação. Não importa listas parcialmente.
Tipos são exatos: strings/frações não viram inteiros e enums não aceitam ordinais, tanto
em definição/versão quanto em estado esperado, ação ou checklist. O mapper local é
configurado explicitamente; flags da aplicação web não configuram esse mapper estático.
Regressão dessa fronteira cobre coerções que Jackson aceita por padrão; proteção não
substitui validação semântica ou a decisão editorial do conjunto exato.

Arquivo de definição: `{format: "parameter-template/v1", definition: TemplateDefinition,
expectedLatest: inteiro}`; versão deve ser expectedLatest+1. Definição tem id/version,
family (`DIRECT_PROPORTION`, `PERCENTAGE`, `SIMPLE_INTEREST`), generatorVersion
`numeric-v1`, baseQuestionId/baseRevisionId/baseFingerprint reais, classification com
M03/M04 e os dois cargos, references e provenance. Nomes de enum no JSON são os do Java;
IDs da interface pública não são um contrato editorial. A fonte/resolução autorais
referenciadas e seus hashes devem ser conferidos na revisão do grupo 4.

Na pasta backend (Windows: `gradlew.bat`; outros ambientes: `./gradlew`):

```text
gradlew.bat parameterizationLocal -PtemplateAction=preview -PtemplateRoot=C:/editorial-local -PtemplateFile=template.json
gradlew.bat parameterizationLocal -PtemplateAction=register -PtemplateRoot=C:/editorial-local -PtemplateFile=template.json
gradlew.bat parameterizationLocal -PtemplateAction=decide -PtemplateRoot=C:/editorial-local -PtemplateFile=decision.json
```

Propriedades são argumentos separados; citar cada `-P...` quando houver espaços.
Preview é offline, não lê DB_URL/senhas e retorna os valores/digests de todo o domínio
para revisão **privada**. Registro/decisão exigem DB_URL, DB_USERNAME e DB_PASSWORD
externos e PostgreSQL local com schema V6 migrado previamente. O launcher mantém
Flyway desabilitado, Hibernate validate e OSIV false; não aceita flags para habilitar
DDL/web ou propriedades arbitrárias. Não roda em build/startup normal.

Registro mostra destino/digest e exige `REGISTER <DB_URL> <SHA-256-do-arquivo>`;
cancelamento/EOF retorna 2 sem conexão. Registro novo fica DRAFT, idêntico é reutilizado
sem restaurar ponteiro. Decisão usa arquivo `{format: "parameter-decision/v1", decision:
TemplateDecision, expectedEligible: inteiro-ou-null, expectedSequence: inteiro}`,
confirmação `DECIDE <DB_URL> <SHA-256-do-arquivo>`. Evidência traz operationId/templateId/
version/action, digests definition/domain/manifest e baseRevisionId/baseFingerprint,
responsible/at/checklist/rationale. Checklist completo inclui COMPLETE_DOMAIN. Mesmo
operationId/conteúdo é retry, payload divergente falha. APPROVE/ACTIVATE recusados em CI.
Google não autoriza editar conteúdo. Fixtures isoladas não são decisões do acervo real.

Só anunciar commit após fechar a transação. Falha/recusa retorna 1 com diagnóstico
genérico, sem imprimir senha, payload rejeitado ou exceção SQL. Rotas HTTP não recebem
esses arquivos nem conseguem ativar o launcher. Migração/revisão/merge/deploy permanecem
ações separadas; este lote não cria famílias reais automaticamente.
