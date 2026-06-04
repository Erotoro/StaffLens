import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "8.3.6"
}

group = "dev.stafflens"
version = "1.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://jitpack.io")
    maven("https://mvn.lib.co.nz/public")
    maven("https://repo.essentialsx.net/releases/")
    maven("https://repo.frostcast.net/repository/maven-releases/")
}

dependencies {
    // Compile against the lowest supported API (1.21) so the single JAR stays forward-compatible
    // across the whole 1.21 -> 26.1 range. Only stable Bukkit/Paper/Adventure/Folia APIs are used.
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    // Real external APIs used directly by the plugin.
    compileOnly("net.luckperms:api:5.4")
    compileOnly("com.github.LeonMangler:PremiumVanishAPI:2.9.18-2") { isTransitive = false }
    compileOnly("me.libraryaddict.disguises:libsdisguises:11.0.13") { isTransitive = false }

    // Compatibility shims for the other integrations are vendored in src/main/java.

    // Database
    implementation("org.xerial:sqlite-jdbc:3.47.0.0")
    implementation("com.mysql:mysql-connector-j:9.2.0")
    implementation("com.zaxxer:HikariCP:5.1.0") {
        exclude(group = "org.slf4j")
    }

    // Metrics (must be shaded and relocated)
    implementation("org.bstats:bstats-bukkit:3.2.1")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveClassifier.set("")
        relocate("com.zaxxer.hikari", "dev.stafflens.libs.hikari")
        relocate("org.bstats", "dev.stafflens.libs.bstats")
    }

    named("build") {
        dependsOn(named("shadowJar"))
    }
}
