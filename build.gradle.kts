import sun.jvmstat.monitor.MonitoredVmUtil.mainClass

plugins {
    kotlin("jvm") version "2.2.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.graalvm.buildtools.native") version "0.10.1"
    application
}

group = "com.aymanetech"
version = "1.0"

application {
    mainClass.set("com.aymanetech.Lox")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks {
    shadowJar {
        archiveFileName.set("klox.jar")
        archiveClassifier.set("")
    }
}

tasks.build {
    dependsOn("shadowJar")
}

graalvmNative {
    binaries {
        main {
            imageName.set("klox")
            mainClass.set("com.aymanetech.Lox")
            buildArgs.add("-H:+RemoveUnusedSymbols")
        }
    }
}