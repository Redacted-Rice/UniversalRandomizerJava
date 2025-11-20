plugins {
    `java-library`
    jacoco
}

group = "redactedrice"
version = "0.5.0"

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // LuaJ for embedded Lua execution
    implementation("org.luaj:luaj-jse:3.0.1")

    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")

    api("org.apache.commons:commons-math3:3.6.1")
    implementation("com.google.guava:guava:33.2.1-jre")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(20)
    }
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude("**/support/**", "**/logger/**")
            }
        })
    )

    reports {
        xml.required = false
        html.required = true
        html.outputLocation.set(file("${rootProject.projectDir}/coverage/${project.name}"))
        csv.required = false
    }
}

// Coverage task - runs tests and generates coverage report
tasks.register("coverage") {
    group = "verification"
    description = "Runs tests and generates code coverage report"
    dependsOn("test", "jacocoTestReport")
}

// Helper task for running coverage on specific tests
// Usage: ./gradlew :libUniversalRandomizerJava:coverageForTests --tests "*EnumRegistryTest"
tasks.register("coverageForTests") {
    group = "verification"
    description = "Runs specific tests and generates code coverage report. Use --tests flag to filter."
    dependsOn("test", "jacocoTestReport")
}

// Task to sync randomizer Lua files from UniversalRandomizerCore into resources
// Using Sync instead of Copy should ensure files are kept up to date
tasks.register<Sync>("copyRandomizerFiles") {
    group = "build"
    description = "Syncs randomizer Lua files from UniversalRandomizerCore into resources"

    from("${rootProject.projectDir}/UniversalRandomizerCore/randomizer") {
        include("*.lua")
    }
    into("${projectDir}/src/main/resources/randomizer")

    doLast {
        // Generate manifest file listing all copied files. We use this in our
        // resource extractor to extract everything out without having to
        // list everything manually
        val manifestFile = file("${projectDir}/src/main/resources/randomizer/.manifest")
        val files = fileTree("${projectDir}/src/main/resources/randomizer") {
            include("*.lua")
            exclude(".manifest")
        }.files.map { it.name }.sorted()
        manifestFile.writeText(files.joinToString("\n"))
    }
}

// Make processResources depend on copyRandomizerFiles to ensure files are copied before packaging
tasks.named("processResources") {
    dependsOn("copyRandomizerFiles")
}
