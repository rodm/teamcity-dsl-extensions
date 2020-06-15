/*
 * Copyright 2019 Rod MacKenzie.
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
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MatrixBuildTest {

    @Test
    fun `no axes defined`() {
        val axes = Axes()
        val combinations = axes.combinations()
        assertEquals(listOf<Map<String,String>>(), combinations)
    }

    @Test
    fun `single axes with single value`() {
        val axes = Axes()
        axes.apply {
            "A"("B")
        }

        val combinations = axes.combinations()
        assertEquals(listOf(mapOf("A" to "B")), combinations)
    }

    @Test
    fun `multiple axes with multiple values`() {
        val axes = Axes()
        axes.apply {
            "A"("B", "C")
            "X"("Y", "Z")
        }

        val combinations = axes.combinations()
        assertEquals(listOf(
            mapOf("A" to "B", "X" to "Y"),
            mapOf("A" to "B", "X" to "Z"),
            mapOf("A" to "C", "X" to "Y"),
            mapOf("A" to "C", "X" to "Z")
        ), combinations)
    }

    @Test
    fun `matrix builds with single axis`() {
        val project = Project {
            pipeline {
                stage ("Stage1") {
                    matrix {
                        axes {
                            "OS"("Linux", "Windows", "Mac OS X")
                        }
                        build {
                            name = "Build - ${axes["OS"]}"
                        }
                    }
                }
            }
        }

        assertEquals(4, project.buildTypes.size) // includes stage build type

        val names = project.buildTypes.map { it.name }
        assertThat(names, hasItems("Build - Linux", "Build - Windows", "Build - Mac OS X"))

        val ids = project.buildTypes.map { it.id.toString() }
        assertThat(ids, hasItems("BuildLinux", "BuildWindows", "BuildMacOsX"))
    }

    @Test
    fun `matrix build with multiple axes`() {
        val project = Project {
            pipeline {
                stage ("Stage1") {
                    matrix {
                        axes {
                            "OS"("Linux", "Windows", "Mac OS X")
                            "JDK"("JDK_18", "JDK_11")
                        }
                        build {
                            name = "Build - ${axes["OS"]} - ${axes["JDK"]}"
                        }
                    }
                }
            }
        }

        assertEquals(7, project.buildTypes.size) // includes stage build type

        val expectedNames = arrayOf(
            "Build - Linux - JDK_18",
            "Build - Linux - JDK_11",
            "Build - Windows - JDK_18",
            "Build - Windows - JDK_11",
            "Build - Mac OS X - JDK_18",
            "Build - Mac OS X - JDK_11")
        val names = project.buildTypes.map { it.name }
        assertThat(names, hasItems(*expectedNames))

        val expectedIds = arrayOf(
            "BuildLinuxJdk18",
            "BuildLinuxJdk11",
            "BuildWindowsJdk18",
            "BuildWindowsJdk11",
            "BuildMacOsXJdk18",
            "BuildMacOsXJdk11")
        val ids = project.buildTypes.map { it.id.toString() }
        assertThat(ids, hasItems(*expectedIds))
    }

    @Test
    fun `allow only one axes configuration`() {
        val exception = assertThrows(Exception::class.java) {
            Project {
                pipeline {
                    stage("Stage1") {
                        matrix {
                            axes {
                                "OS"("Linux", "Windows", "Mac OS X")
                            }
                            axes {
                                "JDK"("JDK11", "JDK18")
                            }
                        }
                    }
                }
            }
        }
        assertEquals("only one axes configuration can be defined", exception.message)
    }

    @Test
    fun `allow only one matrix build configuration`() {
        val exception = assertThrows(Exception::class.java) {
            Project {
                pipeline {
                    stage("Stage1") {
                        matrix {
                            axes {
                                "OS"("Linux", "Windows", "Mac OS X")
                            }
                            build {
                                name = "Build - ${axes["OS"]}"
                            }
                            build {
                                name = "Another build - ${axes["OS"]}"
                            }
                        }
                    }
                }
            }
        }
        assertEquals("only one matrix build configuration can be defined", exception.message)
    }

    @Test
    fun `apply default stage settings to matrix builds`() {
        val project = Project {
            pipeline {
                stage ("Stage1") {
                    defaults {
                        failureConditions {
                            executionTimeoutMin = 15
                        }
                    }
                    matrix {
                        axes {
                            "OS"("Linux", "Windows", "Mac OS X")
                            "JDK"("JDK_18", "JDK_11")
                        }
                        build {
                            name = "Build - ${axes["OS"]} - ${axes["JDK"]}"
                        }
                    }
                }
            }
        }

        val linuxJava11Build = project.findBuildByName("Build - Linux - JDK_11")
        assertEquals(15, linuxJava11Build?.failureConditions?.executionTimeoutMin)
        val windowsJava8Build = project.findBuildByName("Build - Windows - JDK_18")
        assertEquals(15, windowsJava8Build?.failureConditions?.executionTimeoutMin)
    }

    @Test
    fun `matrix build excluding specific combinations`() {
        val project = Project {
            pipeline {
                stage ("Stage1") {
                    matrix {
                        axes {
                            "OS"("Linux", "Windows", "Mac OS X")
                            "JDK"("JDK_18", "JDK_11")
                        }
                        excludes {
                            exclude("OS" to "Windows", "JDK" to "JDK_18")
                            exclude("OS" to "Mac OS X", "JDK" to "JDK_11")
                        }
                        build {
                            name = "Build - ${axes["OS"]} - ${axes["JDK"]}"
                        }
                    }
                }
            }
        }

        assertEquals(5, project.buildTypes.size) // includes stage build type

        val expectedNames = arrayOf(
            "Build - Linux - JDK_18",
            "Build - Linux - JDK_11",
            "Build - Windows - JDK_11",
            "Build - Mac OS X - JDK_18")
        val names = project.buildTypes.map { it.name }
        assertThat(names, hasItems(*expectedNames))

        val expectedIds = arrayOf(
            "BuildLinuxJdk18",
            "BuildLinuxJdk11",
            "BuildWindowsJdk11",
            "BuildMacOsXJdk18")
        val ids = project.buildTypes.map { it.id.toString() }
        assertThat(ids, hasItems(*expectedIds))
    }

    @Test
    fun `matrix build excluding combinations`() {
        val project = Project {
            pipeline {
                stage ("Stage1") {
                    matrix {
                        axes {
                            "OS"("Linux", "Windows", "Mac OS X")
                            "JDK"("JDK_18", "JDK_11")
                        }
                        excludes {
                            exclude("OS" to "Windows")
                        }
                        build {
                            name = "Build - ${axes["OS"]} - ${axes["JDK"]}"
                        }
                    }
                }
            }
        }

        assertEquals(5, project.buildTypes.size) // includes stage build type

        val expectedNames = arrayOf(
            "Build - Linux - JDK_18",
            "Build - Linux - JDK_11",
            "Build - Mac OS X - JDK_11",
            "Build - Mac OS X - JDK_18")
        val names = project.buildTypes.map { it.name }
        assertThat(names, hasItems(*expectedNames))

        val expectedIds = arrayOf(
            "BuildLinuxJdk18",
            "BuildLinuxJdk11",
            "BuildMacOsXJdk11",
            "BuildMacOsXJdk18")
        val ids = project.buildTypes.map { it.id.toString() }
        assertThat(ids, hasItems(*expectedIds))
    }

    private fun Project.findBuildByName(name: String) : BuildType? {
        return buildTypes.find { build -> build.name == name }
    }
}
