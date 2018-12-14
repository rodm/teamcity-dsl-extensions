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

import jetbrains.buildServer.configs.kotlin.v2018_1.BuildType
import jetbrains.buildServer.configs.kotlin.v2018_1.BuildTypeSettings.Type.COMPOSITE
import jetbrains.buildServer.configs.kotlin.v2018_1.Project

lateinit var pipeline: Pipeline

fun Project.pipeline(init: Pipeline.() -> Unit) {
    pipeline = Pipeline()
    pipeline.init()

    pipeline.stages.forEach { stage ->
        buildType(stage.buildType)
        stage.buildTypes.forEach { buildType ->
            buildType(buildType)
        }
    }
}

class Pipeline {
    val stages = arrayListOf<Stage>()

    fun stage(name: String, init: Stage.() -> Unit) {
        val stage = Stage(name)
        stage.init()
        stages.add(stage)
    }
}

class Stage(val name: String) {
    val buildType: BuildType
    val buildTypes = arrayListOf<BuildType>()

    init {
        buildType = BuildType()
        buildType.name = "Stage: ${name}"
        buildType.type = COMPOSITE
    }

    fun build(init: BuildType.() -> Unit) {
        val buildType = BuildType()
        buildType.init()
        buildTypes.add(buildType)
    }
}
