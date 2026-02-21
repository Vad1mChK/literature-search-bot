plugins {
    kotlin("jvm") version "2.3.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.0.3"
}

group = "com.vad1mchk.litsearchbot"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

kotlin {
    jvmToolchain(17)
}

ktlint {
    ignoreFailures = false
    // disabledRules.set(setOf("no-wildcard-imports"))
}

tasks.test {
    useJUnitPlatform()
}
