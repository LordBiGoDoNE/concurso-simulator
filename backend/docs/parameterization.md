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
