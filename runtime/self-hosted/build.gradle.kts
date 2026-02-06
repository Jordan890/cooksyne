plugins {
    java
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))
    implementation(project(":adapters:persistence-jpa"))
    implementation(project(":adapters:auth-local"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    runtimeOnly("org.postgresql:postgresql")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}