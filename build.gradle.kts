plugins {
    jacoco
}

repositories {
    mavenCentral()
}

tasks.register<JacocoReport>("jacocoCombinedReport") {
    group = "verification"
    description = "Generates combined code coverage report from tests and application execution"
    dependsOn(":libUniversalRandomizerJava:test", ":appExample:runWithCoverage")

    val libProject = project(":libUniversalRandomizerJava")
    val appProject = project(":appExample")

    executionData(
        file("${libProject.layout.buildDirectory.get().asFile}/jacoco/test.exec"),
        file("${appProject.layout.buildDirectory.get().asFile}/jacoco/run.exec")
    )

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
        html.outputLocation.set(file("${rootProject.projectDir}/coverage/combined/html"))
        csv.required = false
    }
}

tasks.register("coverage") {
    group = "verification"
    description = "Runs tests and app demo and generates a combined coverage report"
    dependsOn(
        ":libUniversalRandomizerJava:test",
        ":appExample:runWithCoverage",
        "jacocoCombinedReport"
    )
}
