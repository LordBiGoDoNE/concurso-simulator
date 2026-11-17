# Organização da entrega — #160

Autorização do usuário em 2026-11-09: organizar commits/PRs e executar revisão independente
do lote após aprovação do teste local. Sem merges, publicação/deploy, carga real ou arquivamento.

Base do teste local: f61d57d5d55d6d61609815a2faa4b415ed8dca4b. A main remota avançou para
98f6f974110889ca6862a40d4e5d2fa9461dd536 pelo #163; a entrega deve preservar a experiência
de aulas nativas já integrada. O snapshot local aprovado será preservado antes da conciliação,
sem mover branches/worktrees de outras frentes. O preview local existente permanece separado.

A nova regra versionada de 2026-11-09 foi confirmada expressamente pelo usuário: não medir
nem registrar cota. Revisão independente SOL 6.1 xhigh continua obrigatória; tokens só quando
disponibilizados efetivamente pelo harness. Nenhuma nova observação de cota é coletada.

## Composição prevista

Trunk: epic/administrative-question-import, iniciada na main atualizada. Stacks v0.2.1.
Todas as camadas serão rascunhos, com código/testes/docs da mesma fronteira. A composição
abaixo distribui todos os grupos 1–6; não encerra o compromisso em um prefixo de grupo.

1. package: leitor/verificação/contratos e prévia do núcleo, especificação aprovada.
2. atomic-import: unidade atômica, evidências/recibos e V12; depende de package.
3. administrative-identity: cadastro/credencial/recuperação/ciclo de vínculo e V13;
   depende de atomic-import porque a barreira V13 protege também recibos V12.
4. administrative-auth: segurança/sessão/OIDC e HTTP de conta; depende do cadastro.
5. administrative-api: prévia/aplicação protegidas e confirmação; depende dos contratos,
   persistência e autenticação anteriores.
6. administrative-ui: interface mínima e percurso completo; depende da autenticação e
   da API usada pelo teste integrado. Inclui conciliação de App com as aulas nativas da main.

As fronteiras poderão ser ajustadas por dependências reais de compilação/startup, com
registro explícito; nenhum teste será removido do conjunto final. A separação de testes
por fronteira evita dependência de HTTP nas camadas de leitor/persistência. Não reescrever
V12/V13 para fabricar independência nem introduzir serviços temporários de produto.

## Estado

Preparação local em andamento. PRs, SHAs, checks, digest, revisão e triagem serão registrados
após execução efetiva. Implementado/verificado/aprovado localmente não significa integrado.
O gate cumulativo final da spec permanece separado e pendente.

## Conciliação e verificações operacionais

Snapshot aceito preservado em a7300aa0d7f28609e986824f0a5f98d7f6199c2e e ref de backup local.
Conciliação isolada preservada em e5c03ca, sem mover worktrees de outras frentes. Em App,
foram mantidas navegação/aulas nativas de main e a limpeza de aviso privado ao trocar UUID.
O aviso de prática removido pela #158 não foi reintroduzido.

As verificações de runtime/backup precisaram reconhecer V12/V13 e todas as novas tabelas,
sem relaxar a igualdade do inventário. O reinício confere o mesmo proprietário/versão/hash
ADM, e a fixture de entrega exige somente o ADM inicial antes de adicionar contas sintéticas.
Refs antigas de atomic-import/administrative-identity e o rascunho de autenticação foram
preservados antes de realinhar as camadas locais não publicadas. Nenhuma stack existente
ou PR de outra frente foi reorganizado. Os checks devem ser conferidos nos novos heads.

A fixture administrativa integrada da entrega usa portas 19086/19087/5276, separadas do
preview já aberto ao usuário em 18086/18087/5176. Nenhuma migração V12/V13 foi reescrita.
As contagens históricas de verification.md/verification-r2.md permanecem do teste original;
novos resultados e SHAs devem ser registrados como execução desta entrega, sem transporte
indevido de revisão anterior. O guia HTTP de conta também está em docs/administrative-access.md.
