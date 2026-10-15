# Design

## Context

Inspeção do código integrado pelo #84, em `0d0fc48`:

- `AttemptQuestion` contém uma revisão concreta e fingerprint do conteúdo; `Attempt.Item`
  tem mapa de parâmetros, mas rejeita qualquer mapa não vazio. Ainda não existe gerador.
- `Composition.select` sorteia identidades-base distintas. `AttemptUseCases.create`
  captura itens/ordens e grava comando idempotente na mesma transação.
- `JpaAttemptStore` usa REPEATABLE_READ e rollback antes da recuperação de conflitos;
  `EligibleRevisionPool` participa dessa transação e só entrega revisões elegíveis.
- V5 exige FK de `me_item(question_id,revision_id)` para uma revisão real do banco;
  snapshots/itens/comandos são imutáveis. Não cabe fabricar IDs de revisões geradas.
- `QuestionContent.fingerprint()` inclui IDs de alternativas; DTOs públicos são separados
  dos snapshots privados. O frontend valida rigorosamente as chaves de resposta.
- Importação local não aprova conteúdo; Google não concede poderes editoriais. Não há
  specs arquivadas/consolidadas nem publicação da reformulação em main.

## Goals / Non-Goals

**Goals:** variações corretas, didáticas e reprodutíveis das três famílias escolhidas,
com elegibilidade explícita e integração ao ciclo de tentativa existente.

**Non-Goals:** motor genérico de expressões, geração ilimitada, suposto sigilo global
dos gabaritos do legado, recomendação adaptativa, alteração das metas do banco,
administração HTTP ou implantação pública.

## Decisions

### 1. Três famílias e domínios fechados

Valores propostos em reais inteiros; taxa percentual por mês; todos positivos.
Não há arredondamento de resposta na v1. Usar inteiros/racionais exatos no núcleo,
sem ponto flutuante binário nem avaliação de strings. Apresentação em pt-BR.

| Família | Parâmetros permitidos | Cálculo e domínio |
| --- | --- | --- |
| Regra de três direta, M03 | quantidade inicial `a` ∈ {2,3,4,5}; quantidade desejada `b` ∈ {6,7,8,9}; preço unitário `u` ∈ {2,3,5,7} | Informar preço de `a` unidades como `a*u`; perguntar preço de `b`: `(a*u)*b/a`. 64 tuplas. |
| Porcentagem, M04 | base `v` ∈ {100,200,400,800}; percentual `p` ∈ {5,10,15,20,25} | Perguntar `p%` de `v`: `v*p/100`. 20 tuplas; sem aumento/desconto sucessivo. |
| Juros simples, M04 | capital `c` ∈ {1000,2000,4000}; taxa mensal `i` ∈ {1,2,3}; meses `t` ∈ {2,3,6} | Perguntar **juros**, não montante: `c*i*t/100`. 27 tuplas, unidades explícitas. |

Cada família tem um template v1 e uma identidade-base distinta, vinculada a revisão
aprovada compatível. M04 precisa de duas bases distintas. Escolha das revisões e revisão
pedagógica ocorrem no grupo de conteúdo, sem presumir IDs ou aprovação do banco local.
Caso falte base adequada, registrar a dependência e solicitar decisão antes de ampliar
o escopo editorial do banco. Variações não contam como novas identidades para sua meta.

Cada tupla produz enunciado, quatro alternativas numéricas distintas, exatamente uma
correta, resolução com substituição/etapas/unidades e fontes/autoria honestas. Distratores
representam erros identificáveis e passam por validação de equivalência numérica e
formatação, inclusive coincidência com a correta. Uma tupla inválida impede habilitar
toda a versão; não há descarte silencioso nem loop de tentativas até achar números.

### 2. Modelo e fronteiras

`parameterization.domain` protege template/versionamento, domínio e instância gerada;
geradores concretos dessas três famílias são funções Java puras, selecionadas por ID
e versão conhecidos. Não usar Spring/JPA, script engine, reflexão sobre input, downloads
ou DSL. Interfaces somente para portas úteis de persistência/consulta/transação.

UseCases coordenam prévia/validação, registro e decisão editorial local. Adaptadores
JPA executam transações; launcher local explícito faz I/O sem beans de escrita no
startup web. O módulo usa contratos do banco; banco não depende de simulados/gerador.
`mockexam` consome contrato interno de materialização/captura, sem acessar JPA concreto.

### 3. Manifesto exaustivo e elegibilidade editorial

Prévia local enumera 111 tuplas e produz manifesto canônico limitado, com digest da
definição/domínio, revisão-base/fingerprint, versão do gerador e digest de cada saída
sem IDs aleatórios. Inclui alternativas em posições editoriais, resposta/resolução,
fontes, proveniência e classificação. Canonização tem versão, encoding e ordem fixos.
Versões de algoritmo diferentes exigem nova versão de template e novo manifesto.

