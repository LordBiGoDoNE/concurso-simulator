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
submetidas editorialmente no grupo 4; manifesto/validação e transições vêm nas camadas
seguintes dos grupos 1 e 2. O aplicativo público e os snapshots V5 permanecem intactos.

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
