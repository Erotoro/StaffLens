import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "8.3.6"
}

group = "dev.stafflens"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://jitpack.io")
    maven("https://repo.essentialsx.net/releases/")
    maven("https://repo.frostcast.net/repository/maven-releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    // Real external APIs used directly by the plugin.
    compileOnly("net.luckperms:api:5.4")

    // Compatibility shims for the other integrations are vendored in src/main/java.

    // Database
    implementation("org.xerial:sqlite-jdbc:3.47.0.0")
    implementation("com.mysql:mysql-connector-j:9.2.0")
    implementation("com.zaxxer:HikariCP:5.1.0") {
        exclude(group = "org.slf4j")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveClassifier.set("")
        relocate("com.zaxxer.hikari", "dev.stafflens.libs.hikari")
    }

    named("build") {
        dependsOn(named("shadowJar"))
    }
}
