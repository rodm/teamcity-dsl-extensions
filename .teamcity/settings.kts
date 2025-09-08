
import com.github.rodm.teamcity.pipeline
import com.github.rodm.teamcity.project.githubIssueTracker
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger.QuietPeriodMode.USE_DEFAULT
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS
import jetbrains.buildServer.configs.kotlin.version

version = "2025.07"

project {

    val vcsRoot = GitVcsRoot {
        id("TeamcityDslExtensions")
        name = "teamcity-dsl-extensions"
        url = "https://github.com/rodm/teamcity-dsl-extensions.git"
        branch = "refs/heads/main"
        branchSpec = """
            +:refs/heads/(main)
            +:refs/tags/(*)
        """.trimIndent()
        useTagsAsBranches = true
        checkoutPolicy = NO_MIRRORS
    }
    vcsRoot(vcsRoot)

    features {
        githubIssueTracker {
            displayName = "TeamCity DSL Extensions"
            repository = "https://github.com/rodm/teamcity-dsl-extensions"
            pattern = """#(\d+)"""
        }
    }

    params {
        param("teamcity.ui.settings.readOnly", "true")
    }

    val buildTemplate = template {
        id("Build")
        name = "Build"

        params {
            param("gradle.opts", "")
            param("gradle.tasks", "clean build")
        }

        vcs {
            root(vcsRoot)
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

        features {
            feature {
                id = "perfmon"
                type = "perfmon"
            }
        }
    }

    pipeline {
        val serverUrl = "-Pteamcity.server.url=%teamcity.serverUrl%/app/dsl-plugins-repository"

        stage ("Build") {
            build {
                templates(buildTemplate)
                id("BuildTeamCity")
                name = "Build - DSL Library"

                params {
                    param("gradle.opts", serverUrl)
                }
            }
            build {
                templates(buildTemplate)
                id("ReportCodeQuality")
                name = "Report - Code Quality"

                params {
                    param("gradle.opts", "${serverUrl} %sonar.opts%")
                    param("gradle.tasks", "clean build sonarqube")
                }
            }
        }

        stage ("Publish") {
            build {
                templates(buildTemplate)
                id("PublishToNexus")
                name = "Publish to Nexus"

                params {
                    param("gradle.opts", "${serverUrl} %nexus.opts% %signing.opts%")
                    param("gradle.tasks", "clean build publishMavenPublicationToMavenRepository")
                }

                triggers {
                    vcs {
                        quietPeriodMode = USE_DEFAULT
                        branchFilter = ""
                        triggerRules = """
                            -:.github/**
                            -:README.adoc
                        """.trimIndent()
                    }
                }
            }
        }
    }
}
