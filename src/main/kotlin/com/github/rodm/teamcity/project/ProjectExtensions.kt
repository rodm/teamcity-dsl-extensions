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

package com.github.rodm.teamcity.project

import jetbrains.buildServer.configs.kotlin.v2019_2.ProjectFeature
import jetbrains.buildServer.configs.kotlin.v2019_2.ProjectFeatures


class GitHubIssueTracker() : ProjectFeature() {

    init {
        type = "IssueTracker"
        param("type", "GithubIssues")
        param("authType", "anonymous")
    }

    constructor(init: GitHubIssueTracker.() -> Unit): this() {
        init()
    }

    var displayName by stringParameter("name")

    var repository by stringParameter()

    var pattern by stringParameter()
}

fun ProjectFeatures.githubIssueTracker(init: GitHubIssueTracker.() -> Unit): GitHubIssueTracker {
    val tracker = GitHubIssueTracker(init)
    feature(tracker)
    return tracker
}
