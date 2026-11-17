# Verificação local R2 — grupos 4, 5 e 6 da #160

Data: 2026-11-09. Base/HEAD preservados: `f61d57d5d55d6d61609815a2faa4b415ed8dca4b`.
O usuário aprovou a proposta e autorizou os três grupos completos; delimitou LoginPanel,
identity-api e integração de identidade em App para esta entrega. Conciliação com #158
permanece com a central. Não houve comunicação com outras conversas ou edição de outros
checkouts. A implementação R1 foi preservada e estendida; seu relatório permanece histórico.

Estado: **implementado/verificado localmente e teste local aprovado pelo usuário em 2026-11-09**. A aprovação foi dada após acesso ao ambiente isolado com dados sintéticos e Google simulado; não representa smoke com Google real, revisão independente ou aprovação de integração.
Sem commit, branches novas, movimentação de refs, push, PR, revisão independente, merge,
carga de conteúdo real, acesso a banco de uso, deploy ou arquivamento. Estes caminhos são
locais; nenhum backlink ou atualização pública da issue foi inventado.

## Entrega

| Grupo | Resultado local |
| --- | --- |
| 4 | Cadastro único após Flyway, nome configurável/admin, senha configurada ou temporária de 32 bytes aleatórios, emissão privada após commit, hash PBKDF2 versionado, login permanente, troca inicial/normal, sessão/CSRF rotacionados, tentativas limitadas e recuperação local deliberada; interface e guia. |
| 5 | Intenção Google de cinco minutos ligada a state/sessão/versão, provas das duas identidades, Google novo no mesmo UUID, conta existente preservada, transferência apenas de credencial/permissão quando fonte sem histórico, fonte inativa, barreira de concorrência e rollback/retry específico. |
| 6 | Permissão persistente/versão por porta independente na importação, sem fallback de UUID; ausência de destino/chave afeta só importação. DTOs/OpenAPI, descoberta privada, UI com limpeza de segredos/contexto e regressões de estudo/identidade/importação. |

V1–V11 versionadas permanecem inalteradas. V12 da R1 foi mantida. V13 é aditiva e inclui
cadastro singleton, estado ativo e guardas de escrita; dados de tentativas/comandos/recibos
não são reatribuídos. ADR 0002 complementa a decisão histórica, sem alterar ADR 0001,
AGENTS.md, config.yaml ou workflows/regras de publicação.

O cadastro usa trava singleton; vínculo usa READ COMMITTED explicitamente e trava contas
em ordem. Triggers compartilham a barreira das contas em gravações autenticadas, evitando
histórico tardio na fonte inativa. Conflito de identidade conhecido só permite retry após
rollback, em nova unidade; FK/falha desconhecida não viram sucesso.

## Execuções efetivas

Ambiente: Windows, JDK 25.0.1, Gradle Wrapper 9.8.0, Docker Desktop com PostgreSQL 18.6
Testcontainers exclusivo; frontend com dependências do lockfile instaladas pelo cache npm,
Vitest/jsdom e Chromium Playwright. Não houve Google real nem uso de credenciais reais.

```powershell
$env:JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-25.0.1.8-hotspot'
./backend/gradlew.bat -p backend --offline --no-daemon check bootJar
./backend/gradlew.bat -p backend --offline --no-daemon test --tests '*AdministrativeSessionOutageTests' --tests '*AdministrativeAccessArchitectureTests'
# Em frontend/:
npm ci --offline --ignore-scripts
npm run build
npm test -- --reporter=dot
npx --offline playwright test --config playwright.admin.config.ts --reporter=line
```

- Suíte completa final de backend: **307 casos; 306 aprovados, zero falhas/erros, um
  preexistente ignorado**, `TemplateLocalTests.symbolicLinkIsRejectedWhenTheFilesystemSupportsCreatingOne`,
  por suporte de symlink no Windows. `check bootJar` concluiu com BUILD SUCCESSFUL.
- Após essa suíte, dois testes novos adicionais de fronteira/indisponibilidade:
  **2/2 aprovados**, sem alteração de código de produção backend entre as execuções.
  Total distinto coberto: **309 casos, 308 aprovados e um ignorado**. Não se apresenta
  a execução adicional como uma segunda suíte completa. Os 29 casos R2 novos somam
  domínio 4, persistência 11, HTTP/OIDC 8, bootstrap/encoder/recuperação 4, arquitetura 1,
  indisponibilidade administrativa 1.
- Build final do frontend: TypeScript e Vite concluídos. Suíte final: **189/189 testes**,
  21 arquivos, incluindo dez cenários novos de UI administrativa. Testa limpeza de campos,
  aviso visitante, troca obrigatória, Google desligado, falhas, mudança de UUID, resposta
  atrasada e suspensão de rechecagem durante redirecionamento.
