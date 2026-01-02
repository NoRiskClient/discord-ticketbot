import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
    id("com.diffplug.spotless") version "8.1.0"
}

group = "gg.norisk"
version = "2.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
}

dependencies {
    implementation("net.dv8tion", "JDA", "5.6.1") {
        exclude("club.minnced", "opus-java")
        exclude("com.google.crypto.tink", "tink")
    }

    implementation("org.jdbi", "jdbi3-oracle12", "3.44.0")
    implementation("org.xerial", "sqlite-jdbc", "3.50.3.0")
    implementation("org.apache.logging.log4j", "log4j-api", "2.25.1")
    implementation("org.apache.logging.log4j", "log4j-core", "2.25.1")
    implementation("org.apache.logging.log4j", "log4j-slf4j2-impl", "2.25.1")
    implementation("me.carleslc.Simple-YAML", "Simple-Yaml", "1.8.3")
    implementation("com.fasterxml.jackson.core", "jackson-databind", "2.16.1")
    implementation("com.github.Ryzeon", "discord-html-transcripts", "2.1")
    implementation("com.github.ben-manes.caffeine", "caffeine", "3.2.3")

    compileOnly("org.projectlombok", "lombok", "1.18.40")
    annotationProcessor("org.projectlombok", "lombok", "1.18.40")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<ShadowJar> {
    manifest {
        attributes["Main-Class"] = "gg.norisk.ticketbot.Main"
    }
    archiveFileName.set("discord-ticketbot.jar")
}

spotless {
    java {
        googleJavaFormat("1.33.0")
    }
}

tasks.register("checkAnnotations") {
    doLast {
        val forbiddenPackages = listOf(
            "javax.annotation",
            "org.jspecify.annotations",
            "edu.umd.cs.findbugs.annotations"
        )

        val nullabilityAnnotations = listOf(
            "nullable",
            "notnull",
            "nonnull",
            "nullmarked",
            "nullunmarked"
        )

        val forbiddenNames = forbiddenPackages.flatMap { pkg ->
            nullabilityAnnotations.map { ann ->
                "$pkg.$ann"
            }
        }

        val files = fileTree("src") {
            include("**/*.java", "**/*.kt")
        }

        val badUsages = files.filter { file ->
            val text = file.readText().lowercase()
            forbiddenNames.any { name -> text.contains(name.lowercase()) }
        }

        if (!badUsages.isEmpty) {
            println("❌ Forbidden nullability annotations found:")
            badUsages.forEach { println(" - ${it.path}") }
            throw GradleException(
                "Use JetBrains Nullability Annotations instead of javax, jspecify, etc."
            )
        } else {
            println("✅ No forbidden nullability annotations found.")
        }
    }
}