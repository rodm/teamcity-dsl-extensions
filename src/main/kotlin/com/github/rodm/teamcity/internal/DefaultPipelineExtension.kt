/*
 * Copyright 2020 Rod MacKenzie.
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

package com.github.rodm.teamcity.internal

import com.github.rodm.teamcity.Axes
import com.github.rodm.teamcity.Excludes
import com.github.rodm.teamcity.Matrix
import com.github.rodm.teamcity.MatrixBuildType
import com.github.rodm.teamcity.Stage
import jetbrains.buildServer.configs.kotlin.v2019_2.copyTo
import jetbrains.buildServer.configs.kotlin.v2019_2.toId

class DefaultMatrix(private val stage: Stage) : Matrix {
    private var axes = DefaultAxes()
    private var axesDefined = false
    private var buildDefined = false
    private val excludes = DefaultExcludes(axes)

    override fun axes(init: Axes.() -> Unit) : Axes {
        if (axesDefined) throw IllegalStateException("only one axes configuration can be defined")
        axesDefined = true

        return axes.apply(init)
    }

    override fun excludes(init: Excludes.() -> Unit ) {
        excludes.apply(init)
    }

    override fun build(init: MatrixBuildType.() -> Unit) {
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

class DefaultAxes : Axes {
    val axes = linkedMapOf<String, List<String>>()

    override operator fun String.invoke(vararg values: String) {
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

class DefaultExcludes(private val axes: DefaultAxes) : Excludes {
    val excludes = mutableListOf<Map<String, String>>()

    override fun exclude(vararg pairs: Pair<String, String>) {
        pairs.forEach { pair ->
            if (!axes.axes.containsKey(pair.first)) {
                throw IllegalArgumentException("Invalid name: ${pair.first}")
            }
            if (!(axes.axes.get(pair.first)?.contains(pair.second))!!) {
                throw IllegalArgumentException("Invalid value: ${pair.second}")
            }
        }
        excludes.add(pairs.toMap())
    }
}
