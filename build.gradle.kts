plugins {
    kotlin("jvm") version "1.9.24"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.mongodb:mongodb-driver-sync:4.11.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.javalin:javalin:6.3.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("org.slf4j:slf4j-simple:2.0.13")

//    BUAT DATABASE
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.sewadulu.MainKt")
}

