plugins {
    java
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

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.6.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}