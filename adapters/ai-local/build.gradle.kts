// Add a new Gradle Kotlin DSL build for the ai-local adapter module
plugins {
    java
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.jordanahlers.cartandcook.adapters"
version = "0.0.1-SNAPSHOT"
description = "AI Local Adapter"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    // Core module
    implementation(project(":core"))

    // Spring (local components)
    implementation("org.springframework.boot:spring-boot-starter")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// This project is a library module (no Spring Boot main). Disable bootJar to avoid "mainClass" resolution errors
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    // Not all modules will have a Spring Boot main; build as a plain jar
    enabled = false
}

// Ensure the regular jar task is enabled
tasks.named("jar") {
    enabled = true
}
