// Tarefas operacionais deliberadas: fixtures de navegador, verificações de rollback/upgrade e
// ferramentas locais sem web. Nenhuma roda no build, nos testes ou na inicialização normal.
// Aplicado por build.gradle.kts; nomes, grupos, descrições e argumentos fazem parte do contrato
// com os configs do Playwright, os scripts Python e a documentação operacional.

val sourceSets = the<SourceSetContainer>()
val mainSources = sourceSets.getByName("main")
val testSources = sourceSets.getByName("test")

/** Processo isolado a partir do classpath de teste; nunca entra no bootJar. */
fun registerTestProcess(name: String, taskGroup: String, text: String, main: String, configure: JavaExec.() -> Unit = {}) =
    tasks.register<JavaExec>(name) {
        group = taskGroup
        description = text
        dependsOn("testClasses")
        classpath = testSources.runtimeClasspath
        mainClass.set(main)
        configure()
    }

/** Ferramenta local sem web a partir do classpath principal, com entrada do terminal. */
fun registerLocalTool(name: String, text: String, main: String, configure: JavaExec.() -> Unit = {}) =
    tasks.register<JavaExec>(name) {
        group = "application"
        description = text
        classpath = mainSources.runtimeClasspath
        mainClass.set(main)
        standardInput = System.`in`
        configure()
    }

fun rollbackJarArgs(task: JavaExec) {
    if (project.hasProperty("rollbackJar")) task.args(project.property("rollbackJar").toString())
}

registerTestProcess("identityBrowserFixture", "verification",
    "Start isolated PostgreSQL and test-only OIDC/API for Playwright",
    "br.com.concursosimulator.identity.IdentityBrowserFixture")

registerLocalTool("questionBankLocal",
    "Deliberate nonweb question package preview/import; never runs on startup or build",
    "br.com.concursosimulator.questionbank.infrastructure.local.QuestionBankLocal") {
    if (project.hasProperty("localArgs")) args(project.property("localArgs").toString().split(" "))
}

registerTestProcess("mockExamOperationalCheck", "verification",
    "Deliberate isolated PostgreSQL rollback/restart check using an approved previous JAR",
    "br.com.concursosimulator.mockexam.MockExamOperationalFixture") { rollbackJarArgs(this) }

registerLocalTool("parameterizationLocal",
    "Deliberate nonweb numeric template preview/register/decision; never runs on startup or build",
    "br.com.concursosimulator.parameterization.infrastructure.local.TemplateLocal") {
    dependsOn("classes")
    if (project.hasProperty("templateAction")) {
        val action = project.property("templateAction").toString()
        args(action, "--editorial-local", "true", "--root", project.property("templateRoot").toString(),
            if (action == "decide") "--decision" else "--definition", project.property("templateFile").toString())
    }
}

registerTestProcess("parameterizedOperationalCheck", "verification",
    "Deliberate isolated V5/V9 upgrade and compatible rollback check; requires approved V5 JAR",
    "br.com.concursosimulator.parameterization.ParameterizedOperationalFixture") { rollbackJarArgs(this) }

// Synthetic PostgreSQL only.
registerTestProcess("subjectReviewOperationalCheck", "verification",
    "Check V9/V10 and mixed snapshots across separate compatible JAR processes",
    "br.com.concursosimulator.mockexam.SubjectReviewOperationalFixture") {
    dependsOn("bootJar")
    args(tasks.named<AbstractArchiveTask>("bootJar").get().archiveFile.get().asFile.absolutePath)
}

// Separate helper image/classpath; never copied into the production bootJar or startup.
tasks.register<Sync>("localDeliveryFixtureClasses") {
    group = "verification"
    dependsOn("testClasses")
    into(layout.buildDirectory.dir("local-delivery-fixture"))
    from(mainSources.output) { into("classes") }
    from(testSources.output) { into("classes") }
    from(configurations.named("testRuntimeClasspath")) { into("lib") }
}

// Explicit local recovery on an explicitly configured local PostgreSQL.
registerLocalTool("administrativeRecovery",
    "Deliberate password recovery for the existing ADM on an explicitly configured local PostgreSQL",
    "br.com.concursosimulator.identity.infrastructure.AdministrativeRecovery") {
    dependsOn("classes")
    args("--recover-admin")
}

registerTestProcess("administrativeBrowserFixture", "verification",
    "Isolated ADM/OIDC browser fixture on dedicated ports; never packaged in bootJar",
    "br.com.concursosimulator.identity.AdministrativeBrowserFixture")

registerTestProcess("topicPracticeBrowserFixture", "verification",
    "Isolated topic-practice fixture on ports 19160/19161; never packaged in bootJar",
    "br.com.concursosimulator.mockexam.TopicPracticeBrowserFixture")
