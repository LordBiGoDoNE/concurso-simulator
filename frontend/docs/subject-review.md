# Revisão por assunto — interface

Painel somente no contexto conectado, com cargo escolhido explicitamente. Sem escolha,
não há consulta privada; a opção não cria simulados. Os51 assuntos aparecem com contagens,
percentual ausente sem dados, amostra pequena, prioridades e janela/corte/datas explícitos.
Nenhuma nota é calculada no cliente; ele valida o contrato e apresenta métricas do servidor.
A prioridade vem ordenada do servidor; heurística não é selo de domínio nem previsão.

Resultado concluído de visitante/conta carrega apenas a revisão daquela tentativa,
sem carregar painel/histórico implicitamente. Tentativa em andamento não monta a consulta.
Visitante continua temporário e não é adotado pela conta. O painel da conta atualiza após
conclusão confirmada; atualização explícita mantém o cargo e distingue erro de ausência.

DTOs têm chaves exatas,51 IDs públicos distintos, métricas/contagens limitadas, datas,
contexto/cargo/ID e corte consistentes. Campo extra/captura/owner em qualquer nível,
JSON parcial, enum ou contexto incorretos não entram na interface. Requisição usa GET,
credenciais e no-store, sem tokenCSRF artificial; erro401/403/404 não espera corpo.
Timeout finito5s cobre fetch e JSON, mesmo quando o transporte ignora abort.

useSubjectReview limpa o estado e aborta ao trocar cargo, verificar sessão, desmontar
ou atualizar. Cada geração confere sua vigência antes de apresentar resposta. App
suspende somente a revisão durante verificação de sessão, preservando respostas locais
do simulado; logout/troca de conta desmonta o contexto privado. Não há storage, cache
persistente ou dados de desempenho em URL. O cliente não promete revogar resposta já
em voo no servidor; cancela e ignora seu resultado ao mudar o contexto.

137 testes frontend passaram: contratos negativos, erro sem corpo, timeout, cancelamento,
cargo/conta/logout/resposta tardia, retry e conclusão confirmada para visitante/conta,
além das regressões existentes. Acessibilidade usa tabela com caption/cabeçalhos,
estados em texto, seletor rotulado, botões por teclado e foco no painel após ação
explícita. Atualização automática não desloca foco do resultado. Browser real390/1280px e inspeção visual das seis capturas locais passaram.
Tabela tem rolagem própria por teclado, sem overflow da página. Os cinco fluxos novos passaram após as correções de revisão (26,4s). Antes delas,
as regressões reais completas passaram (18 identidade/simulados/revisão,5 parametrizados);
navegação pública/offline6/6, com material montado e links108 páginas/1.757 referências.
Não houve teste com leitor de tela nem auditoria WCAG integral. Capturas contêm somente
fixtures sintéticas e ficam ignoradas em build/, sem publicação de imagens.
O terceiro lote autorizado inclui os grupos completos5/6. Sem deploy/main.

## Regressões da revisão independente do lote

SR-R2-SOL-001 confirmado: um booleano de intenção de foco sobrevivia à falha/cancelamento.
A regra positiva agora vincula intenção ao cargo e contadores da consulta explícita;
erro,cancelamento,revalidação ou atualização automática a descartam. Teste do App
reproduziu falha explícita → conclusão → foco indevido no painel e passou após correção;
regressão separada cobre cancelamento. Browser real confirma foco no resultado após
a mesma sequência, e atualização explícita bem-sucedida continua focando o painel.
Proteção não garante toda combinação possível de eventos ou uso de leitor de tela.

SR-R2-SOL-002 confirmado: converter disciplineId com String aceitava array unitário.
Agora o tipo string é verificado antes do enum,sem coerção. Controles negativos de
painel e individual falharam antes da correção e passaram depois. Validação de contrato
não prova veracidade das métricas; cálculo/correção continuam exclusivamente no servidor.

