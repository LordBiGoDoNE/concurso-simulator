# Frontend

## Primeira experiência do estudante — implementação local

Entrada por cargo, biblioteca e prática seguem a spec `new-student-experience`.
Aulas e orientação inicial usam o catálogo local e funcionam sem API. A navegação
usa `#/inicio`, `#/aulas/P01`, `#/simulados`, `#/praticas` e `#/questoes`;
cargo, disciplina e busca são públicos. Abrir ou atualizar um link não inicia atividades.
Atualização restaura contexto público; respostas pendentes não são persistidas na URL.

Antes de oferecer a prática, a interface consulta a disponibilidade do assunto/cargo.
Erro, carregamento e zero confirmado são distintos. Início exige clique e sessão
verificada; login continua opcional. API/CSRF/idempotência mantêm os contratos atuais.

Para conferir os cenários da jornada no Edge instalado:
`npx playwright test --config playwright.edge.config.ts`.
Protótipo em `../design/prototypes/new-student-experience/`; evidências e limites em
`../openspec/changes/new-student-experience/verification.md`.

O índice aparece antes do título da aula, com cabeçalho Nesta Disciplina - nome
da disciplina. O leitor ocupa toda a largura disponível, com opções de navegação
distribuídas conforme o tamanho da tela. Ao trocar de aula, inclusive pelos
botões Próxima aula/Aula anterior, a página retorna ao topo.

A prática aparece em painel recolhido no final da aula. Abrir o painel consulta
disponibilidade; somente Começar prática inicia a realização e carrega as questões
abaixo do botão, mantendo a rota da aula. Recolher preserva respostas/fila; o resultado
aparece no mesmo painel. A área Práticas mantém acesso à realização e ao histórico.
As 51 aulas usam o leitor nativo: um título, teoria, exemplos resolvidos, tabelas,
fórmulas, esquemas e fontes no layout atual. Não carregam HTML/CSS antigo nem
iframe. Os exercícios antigos não aparecem no texto; use o painel de prática.
Atalhos de revisão abrem a mesma aula nativa. A leitura independe da API.

Conteúdo versionado em `src/lesson-documents/`, gerado a partir das fontes públicas
preservadas por `python scripts/compile_native_lessons.py` (a partir da raiz).
Para verificar reprodutibilidade: `python scripts/compile_native_lessons.py --check`
e `python -m unittest discover -s scripts/tests -p test_native_lessons.py -v`.
O manifesto registra hashes LF das fontes e de cada documento. Alterações nas
fontes exigem regeneração e inspeção; não editar os JSONs manualmente. Python é
necessário nessa autoria/verificação; o build e o runtime usam apenas os dados
preparados. O renderer admite elementos conhecidos, sem HTML bruto ou scripts.
Esquemas e tabelas largos podem ser percorridos na horizontal com teclado.
Falha na leitura oferece retry; navegar cancela/ignora respostas anteriores.

Node **24 LTS**, React **19.3.0**, TypeScript **5.9.3**, Vite **8.3.3**, Vitest **5.0.3**. As versões resolvidas das dependências estão em `package-lock.json`.

```sh
cd frontend
npm ci
cp .env.example .env.local
npm run dev
```

Abra http://127.0.0.1:5173. Se o backend estiver parado, as aulas continuam acessíveis e a interface informa indisponibilidade. Use o botão para consultar novamente.

`VITE_API_URL` e `VITE_STATUS_TIMEOUT_MS` são configurações **públicas**, incorporadas no build. Nunca coloque senhas ou tokens nelas. O leitor nativo usa os documentos do próprio frontend; não depende de `VITE_MATERIAL_URL` ou de um servidor de material estático. O pacote HTML preservado continua disponível separadamente.

## Banco de questões — consulta livre

Catálogo/filtros AND/página/detalhe de revisões elegíveis, sem responder ou corrigir.
Visitante e conectado usam os mesmos contratos públicos, sem cookies na consulta de
questões. Aulas/login continuam independentes. Vazio e indisponível são estados distintos,
com retry; VITE_QUESTION_TIMEOUT_MS (público, default 5000ms) limita headers+JSON de cada
consulta. Mudanças de filtro cancelam/invalidate respostas anteriores e fecham detalhe.
Labels, teclado, fórmulas/ênfase textuais literais/fontes nomeadas e foco de detalhe/paginação;
recursos têm descrições acessíveis, não execução de HTML/SVG nem carregamento indiscriminado.
Limites, teste determinístico e política de renderização em [consulta](../docs/question-browser.md).

```sh
npm test
npm run build
```

O build fica em `dist/`; não é publicado automaticamente. O GitHub Pages atual continua servindo o material original.

## Login opcional

A revisão arquitetural/JPA de 2026-10-08 é interna ao backend (ADR 0001). O frontend continua usando contratos HTTP, nunca modelos ORM. `identity-api.ts` concentra chamadas/validação de respostas; LoginPanel apresenta estados e ações. Não há uma entidade de negócio artificial para cookie/CSRF nem uma cadeia de Services de repasse.

A interface consulta auth/config e me com credentials include, sem iniciar Google automaticamente. Visitante, conectado e indisponível são estados independentes da prontidão. O link das aulas permanece em todos eles. A conta permite salvar/retomar simulados e consultar histórico privado; visitante permanece temporário, sem mescla no login.

Para testar Google local, veja `../backend/docs/google-login.md`: use `SPRING_PROFILES_ACTIVE=local` no backend, API `http://127.0.0.1:8080`, frontend `http://127.0.0.1:5173` e CORS para essa origem. Google desativado funciona sem client. O navegador precisa usar hostname consistente nas duas portas (cookie same-site); nenhuma credencial Google vai para VITE_*.

