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

import com.davils.kreate.coverage.Grouping
import com.davils.kreate.testing.KotestModule
import com.davils.kreate.testing.LegacyTestPolicy
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("kore.base")
}

val koreUrl = "https://github.com/davils-com/kore"
val annotationPackage = "com.davils.kore.core.annotation"
val koreModuleName: String = "com.davils.kore.${name.removePrefix("kore-")}"
val parallelForks: Int = maxOf(1, Runtime.getRuntime().availableProcessors() / 2)
val catalog: VersionCatalog = the<VersionCatalogsExtension>().named("libs")
val jacksonFloor: String = catalog.findVersion("tooling-jackson").get().requiredVersion
val bouncyCastleFloor: String = catalog.findVersion("tooling-bouncycastle").get().requiredVersion

kreate {
    platform {
        javaVersion = JavaVersion.VERSION_25
        explicitApi = true
        allWarningsAsErrors = true

        compiler {
            progressiveMode = true
            extraWarnings = true
            languageVersion = KotlinVersion.KOTLIN_2_4
            apiVersion = KotlinVersion.KOTLIN_2_4
            jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
        }

        jvm {
            moduleName = koreModuleName
            javaWarningsAsErrors = true
        }
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
            libraryRules = true
            typeResolution = true

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

            releaseGuard {
                enabled = true
                releaseBranch = "main"
                tagPrefix = ""
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

        archives {
            legalFiles.from(isolated.rootProject.projectDirectory.file("LICENSE"))
            legalFiles.from(isolated.rootProject.projectDirectory.file("NOTICE"))
            implementationManifest = true
            vendor = "Davils"
        }

        toolClasspaths {
            floor("com.fasterxml.jackson", jacksonFloor) {
                exclude("jackson-annotations")
            }
            floor("org.bouncycastle", bouncyCastleFloor)
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
