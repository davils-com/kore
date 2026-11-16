/*
 * Copyright 2026 Davils
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

@file:Suppress("UnstableApiUsage")

import com.davils.kore.gradle.ModulePatch
import com.davils.kore.gradle.VersionFloor
import com.davils.kreate.coverage.Grouping
import com.davils.kreate.testing.KotestModule
import com.davils.kreate.testing.LegacyTestPolicy
import dev.detekt.gradle.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("kore.base")
}

val koreUrl = "https://github.com/davils-com/kore"
val annotationPackage = "com.davils.kore.core.annotation"
val moduleName: String = "com.davils.kore.${project.name.removePrefix("kore-")}"
val javaRelease = 25
val parallelForks: Int = maxOf(1, Runtime.getRuntime().availableProcessors() / 2)
val catalog: VersionCatalog = the<VersionCatalogsExtension>().named("libs")
val hasModuleDescriptor: Boolean = file("src/main/java/module-info.java").exists()

kreate {
    platform {
        javaVersion = JavaVersion.VERSION_25
        explicitApi = true
        allWarningsAsErrors = true
    }

    project {
        tests {
            enabled = true
            ignoreFailures = false
            failOnNoDiscoveredTests = true
            legacyTestSourceSet = LegacyTestPolicy.FAIL
            maxParallelForks = parallelForks

            report {
                enabled = true
                xml = true
                html = true
            }

            kotest {
                enabled = true
                modules = listOf(KotestModule.ASSERTIONS, KotestModule.PROPERTY, KotestModule.JUNIT_XML)
                addJUnitPlatformLauncher = true
            }

            suites {
                named("integrationTest") {
                    enabled = false
                }
            }
        }

        detekt {
            enabled = true
            allRules = true
            buildUponDefaultConfig = true
            kreateRules = true
            formatting = true

            reports {
                sarif {
                    required = true
                }

                checkstyle {
                    required = true
                }

                html {
                    required = true
                }

                markdown {
                    required = true
                }
            }
        }

        coverage {
            enabled = true

            filters {
                excludes {
                    annotatedBy.add("$annotationPackage.DavilsPlatformBinding")
                    annotatedBy.add("$annotationPackage.DavilsGenerated")
                }
            }

            reports {
                xml {
                    onCheck = true
                }

                html {
                    onCheck = true
                }
            }

            verify {
                runOnCheck = true
                warningInsteadOfFailure = false
                groupBy = Grouping.APPLICATION
                minLineCoverage = 97
                minBranchCoverage = 97
                minInstructionCoverage = 97
            }
        }

        apiValidation {
            enabled = true

            semver {
                enabled = true
                tagPrefix = ""
                requireBreakingCommit = true
                runOnCheck = true
            }
        }

        docs {
            enabled = true
            moduleName = project.name
            copyright = "Copyright 2026 Davils. Licensed under the Apache License, Version 2.0."
        }

        publish {
            enabled = true
            inceptionYear = 2026
            website = koreUrl

            pom {
                issueManagement {
                    system = "GitHub Issues"
                    url = "$koreUrl/issues"
                }

                ciManagement {
                    system = "GitHub Actions"
                    url = "$koreUrl/actions"
                }

                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "repo"
                    }
                }

                developers {
                    developer {
                        id = "davils"
                        name = "Davils"
                        email = "development@davils.com"
                        organization = "Davils"
                        timezone = "Europe/Berlin"
                    }
                }

                scm {
                    url = koreUrl
                    connection = "scm:git:$koreUrl.git"
                    developerConnection = "scm:git:ssh://git@github.com/davils-com/kore.git"
                }
            }

            repositories {
                maven {
                    enabled = true
                    name = "Davils"
                    releases = "https://repo.davils.com/releases"
                    snapshots = "https://repo.davils.com/snapshots"
                    usernameEnv = "MAVEN_USERNAME"
                    passwordEnv = "MAVEN_PASSWORD"
                    signPublications = true
                    allowInsecureProtocol = false
                }
            }
        }
    }

    assurance {
        advisories {
            configurations = setOf(
                "compileClasspath",
                "runtimeClasspath",
                "unitTestCompileClasspath",
                "unitTestRuntimeClasspath",
            )
        }
    }
}

pluginManager.withPlugin("dev.detekt") {
    dependencies.addProvider("detektPlugins", catalog.findLibrary("detekt-rules-libraries").get())
}

tasks.named("check") {
    dependsOn(tasks.matching { task -> task.name == "detektMain" || task.name == "detektUnitTest" })
}

val requestedTasks: List<String> = gradle.startParameter.taskNames
val formatRequested: Boolean = requestedTasks.any { name -> name.substringAfterLast(':') == "kreateFormatApply" }

tasks.withType<Detekt>().matching { task -> task.name == "kreateFormatApply" }.configureEach {
    enabled = formatRequested
    reports.sarif.required = false
}

val jacksonFloor = VersionFloor(
    groups = setOf("com.fasterxml.jackson"),
    floor = catalog.findVersion("tooling-jackson").get().requiredVersion,
    excluded = setOf("jackson-annotations"),
)
val bouncyCastleFloor = VersionFloor(
    groups = setOf("org.bouncycastle"),
    floor = catalog.findVersion("tooling-bouncycastle").get().requiredVersion,
)

configurations.matching { configuration -> configuration.name.startsWith("dokka") }.configureEach {
    resolutionStrategy.eachDependency(jacksonFloor)
}

configurations.matching { configuration -> configuration.name == "kotlinBouncyCastleConfiguration" }.configureEach {
    resolutionStrategy.eachDependency(bouncyCastleFloor)
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        progressiveMode = true
        extraWarnings = true
        jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
        languageVersion = KotlinVersion.KOTLIN_2_4
        apiVersion = KotlinVersion.KOTLIN_2_4
    }
}

val kotlinClasses: Provider<Directory> = tasks.named<KotlinJvmCompile>("compileKotlin")
    .flatMap { compile -> compile.destinationDirectory }

tasks.named<JavaCompile>("compileJava") {
    options.release = javaRelease
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))

    if (hasModuleDescriptor) {
        options.compilerArgumentProviders.add(ModulePatch(moduleName, kotlinClasses))
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    dependsOn(":koreReleaseGuard")
}

val implementationVersion: Provider<String> = provider { project.version.toString() }

tasks.named<Jar>("jar") {
    from(isolated.rootProject.projectDirectory.file("LICENSE")) {
        into("META-INF")
    }

    from(isolated.rootProject.projectDirectory.file("NOTICE")) {
        into("META-INF")
    }

    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to moduleName,
                "Implementation-Vendor" to "Davils",
                "Implementation-Version" to implementationVersion,
            )
        )

        if (!hasModuleDescriptor) {
            attributes(mapOf("Automatic-Module-Name" to moduleName))
        }
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
