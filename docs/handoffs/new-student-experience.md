# Retomada em outra máquina — 2026-11-08

[Issue #158](https://github.com/LordBiGoDoNE/concurso-simulator/issues/158) ·
[PR #159, rascunho](https://github.com/LordBiGoDoNE/concurso-simulator/pull/159).

## Branch com a implementação atual

`task/new-student-experience/student-journey`, destinada a `epic/new-student-experience`.
A base da épica continua main f61d57d. O envio anterior foi 50edf8f; os refinamentos
seguintes estão sendo enviados por solicitação explícita do usuário para continuar
na máquina de casa. Não houve merge ou publicação da aplicação.

Inclui entrada/biblioteca/rotas públicas, prática recolhível após a aula, questões
carregadas somente após o botão, índice Nesta Disciplina - nome da disciplina
antes do título e leitor usando toda a largura. As 51 aulas são nativas do React,
sem iframe ou navegação antiga: teoria, exemplos, tabelas, fórmulas, esquemas e
fontes preservados; exercícios pelo painel de prática. Próxima aula/Aula anterior
retornam ao topo. Atalhos de revisão abrem a aula nativa.

Conteúdo gerado: 51 JSONs e manifesto em frontend/src/lesson-documents, 744702 bytes.
Fontes estáticas/editoriais preservadas. O gerador e a validação estão versionados;
não editar os JSONs manualmente. Manifesto/digests e limites estão na verificação da spec.

## O que ficou somente planejado

Prática independente de cargo NÃO foi implementada. O código atual ainda consulta
prática por assunto/cargo e mostra o seletor. A próxima mudança está formalizada em:

- [Issue #161](https://github.com/LordBiGoDoNE/concurso-simulator/issues/161).
- [PR documental #162, rascunho](https://github.com/LordBiGoDoNE/concurso-simulator/pull/162).
- Branch `task/lesson-practice-by-topic/proposal`, commit 069b027,
  base `epic/lesson-practice-by-topic` em f61d57d.
- Spec `openspec/changes/lesson-practice-by-topic/` nessa branch: proposta, design,
  requisitos, tarefas e verificação. A publicação contém somente documentação.

Grupos pendentes: 1 núcleo/persistência; 2 HTTP; 3 interface, também dependente
#158; 4 verificação integrada/entrega local. Nenhum grupo foi autorizado.
Aprofundamento por matéria será uma proposta futura. Não misturar as branches,
transportar stacks ou integrar outra spec implicitamente.

Regra registrada no AGENTS.md e contexto OpenSpec: planejamento → spec + issue
vinculadas → apresentação dos grupos → confirmação explícita antes de implementar.
O usuário pediu parar e continuar em outra máquina; o envio não autoriza implementar.

## Obter o código

Clone novo:

```sh
git clone --branch task/new-student-experience/student-journey https://github.com/LordBiGoDoNE/concurso-simulator.git
cd concurso-simulator
```

Clone existente: conferir git status e preservar qualquer alteração local antes
de trocar a branch. Se ela já existir:

```sh
git fetch origin
git switch task/new-student-experience/student-journey
git pull --ff-only origin task/new-student-experience/student-journey
```

Se ainda não existir, após o fetch usar
`git switch --track origin/task/new-student-experience/student-journey`.
Não fazer reset, force-push, merge das épicas ou apagar o trabalho da máquina de casa.

## Executar e conferir

Node24; JDK25/Docker Linux/Compose para backend e regressões integradas. Seguir
frontend/README.md e infra/local-app/README.md. Para leitura sem API:

```sh
cd frontend
npm ci
npm run dev
```

Abrir http://127.0.0.1:5173/. As aulas nativas vêm do próprio frontend e não precisam
do servidor de material antigo. Prática real exige API e acervo próprio local.
Banco, carga, volumes, processos, portas customizadas, logs/capturas, caches,
node_modules e credenciais deste PC não são transportados pelo Git. O ambiente
18088 desta máquina não deve ser presumido na outra; usar seus procedimentos locais.
Fontes de conteúdo e instruções de preparação estão no repo; não copiar segredos
ou snapshots privados para uma branch.

Validações de conteúdo, pela raiz:

```sh
python scripts/compile_native_lessons.py --check
python -m unittest discover -s scripts/tests -p test_native_lessons.py -v
```

OpenSpec fixado em 1.14.1: `validate new-student-experience --strict` nesta branch;
a spec lesson-practice-by-topic é validada em sua própria branch documental.

## Evidências e pendências

Ver openspec/changes/new-student-experience/verification.md para execuções e limites:
build, 212 unitários na rodada ampla; 26 cenários Chromium/12 Edge; nova verificação
de topo em dois cenários por navegador e três testes existentes de biblioteca.
A regressão integrada teve 18 aprovações e seis seletores legados falhos; depois
os dez cenários de revisão passaram na revalidação isolada, cobrindo os 24 cenários
pelas duas execuções. Não declarar uma única execução integrada completa aprovada.
Suíte de imagens Docker não foi executada após os refinamentos.

Esses resultados são desta máquina, não execuções na máquina de casa. As aulas e
práticas foram conferidas também no preview com acervo local; dados de smoke ficaram
somente naquele banco. Aceite final, revisão independente SOL, gate cumulativo,
merge, arquivamento e deploy continuam pendentes. O envio atual é para retomada,
sem iniciar novo lote, review por modelo ou implementação da #161.
