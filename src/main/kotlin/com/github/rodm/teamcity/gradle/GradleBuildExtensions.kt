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

import jetbrains.buildServer.configs.kotlin.BuildFeature
import jetbrains.buildServer.configs.kotlin.BuildFeatures
import jetbrains.buildServer.configs.kotlin.ErrorConsumer
import jetbrains.buildServer.configs.kotlin.BuildSteps
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.Template
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

class GradleInitScript() : BuildFeature() {

    init {
        type = "gradle-init-scripts"
    }

    constructor(init: GradleInitScript.() -> Unit): this() {
        init()
    }

    var scriptName by stringParameter("initScriptName")

    override fun validate(consumer: ErrorConsumer) {
        super.validate(consumer)
        if (scriptName == null && !hasParam("scriptName")) {
            consumer.consumePropertyError("scriptName", "mandatory 'scriptName' property is not specified")
        }
    }
}

fun BuildFeatures.gradleInitScript(init: GradleInitScript.() -> Unit): GradleInitScript {
    val result = GradleInitScript(init)
    feature(result)
    return result
}

class GradleBuildCache() : BuildFeature() {

    init {
        type = "gradle-build-cache"
    }

    constructor(init: GradleBuildCache.() -> Unit): this() {
        init()
    }
}

fun BuildFeatures.gradleBuildCache(init: GradleBuildCache.() -> Unit): GradleBuildCache {
    val result = GradleBuildCache(init)
    feature(result)
    return result
}

fun BuildSteps.switchGradleBuildStep() {
    switchGradleBuildStep("%default.java.home%", "%gradle.version%")
}

fun BuildSteps.switchGradleBuildStep(javaHome: String, gradleVersion: String) {
    gradle {
        id = "SWITCH_GRADLE"
        name = "Switch Gradle"
        tasks = "wrapper --gradle-version $gradleVersion"
        jdkHome = javaHome
    }
}

fun Project.gradleBuildTemplate(init: Template.() -> Unit = {}): Template {
    val template = com.github.rodm.teamcity.gradle.gradleBuildTemplate(init)
    templates.add(template)
    return template
}

fun gradleBuildTemplate(init: Template.() -> Unit = {}): Template {
    val template = Template()
    template.apply {
        params {
            param("gradle.tasks", "clean build")
            param("gradle.opts", "")
        }
        steps {
            gradle {
                id = "GRADLE_BUILD"
                tasks = "%gradle.tasks%"
                gradleParams = "%gradle.opts%"
                enableStacktrace = true
                jdkHome = "%java.home%"
            }
        }
    }
    template.apply(init)
    return template
}
