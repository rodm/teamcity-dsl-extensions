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

import com.github.rodm.teamcity.internal.DefaultStage
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.TeamCityDsl
import jetbrains.buildServer.configs.kotlin.v2019_2.Template
import jetbrains.buildServer.configs.kotlin.v2019_2.VcsSettings

lateinit var pipeline: Pipeline

fun Project.pipeline(init: Pipeline.() -> Unit) {
    pipeline = Pipeline().apply(init)

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
}

@TeamCityDsl
class Pipeline {
    val stages = arrayListOf<DefaultStage>()
    private val names = mutableSetOf<String>()

    fun stage(name: String, init: Stage.() -> Unit) : Stage {
        if (names.contains(name)) throw DuplicateNameException("Stage name '${name}' already exists")
        names.add(name)

        val stage = DefaultStage(name, this).apply(init)
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

    fun stage(name: String) : Stage {
        return stages.find { stage -> stage.name == name } ?: throw NameNotFoundException("Stage '$name' not found")
    }
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
    fun dependsOn(stage: Stage)
    fun matrix(init: Matrix.() -> Unit) : Matrix
}

open class StageBuildType(val stage: Stage) : BuildType() {
    fun template(name: String) : Template {
        return stage.template(name)
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
class MatrixBuildType(stage: Stage, val axes: Map<String,String>) : StageBuildType(stage)

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
