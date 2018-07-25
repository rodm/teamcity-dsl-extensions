/*
 * Copyright 2018 Rod MacKenzie.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rodm.teamcity.gradle

import jetbrains.buildServer.configs.kotlin.v2018_1.Project
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GradleBuildExtensionsTest {

    @Test
    fun `configuration adds a build type to a project`() {
        val project = Project {
            configurations {
                configuration("My Test Build")
            }
        }

        assertEquals(1, project.buildTypes.size)
        val buildType = project.buildTypes[0]
        assertEquals("My Test Build", buildType.name)
        assertEquals("MyTestBuild", buildType.id?.value)
    }

    @Test
    fun `adds parameter with path to java home`() {
        val javaHome = "/opt/jdk1.8"
        val project = Project {
            configurations {
                configuration("Test Build", javaHome)
            }
        }

        val buildType = project.buildTypes[0]
        val param = buildType.params.params.find { parameter -> parameter.name == "java.home" }
        assertEquals(javaHome, param?.value)
    }

    @Test
    fun `adds build step to switch Gradle version`() {
        val javaHome = "/opt/jdk1.8"
        val gradleVersion = "4.8"
        val project = Project {
            configurations {
                configuration("Test Build", javaHome, gradleVersion)
            }
        }

        val buildType = project.buildTypes[0]
        val param = buildType.params.params.find { parameter -> parameter.name == "gradle.version" }
        assertEquals(gradleVersion, param?.value)

        assertEquals(1, buildType.steps.items.size)
        val buildSteps = buildType.steps
        assertEquals("simpleRunner", buildSteps.items[0].type)
        assertEquals(arrayListOf("RUNNER_2", "RUNNER_1"), buildSteps.stepsOrder)
    }

    @Test
    fun `configuration order defines UI order`() {
        val project = Project {
            configurations {
                configuration("2 - Test Build")
                configuration("3 - Test Build")
                configuration("1 - Test Build")
            }
        }

        val buildTypesOrder = project.buildTypesOrder.map { buildType -> buildType.id?.value }
        assertEquals(arrayListOf("2TestBuild", "3TestBuild", "1TestBuild"), buildTypesOrder)
    }
}
