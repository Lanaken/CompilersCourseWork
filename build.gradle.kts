plugins {
    kotlin("jvm") version "1.9.22" // укажите нужную версию Kotlin
    application
}

group = "com.example"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
}

application {
    mainClass.set("com.example.MainKt")
}

tasks.test {
    useJUnitPlatform()
}