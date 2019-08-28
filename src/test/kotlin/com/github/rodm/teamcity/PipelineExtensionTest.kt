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
import jetbrains.buildServer.configs.kotlin.v2018_2.BuildTypeSettings.Type.DEPLOYMENT
import jetbrains.buildServer.configs.kotlin.v2018_2.BuildTypeSettings.Type.REGULAR
import jetbrains.buildServer.configs.kotlin.v2018_2.Project
import jetbrains.buildServer.configs.kotlin.v2018_2.Template
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PipelineExtensionTest {

    @Test
    fun `define a build pipeline for a project`() {
        Project {
            pipeline {
            }
        }

        assertNotNull(pipeline)
    }

    @Test
    fun `build pipeline contains a named stage`() {
        Project {
            pipeline {
                stage ("Build") {
                }
            }
        }

        assertEquals(1, pipeline.stages.size)
        assertEquals("Build", pipeline.stages[0].name)
    }

    @Test
    fun `build pipeline contains multiple named stages`() {
        Project {
            pipeline {
                stage ("Build") {
                }
                stage ("Test") {
                }
            }
        }

        assertEquals(2, pipeline.stages.size)
        assertEquals("Test", pipeline.stages[1].name)
    }

    @Test
    fun `stage has a named composite build configuration`() {
        Project {
            pipeline {
                stage("Build") {
                }
            }
        }

        val stage = pipeline.stages[0]
        assertEquals("Stage: Build", stage.buildType.name)
        assertEquals(COMPOSITE, stage.buildType.type)
        assertEquals("", stage.buildType.description)
    }

    @Test
    fun `stage can have a description`() {
        Project {
            Project {
                pipeline {
                    stage("Build") {
                        description = "Build stage description"
                    }
                }
            }

            val stage = pipeline.stages[0]
            assertEquals("Build stage description", stage.buildType.description)
        }
    }

    @Test
    fun `stage names should be unique`() {
        val exception = assertThrows<DuplicateNameException> {
            Project {
                pipeline {
                    stage("Stage") {}
                    stage("Stage") {}
                }
            }
        }

        assertEquals("Stage name 'Stage' already exists", exception.message)
    }

    @Test
    fun `stage composite builds are added to project`() {
        val project = Project {
            pipeline {
                stage("Build") {
                }
            }
        }

        assertEquals(1, project.buildTypes.size)
        assertEquals("Stage: Build", project.buildTypes[0].name)
    }

    @Test
    fun `build within a stage adds a regular build configuration to project`() {
        val project = Project {
            pipeline {
                stage("Build") {
                    build {
                        name = "Compile"
                    }
                }
            }
        }

        assertEquals(2, project.buildTypes.size)
        assertEquals("Compile", project.buildTypes[1].name)
        assertEquals(REGULAR, project.buildTypes[1].type)
    }

    @Test
    fun `stages and builds configuration order is display order`() {
        val project = Project {
            pipeline {
                stage ("Stage1") {
                    build {
                        name = "Build1"
                    }
                }
                stage ("Stage2") {
                    build {
                        name = "Build2"
                    }
                }
            }
        }

        assertEquals(4, project.buildTypesOrder.size)
        assertEquals("Stage: Stage1", project.buildTypesOrder[0].name)
        assertEquals("Build1", project.buildTypesOrder[1].name)
        assertEquals("Stage: Stage2", project.buildTypesOrder[2].name)
        assertEquals("Build2", project.buildTypesOrder[3].name)
    }

    @Test
    fun `stage depends on builds defined within a stage`() {
        val project = Project {
            pipeline {
                stage ("Stage1") {
                    build {
                        name = "Build1"
                    }
                }
            }
        }

        val dependencies = project.findBuildByName("Stage: Stage1")?.dependencies?.items
        assertEquals(1, dependencies?.size)
        val dependencyBuild = dependencies?.get(0)?.buildTypeId as BuildType
        assertEquals("Build1", dependencyBuild.name)
    }

    @Test
    fun `stage depends on previous stage`() {
        val project = Project {
            pipeline {
                stage ("Stage1") {
                }
                stage ("Stage2") {
                }
            }
        }

        val dependencies = project.findBuildByName("Stage: Stage2")?.dependencies
        assertEquals(1, dependencies?.items?.size)
        val dependencyBuild = dependencies?.items?.get(0)?.buildTypeId as BuildType
        assertEquals("Stage: Stage1", dependencyBuild.name)
    }

    @Test
    fun `stage depends on specified stage`() {
        val project = Project {
            pipeline {
                val stage1 = stage ("Stage1") {
                }
                stage ("Stage2") {
                }
                stage ("Stage3") {
                    dependsOn(stage1)
                }
            }
        }

        val dependencies = project.findBuildByName("Stage: Stage3")?.dependencies?.items
        assertEquals(1, dependencies?.size)
        val dependencyBuild = dependencies?.get(0)?.buildTypeId as BuildType
        assertEquals("Stage: Stage1", dependencyBuild.name)
    }

    @Test
    fun `stage depends on multiple earlier stages`() {
        val project = Project {
            pipeline {
                val stage1 = stage ("Stage1") {
                }
                val stage2 = stage ("Stage2") {
                }
                stage ("Stage3") {
                    dependsOn(stage1)
                    dependsOn(stage2)
                }
            }
        }

        val dependencies = project.findBuildByName("Stage: Stage3")?.dependencies?.items
        assertEquals(2, dependencies?.size)
        val dependencyBuild1 = dependencies?.get(0)?.buildTypeId as BuildType
        val dependencyBuild2 = dependencies[1].buildTypeId as BuildType
        assertEquals("Stage: Stage1", dependencyBuild1.name)
        assertEquals("Stage: Stage2", dependencyBuild2.name)
    }

    @Test
    fun `stage depends on earlier stage by name`() {
        val project = Project {
            pipeline {
                stage ("Stage1") {
                }
                stage ("Stage2") {
                }
                stage ("Stage3") {
                    dependsOn (stage("Stage1"))
                }
            }
        }

        val dependencies = project.findBuildByName("Stage: Stage3")?.dependencies?.items
        assertEquals(1, dependencies?.size)
        val dependencyBuild = dependencies?.get(0)?.buildTypeId as BuildType
        assertEquals("Stage: Stage1", dependencyBuild.name)
    }

    @Test
    fun `lookup by invalid stage throws exception`() {
        val exception = assertThrows<NameNotFoundException> {
            Project {
                pipeline {
                    stage("Invalid name")
                }
            }
        }

        assertEquals("Stage 'Invalid name' not found", exception.message)
    }

    @Test
    fun `stage build type has id`() {
        val project = Project {
            pipeline {
                stage ("Stage1") {
                }
            }
        }

        val stageBuildType = project.buildTypes[0]
        assertEquals("Stage_Stage1", stageBuildType.id?.toString())
    }

    @Test
    fun `stage build type has id without whitespace`() {
        val project = Project {
            pipeline {
                stage ("Stage One") {
                }
            }
        }

        val stageBuildType = project.buildTypes[0]
        assertEquals("Stage_StageOne", stageBuildType.id?.toString())
    }

    @Test
    fun `build types in a stage depend on the previous stage`() {
        val project = Project {
            pipeline {
                stage ("Stage1") {
                }
                stage ("Stage2") {
                    build {
                        name = "Build"
                    }
                }
            }
        }

        val dependencies = project.findBuildByName("Build")?.dependencies
        assertEquals(1, dependencies?.items?.size)
        val dependencyBuild = dependencies?.items?.get(0)?.buildTypeId as BuildType
        assertEquals("Stage: Stage1", dependencyBuild.name)
    }

    @Test
    fun `define default build configuration settings for a stage`() {
        val project = Project {
            pipeline {
                stage ("Stage1") {
                    defaults {
                        artifactRules = "artifactRules"
                    }
                    build {
                        name = "Build1"
                    }
                }
            }
        }

        assertEquals("artifactRules", project.findBuildByName("Build1")?.artifactRules)
    }

    @Test
    fun `redefine default build configuration settings for a stage`() {
        val project = Project {
            pipeline {
                stage ("Stage1") {
                    defaults {
                        artifactRules = "artifactRules"
                    }
                    build {
                        name = "Build1"
                    }
                    defaults {
                        artifactRules = "newArtifactRules"
                    }
                    build {
                        name = "Build2"
                    }
                }
            }
        }

        assertEquals("artifactRules", project.findBuildByName("Build1")?.artifactRules)
        assertEquals("newArtifactRules", project.findBuildByName("Build2")?.artifactRules)
    }

    @Test
    fun `define a deploy build configuration`() {
        val project = Project {
            pipeline {
                stage ("Stage1") {
                    deploy {
                        name = "Deploy"
                    }
                }
            }
        }

        assertEquals(2, project.buildTypes.size)
        assertEquals("Deploy", project.buildTypes[1].name)
        assertEquals(DEPLOYMENT, project.buildTypes[1].type)
        assertEquals(false, project.buildTypes[1].enablePersonalBuilds)
        assertEquals(1, project.buildTypes[1].maxRunningBuilds)
    }

    @Test
    fun `define a deploy build configuration overriding defaults`() {
        val project = Project {
            pipeline {
                stage ("Stage1") {
                    deploy {
                        name = "Deploy"
                        enablePersonalBuilds = true
                        maxRunningBuilds = 2
                    }
                }
            }
        }

        assertEquals(2, project.buildTypes.size)
        assertEquals("Deploy", project.buildTypes[1].name)
        assertEquals(DEPLOYMENT, project.buildTypes[1].type)
        assertEquals(true, project.buildTypes[1].enablePersonalBuilds)
        assertEquals(2, project.buildTypes[1].maxRunningBuilds)
    }

    @Test
    fun `define a template in a stage`() {
        val project = Project {
            pipeline {
                stage ("Stage1") {
                    template {
                        name = "Template1"
                    }
                }
            }
        }

        assertEquals(1, project.templates.size)
        assertEquals("Template1", project.templates[0].name)
    }

    @Test
    fun `define and use a template in a stage`() {
        val project = Project {
            pipeline {
                stage ("Stage1") {
                    val template = template {
                        name = "Template1"
                    }
                    build {
                        name = "Build1"
                        templates(template)
                    }
                }
            }
        }

        val build = project.findBuildByName("Build1")
        assertEquals(1, build?.templates?.size)
        val template = build?.templates?.get(0) as Template
        assertEquals("Template1", template.name)
    }

    @Test
    fun `define and use a template by name`() {
        val project = Project {
            pipeline {
                stage ("Stage1") {
                    template {
                        name = "Template1"
                    }
                    build {
                        name = "Build1"
                        templates(template("Template1"))
                    }
                }
            }
        }

        val build = project.findBuildByName("Build1")
        assertEquals(1, build?.templates?.size)
        val template = build?.templates?.get(0) as Template
        assertEquals("Template1", template.name)
    }

    @Test
    fun `default vcs settings for stage`() {
        val project = Project {
            pipeline {
                stage ("Stage1") {}
            }
        }

        val stageBuild = project.findBuildByName("Stage: Stage1")
        assertEquals(true, stageBuild?.vcs?.showDependenciesChanges)
        assertEquals(false, stageBuild?.vcs?.excludeDefaultBranchChanges)
        assertEquals("+:*", stageBuild?.vcs?.branchFilter)
    }

    @Test
    fun `change vcs settings for stage`() {
        val project = Project {
            pipeline {
                stage ("Stage1") {
                    vcs {
                        showDependenciesChanges = false
                        excludeDefaultBranchChanges = true
                        branchFilter = "+:master"
                    }
                }
            }
        }

        val stageBuild = project.findBuildByName("Stage: Stage1")
        assertEquals(false, stageBuild?.vcs?.showDependenciesChanges)
        assertEquals(true, stageBuild?.vcs?.excludeDefaultBranchChanges)
        assertEquals("+:master", stageBuild?.vcs?.branchFilter)
    }

    @Test
    fun `define artifacts with producer and consumer rules`() {
        val artifact = Artifact("producerRules", "consumerRules")

        assertEquals("producerRules", artifact.producerRules)
        assertEquals("consumerRules", artifact.consumerRules)
        assertNull(artifact.producer)
    }

    @Test
    fun `attach build that produces the artifacts`() {
        val artifact = Artifact("producerRules", "consumerRules")
        val project = Project {
            pipeline {
                stage("Stage1") {
                    build {
                        name = "Build1"
                        produces(artifact)
                    }
                }
            }
        }

        assertNotNull(artifact.producer)
        assertSame(project.findBuildByName("Build1"), artifact.producer)
    }

    @Test
    fun `consumer rules are set on the artifact dependency of the consuming build`() {
        val artifact = Artifact("producerRules", "consumerRules")
        val project = Project {
            pipeline {
                stage("Stage1") {
                    build {
                        name = "Build1"
                        produces(artifact)
                    }
                    build {
                        name = "Build2"
                        consumes(artifact)
                    }
                }
            }
        }

        val producingBuild = project.findBuildByName("Build1")
        val consumingBuild = project.findBuildByName("Build2")
        assertSame(producingBuild, consumingBuild?.dependencies?.items?.get(0)?.buildTypeId as BuildType)

        val artifactDependency = consumingBuild.dependencies.items[0].artifacts[0]
        assertEquals("consumerRules", artifactDependency.artifactRules)
    }

    @Test
    fun `consuming an artifact without a producer throws an exception`() {
        val artifact = Artifact("producerRules", "consumerRules")
        val exception = assertThrows<IllegalStateException> {
            Project {
                pipeline {
                    stage("Stage1") {
                        build {
                            name = "Build2"
                            consumes(artifact)
                        }
                    }
                }
            }
        }

        assertEquals("Missing producer", exception.message)
    }

    @Test
    fun `artifact cannot be produced and consumed by the same build`() {
        val artifact = Artifact("producerRules", "consumerRules")
        val exception = assertThrows<IllegalStateException> {
            Project {
                pipeline {
                    stage("Stage1") {
                        build {
                            name = "Build"
                            produces(artifact)
                            consumes(artifact)
                        }
                    }
                }
            }
        }

        assertEquals("Consumer and producer cannot be the same build", exception.message)
    }

    @Test
    fun `artifact cannot be produced by multiple builds`() {
        val artifact = Artifact("producerRules", "consumerRules")
        val exception = assertThrows<IllegalStateException> {
            Project {
                pipeline {
                    stage("Stage1") {
                        build {
                            name = "Build 1"
                            produces(artifact)
                        }
                        build {
                            name = "Build 2"
                            produces(artifact)
                        }
                    }
                }
            }
        }

        assertEquals("Artifact is produced by build 'Build 1'", exception.message)
    }

    private fun Project.findBuildByName(name: String) : BuildType? {
        return buildTypes.find { build -> build.name == name }
    }
}
