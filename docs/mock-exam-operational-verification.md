# Verificação operacional de tentativas

Execução local deliberada de 2026-10-15, PostgreSQL18.6 descartável e dados sintéticos.
Nenhum dado/credencial do acervo aprovado local foi usado. Não altera main ou deploy.

## Cenário exercitado

1. Flyway V1–V5 em schema novo, JPA validate; conta, identidade externa, sessão JDBC,
   acervo aprovado sintético, tentativa de conta em andamento/respondida e concluída,
   visitante válido e visitante vencido há uma hora.
2. Executar o JAR real anterior ao recurso, commit `54896dae37a65dd0cd5b1cbc9f03fe387eb595a8`,
   sobre o mesmo banco V5, com Flyway padrão habilitado. Sem alterar/apagar migrações.
   Identidade/sessão e consulta pública continuam funcionais; endpoints novos permanecem
   fechados. As quatro tentativas e conta/identidade/acervo são preservados.
3. Encerrar o processo anterior e iniciar a aplicação atual sobre o mesmo banco.
   Conta retoma seleção/ordens/resposta/versão originais e resultado fechado; mesma
   sessão autentica por HTTP. Visitante válido continua acessível. Startup remove
   somente o vencido com cascata de item/comando; três tentativas ficam preservadas.
4. Contextos/processos e container são encerrados ao final. Logs com detalhes de
   runtime/conexão ficam ignorados no build; o marcador final é `OPERATIONAL_PASS`.

Resultado: **PASS**, `mockExamOperationalCheck` concluiu em 25 segundos nesta execução.
JAR anterior observado: SHA256 `f00cc6330eee1e772b1a08507de310885e062f1a09836f9f64d23aecd822368b`.
Isso demonstra rollback/reinício nesse schema/dataset e binário; não cobre todas as
falhas operacionais/carga, backups externos ou o deployment ainda não escolhido.

## Reproduzir

Gerar o JAR anterior do commit fixado em diretório isolado dentro de backend/build,
sem trocar a branch ativa, por exemplo git archive/Expand-Archive desse commit e
`gradlew.bat --no-daemon bootJar` no backend extraído, usando Java25. Conferir a origem
antes de executar um JAR. A execução usada preservou a branch e os commits da stack.

Depois, da raiz do backend:

```powershell
./gradlew.bat --no-daemon --console=plain mockExamOperationalCheck "-ProllbackJar=C:/caminho/para/artefato-anterior-aprovado.jar"
```

A tarefa usa exclusivamente o classpath de testes, cria seu próprio PostgreSQL,
contas/sessões/acervo/tentativas e processos localhost. Nenhuma fixture entra no
bootJar. Não recebe DB_URL de produção nem fornece rota de administração real.
É uma verificação adicional deliberada; `build`/CI não inicia esse processo anterior
nem necessita baixar um binário histórico. CI executa os testes determinísticos de
schema/upgrade/JPA/expiração, HTTP e navegador real com restart da aplicação atual.

## Rollback operacional

Voltar a aplicação/SPA anterior aprovada, mantendo o schema V5 e seus dados. O fluxo
novo fica fechado e pode ser reativado com a versão atual; não executar downmigration,
Flyway clean, DDL Hibernate update ou exclusão de tentativas como estratégia de rollback.
O código anterior aceitou o schema futuro nessa execução, com contratos existentes
preservados. Deployment/backup/restauração pública continuam exigindo a spec própria.
