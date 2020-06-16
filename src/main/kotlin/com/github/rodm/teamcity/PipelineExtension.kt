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

import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildTypeSettings.Type.COMPOSITE
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildTypeSettings.Type.DEPLOYMENT
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.copyTo
import jetbrains.buildServer.configs.kotlin.v2019_2.TeamCityDsl
import jetbrains.buildServer.configs.kotlin.v2019_2.Template
import jetbrains.buildServer.configs.kotlin.v2019_2.VcsSettings
import jetbrains.buildServer.configs.kotlin.v2019_2.toId

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
    val stages = arrayListOf<Stage>()
    private val names = mutableSetOf<String>()

    fun stage(name: String, init: Stage.() -> Unit) : Stage {
        if (names.contains(name)) throw DuplicateNameException("Stage name '${name}' already exists")
        names.add(name)

        val stage = Stage(name, this).apply(init)
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
class Stage(val name: String, private val pipeline: Pipeline) {
    val buildType: BuildType = BuildType()
    val buildTypes = arrayListOf<BuildType>()
    var defaults = BuildType()
    val dependencies = arrayListOf<Stage>()
    val templates = arrayListOf<Template>()
    var description: String
        get() = buildType.description
        set(value) { buildType.description = value }

    init {
        buildType.id(name.toId("Stage_"))
        buildType.name = "Stage: ${name}"
        buildType.type = COMPOSITE
        buildType.vcs {
            showDependenciesChanges = true
        }
    }

    fun vcs(init: VcsSettings.() -> Unit) {
        buildType.vcs.apply(init)
    }

    fun template(init: Template.() -> Unit) : Template {
        val template = Template().apply(init)
        templates.add(template)
        return template
    }

    fun defaults(init: BuildType.() -> Unit) {
        defaults = BuildType().apply(init)
    }

    fun build(init: StageBuildType.() -> Unit) {
        val buildType = StageBuildType(this)
        defaults.copyTo(buildType)
        buildType.init()
        buildTypes.add(buildType)
    }

    fun deploy(init: StageBuildType.() -> Unit) {
        val buildType = StageBuildType(this)
        buildType.enablePersonalBuilds = false
        buildType.maxRunningBuilds = 1
        buildType.init()
        buildType.type = DEPLOYMENT
        buildTypes.add(buildType)
    }

    fun stage(name: String) : Stage {
        return pipeline.stage(name)
    }

    fun template(name: String) : Template {
        return templates.find { it.name == name } ?: throw NameNotFoundException("Template '${name}' not found")
    }

    fun dependsOn(stage: Stage) {
        dependencies.add(stage)
    }

    fun matrix(init: Matrix.() -> Unit) : Matrix {
        return Matrix(this).apply(init)
    }
}

open class StageBuildType(val stage: Stage) : BuildType() {
    fun template(name: String) : Template {
        return stage.template(name)
    }
}

@TeamCityDsl
class Matrix(private val stage: Stage) {

    var axes = Axes()
    var axesDefined = false
    var buildDefined = false
    private val excludes = Excludes()

    fun axes(init: Axes.() -> Unit) : Axes {
        if (axesDefined) throw IllegalStateException("only one axes configuration can be defined")
        axesDefined = true

        return axes.apply(init)
    }

    fun excludes(init: Excludes.() -> Unit ) {
        excludes.apply(init)
    }

    fun build(init: MatrixBuildType.() -> Unit) {
        if (buildDefined) throw IllegalStateException("only one matrix build configuration can be defined")
        buildDefined = true

        val combinations = axes.combinations()
        combinations.filter { combination ->
            include(combination)
        }.forEach { combination ->
            val buildType = MatrixBuildType(stage, combination)
            stage.defaults.copyTo(buildType)
            buildType.init()
            buildType.id(buildType.name.toId(""))
            stage.buildTypes.add(buildType)
        }
    }

    private fun include(combination: Map<String, String>) : Boolean {
        return !excludes.excludes.any { exclusion -> combination.containsMap(exclusion) }
    }

    private fun Map<String, String>.containsMap(map: Map<String, String>) : Boolean {
        return map.all { entry -> entry.value == get(entry.key) }
    }
}

@TeamCityDsl
class Axes {
    val axes = linkedMapOf<String, List<String>>()

    operator fun String.invoke(vararg values: String) {
        axes.putIfAbsent(this, values.toList())
    }

    fun combinations() : List<Map<String, String>> {
        val combinations = listOf(mapOf<String,String>())
        return when (axes.size) {
            0 -> emptyList()
            else -> axes.entries.fold(combinations) { acc, axis ->
                acc.flatMap { map ->
                    axis.value.map { value ->
                        map.toMutableMap().apply { put(axis.key, value) }
                    }
                }
            }.toList()
        }
    }
}

@TeamCityDsl
class Excludes {
    val excludes = mutableListOf<Map<String, String>>()

    fun exclude(vararg pairs: Pair<String, String>) {
        excludes.add(pairs.toMap())
    }
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
