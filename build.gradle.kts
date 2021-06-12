import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.0"
    kotlin("plugin.serialization") version "1.5.0"
    application
}

group = "io.slama"
version = "0.1"

repositories {
    mavenCentral()
    maven {
        url = uri("https://m2.dv8tion.net/releases")
        name = "m2-dv8tion"
    }
}

dependencies {
    // test api
    testImplementation(kotlin("test"))

    // kotlin api
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")

    // JDA & co.
    implementation("net.dv8tion:JDA:4.2.1_270") {
        exclude("opus-java")
    }
    implementation("ch.qos.logback:logback-classic:1.2.3")

    // Others
    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("com.notkamui.libs:keval:0.7.4")

}
tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("MainKt")
}