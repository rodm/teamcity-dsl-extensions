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

import jetbrains.buildServer.configs.kotlin.v2018_2.BuildFeature
import jetbrains.buildServer.configs.kotlin.v2018_2.BuildFeatures
import jetbrains.buildServer.configs.kotlin.v2018_2.ErrorConsumer
import jetbrains.buildServer.configs.kotlin.v2018_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2018_2.Project
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2018_2.Template
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.gradle

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
    script {
        id = "SWITCH_GRADLE"
        scriptContent = """
            #!/bin/sh
            JAVA_HOME=%java8.home% ./gradlew wrapper --gradle-version=%gradle.version%
            JAVA_HOME=%java.home% ./gradlew --version
            """.trimIndent()
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
