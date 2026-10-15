plugins {
    java
    id("org.springframework.boot") version "4.1.1"
}

group = "br.com.concursosimulator"
version = "0.1.0-SNAPSHOT"
description = "Aplicação de estudo para concursos públicos"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.1"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-session-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.5.1")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.test {
    useJUnitPlatform()
    workingDir = projectDir // O teste de contrato lê openapi.yaml na raiz do backend.
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.register<JavaExec>("identityBrowserFixture") {
    group = "verification"
    description = "Start isolated PostgreSQL and test-only OIDC/API for Playwright"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("br.com.concursosimulator.identity.IdentityBrowserFixture")
}

tasks.register<JavaExec>("questionBankLocal") {
    group = "application"
    description = "Deliberate nonweb question package preview/import; never runs on startup or build"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("br.com.concursosimulator.questionbank.infrastructure.local.QuestionBankLocal")
    standardInput = System.`in`
    if (project.hasProperty("localArgs")) args(project.property("localArgs").toString().split(" "))
}


tasks.register<JavaExec>("mockExamOperationalCheck") {
    group = "verification"
    description = "Deliberate isolated PostgreSQL rollback/restart check using an approved previous JAR"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("br.com.concursosimulator.mockexam.MockExamOperationalFixture")
    if (project.hasProperty("rollbackJar")) args(project.property("rollbackJar").toString())
}

tasks.register<JavaExec>("parameterizationLocal") {
    group = "application"
    description = "Deliberate nonweb numeric template preview/register/decision; never runs on startup or build"
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("br.com.concursosimulator.parameterization.infrastructure.local.TemplateLocal")
    standardInput = System.`in`
    if (project.hasProperty("templateAction")) {
        val action = project.property("templateAction").toString()
        args(action, "--editorial-local", "true", "--root", project.property("templateRoot").toString(),
            if (action == "decide") "--decision" else "--definition", project.property("templateFile").toString())
    }
}
