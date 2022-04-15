import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    kotlin("jvm") version "1.5.30"
    kotlin("plugin.serialization") version "1.5.20"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
    java
    application
}

group = "io.slama"
version = "2.1.4"

ktlint {
    debug.set(true)
    outputToConsole.set(true)
    disabledRules.add("filename")
    reporters {
        reporter(ReporterType.CHECKSTYLE)
        reporter(ReporterType.PLAIN)
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://m2.dv8tion.net/releases")
        name = "m2-dv8tion"
    }
}

dependencies {
    // test api
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.5.21")

    // kotlin api
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

    // JDA & co.
    implementation("net.dv8tion:JDA:4.3.0_331") {
        exclude("opus-java")
    }
    implementation("ch.qos.logback:logback-classic:1.2.10")
    implementation("ch.qos.logback:logback-core:1.2.10")
    implementation("org.slf4j:slf4j-api:1.7.33")

    // Others
    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("com.notkamui.libs:keval:0.8.0")
    implementation("com.notkamui.libs:kourrier:0.2.2")
    implementation("com.sun.mail:javax.mail:1.6.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    manifest {
        attributes["Main-Class"] = "io.slama.MainKt"
    }
    configurations["runtimeClasspath"]
        .filter { it.name.endsWith("jar") }
        .forEach { file ->
            from(zipTree(file.absoluteFile))
        }
}

application {
    mainClass.set("io.slama.MainKt")
}
