# Verificação local —2026-11-05

Implementação local no worktree `concurso-new-student-experience`, branch
`task/new-student-experience/student-journey`, baseada na main `f61d57d`.
Estado dessa verificação em 2026-11-05: alterações sem commit. Não há revisão
independente ou integração certificada. Em 2026-11-08 o usuário autorizou o envio
ao GitHub para continuar no notebook; essa autorização não equivale a aceite visual.
Acompanhamento: [issue #158](https://github.com/LordBiGoDoNE/concurso-simulator/issues/158).

## Resultados

- `npm run build`: TypeScript e bundle Vite aprovados.
- `npm test -- --reporter=dot`:23 arquivos,206 testes aprovados.
- Pacote estático montado com `scripts/build_pages.py`:108 páginas e1757
  referências locais verificadas;9 módulos/90 respostas conferidos.
- `npm run test:e2e -- --output=test-results/chromium --reporter=line`, com
  `STUDY_ROOT` no pacote isolado:21 testes Chromium aprovados, nenhum ignorado.
- `npx playwright test --config playwright.edge.config.ts --output=test-results/edge --reporter=line`:
  7 testes Microsoft Edge aprovados.
- OpenSpec1.14.1, com `OPENSPEC_TELEMETRY=0`: change criada por `new change`,
  instruções de artefatos consultadas, status reconhecido e `validate new-student-experience --strict` aprovado.
- `git diff --check`: aprovado.

## Cobertura e conferência visual

Visitante, seleção dos dois cargos,40/43 aulas por cargo e51 na exploração livre;
busca/disciplinas, restauração de aula e filtros, atualização, Voltar/Avançar,
teclado/foco e ausência de corte horizontal em390/1440px. Endereços inválidos,
parâmetros duplicados ou privados e associação incompatível são rejeitados.

Disponibilidade cobre carga, vazio confirmado, erro, retry, contrato inválido,
resposta tardia de outro cargo e saída/retorno da área. Contagem de comandos da
aplicação confirma que navegação não cria atividades; apenas o clique explícito
inicia a prática. A requisição conserva o mecanismo existente de comando/CSRF.

Regressões existentes de identidade, logout, filas e repetição idempotente passaram.
Leitor integral testado com arquivos reais locais, CSS e figura da aula; as novas
fixtures de jornada não substituem essa verificação. O protótipo documental foi
percorrido em dois tamanhos, nos dois navegadores. Capturas conferidas visualmente.

Evidências locais ignoradas pelo Git: `backend/build/new-student-evidence/` e logs
`backend/build/new-student-chromium.log`, `new-student-edge.log` e `test-results-unit.log`.

## Preview para o usuário

Servidor local em `http://127.0.0.1:5186/`. Navegação cargo→biblioteca conferida
no navegador interno, seguida de Voltar e atualização com cargo preservado.
O serviço só escuta em loopback. Processo/PID e logs ficam em
`backend/build/new-student-preview/`.

Não há API local ativa nas portas8080/8088 nesta validação. O preview mostra essa
indisponibilidade e conserva a entrada e as aulas; não oferece prática funcional
contra dados reais. A criação e os demais estados foram exercitados com fixtures
de transporte nas suites locais, sem acessar a VPS ou o banco do operador.

## Limites e próximos gates

Não foi executada suite backend/PostgreSQL: backend e contratos HTTP não mudaram.
Não constitui auditoria WCAG completa nem aceite de estudantes reais. Nenhum
acervo foi importado; Google, HTTPS, produção, main e stable foram preservados.

O usuário testa e valida antes de revisão por modelo, conforme AGENTS.md. O envio
ao GitHub foi autorizado separadamente em 2026-11-08 para a troca de máquina.
Revisão SOL6.1xHigh não executada; cota/tokens dessa revisão não medidos, sem
estimar consumo zero. Revisão de lote, gate cumulativo e merge permanecem pendentes.
Não arquivar automaticamente.
