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

    val randomizerSourceDir = rootProject.layout.projectDirectory.dir("UniversalRandomizerCore/randomizer")
    val randomizerDestDir = layout.projectDirectory.dir("src/main/resources/randomizer")
    val manifestFile = layout.projectDirectory.file("src/main/resources/randomizer/.manifest")

    from(randomizerSourceDir) {
        include("*.lua")
    }
    into(randomizerDestDir)

    doLast {
        // Generate manifest file listing all copied files. We use this in our
        // resource extractor to extract everything out without having to
        // list everything manually
        val destDirFile = randomizerDestDir.asFile
        val files = destDirFile.listFiles { file ->
            file.isFile && file.name.endsWith(".lua") && file.name != ".manifest"
        }?.map { it.name }?.sorted() ?: emptyList()
        manifestFile.asFile.writeText(files.joinToString("\n"))
    }
}

val generateUrjVersionProperties = tasks.register("generateUrjVersionProperties") {
    group = "build"
    description = "Generates urj-version.properties from the Gradle project version"

    val urjVersion = version.toString()
    val outputFile = layout.buildDirectory.file(
        "generated/resources/redactedrice/randomizer/urj-version.properties")

    outputs.file(outputFile)

    doLast {
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText("version=$urjVersion\n")
    }
}

sourceSets.main.get().resources.srcDir(
    layout.buildDirectory.dir("generated/resources"))

// Make processResources depend on copyRandomizerFiles to ensure files are copied before packaging
tasks.named("processResources") {
    dependsOn("copyRandomizerFiles", "generateUrjVersionProperties")
}
