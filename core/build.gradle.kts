plugins {
    `java-library`
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.jordanahlers.cartandcook.core"
version = "0.0.1-SNAPSHOT"
description = "Core module - domain, services, and API interfaces"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    // Optional: if using javax validation, annotations, or commons libraries
    implementation("jakarta.validation:jakarta.validation-api:3.1.1")

    // Jackson for AI response parsing
    api("com.fasterxml.jackson.core:jackson-databind:2.18.0")

    // Tesseract OCR
    api("net.sourceforge.tess4j:tess4j:5.13.0")

    // Logging
    api("org.slf4j:slf4j-api:2.0.9")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.6.0")
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")
}

tasks.withType<Test> {
    useJUnitPlatform()
}