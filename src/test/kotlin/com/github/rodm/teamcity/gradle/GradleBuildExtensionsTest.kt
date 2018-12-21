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

import jetbrains.buildServer.configs.kotlin.v2018_1.BuildFeature
import jetbrains.buildServer.configs.kotlin.v2018_1.BuildType
import jetbrains.buildServer.configs.kotlin.v2018_1.Consumer
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
        assertEquals("init.gradle", feature.findParam("name"))
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
    fun `add build step to switch Gradle version`() {
        val buildType = BuildType {
            steps {
                switchGradleBuildStep()
            }
        }

        assertEquals(1, buildType.steps.items.size)
        val buildSteps = buildType.steps
        assertEquals("simpleRunner", buildSteps.items[0].type)
    }
}

fun BuildFeature.findParam(name: String) : String? {
    params.forEach { param ->
        if (param.name == name) return param.value
    }
    return null
}
