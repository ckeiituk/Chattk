plugins {
    kotlin("jvm") version "2.0.0"
    application
    id("org.openjfx.javafxplugin") version "0.0.13"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.6.0")
}

javafx {
    version = "17"
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("ClientKt")
}

tasks.withType<JavaExec> {
    systemProperty("prism.order", "sw")
    systemProperty("prism.text", "t2k")
}
