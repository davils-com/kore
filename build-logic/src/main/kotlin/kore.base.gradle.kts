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

import com.davils.kreate.assurance.ReportFormat
import com.davils.kreate.assurance.Severity
import java.time.Duration

plugins {
    id("com.davils.kreate")
}

group = "com.davils"

val everySeverity: List<Severity> = Severity.entries.toList()
val isRoot: Boolean = path == ":"
val scannedRoot: String = projectDir.absolutePath
val generatedDirectories: List<String> = listOf("**/build", "**/.gradle", "**/.kotlin", "**/.git")
    .map { directory -> "$scannedRoot/$directory" } + listOf("$scannedRoot/.idea", "$scannedRoot/.port")

kreate {
    project {
        version {
            property = "version"
            snapshotUnlessTagged = true
        }

        dependencyLocking {
            enabled = true
            strict = true
            lockAllConfigurations = true
        }

        gitHooks {
            enabled = true
        }
    }

    trivy {
        enabled = true
        useInstalled = false

        secrets {
            severity = everySeverity
            failOnFindings = true
            runOnCheck = true
            secretConfig = isolated.rootProject.projectDirectory.file("trivy-secret.yaml")

            if (isRoot) {
                sourceFiles.setFrom(
                    fileTree(projectDir) {
                        exclude("kore-*/**", "**/build/**", "**/.gradle/**", "**/.kotlin/**", ".git/**", ".idea/**")
                        exclude(".port/**", "gradle/wrapper/gradle-wrapper.jar", "gradle/verification-keyring.gpg")
                    }
                )
            }
        }

        misconfig {
            enabled = isRoot
            severity = everySeverity
            failOnFindings = true
            skipDirectories = generatedDirectories
        }

        filesystem {
            enabled = isRoot
            severity = everySeverity
            failOnFindings = true
            skipDirectories = generatedDirectories
        }

        vulnerability {
            severity = everySeverity
            failOnFindings = true
            failOnMissingLockFiles = !isRoot
            ignoreUnfixed = false

            if (isRoot) {
                lockFiles.from(layout.projectDirectory.file("build-logic/gradle.lockfile"))
            }
        }

        license {
            severity = everySeverity - Severity.LOW
            failOnFindings = true
            failOnMissingLockFiles = !isRoot
        }
    }

    assurance {
        enabled = true
        runOnCheck = true
        reportOnly = false
        failOn = Severity.UNKNOWN
        minSeverity = Severity.UNKNOWN
        forbidIgnoreOf = setOf(Severity.CRITICAL, Severity.HIGH)
        internalGroups = setOf("com.davils")
        maxAdvisoryAge = Duration.ofDays(1)
        reuseVerdictFor = Duration.ofHours(1)

        ignore("LOW_SCORECARD") {
            reason = "JetBrains' nullability annotations, which kotlinx-coroutines brings along, carry no runtime code."
            module = "org.jetbrains:annotations"
            expires = "2027-05-16"
        }

        baseline {
            maxSeverity = Severity.LOW
            failOnStale = true
        }

        advisories {
            enabled = true
        }

        inspection {
            enabled = true
            failOnRisk = true
        }

        trust {
            enabled = true
            includeBuildscript = true
        }

        confusion {
            enabled = true
            includeBuildscript = true
        }

        dependencyPolicy {
            enabled = true
            forbidDynamicVersions = true
            forbidChangingVersions = true
            requireLocking = true
            requireVerification = true
            allowedRepositoryHosts = setOf("repo.maven.apache.org", "plugins.gradle.org", "repo.davils.com")
            checkOutdated = true
            maxMajorBehind = 0
        }

        workspace {
            enabled = true
            alignVersions = true
        }

        aggregate {
            enabled = isRoot
            members = setOf(":", ":kore-core", ":kore-testing")
        }

        reports {
            formats = ReportFormat.entries.toSet()
            ingestQuality = true
        }
    }
}