Verificação automática usa oráculo aritmético independente e casos pedagógicos de
referência, além de cardinalidade, unicidade, limites e segurança de conteúdo. Digest
detecta mudança em relação ao conjunto registrado; não demonstra correção matemática.

Estados `DRAFT`, `IN_REVIEW`, `APPROVED`, com retirada explícita separada da definição
imutável. Aprovação exige responsável/data/checklist/justificativa e digests exatos de
template, domínio, manifesto e revisão-base. Checklist cobre correção, distratores,
clareza, passos/unidades, acessibilidade, fontes/classificação e domínio completo.
Importação/testes/CI não aprovam; operação editorial exige seleção e confirmação local
do destino/digest, como no banco. Fixtures não representam decisões sobre conteúdo real.

Prévia sem banco e sem gravação; registro/decisão com prévia, base/versão esperadas e
transação atômica. Repetição idêntica não duplica nem reativa retirada; conflito conhecido
só é recuperado após rollback. Erros de FK/indisponibilidade não viram conflito conhecido.
Banco e template precisam estar elegíveis no mesmo snapshot. Troca da revisão-base
invalida o vínculo para novas capturas até aprovação de versão correspondente; retirada
de template desabilita a família para novas gerações. Nada altera tentativas existentes.

Cada família mantém um único ponteiro de versão elegível, separado dos estados
históricos `APPROVED`; a mesma identidade-base não pertence a famílias concorrentes.
Aprovar v2 substitui explicitamente v1 nesse ponteiro, de modo atômico e com versão
esperada da decisão/ponteiro. Não selecionar por maior número ou por qualquer status
aprovado. Retirar a versão atual limpa o ponteiro, sem reativar v1 automaticamente;
retirar uma versão histórica não altera a atual. Decisões concorrentes incompatíveis
falham sem sobrescrita; nova ativação exige decisão explícita, não reimportação.

### 4. Composição e captura atômica

Acrescentar `useParameterizedQuestions` booleano opcional ao comando de criação;
ausente significa false. Valores de outro tipo e campos extras continuam rejeitados.
Incluir o valor normalizado no hash idempotente e na composição imutável; mesmo
commandId com opção diferente resulta em 409. Clientes antigos continuam criando fixas.

Para false, inclusive opção ausente, preservar **exatamente** a canonização/hash de
criação V5 (cargo/disciplinas/assuntos), sem acrescentar o literal false nem reescrever
hashes gravados. True usa discriminador versionado novo e não pode recuperar um
comando fixo anterior. Regressão usa comando/hash realmente produzido pelo código V5:
ausente e false recuperam a criação original; true com a mesma chave retorna 409.
Compatibilidade de hash é distinta da compatibilidade dos codecs JSON.

Primeiro selecionar identidades/revisões-base com os critérios atuais, uniformemente
e sem reposição. Se opção true, materializar somente bases com template atualmente
elegível; demais bases permanecem fixas. Por base parametrizada, sortear uniformemente
uma tupla do domínio aprovado, com sorteio substituível em testes. Não aumentar a
quantidade pedida nem sortear 111 instâncias como se fossem 111 questões diferentes.

Ausência/retirada editorial de template permite base fixa, informada na composição.
Template declarado elegível mas sem implementação/manifesto compatíveis, ou saída
divergente, é falha de integridade: criação inteira falha sem fallback silencioso.
Não falhar leitura/conclusão de tentativas já capturadas por indisponibilidade do gerador.

Validar a saída sem IDs contra o manifesto; depois atribuir UUIDs opacos de alternativas
e ordem final, sem codificar resposta em IDs/nomes. Capturar tudo na mesma unidade
REPEATABLE_READ da seleção e gravação/comando, incluindo elegibilidade de template.
Concorrência antes/depois do snapshot segue a semântica já aprovada no banco/simulados.

### 5. Instância não é revisão nova do banco

Cada item mantém IDs da questão/revisão-base reais, atendendo à FK V5; a fonte tipada
distingue revisão fixa de instância parametrizada. A instância guarda conteúdo final,
fingerprint renderizado, templateId/version/digest, base revision/fingerprint, manifesto,
tupla canônica e versão do gerador. Não declarar que o conteúdo gerado é o payload
original da revisão-base nem reutilizar o fingerprint dela para o conteúdo diferente.

Metadados ficam no snapshot privado e no registro imutável da instância ligado ao item,
gravados atomicamente e conferidos na restauração. Um único VO de captura evita mapas
arbitrários: campos exatos, tipos/ranges, domínio e consistência com fonte/conteúdo.
Parâmetros vazios só para fonte fixa; geração tem fonte/tupla completas. A revisão-base
identifica a origem; fingerprint renderizado identifica o conteúdo concreto utilizado.

