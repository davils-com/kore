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

import org.gradle.api.Action
import org.gradle.api.artifacts.DependencyResolveDetails

/**
 * Raises every module of [groups] that a tool resolves below [floor] to [floor], for example a patched release.
 *
 * The rule only ever raises a version, never lowers one, and it is meant for the classpaths of tools such as Dokka
 * or the Kotlin Gradle plugin, which Kreate locks and Trivy scans like every other configuration. Modules named in
 * [excluded] keep the version they asked for, such as an annotations artifact that follows its own numbering.
 *
 * @property groups the groups whose modules are raised; a group matches itself and every subgroup.
 * @property floor the lowest version that may be resolved, such as `2.22.3`.
 * @property excluded the names of modules that are never raised.
 * @since 1.0.0
 */
public class VersionFloor(
    private val groups: Set<String>,
    private val floor: String,
    private val excluded: Set<String> = emptySet(),
) : Action<DependencyResolveDetails> {
    /**
     * Raises the requested version of a matching module when it is below the floor.
     *
     * @param details the dependency being resolved.
     * @since 1.0.0
     */
    override fun execute(details: DependencyResolveDetails) {
        val requested = details.requested
        val isRaised = matches(requested.group, requested.name) && isBelow(requested.version.orEmpty())
        if (!isRaised) return
        details.useVersion(floor)
        details.because("Raised to $floor, the first release without the known vulnerabilities")
    }

    private fun matches(group: String, name: String): Boolean {
        if (name in excluded) return false
        return groups.any { prefix -> group == prefix || group.startsWith("$prefix.") }
    }

    private fun isBelow(version: String): Boolean {
        val actual = numbersOf(version)
        val wanted = numbersOf(floor)
        val firstDifference = actual.zip(wanted).firstOrNull { (left, right) -> left != right }
        return firstDifference?.let { (left, right) -> left < right } ?: (actual.size < wanted.size)
    }

    private fun numbersOf(version: String): List<Int> {
        val release = version.substringBefore('-')
        return release.split('.').map { part -> part.toIntOrNull() ?: 0 }
    }
}
