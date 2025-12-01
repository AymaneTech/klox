plugins {
    kotlin("jvm") version "2.2.20"
    application
}

group = "com.aymanetech"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.aymanetech.Lox")
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

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.aymanetech.Lox"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
