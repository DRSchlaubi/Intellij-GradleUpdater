/*
 * MIT License
 *
 * Copyright (c) 2020 Michael Rittmeister
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

plugins {
    java
    id("org.jetbrains.intellij") version "0.7.2"
    kotlin("jvm") version "1.4.32"
    kotlin("plugin.serialization") version "1.4.32"
}

group = "me.schlaubi"
version = "2.0-RC2"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.1.0")

    implementation(platform("io.ktor:ktor-bom:1.5.3"))
    implementation("io.ktor", "ktor-client-okhttp")
    implementation("io.ktor", "ktor-client-serialization-jvm")
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "2021.1"

    setPlugins(
        // For gradle support
        "gradle",
        // To properly parse gradle-wrapper.properties
        "properties",
        // Some kotlin classes depend on the java plugin
        "java",
        // To properly parse build.gradle.kts
        "org.jetbrains.kotlin"
    )
}
tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    changeNotes(
        """
        - Migrate to IntelliJ 2020.3
        - Refactor code
        - Update dependencies
    """.trimIndent()
    )
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
