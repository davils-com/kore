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

import com.davils.kore.gradle.LicenseHeaderCheck
import com.davils.kore.gradle.ReleaseGuard

plugins {
    base
    id("kore.base")
}

kreate {
    project {
        changelog {
            enabled = true
            runOnCheck = true
            includeDates = true
            tagPrefix = ""
        }
    }
}

val catalog: VersionCatalog = the<VersionCatalogsExtension>().named("libs")
val tools: List<String> = listOf("kreate", "detekt", "kotlin").map { tool ->
    "$tool ${catalog.findVersion(tool).get().requiredVersion}"
}
val mainContainsHead: Provider<Boolean> = providers.exec {
    commandLine("git", "merge-base", "--is-ancestor", "HEAD", "origin/main")
    isIgnoreExitValue = true
}.result.map { result -> result.exitValue == 0 }

val releaseGuard = tasks.register<ReleaseGuard>("koreReleaseGuard") {
    description = "Refuses to publish a version that does not match gradle.properties and the release tag."
    group = "verification"
    referenceType = providers.environmentVariable("GITHUB_REF_TYPE").orElse("")
    referenceName = providers.environmentVariable("GITHUB_REF_NAME").orElse("")
    declaredVersion = providers.gradleProperty("version")
    resolvedVersion = provider { project.version.toString() }
    onMain = mainContainsHead
    toolVersions = tools
}

val licenseHeaderCheck = tasks.register<LicenseHeaderCheck>("koreLicenseHeaderCheck") {
    description = "Verifies that every source, build script and configuration file starts with the license header."
    group = "verification"
    sources.from(
        fileTree(layout.projectDirectory) {
            include("**/*.kt", "**/*.kts", "**/*.java", "**/*.properties", "**/*.yaml", "**/*.yml")
            exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**", ".port/**", ".idea/**")
            exclude("gradle/wrapper/**", "gradle/gradle-daemon-jvm.properties")
        }
    )
    report = layout.buildDirectory.file("reports/kore/license-headers.txt")
}

tasks.named("check") {
    dependsOn(releaseGuard, licenseHeaderCheck)
}
