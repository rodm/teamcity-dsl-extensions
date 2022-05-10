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

package com.github.rodm.teamcity

import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.Parametrized

fun Project.findBuildByName(name: String) : BuildType? {
    return buildTypes.find { build -> build.name == name }
}

// supports ProjectFeature and BuildFeature tests
fun Parametrized.findParam(name: String) : String? {
    params.forEach { param ->
        if (param.name == name) return param.value
    }
    return null
}
