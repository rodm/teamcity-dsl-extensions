
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id ("org.gradle.signing")
    id ("org.gradle.maven-publish")
    id ("org.gradle.jacoco")
    id ("org.jetbrains.kotlin.jvm") version "1.6.21"
    id ("org.sonarqube") version "3.3"
}

group = "com.github.rodm"
version = "0.9-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://download.jetbrains.com/teamcity-repository")
    }
    maven {
        url = uri(project.findProperty("teamcity.server.url") ?: "http://localhost:8111/app/dsl-plugins-repository")
        isAllowInsecureProtocol = true
    }
}

configurations {
    testImplementation.get().extendsFrom(configurations.compileOnly.get())
}

dependencies {
    compileOnly (group = "org.jetbrains.teamcity", name = "configs-dsl-kotlin-plugins-latest", version = "1.0-SNAPSHOT")
    compileOnly (group = "org.jetbrains.teamcity", name = "configs-dsl-kotlin-latest")

    testImplementation (platform("org.junit:junit-bom:5.8.2"))
    testImplementation (group = "org.junit.jupiter", name = "junit-jupiter-api")
    testImplementation (group = "org.hamcrest", name = "hamcrest", version = "2.2")

    testRuntimeOnly (group = "org.junit.jupiter", name = "junit-jupiter-engine")
    testRuntimeOnly (group = "org.jetbrains.teamcity", name = "server-api", version = "2022.04")
}

java {
    withJavadocJar()
    withSourcesJar()
}

jacoco {
    toolVersion = "0.8.8"
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    reports {
        xml.required.set(true)
    }
}

publishing {
    repositories {
        maven {
            val urlName = if ("${project.version}".endsWith("-SNAPSHOT"))  "repository.snapshots.url" else "repository.releases.url"
            url = uri(findProperty(urlName) ?: "")
            isAllowInsecureProtocol = "true" == (findProperty("repository.allowInsecureProtocol") ?: "")

            credentials {
                val repositoryUsername = findProperty("repository.username") as String? ?: ""
                val repositoryPassword = findProperty("repository.password") as String? ?: ""

                username = repositoryUsername
                password = repositoryPassword
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("TeamCity DSL Extension Library")
                description.set("A library that extends the TeamCity Kotlin DSL")
                url.set("https://github.com/rodm/teamcity-dsl-extensions")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("rodm")
                        name.set("Rod MacKenzie")
                        email.set("rod.n.mackenzie@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/rodm/teamcity-dsl-extensions.git")
                    developerConnection.set("scm:git:ssh://github.com/rodm/teamcity-dsl-extensions.git")
                    url.set("https://github.com/rodm/teamcity-dsl-extensions")
                }
            }
        }
    }
}

signing {
    isRequired = !"${project.version}".endsWith("-SNAPSHOT")
    sign(publishing.publications["maven"])
}
