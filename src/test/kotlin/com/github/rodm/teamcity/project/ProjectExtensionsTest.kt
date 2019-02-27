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

package com.github.rodm.teamcity.project

import jetbrains.buildServer.configs.kotlin.v2018_2.Project
import jetbrains.buildServer.configs.kotlin.v2018_2.ProjectFeature
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProjectExtensionsTest {

    @Test
    fun `add GitHub issue tracker feature to project`() {
        val project = Project {
            features {
                githubIssueTracker {}
            }
        }

        assertEquals(1, project.features.items.size)
        val feature = project.features.items[0]
        assertEquals("IssueTracker", feature.type)
        assertEquals("GithubIssues", feature.findParam("type"))
    }

    @Test
    fun `configure GitHub issue tracker with anonymous authentication`() {
        val project = Project {
            features {
                githubIssueTracker {}
            }
        }

        val feature = project.features.items[0]
        assertEquals("anonymous", feature.findParam("authType"))
    }

    @Test
    fun `configure GitHub issue tracker`() {
        val project = Project {
            features {
                githubIssueTracker {
                    displayName = "DisplayName"
                    repository = "https://example.com/org/project"
                    pattern = """#(\d+)"""
                }
            }
        }

        val feature = project.features.items[0]
        assertEquals("DisplayName", feature.findParam("name"))
        assertEquals("https://example.com/org/project", feature.findParam("repository"))
        assertEquals("""#(\d+)""", feature.findParam("pattern"))
    }
}

fun ProjectFeature.findParam(name: String) : String? {
    params.forEach { param ->
        if (param.name == name) return param.value
    }
    return null
}
