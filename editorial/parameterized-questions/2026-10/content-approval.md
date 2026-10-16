# Decisão editorial aprovada e registrada

**Seleção aprovada explicitamente pelo usuário; atos registrados na cópia local isolada.**

A solicitação e seus digests foram preservados abaixo. Resultado efetivo em [eligibility-after.json](eligibility-after.json); retirada/replay somente no [clone de ensaio](withdrawal-probe.json). A intenção JSON mantém o estado histórico anterior à decisão.

Escopo: aprovar a revisão-base M04-Q05 abaixo e as versões v1 das três famílias, exclusivamente na cópia local isolada do PostgreSQL.
A preparação de M04-Q05 foi autorizada pelo usuário. Esta é a decisão editorial separada prevista na tarefa 4.3.

Seleção exata: [approval-selection.json](approval-selection.json), SHA-256 `879e9fa5d43d885a6125c9cbccc8590b373abc8d2321929d370159426ef1dba3`.

Responsável pela revisão: Codex, assistido por IA, sem revisão humana independente. Referências temáticas não representam validação oficial.

## Nova base — M04-Q05

Questão `0a4e9bac-c340-4562-a906-aaa0a33c64c8`; revisão `80f32d25-9f88-4616-8ba0-8afafbeea9ab`; fingerprint `2d862a94c761041955772ac972a9f4dc9f2ce81fe8b6f2a12824450d592eb81e`.

Um capital de R$ 2.000,00 é aplicado a juros simples de 3% ao mês durante 4 meses. Qual é o valor dos juros produzidos nesse período?

| Alternativa | Valor |
| --- | --- |
| A | R$ 60,00 |
| B | R$ 2.060,00 |
| C | R$ 2.240,00 |
| D (correta) | R$ 240,00 |

Resolução: 1% de 2.000 = R$ 20,00; juros mensais = 20 × 3 = R$ 60,00; quatro meses = 60 × 4 = R$ 240,00. O capital inicial continua sendo a base em todos os meses. A responde só um mês; B soma capital e juros de um mês; C responde o montante dos quatro meses.

Histórico: revisão preparada em DRAFT e posteriormente submetida/aprovada após autorização explícita, com ponteiro elegível confirmado. [Pacote exato](base-m04-q05-package.json) e [registro de preparação/reimportação](base-preparation.json).

## Versões e manifestos exatos

Todas as [111 variantes](variants.md) incluem enunciado, quatro alternativas, correta, resolução, cálculo racional independente e digest. O [relatório de auditoria](audit.json) confere a observação posterior das bases, sem atribuir aprovação por testes; os atos reais estão em eligibility-after.json.

### DIRECT_PROPORTION — v1, 64 variantes

Template `ae2f9e5e-e000-5e95-bbae-9499ab5ade07`; [definição](direct-proportion.json).

- Definição: `814856a1723091fd5b1c19d4a60f043a2b932e302fb3203bd5a6d9858a82caf3`.
- Domínio: `4e79ada209d2b163b2be7379ccfef710e46715c1daa7d22bf1b26f73861e2a9d`.
- Manifesto: `6814d912447caccc81fdd7ac8c188847083bb89ff6fe94818c51a769b20dba21`.
- Base: `2bb8c482-8107-47bc-beec-abcefc540caf` / revisão `632ee809-9a3b-4e97-8144-9539f6b951bb` / fingerprint `cadb800275af1eaf48256c86104b921325bcaba5dcc7b69ae26c5535a0b2c98b`.

### PERCENTAGE — v1, 20 variantes

Template `ba13d9bf-2bc6-5a19-881f-b73e3062e2c3`; [definição](percentage.json).

- Definição: `b6ab95874bee5d34a69312ddd6cd88598b21dd8f77419037fde5f3b0b0c0277a`.
- Domínio: `c5607626f484fb4bc1faa0a3c95d6a2521abc98bd97d2203c643b52fff890e39`.
- Manifesto: `9bba42ad7fb4364584cfc588b8cabbf7a504e36d3b16f6c58398ffc0ac4ca9bc`.
- Base: `db48c5ab-a883-4dbd-954c-2615d1624d3a` / revisão `f4cfd6d7-96a5-4268-b257-638f4d6ae494` / fingerprint `e996529dde883cacb6ed642e5272cfa54665a3afb9ac162cb92a0980339cb2ab`.

### SIMPLE_INTEREST — v1, 27 variantes

Template `e574536d-2328-5f5d-931b-01741b8f1092`; [definição](simple-interest.json).

- Definição: `1661c38884b4f71c61e943b83c6fd51c0a250b027dcf6488b324cb413803fb6c`.
- Domínio: `4f245cbdd68f2b450e4d091e65979190fdeb0bbda9baada91599770d1a9d57cc`.
- Manifesto: `0617b27c85219241fcf9d336be6b9a492671df15bfb246dfe58e5f1696eedc30`.
- Base: `0a4e9bac-c340-4562-a906-aaa0a33c64c8` / revisão `80f32d25-9f88-4616-8ba0-8afafbeea9ab` / fingerprint `2d862a94c761041955772ac972a9f4dc9f2ce81fe8b6f2a12824450d592eb81e`.

## Alcance da decisão

Após autorização explícita, executar SUBMIT/APPROVE da base M04-Q05 com estado esperado, registrar os três templates em DRAFT e executar SUBMIT/APPROVE de cada versão com seus digests e checklist completo. Cada ato terá timestamp real de execução, responsável e confirmação de destino/digest; registrar resultados/replay separadamente. Esta seleção não contém atos fictícios previamente executados.

O banco original mantém 250 revisões elegíveis. Após os atos autorizados, a cópia tem 251 identidades elegíveis e três templates v1 aprovados. A nova base acrescentou uma identidade editorial; as 111 variantes acrescentaram zero identidades.

A autorização editorial não integra PRs, não publica a aplicação nem aprova o gate cumulativo. Registro e ensaio operacional concluídos; o lote 3/4 aguarda revisão independente SOL 6.1 xhigh, e merges exigem autorização própria.