- Playwright final: **1/1 percurso completo aprovado**, 32,0 s, com banco e OIDC exclusivos
  em 18086/18087 e frontend em 5176. Cobre senha temporária emitida por recuperação da
  fixture, troca, conta Google com simulado prévio, preservação do UUID/snapshot, login
  por senha e Google, importação/replay de SIMULADO e PRATICA_AULA, rascunhos invisíveis
  ao público, reinício preservando sessão e teclado. Viewport 390 px sem rolagem horizontal.
  Capturas desktop/mobile foram inspecionadas; campos sem segredos. O teste de navegador
  não afirma ter lido a senha aleatória de um cadastro real: geração/emissão inicial é
  coberta separadamente pelos testes de bootstrap e concorrência.
- OpenAPI YAML parseado, 29 rotas e 214 referências locais resolvidas. OpenSpec estrito
  e verificação de whitespace/UTF-8 conferidos no fechamento desta entrega.
- JAR local: `backend/build/libs/concurso-simulator-0.1.0-SNAPSHOT.jar`, SHA-256
  `7dc5d58d1833c6a62e5b7a8c593f090a90a57b60e9cf89266ac96ae6b91fe3de`.
  Inspeção confirmou ausência de fixtures OIDC/navegador e configuração de senha de testes.
  Essa inspeção não é um detector universal de segredos.

Agregados privados ignorados: `backend/build/import-validation/r2-full-suite.json` e
`r2-supplemental.json`. Gradle substitui os XMLs ao executar seleção posterior; os agregados
preservam contagens/nomes/posição de cada caso na suíte sem copiar stdout, tokens ou senhas. Labels de invocações parametrizadas podem se repetir entre métodos; não se deduplicam casos apenas pelo display name. Capturas
ficam em `frontend/test-results/administrative.real-*/admin-account*.png`, também ignoradas.
Nenhum resultado R1 foi reutilizado como nova execução R2.

## Correções encontradas durante a verificação

- O aviso de conteúdo privado limpo sumia na rechecagem comum da sessão. A UI agora limpa
  avisos anteriores apenas quando outra conta conectada substitui o UUID; as regressões
  anteriores de expiração/indisponibilidade continuam aprovadas.
- O teste de reinício com captura histórica não previa a nova conta ADM/coluna active.
  Passou a exigir exatamente um ADM adicional, mesmo UUID nos dois reinícios, comparação
  exata dos campos antigos e todos os demais dados históricos intactos. Não se suprimiu
  a verificação de preservação de estudo.
- Indisponibilidade antes do CorsFilter podia deixar 503 de estudo sem CORS para origem
  conhecida. O filtro anterior à sessão preserva agora os mesmos cabeçalhos; o teste HTTP
  anterior foi mantido e aprovado, além do novo teste administrativo.
- No navegador, o vínculo concluía mas uma execução retornou à UI como visitante. O fluxo
  iniciava rechecagem da sessão antiga depois de chamar navegação. Agora mantém a suspensão
  durante o redirecionamento, aborta leituras pendentes e trata retorno por bfcache com
  rechecagem e formulário fechado. A regressão automatizada exige não liberar rechecagem
  nessa passagem; o percurso completo foi repetido e aprovado.
- A política de senha usa 15–128 caracteres Unicode sem normalização/truncamento e recusa
  UTF-16 malformado. Entrada inválida nunca pode equivaler a uma senha sentinela; permanece
  trabalho de hash comparável e erro genérico. Há regressões de fronteira/invariante.

## Responsabilidades HTTP e limites

Checklist do ADR 0001/HTTP aplicado pelo implementador: autenticação, confiança no principal,
autorização de rota, CORS/CSRF, sessões/versão, limites e redirecionamento ficam na segurança/
infraestrutura; controllers recebem ator/versão verificados e traduzem DTOs. Ações de senha/
vínculo e invariantes ficam no núcleo; JPA/SQL/transações no adaptador. Bootstrap e recuperação
não têm endpoint público. Principal continua UUID; evidência técnica contém só versão.
ArchUnit e cadeia HTTP cobrem essas fronteiras, sem afirmar proteção semântica completa.

Risco **crítico**: credencial, vínculo/transferência de acesso, concorrência, sessões e
persistência de evidência privada. Ensaios usam dados sintéticos/ambiente isolado e não
certificam Google real, operação produtiva, desempenho/capacidade ou curadoria de questões.
A saída inicial de senha é privada. Mesclagem, desvínculo/troca Google, gestão de outros ADMs,
upload UI, publicação editorial e recuperação pública continuam fora do escopo.

## Próximo passo e responsável

1. **Teste local aprovado:** o usuário respondeu “Aprovado” em 2026-11-09 após receber acesso ao ambiente sintético. Smoke com sua configuração Google real permanece complementar; nada foi aplicado ao banco de uso.
2. **Após validação e autorização:** coordenador/central organizar envio em épica/tarefas da
   #160, conciliar frontend com #158 e registrar bases/heads/PRs e dependências reais.
3. **Revisão independente futura:** somente SOL 6.1 xhigh confirmado, contexto novo/read-only,
   cota antes/depois e relatório real. Não foi executada nesta rodada; cota/tokens não foram
   medidos, não são zero. Gate cumulativo final permanece separado e também pendente.
4. Merge, publicação, carga real e arquivamento exigem autorizações próprias. Nenhuma tarefa
   foi declarada integrada/aprovada com base na implementação ou nos testes locais.
