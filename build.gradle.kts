plugins {
    jacoco
}

repositories {
    mavenCentral()
}

tasks.register("test") {
    group = "verification"
    description = "Runs tests for the library"
    dependsOn(":libUniversalRandomizerJava:test")
}

tasks.register("coverage") {
    group = "verification"
    description = "Runs tests and generates coverage report for the library"
    dependsOn(":libUniversalRandomizerJava:coverage")
}

tasks.register("runExample") {
    group = "application"
    description = "Runs the example application"

    val appProject = project.findProject(":appExample")
    if (appProject != null) {
        dependsOn(":appExample:run")
    } else {
        doFirst {
            throw GradleException( "Unexpected error. AppExample should have been automatically included")
        }
    }
}

tasks.register<JacocoReport>("coverageCombined") {
    group = "verification"
    description = "Runs tests and app example and generates a combined coverage report"

    val libProject = project(":libUniversalRandomizerJava")
    val appProject = project.findProject(":appExample")

    if (appProject == null) {
        doFirst {
            throw GradleException( "Unexpected error. AppExample should have been automatically included")
        }
    }

    val taskDependencies = mutableListOf<String>(":libUniversalRandomizerJava:test")
    if (appProject != null) {
        taskDependencies.add(":appExample:runWithCoverage")
    }
    dependsOn(taskDependencies)

    val executionDataFiles = mutableListOf<File>(
        file("${libProject.layout.buildDirectory.get().asFile}/jacoco/test.exec")
    )

    if (appProject != null) {
        executionDataFiles.add(
            file("${appProject.layout.buildDirectory.get().asFile}/jacoco/run.exec")
        )
    }

    executionData(executionDataFiles)

    sourceDirectories.setFrom(libProject.files("src/main/java"))
    classDirectories.setFrom(
        files("${libProject.layout.buildDirectory.get().asFile}/classes/java/main").map {
            fileTree(it) {
                exclude("**/support/**", "**/logger/**")
            }
        }
    )

    reports {
        xml.required = false
        html.required = true
        html.outputLocation.set(file("${rootProject.projectDir}/coverage/combined"))
        csv.required = false
    }
}

tasks.register("coverageExample") {
    group = "verification"
    description = "Runs the example app with coverage and generates coverage report"

    val appProject = project.findProject(":appExample")
    if (appProject != null) {
        dependsOn(":appExample:coverage")
    } else {
        doFirst {
            throw GradleException( "Unexpected error. AppExample should have been automatically included")
        }
    }
}
