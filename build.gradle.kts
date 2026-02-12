plugins {
    kotlin("jvm") version "2.2.20"

    kotlin("plugin.spring") version "2.2.0"
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("io.ktor:ktor-client-cio:3.2.3")
    implementation("io.modelcontextprotocol:kotlin-sdk:0.8.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}