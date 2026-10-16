# Variações na composição

Checkbox nativo rotulado e descrito, false por padrão. A explicação informa as três
famílias, mistura de fixas/variações e possibilidade de repetição. Critérios e opção
ficam desabilitados enquanto o pedido está em curso. O retry com os mesmos critérios
preserva a escolha e o commandId; mudar a opção cria outra operação. Troca de contexto
de identidade desmonta o formulário e volta ao padrão false.

A opção só participa da criação; a realização e o resultado usam conteúdo renderizado
do servidor. Não há parâmetro escolhido pelo aluno, solver ou fórmula no frontend,
nem metadados no DOM/storage/URL. Validadores estritos continuam rejeitando chaves
extras nas projeções. O contrato corretivo só é aceito após conclusão. A promessa de
sigilo limita-se à aplicação nova; o material estático legado segue público.

Regressões em AttemptComposer, AttemptContext e attempt-api verificam padrão,
descrição acessível, bloqueio enquanto envia, retry/alteração da escolha, limpeza
na troca de conta, payload público exato e rejeição de metadados privados. Navegador
e backend reais completam a evidência no grupo de verificação, sem tratar mocks como
prova de persistência/segurança. `npm run test:parameterized` usa fixture explícita
em Testcontainers/OIDC local e passa por teclado/mobile/desktop, confirmação perdida
da criação, retomada após reinício/alteração da base, duas abas e expiração visitante.
A fixture só é habilitada no launcher de teste por `PARAMETERIZED_BROWSER_FIXTURE`;
não é seed do JAR ou aprovação das famílias reais. Seus metadados/gerador/sentinelas
são rejeitados pelo scanner de artefatos também em chunks tardios.
