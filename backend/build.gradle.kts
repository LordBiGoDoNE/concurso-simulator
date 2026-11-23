import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.1.1"
    id("com.diffplug.spotless") version "8.11.0"
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
    // BOM derivado do plugin: a versão do Spring Boot fica só no bloco plugins.
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
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
    // -serial: as advertências só aparecem em exceções da aplicação, que nunca são gravadas na
    // sessão JDBC nem serializadas; serialVersionUID nelas seria ruído em ~30 classes. Os atributos
    // de sessão são records Serializable, que essa advertência não cobre.
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all", "-Xlint:-serial"))
}

// Advertências do compilador quebram o build.
tasks.compileJava {
    options.compilerArgs.add("-Werror")
}

tasks.compileTestJava {
    // -try: fixtures operacionais encerram processos filhos em close() (waitFor lança
    // InterruptedException) e MigrationFailureTests abre um contexto só para vê-lo falhar.
    options.compilerArgs.addAll(listOf("-Xlint:-try", "-Werror"))
}

tasks.test {
    useJUnitPlatform()
    workingDir = projectDir // O teste de contrato lê openapi.yaml na raiz do backend.
    testLogging {
        events("passed", "skipped", "failed")
    }
    finalizedBy(tasks.jacocoTestReport)
}

// Cobertura só como relatório local (build/reports/jacoco); sem limite mínimo nem publicação.
jacoco {
    toolVersion = "0.8.14"
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}

// Formatação do Java verificada em check (logo em build); corrigir com ./gradlew spotlessApply.
spotless {
    // LF em qualquer sistema: o repositório versiona os fontes com LF.
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
    java {
        target("src/**/*.java")
        palantirJavaFormat("2.103.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

apply(from = "gradle/operational-tasks.gradle.kts")
