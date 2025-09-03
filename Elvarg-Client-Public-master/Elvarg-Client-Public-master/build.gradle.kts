plugins {
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.openjfx.javafxplugin") version "0.0.13"
}

repositories { mavenCentral() }

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } // you have JDK 21
}

tasks.withType<JavaCompile> {
    options.release.set(11)      // emit Java 8 bytecode
    options.encoding = "UTF-8"
}

application {
    // Most RSPS clients start here; we can switch to RuneLite main later if needed
    mainClass.set("com.runescape.ClientLauncher")
    applicationDefaultJvmArgs = listOf("--add-modules=javafx.base,javafx.graphics,javafx.controls")
}

javafx {
    version = "16"
    modules = listOf("javafx.base", "javafx.graphics", "javafx.controls")
}

dependencies {
    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    // Core libs used all over this codebase
    implementation("com.google.guava:guava:32.1.3-jre")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("commons-io:commons-io:2.15.1")
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("org.apache.commons:commons-text:1.10.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.12")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.12")

    // DI / annotations used by the RuneLite bits
    implementation("com.google.inject:guice:4.2.3")
    implementation("javax.inject:javax.inject:1")
    implementation("com.google.code.findbugs:jsr305:3.0.2")      // @Nullable, @Nonnull, @ThreadSafe

    // JetBrains @Nullable (some files use this too)
    compileOnly("org.jetbrains:annotations:24.1.0")
}

tasks.named<Jar>("jar") {
    manifest { attributes["Main-Class"] = "com.runescape.Client" }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("elvarg-client")
    archiveClassifier.set("all")
    archiveVersion.set("")
}
sourceSets {
    named("main") {
        java {
            setSrcDirs(listOf("src/main/java"))
            // include core client and the light RuneLite API/ws messages the code uses
            include("com/runescape/**", "net/runelite/api/**", "net/runelite/http/**")
            // exclude desktop/discord/gpu bits that bring LWJGL/JOCL etc
            exclude(
                "net/runelite/client/RuneLite.java","net/runelite/client/ImagePacker.java",
                "net/runelite/discord/**",
                "net/runelite/rlawt/**"
            )
        }
        resources {
            setSrcDirs(listOf("src/main/resources"))
        }
    }
}
dependencies {
  compileOnly("javax.annotation:javax.annotation-api:1.3.2")
}
sourceSets {
  named("main") {
    java {
      include("net/runelite/rs/**")
    }
  }
}
sourceSets {
  named("main") {
    java {
      // start from a clean slate for includes/excludes
      setExcludes(emptySet())
      setIncludes(emptySet())

      setSrcDirs(listOf("src/main/java"))
      include(
        "com/runescape/**",
        "net/runelite/api/**",
        "net/runelite/http/**",
        "net/runelite/rs/**",
        "net/runelite/mapping/**",
        // include only this one file from net.runelite.client
        "net/runelite/client/RuneLite.java","net/runelite/client/ImagePacker.java"
      )
    }
    resources { setSrcDirs(listOf("src/main/resources")) }
  }
}







