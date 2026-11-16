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

package com.davils.kore.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Refuses to publish a version that does not belong to the build it comes from.
 *
 * Every build must resolve exactly the version declared in `gradle.properties`, so no environment variable can change
 * what is published. A build without a release tag must publish that version as a `-SNAPSHOT`. A tag build must
 * publish exactly the version its tag names, that version must be a release in the grammar of Semantic Versioning
 * 2.0.0 (the expression of semver.org without build metadata), the tagged commit must be part of `main`, and none of
 * the build tools may be a snapshot, because a snapshot is neither signed nor verified. Kreate routes a version by its
 * suffix alone, so without this guard a misconfigured pipeline could push anything to the releases repository.
 *
 * @since 1.0.0
 */
@DisableCachingByDefault(because = "The verdict depends on the environment of the build, which is never cached.")
public abstract class ReleaseGuard : DefaultTask() {

    /**
     * The kind of Git reference the build runs for, `tag` on a release.
     *
     * @since 1.0.0
     */
    @get:Input
    public abstract val referenceType: Property<String>

    /**
     * The short name of the Git reference the build runs for.
     *
     * @since 1.0.0
     */
    @get:Input
    public abstract val referenceName: Property<String>

    /**
     * The version declared in `gradle.properties`.
     *
     * @since 1.0.0
     */
    @get:Input
    public abstract val declaredVersion: Property<String>

    /**
     * The version the build resolved and would publish.
     *
     * @since 1.0.0
     */
    @get:Input
    public abstract val resolvedVersion: Property<String>

    /**
     * Whether the commit the build runs for is part of the history of `main`.
     *
     * @since 1.0.0
     */
    @get:Input
    public abstract val onMain: Property<Boolean>

    /**
     * The versions of the tools the build runs with, such as Kreate, Detekt and Kotlin, each as `name version`.
     *
     * @since 1.0.0
     */
    @get:Input
    public abstract val toolVersions: ListProperty<String>

    /**
     * Fails the build when the resolved version does not match the declared version or the kind of build.
     *
     * @since 1.0.0
     */
    @TaskAction
    public fun verify() {
        val declared = declaredVersion.get()
        val version = resolvedVersion.get()
        requireDeclared(declared, version.removeSuffix(SNAPSHOT_SUFFIX))
        if (referenceType.get() != TAG) {
            requireSnapshot(version)
            return
        }
        requireRelease(version, referenceName.get())
    }

    private fun requireDeclared(declared: String, resolved: String) {
        if (declared == resolved) return

        throw GradleException(
            "The build resolved the version '$resolved', but gradle.properties declares '$declared'. " +
                "Change the version in gradle.properties, never through the environment.",
        )
    }

    private fun requireSnapshot(version: String) {
        if (version.endsWith(SNAPSHOT_SUFFIX)) return

        throw GradleException("A build without a release tag must publish a snapshot, but it resolved '$version'.")
    }

    private fun requireRelease(version: String, tag: String) {
        if (version != tag) {
            throw GradleException("The release tag '$tag' does not match the version '$version' of the build.")
        }
        if (!RELEASE_VERSION.matches(version)) {
            throw GradleException("The release version '$version' is not a Semantic Versioning 2.0.0 release.")
        }
        if (!onMain.get()) {
            throw GradleException("The release tag '$tag' is on a commit that is not part of main.")
        }
        val snapshots = toolVersions.get().filter { tool -> tool.endsWith(SNAPSHOT_SUFFIX) }
        if (snapshots.isEmpty()) return

        throw GradleException("A release cannot be built with snapshot tooling: ${snapshots.joinToString()}.")
    }

    private companion object {
        const val TAG: String = "tag"
        const val SNAPSHOT_SUFFIX: String = "-SNAPSHOT"
        val RELEASE_VERSION: Regex = Regex(
            "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)" +
                "(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?$"
        )
    }
}
