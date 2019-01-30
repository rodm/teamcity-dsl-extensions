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
import jetbrains.buildServer.configs.kotlin.v2018_1.BuildTypeSettings.Type.REGULAR
import jetbrains.buildServer.configs.kotlin.v2018_1.Project
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

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

        val dependencies = project.findBuildByName("Stage: Stage1")?.dependencies
        assertEquals(1, dependencies?.items?.size)
        val dependencyBuild = dependencies?.items?.get(0)?.buildTypeId as BuildType
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

    private fun Project.findBuildByName(name: String) : BuildType? {
        return buildTypes.find { build -> build.name == name }
    }
}
