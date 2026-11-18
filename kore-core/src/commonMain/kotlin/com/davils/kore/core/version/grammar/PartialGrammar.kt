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

package com.davils.kore.core.version.grammar

import com.davils.kore.core.version.SemanticVersion
import com.davils.kore.core.version.interval.releaseFloor

private const val WILDCARD_REASON: String = "has a wildcard followed by a number, such as 1.x.3"
private const val PARTIAL_REASON: String = "must be a version, or up to three numbers " +
    "where x or * stands for any, such as 1.2.x"

private val wildcards: Set<String> = setOf("x", "X", "*")

internal fun partialOf(version: SemanticVersion): Partial =
    Partial(listOf(version.major, version.minor, version.patch), version)

internal fun readPartial(text: String): PartialReading {
    val isQualified = text.any { character -> character == PRE_RELEASE_SEPARATOR || character == BUILD_SEPARATOR }
    if (isQualified) return readQualified(text)
    return readNumbers(text.split(IDENTIFIER_SEPARATOR))
}

private fun readQualified(text: String): PartialReading = when (val reading = readVersion(text)) {
    is VersionReading.Read -> PartialReading.Read(partialOf(reading.version))
    is VersionReading.Refused -> PartialReading.Refused(reading.reason)
}

private fun readNumbers(segments: List<String>): PartialReading {
    val given = segments.takeWhile { segment -> segment !in wildcards }
    val problem = segmentsProblem(segments, given)
    if (problem != null) return PartialReading.Refused(problem)
    val numbers = given.map(String::toInt)
    val complete = releaseFloor(numbers).takeIf { numbers.size == CORE_SEGMENTS }
    return PartialReading.Read(Partial(numbers, complete))
}

private fun segmentsProblem(segments: List<String>, given: List<String>): String? {
    if (segments.size > CORE_SEGMENTS) return PARTIAL_REASON
    return wildcardProblem(segments.drop(given.size))
        ?: given.firstNotNullOfOrNull { segment -> numberProblem(segment, PARTIAL_REASON) }
}

private fun wildcardProblem(rest: List<String>): String? =
    WILDCARD_REASON.takeUnless { rest.all { segment -> segment in wildcards } }
