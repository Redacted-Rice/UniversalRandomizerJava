plugins {
    application
    jacoco
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":libUniversalRandomizerJava"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(20)
    }
}

application {
    mainClass.set("redactedrice.randomizer.ExampleApp")
}

// Extract JaCoCo agent jar from zip (the Maven artifact is a zip containing the jar)
val extractJacocoAgent = tasks.register<Copy>("extractJacocoAgent") {
    val agentZip = configurations.detachedConfiguration(
        dependencies.create("org.jacoco:org.jacoco.agent:0.8.9")
    ).singleFile
    val destDir = layout.buildDirectory.dir("jacoco-agent").get().asFile

    from(zipTree(agentZip)) {
        include("jacocoagent.jar")
    }
    into(destDir)

    outputs.file("${destDir}/jacocoagent.jar")
}

// Task to run the application with JaCoCo agent
val runWithCoverage = tasks.register<JavaExec>("runWithCoverage") {
    group = "application"
    description = "Runs the application with JaCoCo agent to collect execution coverage"
    dependsOn("classes", extractJacocoAgent)

    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })

    val agentJar = file("${layout.buildDirectory.get()}/jacoco-agent/jacocoagent.jar")
    val runExecFile = file("${layout.buildDirectory.get()}/jacoco/run.exec")

    jvmArgs = listOf("-javaagent:${agentJar.absolutePath}=destfile=${runExecFile.absolutePath}")

    mainClass.set(application.mainClass.get())
    classpath = sourceSets["main"].runtimeClasspath

    // Set working directory to appExample root so it can find lua_modules
    workingDir = projectDir

    standardOutput = System.out
    errorOutput = System.err
}

// Coverage report from application execution - measures library coverage
tasks.register<JacocoReport>("jacocoCoverageReport") {
    group = "verification"
    description = "Generates code coverage report for library code from application execution"
    dependsOn("runWithCoverage", ":libUniversalRandomizerJava:classes")

    val libProject = project(":libUniversalRandomizerJava")

    executionData(file("${layout.buildDirectory.get()}/jacoco/run.exec"))

    sourceDirectories.setFrom(libProject.files("src/main/java"))

    classDirectories.setFrom(
        files(libProject.sourceSets["main"].output.classesDirs).map {
            fileTree(it) {
                exclude(
                    "**/support/**",
                    "**/logger/**"
                )
            }
        }
    )

    reports {
        xml.required = false
        html.required = true
        html.outputLocation.set(file("${rootProject.projectDir}/coverage/${project.name}"))
        csv.required = false
    }
}

// Coverage task - runs application with coverage and generates report
tasks.register("coverage") {
    group = "verification"
    description = "Runs application execution with coverage and generates report"
    dependsOn("runWithCoverage", "jacocoCoverageReport")
}

