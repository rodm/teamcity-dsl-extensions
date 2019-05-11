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
import jetbrains.buildServer.configs.kotlin.v2018_2.Template
import jetbrains.buildServer.configs.kotlin.v2018_2.VcsSettings
import jetbrains.buildServer.configs.kotlin.v2018_2.toId

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

    fun stage(name: String) : Stage {
        return pipeline.stage(name)
    }

    fun template(name: String) : Template {
        return templates.find { it.name == name } ?: throw NameNotFoundException("Template '${name}' not found")
    }

    fun dependsOn(stage: Stage) {
        dependencies.add(stage)
    }
}

class StageBuildType(val stage: Stage) : BuildType() {
    fun template(name: String) : Template {
        return stage.template(name)
    }
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
