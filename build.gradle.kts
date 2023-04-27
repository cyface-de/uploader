/*
 * Copyright (C) 2019-2023 Cyface GmbH
 *
 * This file is part of the Cyface Uploader.
 *
 *  The Cyface Uploader is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  The Cyface Uploader is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with the Cyface Uploader.  If not, see <http://www.gnu.org/licenses/>.
 */
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
//import org.jetbrains.dokka.base.DokkaBase
//import org.jetbrains.dokka.base.DokkaBaseConfiguration
import java.net.URL
/**
 * The build gradle file for the Cyface Uploader.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 1.0.0
 */
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    // This is required to configure the dokka base plugin to include images.
    // classpath("<plugin coordinates>:<plugin version>")
    classpath("org.jetbrains.dokka:dokka-base:1.7.10")
  }
}

plugins {
  id("eclipse")
  id("idea")
  // For building executable fat jars
  id("com.github.johnrengelman.shadow").version("7.1.2")
  // Plugin to display the Gradle task graph
  //noinspection SpellCheckingInspection
  id("org.barfuin.gradle.taskinfo").version("2.1.0")

  id("java-library") // FIXME: See if this works without this
  id("maven-publish")
  kotlin("jvm").version("1.8.20")

  // For static code checks
  id("io.gitlab.arturbosch.detekt").version("1.22.0")
  // For Generation of Documentation
  id("org.jetbrains.dokka").version("1.8.10")

}

group = "de.cyface"
version = "1.0.0-beta3" // Automatically overwritten by CI

// Versions of dependencies
extra["slf4jVersion"] = "2.0.7"
extra["cyfaceApiVersion"] = "2.1.2"
extra["cyfaceSerializationVersion"] = "2.3.7"
extra["googleApiClientVersion"] = "2.2.0" // transmission protocol
extra["gradleWrapperVersion"] = "7.6.1"

// Versions of testing dependencies
extra["junitVersion"] = "5.9.2"
//extra["mockitoVersion"] = "5.2.0"
extra["hamKrestVersion"] = "1.8.0.1"
extra["mockitoKotlinVersion"] = "4.1.0"
extra["dokkaVersion"] = "1.8.10"
extra["detektVersion"] = "1.22.0"

tasks.withType<JavaCompile>() {
  options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    jvmTarget = "11"
  }
}

tasks.wrapper {
  gradleVersion = project.extra["gradleWrapperVersion"].toString()
}

tasks.test {
  useJUnitPlatform()
  testLogging {
    events("passed", "skipped", "failed")

    // Also show assert message (e.g. on the CI) when tests fail to identify cause
    showExceptions = true
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    showCauses = true
    showStackTraces = true
    showStandardStreams = false
  }
}

dependencies {
  // Internal Cyface Dependencies
  implementation("de.cyface:serializer:${project.extra["cyfaceSerializationVersion"]}")
  implementation("com.google.api-client:google-api-client:${project.extra["googleApiClientVersion"]}")

  // Kotlin Support
  implementation(kotlin("stdlib-jdk8"))

  // Logging
  implementation("org.slf4j:slf4j-api:${project.extra["slf4jVersion"]}")

  // Testing Dependencies
  testImplementation(platform("org.junit:junit-bom:${project.extra["junitVersion"]}"))
  testImplementation("org.junit.jupiter:junit-jupiter-params")  // Required for parameterized tests
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
  testImplementation("org.junit.jupiter:junit-jupiter:${project.extra["junitVersion"]}")
  testImplementation("com.natpryce:hamkrest:${project.extra["hamKrestVersion"]}")
  testImplementation(kotlin("reflect")) // Required by hamkrest
  testImplementation(kotlin("test"))
  //testImplementation("org.mockito:mockito-core:${project.extra["mockitoVersion"]}")
  //testImplementation("org.mockito:mockito-junit-jupiter:${project.extra["mockitoVersion"]}")
  testImplementation("org.mockito.kotlin:mockito-kotlin:${project.extra["mockitoKotlinVersion"]}")

  // Required to create inline documentation
  dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:${project.extra["dokkaVersion"]}")
  dokkaHtmlPlugin("org.jetbrains.dokka:dokka-base:${project.extra["dokkaVersion"]}")

  // Required for Linting
  detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:${project.extra["detektVersion"]}")
}

// Definitions for the maven-publish Plugin
publishing {
  // The following repositories are used to publish artifacts to.
  repositories {
    maven {
      name = "github"
      url = uri("https://maven.pkg.github.com/cyface-de/uploader")
      credentials {
        username = (project.findProperty("gpr.user") ?: System.getenv("USERNAME")) as String
        password = (project.findProperty("gpr.key") ?: System.getenv("PASSWORD")) as String
      }
    }
    maven {
      name = "local"
      url = uri("file://${rootProject.buildDir}/repo")
    }
  }
}

// The following repositories are used to load artifacts from.
repositories {
  mavenCentral()
  maven {
    name = "local"
    url = uri("file://${rootProject.buildDir}/repo")
  }
  maven {
    name = "github"
    url = uri("https://maven.pkg.github.com/cyface-de/serializer")
    credentials {
      username = (project.findProperty("gpr.user") ?: System.getenv("USERNAME")) as String
      password = (project.findProperty("gpr.key") ?: System.getenv("PASSWORD")) as String
    }
  }
}

publishing {
  publications {
    //noinspection GroovyAssignabilityCheck
    create<MavenPublication>("publishLibrary") {
      //noinspection GroovyAssignabilityCheck
      from(components["java"])
    }
  }
}

// Detekt configuration
detekt {
  buildUponDefaultConfig = true // preconfigure defaults
  allRules = false // activate all available (even unstable) rules.
  config = files("$projectDir/config/detekt.yml") // point to custom config, overwriting default behavior
  //baseline = file("$projectDir/config/baseline.xml") // a way of suppressing issues before introducing detekt
}
tasks.withType<Detekt>().configureEach {
  reports {
    html.required.set(true) // observe findings in your browser with structure and code snippets
    // xml.required.set(true) // checkstyle like format mainly for integrations like Jenkins
    // txt.required.set(true) // similar to the console output, contains issue signature to manually edit baseline files
    // sarif.required.set(true) // SARIF format (https://sarifweb.azurewebsites.net/) integrate with Github Code Scan
    // md.required.set(true) // simple Markdown format
  }
}
tasks.withType<Detekt>().configureEach {
  jvmTarget = "11"
}
tasks.withType<DetektCreateBaselineTask>().configureEach {
  jvmTarget = "11"
}

// Dokka configuration
tasks.dokkaHtml.configure {
  outputDirectory.set(file("doc/"))
  /*pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
    customAssets = listOf(file("doc/storage-service.png"))
  }*/
  dokkaSourceSets {
    named("main") {
      includes.from("README.md")
      sourceLink {
        localDirectory.set(file("src/main/kotlin"))
        remoteUrl.set(URL("https://github.com/cyface-de/uploader/build/dokka/"))
      }
    }
  }
}
