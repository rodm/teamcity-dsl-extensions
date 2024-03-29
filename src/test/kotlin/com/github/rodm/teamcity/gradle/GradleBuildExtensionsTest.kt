/*
 * Copyright 2018 Rod MacKenzie.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rodm.teamcity.gradle

import com.github.rodm.teamcity.findParam
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.Consumer
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.buildSteps.GradleBuildStep
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GradleBuildExtensionsTest {

    @Test
    fun `add Gradle Init Script feature to a build configuration`() {
        val buildType = BuildType {
            features {
                gradleInitScript {}
            }
        }

        assertEquals(1, buildType.features.items.size)
        val feature = buildType.features.items[0]
        assertEquals("gradle-init-scripts", feature.type)
    }

    @Test
    fun `add Gradle Init Script feature with script name`() {
        val buildType = BuildType {
            features {
                gradleInitScript {
                    scriptName = "init.gradle"
                }
            }
        }

        val feature = buildType.features.items[0]
        assertEquals("init.gradle", feature.findParam("initScriptName"))
    }

    @Test
    fun `validating Gradle Init Script feature with script name reports no errors`() {
        val buildType = BuildType {
            features {
                gradleInitScript {
                    scriptName = "init.gradle"
                }
            }
        }

        val validationConsumer = Consumer()
        buildType.validate(validationConsumer)
        assertEquals(0, validationConsumer.errors.size)
    }

    @Test
    fun `validating Gradle Init Script feature without script name reports errors`() {
        val buildType = BuildType {
            features {
                gradleInitScript {}
            }
        }

        val validationConsumer = Consumer()
        buildType.validate(validationConsumer)
        assertEquals(1, validationConsumer.errors.size)
        assertEquals("build feature [1/1]: mandatory 'scriptName' property is not specified", validationConsumer.errors[0])
    }

    @Test
    fun `add Gradle Build Cache feature to a build configuration`() {
        val buildType = BuildType {
            features {
                gradleBuildCache {}
            }
        }

        assertEquals(1, buildType.features.items.size)
        val feature = buildType.features.items[0]
        assertEquals("gradle-build-cache", feature.type)
    }

    @Test
    fun `add build step to switch Gradle version`() {
        val buildType = BuildType {
            steps {
                switchGradleBuildStep()
            }
        }

        assertEquals(1, buildType.steps.items.size)
        val gradleBuildStep = buildType.steps.items[0] as GradleBuildStep
        assertEquals("gradle-runner", gradleBuildStep.type)
        assertEquals("SWITCH_GRADLE", gradleBuildStep.id)
        assertEquals("%default.java.home%", gradleBuildStep.jdkHome)
        assertEquals("wrapper --gradle-version %gradle.version%", gradleBuildStep.tasks)
    }

    @Test
    fun `add build step to switch Gradle version with specified Java home and Gradle version`() {
        val buildType = BuildType {
            steps {
                switchGradleBuildStep("/example/jdk", "1.2.3")
            }
        }

        assertEquals(1, buildType.steps.items.size)
        val gradleBuildStep = buildType.steps.items[0] as GradleBuildStep
        assertEquals("/example/jdk", gradleBuildStep.jdkHome)
        assertEquals("wrapper --gradle-version 1.2.3", gradleBuildStep.tasks)
    }

    @Test
    fun `create a template with a Gradle build step`() {
        val template = gradleBuildTemplate()

        assertEquals(1, template.steps.items.size)
        val buildSteps = template.steps
        assertEquals("gradle-runner", buildSteps.items[0].type)
        assertEquals("GRADLE_BUILD", buildSteps.items[0].id)
    }

    @Test
    fun `gradle build template uses a parameter for tasks`() {
        val template = gradleBuildTemplate()

        val buildStep = template.steps.items[0] as GradleBuildStep
        assertEquals("%gradle.tasks%", buildStep.tasks)
    }

    @Test
    fun `gradle build template uses a parameter for options`() {
        val template = gradleBuildTemplate()

        val buildStep = template.steps.items[0] as GradleBuildStep
        assertEquals("%gradle.opts%", buildStep.gradleParams)
    }

    @Test
    fun `gradle build template enables stack trace`() {
        val template = gradleBuildTemplate()

        val buildStep = template.steps.items[0] as GradleBuildStep
        assertEquals(true, buildStep.enableStacktrace)
    }

    @Test
    fun `gradle build template set default values for parameters`() {
        val template = gradleBuildTemplate()

        val params = template.params.params
        assertEquals("clean build", params.find { param -> param.name == "gradle.tasks" }?.value)
        assertEquals("", params.find { param -> param.name == "gradle.opts"}?.value)
    }

    @Test
    fun `gradle build template uses a parameter for Java home`() {
        val template = gradleBuildTemplate()

        val buildStep = template.steps.items[0] as GradleBuildStep
        assertEquals("%java.home%", buildStep.jdkHome)
    }

    @Test
    fun `customize gradle build template`() {
        val template = gradleBuildTemplate {
            name = "Gradle Build"
        }

        assertEquals("Gradle Build", template.name)
    }

    @Test
    fun `gradle template added to project templates`() {
        val project = Project {
            gradleBuildTemplate {  }
        }

        assertEquals(1, project.templates.size)
    }
}
