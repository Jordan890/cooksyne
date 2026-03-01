// Add a new Gradle Kotlin DSL build for the ai-remote adapter module
plugins {
    java
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.jordanahlers.cartandcook.adapters"
version = "0.0.1-SNAPSHOT"
description = "AI Remote Adapter"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    // Core module
    implementation(project(":core"))

    // Web client / REST support for remote AI calls
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// This project is a library module (no Spring Boot main). Disable bootJar to avoid "mainClass" resolution errors
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named("jar") {
    enabled = true
}
