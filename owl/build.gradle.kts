plugins {
    kotlin("jvm")
    application
}

// The widget language: plain Kotlin, shared by the app and the `owl` CLI.
// Kept Android-free so the same sources run in CI on the JVM. Target 17 without
// pinning a toolchain, so builds work with any JDK new enough to emit 17 bytecode.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

/** The `owl` command line (check, review, programs, index): its own source set. */
val cli: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}
configurations[cli.implementationConfigurationName].extendsFrom(configurations.implementation.get())

application {
    mainClass = "com.bellatrixos.widgets.cli.MainKt"
    applicationName = "owl"
}

tasks.named<JavaExec>("run") { classpath = cli.runtimeClasspath }

dependencies {
    testImplementation("junit:junit:4.13.2")
}

/** A single runnable jar (`java -jar owl.jar check ...`) for CI. */
tasks.register<Jar>("owlJar") {
    archiveFileName = "owl.jar"
    destinationDirectory = layout.buildDirectory.dir("cli")
    manifest { attributes["Main-Class"] = "com.bellatrixos.widgets.cli.MainKt" }
    from(sourceSets.main.get().output, cli.output)
    from(cli.runtimeClasspath.filter { it.name.endsWith(".jar") }.map { zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("META-INF/*.kotlin_module", "META-INF/versions/**")
}
