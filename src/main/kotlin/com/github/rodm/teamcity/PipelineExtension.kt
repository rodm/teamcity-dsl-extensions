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

package com.github.rodm.teamcity

import jetbrains.buildServer.configs.kotlin.v2018_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2018_2.BuildTypeSettings.Type.COMPOSITE
import jetbrains.buildServer.configs.kotlin.v2018_2.Project
import jetbrains.buildServer.configs.kotlin.v2018_2.copyTo
import jetbrains.buildServer.configs.kotlin.v2018_2.TeamCityDsl

lateinit var pipeline: Pipeline

fun Project.pipeline(init: Pipeline.() -> Unit) {
    pipeline = Pipeline().apply(init)

    pipeline.stages.forEach { stage ->
        buildType(stage.buildType)
        stage.buildTypes.forEach { buildType ->
            buildType(buildType)
        }
    }
    buildTypesOrder = buildTypes.toList()
}

@TeamCityDsl
class Pipeline {
    val stages = arrayListOf<Stage>()

    fun stage(name: String, init: Stage.() -> Unit) : Stage {
        val stage = Stage(name).apply(init)
        if (stage.dependencies.isEmpty() && !stages.isEmpty()) stage.dependsOn(stages.last())

        val stageDependencies = stage.dependencies
        stage.buildType.apply {
            dependencies {
                stage.buildTypes.forEach { build ->
                    snapshot(build) {}
                    stageDependencies.forEach { stageDependency ->
                        build.dependencies.snapshot(stageDependency.buildType) {}
                    }
                }
                stageDependencies.forEach { stageDependency ->
                    snapshot(stageDependency.buildType) {}
                }
            }
        }
        stages.add(stage)
        return stage
    }
}

@TeamCityDsl
class Stage(val name: String) {
    val buildType: BuildType = BuildType()
    val buildTypes = arrayListOf<BuildType>()
    var defaults = BuildType()
    val dependencies = arrayListOf<Stage>()

    init {
        buildType.id("Stage_${name}".replace("\\W".toRegex(), ""))
        buildType.name = "Stage: ${name}"
        buildType.type = COMPOSITE
    }

    fun defaults(init: BuildType.() -> Unit) {
        defaults = BuildType().apply(init)
    }

    fun build(init: BuildType.() -> Unit) {
        val buildType = BuildType()
        defaults.copyTo(buildType)
        buildType.init()
        buildTypes.add(buildType)
    }

    fun dependsOn(stage: Stage) {
        dependencies.add(stage)
    }
}