Retomada, resposta, conclusão e resultado leem conteúdo capturado, sem rodar gerador,
resolver com valores atuais ou depender da aprovação atual. Novas versões não reescrevem
templates, decisões, instâncias, comandos ou resultados anteriores. Pontuação segue
uniform-v1, calculada no servidor com a alternativa capturada.

### 6. Persistência e compatibilidade

Flyway aditivo após V5: templates/versionamento/manifesto/decisões e vínculo de
instância ao `me_item`, com FKs/unicidade/imutabilidade. Não editar V1–V5 nem afrouxar
FKs para acomodar revisões fictícias. Hibernate validate/OSIV false permanecem.
Cleanup visitante remove somente dados da instância dependente junto de seu item;
não remove template/revisão/decisão editorial compartilhados.

Versionar formato interno novo e manter codec explícito do snapshot antigo: composição
sem opção equivale a false, item antigo sem metadados é fixo com parâmetros vazios.
Isso vale também para `me_command.outcome`, não só `me_attempt`/`me_item`. Não reescrever
histórico, triggers ou comandos antigos para preencher campos. Testar desserialização,
replay, respostas e conclusão de tentativas V5, inclusive em andamento. Ao atualizar
uma tentativa antiga, escrever no formato de origem, preservando exatamente os campos
imutáveis de composição/itens/política protegidos pelos triggers V5; somente aceitar
o JSON antigo na leitura e reserializar tudo no formato novo seria incompatível.

Rollback após a primeira escrita no formato novo usa artefato desta evolução com geração desabilitada e
leitores compatíveis, preservando ler/retomar/concluir os dois formatos. **Não prometer
que o JAR anterior consegue ler snapshots novos, mesmo de questões fixas.** Antes dessa
primeira escrita, avaliar artefato anterior sobre schema aditivo em teste isolado;
depois dela, proibir esse retorno incompatível no procedimento. Sem downmigration ou
apagamento de dados. Gerar parâmetro não é o único evento que define esse limite.

### 7. HTTP, interface e privacidade

Única extensão pública proposta: opção na criação `POST /api/v1/attempts`. Nenhum
endpoint de template, manifesto, geração livre, parâmetro, prévia editorial ou solver.
GETs de tentativa/resultado/histórico e questões mantêm DTOs tipados atuais; conteúdos
já renderizados usam `TextContent`, incluindo descrição de recursos e unidades.
Maps de parâmetros internos não viram contratos JSON dinâmicos da web.

Spring Security mantém autenticação/principal, rota/sessão/CSRF; controller traduz a
opção tipada; UseCase coordena captura; domínio protege elegibilidade e integridade;
JPA executa transação. Posse e expiração não mudam. No-store/limites atuais permanecem.
400 input inválido; 409 comando divergente/insuficiência; falha interna de integridade
retorna erro genérico 500, indisponibilidade 503, sem dados de template/correção.

Checkbox acessível, desligado por padrão: “Usar variações numéricas quando disponíveis”.
Explicar que pode misturar fixas e variações dos três tipos, sem garantir questão nova
a cada tentativa. Congelar opção no retry; não modificar uma tentativa iniciada.
Resultado mostra resolução concreta e acessível como hoje. Não expor templateId,
parâmetros, manifesto, seed, solver, gabarito ou validações corretivas antes de concluir.
Sem localStorage/cache/URL com esses dados. Aulas e consulta pública preservadas.

## Risks / Trade-offs

- Domínio finito permite validação exaustiva, mas pode repetir valores entre tentativas;
  não se promete novidade infinita, dificuldade calibrada ou aprendizagem medida.
- Aprovação do exemplo-base não aprova a família; base e template têm decisões distintas.
- Código compartilhado entre gerador e teste pode repetir erro: oráculo independente,
  revisão pedagógica e manifesto completo são evidências complementares, sem garantia total.
- Novo formato exige leitor compatível no rollback e preservação dos comandos antigos.
- Gabaritos do legado seguem públicos; proteger contratos novos não oferece sigilo global.

## Migration Plan

Somente após aprovação da proposta e escolha de grupos completos. Risco cumulativo
**crítico**: elegibilidade, parâmetros/gabaritos, persistência, atomicidade e histórico.
Tarefas com código/testes/docs coesos em PRs pequenos; Stacks v0.2.1 só para dependências
reais da mesma spec, sem sync/rebase com worktree ocupado nem merge presumido.
Revisões de lote e gate cumulativo independentes SOL 6.1 xhigh, com medição conforme
`docs/review-quota.md`; `[SPEC]` para épica geral. Sem main/deploy/arquivamento.
