/*
 * Copyright 2020 Rod MacKenzie.
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
import jetbrains.buildServer.configs.kotlin.v2018_2.Project
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PipelineArtifactsTest {

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
