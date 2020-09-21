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

package com.github.rodm.teamcity

import com.github.rodm.teamcity.internal.DefaultPipeline
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.TeamCityDsl
import jetbrains.buildServer.configs.kotlin.v2019_2.Template
import jetbrains.buildServer.configs.kotlin.v2019_2.VcsSettings

fun Project.pipeline(init: Pipeline.() -> Unit) : Pipeline {
    val pipeline = DefaultPipeline().apply(init)

    pipeline.stages.forEach { stage ->
        stage.templates.forEach { template ->
            template(template)
        }
        buildType(stage.buildType)
        stage.buildTypes.forEach { buildType ->
            buildType(buildType)
        }
    }
    buildTypesOrder = buildTypes.toList()
    return pipeline
}

@TeamCityDsl
interface Pipeline {
    fun stage(name: String, init: Stage.() -> Unit) : Stage
    fun stage(name: String) : Stage
}

@TeamCityDsl
interface Stage {
    var description: String
    fun vcs(init: VcsSettings.() -> Unit)
    fun template(init: Template.() -> Unit) : Template
    fun defaults(init: BuildType.() -> Unit)
    fun build(init: StageBuildType.() -> Unit)
    fun deploy(init: StageBuildType.() -> Unit)
    fun stage(name: String) : Stage
    fun template(name: String) : Template
    fun build(name: String) : BuildType
    fun dependsOn(stage: Stage)
    fun matrix(init: Matrix.() -> Unit) : Matrix
}

open class StageBuildType(private val stage: Stage) : BuildType() {
    fun dependsOn(build: BuildType) {
        dependencies {
            snapshot(build) {}
        }
    }

    fun template(name: String) : Template {
        return stage.template(name)
    }

    fun build(name: String) : BuildType {
        return stage.build(name)
    }
}

@TeamCityDsl
interface Matrix {
    fun axes(init: Axes.() -> Unit) : Axes
    fun excludes(init: Excludes.() -> Unit)
    fun build(init: MatrixBuildType.() -> Unit)
}

@TeamCityDsl
interface Axes {
    operator fun String.invoke(vararg values: String)
}

@TeamCityDsl
interface Excludes {
    fun exclude(vararg pairs: Pair<String, String>)
}

@TeamCityDsl
class MatrixBuildType(stage: Stage, val axes: Map<String,String>) : StageBuildType(stage) {
    fun matrixName(): String = axes.entries.joinToString(" - ") { entry -> entry.value }
}

class DuplicateNameException(message: String) : Exception(message)

class NameNotFoundException(message: String) : Exception(message)

class Artifact(val producerRules: String, val consumerRules: String) {
    var producer: BuildType? = null
}

fun BuildType.produces(artifact: Artifact) {
    val producer = artifact.producer
    if (producer !== null) throw IllegalStateException("Artifact is produced by build '${producer.name}'")
    artifactRules = artifact.producerRules
    artifact.producer = this
}

fun BuildType.consumes(artifact: Artifact) {
    val producer = artifact.producer ?: throw IllegalStateException("Missing producer")
    if (producer === this) throw IllegalStateException("Consumer and producer cannot be the same build")
    dependencies.artifacts(producer) {
        artifactRules = artifact.consumerRules
    }
}
