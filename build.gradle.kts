plugins {
    kotlin("jvm") version "2.0.21"
    application
}

group = "com.nexus.connect"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.nexus.connect.ApplicationKt")
}

val ktorVersion: String by project
val kotlinVersion: String by project



dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.valueOf("JVM_21"))
    }
}