O CI identificou uma corrida no teste positivo de foco: findByText podia terminar
antes do useEffect de foco. A assertion agora aguarda o título receber foco com
waitFor; continua exigindo o mesmo destino, como o Playwright já fazia. Presença de
texto não certifica conclusão de efeitos. Não houve mudança do comportamento de produção.

## Aulas (5.1)

lesson-paths.json é a cópia pública controlada dos51 pares id/conteudo de manifesto_v3.json.
O teste exige igualdade completa e existência de cada HTML; não importa gabarito, dados de
conta ou conteúdo de API. lessonUrl consulta somente chaves próprias do mapa, valida caminho
relativo e baseHTTP(S) sem credenciais/query/fragmento, codifica cada segmento uma vez e
preserva subpath da VITE_MATERIAL_URL. Configuração inválida mostra link indisponível.
Links abrem nova aba com aviso e noopener/noreferrer, preservando a realização e respostas
locais na aba original. O material estático permanece no mesmo lugar e funciona sem API.

## Prática direcionada (5.2/5.3)

Praticar em qualquer linha (inclusive sem dados) passa somente cargo/disciplina/assunto
públicos ao workspace. Nova montagem do compositor zera outras disciplinas/assuntos,
seleciona uma questão e desliga variações. O catálogo confirma a seleção antes de
habilitar o formulário; inconsistência falha sem fallback. Nada é criado até Iniciar.
O usuário pode alterar os critérios e optar por variações; não se promete novidade.

O workspace pede a confirmação existente antes de desmontar qualquer realização:
cancelar preserva a fila, respostas locais e commandId. Aula em nova aba não desmonta
a realização. Enquanto cria, atalhos e cargo do painel ficam desabilitados; verificação
de sessão suspende comandos. Troca de cargo do painel limpa prefill/comando e aborta
catálogo anterior; identidade/expiração/logout desmontam o workspace e cancelam operações.
Respostas tardias não restauram contexto. Não há storage ou querystring para o prefill.

Criação usa o POST, CSRF e idempotência existentes. Retry sem mudar critérios mantém o
commandId; qualquer edição descarta o comando anterior, mesmo se valores retornarem.
Insuficiência mantém critérios/contagens reais, sem mudar assunto, reduzir ou misturar
acervo automaticamente. Regressões unitárias cobrem essas transições com componentes
reais e portas substituídas; testes reais de rede/abas/retirada completam a cobertura.

## Fluxo integrado por ator (5.3/5.4)

Playwright subject-review.real.spec.ts passou10/10 (33,6s antes do ajuste de largura):
conta em390/1280px abre aula real e preenche M01/Secretário, cria só ao confirmar,
POST semCSRF recebe403, recupera criação gravada com a mesma operação após perda da
resposta e conclui outra vez a única base do assunto. A última observação substitui
branco por acerto (bases1/exposições2), sem criar denominador artificial. Outra sessão
da mesma conta recebe o mesmo resultado capturado. Visitante usa aula/atalho individual
sem painel/histórico; cancelar abandono e abrir aula preservam escolha local. Duas
abas mantêm conflito/recarga/salvamento explícitos. Retirada sintética da única M01
produz409/zero disponíveis, preserva critérios/comando, não cria parcial e não muda
resultado antigo. Os51 links permanecem acessíveis mesmo sem evidência.

O hosting externo é substituído pelo HTML/assets públicos reais do repositório no
teste; API/OIDC/PostgreSQL/navegador são reais. Isso verifica navegação/encoding/material,
sem certificar disponibilidade do CDN de produção. Fixture/rota de retirada existem
somente em src/test, não no JAR. Capturas locais sintéticas390/1280 foram inspecionadas;
a coluna de estudo tem largura mínima própria e rolagem contida, sem botão fragmentado.
Sem auditoria integral de leitor de tela ou WCAG; nenhuma decisão editorial real.