“Entrar com Google” é navegação à API. “Sair” busca novo CSRF e faz POST; 403 oferece nova tentativa, erro de rede não afirma saída. “Atualizar sessão”, retorno à aba visível/foco e documento restaurado consultam novamente e reconhecem expiração. Falha no login mostra mensagem genérica e remove auth=failed da URL, preservando demais parâmetros. Não há tokens Google em localStorage/sessionStorage. Testes Vitest cobrem estados/falhas/logout e Playwright cobre teclado/390px/1280px.

O prazo de 5 segundos das chamadas de identidade inclui headers e consumo do JSON necessário (config/me/CSRF); logout usa somente o status, sem ler corpo. Timers e listeners são liberados ao concluir ou falhar. LoginPanel invalida consultas anteriores e concorrentes ao logout: respostas tardias não podem sobrescrever seu resultado, e uma nova atualização continua possível. Essas proteções cuidam do transporte/estado local; autenticação, expiração e invalidação real da sessão continuam sendo responsabilidade do backend.

Testes de navegador: `npx playwright install chromium` e `npm run test:e2e`. Para incluir a regressão offline das aulas, monte o pacote com `scripts/build_pages.py` e defina `STUDY_ROOT=/caminho/absoluto/do/pacote` ao executar os testes. Sem essa variável, o teste offline é explicitamente ignorado; o CI sempre a define.

## Navegador integrado sem Google real

`npm run test:identity` requer Docker e JDK 25 (configure JAVA_HOME se necessário), além do Chromium. Playwright inicia Vite, backend real, PostgreSQL 18.6 isolado e provedor OIDC local com discovery/token/JWK. Reserve portas 5173, 18080 e 18081. Não reutiliza banco/servidor local existente nem precisa de credenciais Google. O launcher e controle de subject/cancelamento ficam exclusivamente em src/test, fora do JAR de produção.

O mesmo comando agora inclui `questions.real.spec.ts` além dos três cenários de identidade:
filtros/paginação/detalhe/teclado/foco/fórmulas/fontes em 390/1280px, mesmos filtros após
login OIDC real e falha de transporte com aulas acessíveis, JSON/DOM/bundle sem gabarito.
O harness compila o bundle antes de abrir Vite para inspeção; testes de UI usam backend/PG
reais, e falha de rede é injetada no Playwright (não alegamos derrubar PG no browser).
Somente esse harness em src/test importa/aprova/retira fixtures claramente sintéticas;
nunca conteúdo real, bootJar/produção ou evidência de curadoria. CI existente continua
chamando o mesmo comando, sem alterar workflow/proteções/publicação; suíte de identidade
mantém recuperação/UUID/dispositivos/cookie/CSRF/expiração/cancelamento. Timeout20s é
exclusivo desse launcher. Não substitui smoke Google real antes do deployment.

## Simulados e histórico

Composição livre por cargo/disciplina/assuntos, de 1 a 100 questões, sem formato oficial
presumido. Realização preserva ordem/IDs, fila de respostas e retry do mesmo comando;
conflitos pedem recarga explícita com escolha local preservada. Concluir exige confirmação
e fila resolvida. Pontos/resoluções vêm exclusivamente do backend após concluir.

Conta abre páginas de histórico e retoma o snapshot confirmado em outra sessão. Visitante
não possui histórico permanente e depende da sessão/validade de 24h; login avisa que não
transfere a tentativa. Logout/troca de conta/rejeições limpam conteúdo privado e abortam
requisições anteriores. Dados ficam só em memória, sem storage/cache/service worker.
[Fluxos e limites](../docs/mock-exam-interface.md) e [matriz de testes](../openspec/changes/mock-exam-attempts/verification.md).

`npm run test:identity` inclui também oito cenários em attempts.real.spec.ts: visitante
390/1280/teclado/dialog, resposta confirmada com confirmação perdida, retomada após
restart, duas contas/abas, edição posterior do acervo, login sem transferência e expiração.
São 14 cenários reais no comando, incluindo ausência de download do renderer após concluir.
A apresentação está no código inicial; o validador corretivo permanece em módulo tardio.
Todos os chunks ficam sem payloads de fixtures. A inspeção do entry inicial e
JSON/DOM ativos continua proibindo contrato/material corretivo antecipado.

## Tokens visuais e tema (focus-visual-refresh)

Cores, sombras e a família tipográfica vêm só de `src/tokens.css`, com tema claro
em `:root` e escuro por `prefers-color-scheme` ou `data-theme="dark"`. Os
componentes usam `var(--…)`; `src/visual-tokens.test.ts` falha com qualquer cor
literal em outra folha de estilo e mede o contraste dos pares de tokens nos dois
temas (texto ≥4,5:1; bordas de controle e foco ≥3:1).

O seletor "Tema" da barra superior guarda só `light` ou `dark` na chave
`concurso-simulator.theme` do `localStorage` (automático remove a chave). O
`index.html` aplica essa chave antes da primeira pintura; mantenha a chave igual
à de `src/theme.ts`. Sem armazenamento, o tema segue o sistema.

A fonte Figtree vem empacotada pelo pacote `@fontsource-variable/figtree` (OFL),
sem requisições externas. O Vitest processa CSS (`test.css.include` no
`vite.config.ts`) só para que os testes leiam as folhas com `?raw`.

Limite: diagramas SVG das aulas nativas mantêm as próprias cores e placa clara
também no tema escuro.

Folhas por área (grupos 2, 3 e 5): `src/styles/shell.css` (navegação, barra
superior, sessão e início) é importada pelo `App`. Regras globais de base
(botões, campos, seções) continuam em `style.css`, que o `main.tsx` carrega antes
do `App` para que as folhas de área prevaleçam.
