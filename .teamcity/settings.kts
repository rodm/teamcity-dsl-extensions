
import com.github.rodm.teamcity.pipeline
import jetbrains.buildServer.configs.kotlin.v2018_1.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2018_1.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2018_1.project
import jetbrains.buildServer.configs.kotlin.v2018_1.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.v2018_1.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2018_1.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.v2018_1.version

version = "2018.1"

project {

    val vcsRoot = GitVcsRoot {
        id("TeamcityDslExtensions")
        name = "teamcity-dsl-extensions"
        url = "https://github.com/rodm/teamcity-dsl-extensions.git"
        useMirrors = false
    }
    vcsRoot(vcsRoot)

    val buildTemplate = template {
        id("Build")
        name = "Build"

        params {
            param("gradle.opts", "")
            param("gradle.tasks", "clean build")
        }

        vcs {
            root(vcsRoot)
            checkoutMode = CheckoutMode.ON_SERVER
        }

        steps {
            gradle {
                id = "RUNNER_30"
                tasks = "%gradle.tasks%"
                buildFile = ""
                gradleParams = "%gradle.opts%"
                enableStacktrace = true
                jdkHome = "%java8.home%"
            }
        }

        triggers {
            vcs {
                id = "vcsTrigger"
                quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_DEFAULT
                branchFilter = ""
            }
        }

        features {
            feature {
                id = "perfmon"
                type = "perfmon"
            }
        }
    }

    pipeline {
        stage ("Build") {
            build {
                templates(buildTemplate)
                id("BuildTeamCity20181")
                name = "Build - TeamCity 2018.1"

                params {
                    param("gradle.opts", "-Pteamcity.server.url=%teamcity.serverUrl%/app/dsl-plugins-repository")
                }
            }
            build {
                templates(buildTemplate)
                id("ReportCodeQuality")
                name = "Report - Code Quality"

                params {
                    param("gradle.opts", "-Pteamcity.server.url=%teamcity.serverUrl%/app/dsl-plugins-repository %sonar.opts%")
                    param("gradle.tasks", "clean build sonarqube")
                }
            }
        }
    }
}
