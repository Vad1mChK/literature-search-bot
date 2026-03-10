plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    application
    id("org.jlleitschuh.gradle.ktlint") version "12.0.3"
}

group = "com.vad1mchk.litsearchbot"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

val libVersions =
    mapOf(
        "dotenv" to "6.5.1",
        "tg-bot" to "6.3.0",
        "exposed" to "0.61.0",
        "sqlite-jdbc" to "3.51.1.0",
        "kotlinx-datetime" to "0.6.0",
        "logback" to "1.5.25",
        "apache-poi" to "5.5.1",
        "apache-pdfbox" to "3.0.6",
        "apache-compress" to "1.26.0",
    )

dependencies {
    // Environment variables
    implementation("io.github.cdimascio:dotenv-kotlin:${libVersions["dotenv"]}")
    // Kotlin datetime support
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:${libVersions["kotlinx-datetime"]}")
    // Kotlin serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    // Telegram Bot API
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:${libVersions["tg-bot"]}")
    // Logging
    implementation("ch.qos.logback:logback-classic:${libVersions["logback"]}")
    // PDF Extraction
    implementation("org.apache.pdfbox:pdfbox:${libVersions["apache-pdfbox"]}")
    implementation("org.apache.poi:poi-ooxml:${libVersions["apache-poi"]}")
    implementation("org.apache.commons:commons-compress:${libVersions["apache-compress"]}")
    // Database: Exposed
    implementation("org.jetbrains.exposed:exposed-core:${libVersions["exposed"]}")
    implementation("org.jetbrains.exposed:exposed-dao:${libVersions["exposed"]}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${libVersions["exposed"]}")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:${libVersions["exposed"]}")
    // SQLite Driver & Connection Pooling
    implementation("org.xerial:sqlite-jdbc:${libVersions["sqlite-jdbc"]}")
    // HTTP client
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.vad1mchk.litsearchbot.MainKt")
}

ktlint {
    ignoreFailures = false
}

tasks.test {
    useJUnitPlatform()
}
