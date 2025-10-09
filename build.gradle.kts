plugins {
    kotlin("jvm") version "2.2.20"
    application
}

group = "com.aymanetech"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.aymanetech.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}