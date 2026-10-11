# Frontend

Node **24 LTS**, React **19.3.0**, TypeScript **5.9.3**, Vite **8.3.3**, Vitest **5.0.3**. As versões resolvidas das dependências estão em `package-lock.json`.

```sh
cd frontend
npm ci
cp .env.example .env.local
npm run dev
```

Abra http://127.0.0.1:5173. Se o backend estiver parado, o link das aulas continua acessível e a interface informa indisponibilidade. Use o botão para consultar novamente.

`VITE_API_URL`, `VITE_MATERIAL_URL` e `VITE_STATUS_TIMEOUT_MS` são configurações **públicas**, incorporadas no build. Nunca coloque senhas ou tokens nelas. Para estudar offline, configure o link das aulas para um servidor local com o pacote estático montado; o material também pode ser aberto diretamente por arquivo.

```sh
npm test
npm run build
```

O build fica em `dist/`; não é publicado automaticamente. O GitHub Pages atual continua servindo o material original.

## Login opcional

A revisão arquitetural/JPA de 2026-10-08 é interna ao backend (ADR 0001). O frontend continua usando contratos HTTP, nunca modelos ORM. `identity-api.ts` concentra chamadas/validação de respostas; LoginPanel apresenta estados e ações. Não há uma entidade de negócio artificial para cookie/CSRF nem uma cadeia de Services de repasse.

A interface consulta auth/config e me com credentials include, sem iniciar Google automaticamente. Visitante, conectado e indisponível são estados independentes da prontidão. O link das aulas permanece em todos eles. Login prepara apenas identidade, sem histórico sincronizado nesta etapa.

Para testar Google local, veja `../backend/docs/google-login.md`: use `SPRING_PROFILES_ACTIVE=local` no backend, API `http://127.0.0.1:8080`, frontend `http://127.0.0.1:5173` e CORS para essa origem. Google desativado funciona sem client. O navegador precisa usar hostname consistente nas duas portas (cookie same-site); nenhuma credencial Google vai para VITE_*.

“Entrar com Google” é navegação à API. “Sair” busca novo CSRF e faz POST; 403 oferece nova tentativa, erro de rede não afirma saída. “Atualizar sessão” e retorno do foco consultam novamente e reconhecem expiração. Falha no login mostra mensagem genérica e remove auth=failed da URL, preservando demais parâmetros. Não há tokens Google em localStorage/sessionStorage. Testes Vitest cobrem estados/falhas/logout e Playwright cobre teclado/390px/1280px.

O prazo de 5 segundos das chamadas de identidade inclui headers e consumo do JSON necessário (config/me/CSRF); logout usa somente o status, sem ler corpo. Timers e listeners são liberados ao concluir ou falhar. LoginPanel invalida consultas anteriores e concorrentes ao logout: respostas tardias não podem sobrescrever seu resultado, e uma nova atualização continua possível. Essas proteções cuidam do transporte/estado local; autenticação, expiração e invalidação real da sessão continuam sendo responsabilidade do backend.

Testes de navegador: `npx playwright install chromium` e `npm run test:e2e`. Para incluir a regressão offline das aulas, monte o pacote com `scripts/build_pages.py` e defina `STUDY_ROOT=/caminho/absoluto/do/pacote` ao executar os testes. Sem essa variável, o teste offline é explicitamente ignorado; o CI sempre a define.

## Navegador integrado sem Google real

`npm run test:identity` requer Docker e JDK 25 (configure JAVA_HOME se necessário), além do Chromium. Playwright inicia Vite, backend real, PostgreSQL 18.6 isolado e provedor OIDC local com discovery/token/JWK. Reserve portas 5173, 18080 e 18081. Não reutiliza banco/servidor local existente nem precisa de credenciais Google. O launcher e controle de subject/cancelamento ficam exclusivamente em src/test, fora do JAR de produção.

Os três cenários cobrem visitante, login real, recuperação da sessão após reiniciar o contexto Spring mantendo PostgreSQL/cookies, UUID estável em novo login, duas identidades e dispositivos independentes, cookie, CSRF/saída, expiração real de 20s e cancelamento genérico. O timeout reduzido é exclusivo desse launcher. CI executa esse comando num job separado, sem publicação. Não substitui o smoke Google real antes do deployment.
