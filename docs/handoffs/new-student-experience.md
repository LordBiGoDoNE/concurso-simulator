# Retomada no notebook — new-student-experience

Atualização: 2026-11-08. [Issue #158](https://github.com/LordBiGoDoNE/concurso-simulator/issues/158).

## O que está disponível

- Spec OpenSpec em `openspec/changes/new-student-experience/`, criada pela CLI1.14.1.
- Proposta, requisitos, design, tarefas e verificação ligados à issue.
- Protótipo navegável em `design/prototypes/new-student-experience/index.html`.
- Implementação React, testes e documentação na branch
  `task/new-student-experience/student-journey`, destinada a `epic/new-student-experience`.
- Entrada por cargo, biblioteca local, URLs públicas restauráveis e disponibilidade
  de prática. As realizações mantêm os mecanismos anteriores de identidade/fila.
- Regra permanente em AGENTS.md e no contexto OpenSpec: criar a issue junto com a
  spec e manter links nos dois sentidos, sem duplicar issues existentes.

## Obter o código

Clone novo, em uma pasta de sua escolha:

```sh
git clone --branch task/new-student-experience/student-journey https://github.com/LordBiGoDoNE/concurso-simulator.git
cd concurso-simulator
```

Em clone existente, confira se `git status` está limpo antes de trocar de branch:

```sh
git fetch origin
git switch --track origin/task/new-student-experience/student-journey
```

Se essa branch já existir localmente, use `git switch task/new-student-experience/student-journey`
e confira a referência remota antes de continuar. Não descartar trabalho local.

## Conferir e executar

Requer Node24. Não copiar `node_modules`, caches ou configurações privadas do PC.

```sh
cd frontend
npm ci
npm run build
npm test
npm run dev
```

Preview padrão em `http://127.0.0.1:5173/`. A entrada e as aulas funcionam sem API.
Práticas reais exigem backend acessível e acervo próprio disponível; nenhum dado
do PC foi transferido ou importado na VPS. O arquivo frontend/README.md e o
procedimento infra/local-app/README.md descrevem os ambientes existentes.

OpenSpec pela raiz do repositório, com a versão fixada:

```powershell
$env:OPENSPEC_TELEMETRY='0'
npx --yes @fission-ai/openspec@1.14.1 status --change new-student-experience
npx --yes @fission-ai/openspec@1.14.1 validate new-student-experience --strict
```

Para testes de navegador, instalar Chromium do Playwright e executar
`npm run test:e2e` em frontend; o cenário opcional offline precisa de STUDY_ROOT
apontando para o pacote montado por scripts/build_pages.py. O arquivo
playwright.edge.config.ts exige Microsoft Edge instalado. Não usar contas reais
ou banco de produção nas fixtures.

## Evidências e pendências

Última validação local do código: 2026-11-05, build aprovado,206 unitários,
21 cenários Chromium,7 Edge, pacote estático e OpenSpec strict aprovados.
Esses números são evidência do PC, não uma execução no notebook. Logs/capturas,
dependências, processos locais, credenciais e bancos permanecem fora do Git.

O envio foi autorizado para continuar em outra máquina; não é aceite visual ou
revisão independente. Próximo passo: testar a experiência com o usuário e então
executar a revisão de lote SOL6.1xHigh conforme AGENTS.md e docs/review-quota.md.
Corrigir/revalidar achados antes de solicitar integração. Gate cumulativo final,
merge, arquivamento, importação e deploy continuam sem autorização nesta entrega.

O PR de rascunho e seus checks serão acompanhados pela issue #158.
