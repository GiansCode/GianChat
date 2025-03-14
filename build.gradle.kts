plugins {
    `java-library`
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
}

dependencies {
    compileOnly(libs.io.papermc.paper.paper.api)
    compileOnly(libs.me.clip.placeholderapi)
}

group = "gg.gianluca"
version = "1.0-SNAPSHOT"
description = "GianChat"
java.sourceCompatibility = JavaVersion.VERSION_17

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.named("processResources", Copy::class.java) {
    val props = mapOf("version" to rootProject.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    filesNotMatching("**/*.zip") {
        expand(props)
    }
}