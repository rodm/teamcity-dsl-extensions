
package com.github.rodm.teamcity.gradle

import jetbrains.buildServer.configs.kotlin.v2018_1.BuildType
import jetbrains.buildServer.configs.kotlin.v2018_1.Project
import jetbrains.buildServer.configs.kotlin.v2018_1.Template
import jetbrains.buildServer.configs.kotlin.v2018_1.buildSteps.script


fun Project.configurations(init: Configurations.() -> Unit = {}) {
    val configurations = Configurations()
    configurations.init()

    val templates = configurations.templates.toTypedArray()
    configurations.configurations.forEach { configuration ->
        buildType(TestBuildType(configuration, templates))
    }
}

class Configurations {
    val configurations = arrayListOf<BuildParameters>()
    val templates = arrayListOf<Template>()

    fun configuration(name: String, javaHome: String? = null, gradleVersion: String? = null) {
        val params = BuildParameters(name, javaHome, gradleVersion)
        configurations.add(params)
    }

    fun template(template: Template) {
        templates.add(template)
    }
}

data class BuildParameters(val name: String, val javaHome: String? = null, val gradleVersion: String? = null)

class TestBuildType(buildParameters: BuildParameters, buildTemplates: Array<Template>) : BuildType() {
    init {
        id(buildParameters.name.replace("\\W".toRegex(), "").capitalize())
        name = buildParameters.name
        templates(*buildTemplates)

        params {
            param("gradle.tasks", "clean functionalTest")
            if (buildParameters.javaHome != null) {
                param("java.home", buildParameters.javaHome)
            }
        }

        if (buildParameters.gradleVersion != null) {
            params {
                param("gradle.version", buildParameters.gradleVersion)
            }
            steps {
                script {
                    id = "RUNNER_2"
                    scriptContent = """
                #!/bin/sh
                JAVA_HOME=%java8.home% ./gradlew wrapper --gradle-version=%gradle.version%
                JAVA_HOME=%java.home% ./gradlew --version
                """.trimIndent()
                }
                stepsOrder = arrayListOf("RUNNER_2", "RUNNER_1")
            }
        }

        failureConditions {
            executionTimeoutMin = 20
        }
    }
}
