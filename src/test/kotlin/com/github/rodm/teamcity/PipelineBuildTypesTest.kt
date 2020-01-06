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
import jetbrains.buildServer.configs.kotlin.v2018_2.BuildTypeSettings
import jetbrains.buildServer.configs.kotlin.v2018_2.Project
import jetbrains.buildServer.configs.kotlin.v2018_2.Template
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PipelineBuildTypesTest {

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
        assertEquals(BuildTypeSettings.Type.DEPLOYMENT, project.buildTypes[1].type)
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
        assertEquals(BuildTypeSettings.Type.DEPLOYMENT, project.buildTypes[1].type)
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

    private fun Project.findBuildByName(name: String) : BuildType? {
        return buildTypes.find { build -> build.name == name }
    }
}
