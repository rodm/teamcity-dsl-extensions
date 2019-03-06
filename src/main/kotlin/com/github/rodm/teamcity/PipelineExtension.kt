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

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class PipelineMarker

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

@PipelineMarker
class Pipeline {
    val stages = arrayListOf<Stage>()

    fun stage(name: String, init: Stage.() -> Unit) {
        val stage = Stage(name).apply(init)

        val previousStage = stages.lastOrNull()
        stage.buildType.apply {
            dependencies {
                stage.buildTypes.forEach { build ->
                    snapshot(build) {}
                    previousStage?.let {
                        build.dependencies.snapshot(previousStage.buildType) {}
                    }
                }
                previousStage?.let {
                    snapshot(previousStage.buildType) {}
                }
            }
        }
        stages.add(stage)
    }

    @Suppress("UNUSED_PARAMETER")
    @Deprecated(level = DeprecationLevel.ERROR, message = "Pipelines can't be nested.")
    fun pipeline(param: () -> Unit = {}) {}
}

@PipelineMarker
class Stage(val name: String) {
    val buildType: BuildType = BuildType()
    val buildTypes = arrayListOf<BuildType>()
    var defaults = BuildType()

    init {
        buildType.id("Stage_${name}".replace("\\W".toRegex(), ""))
        buildType.name = "Stage: ${name}"
        buildType.type = COMPOSITE
    }

    fun defaults(init: (@PipelineMarker BuildType).() -> Unit) {
        defaults = BuildType().apply(init)
    }

    fun build(init: (@PipelineMarker BuildType).() -> Unit) {
        val buildType = BuildType()
        defaults.copyTo(buildType)
        buildType.init()
        buildTypes.add(buildType)
    }
}
