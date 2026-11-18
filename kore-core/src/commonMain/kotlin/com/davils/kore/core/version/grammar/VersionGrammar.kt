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

import com.davils.kore.core.Limits
import com.davils.kore.core.version.SemanticVersion

internal const val CORE_SEGMENTS: Int = 3
internal const val PRE_RELEASE_SEPARATOR: Char = '-'
internal const val BUILD_SEPARATOR: Char = '+'
internal const val IDENTIFIER_SEPARATOR: Char = '.'
internal const val EMPTY_REASON: String = "is empty"
internal const val CORE_REASON: String = "must start with three dot-separated numbers, such as 1.4.0"

internal fun readVersion(text: String, minimumCoreSegments: Int = CORE_SEGMENTS): VersionReading {
    val problem = lengthProblem(text)
    if (problem != null) return VersionReading.Refused(problem)
    return readBuild(text, minimumCoreSegments)
}

private fun lengthProblem(text: String): String? {
    if (text.length > Limits.MAX_VERSION_LENGTH) return VERSION_TOO_LONG_REASON
    return EMPTY_REASON.takeIf { text.isEmpty() }
}

private fun readBuild(text: String, minimumCoreSegments: Int): VersionReading {
    val build = identifiersOf(partAfter(text, BUILD_SEPARATOR)) ?: return VersionReading.Refused(BUILD_REASON)
    return readPreRelease(text.substringBefore(BUILD_SEPARATOR), minimumCoreSegments, build)
}

private fun readPreRelease(withoutBuild: String, minimumCoreSegments: Int, build: List<String>): VersionReading {
    val identifiers = identifiersOf(partAfter(withoutBuild, PRE_RELEASE_SEPARATOR))
    val preRelease = identifiers?.takeIf { all -> all.all(::isPreReleaseIdentifier) }
        ?: return VersionReading.Refused(PRE_RELEASE_REASON)
    return readCore(
        core = withoutBuild.substringBefore(PRE_RELEASE_SEPARATOR),
        minimumCoreSegments = minimumCoreSegments,
        preRelease = preRelease,
        build = build,
    )
}

private fun readCore(
    core: String,
    minimumCoreSegments: Int,
    preRelease: List<String>,
    build: List<String>,
): VersionReading {
    val segments = core.split(IDENTIFIER_SEPARATOR)
    val isQualified = preRelease.isNotEmpty() || build.isNotEmpty()
    val problem = shapeProblem(segments = segments, minimumCoreSegments = minimumCoreSegments, qualified = isQualified)
        ?: segments.firstNotNullOfOrNull { segment -> numberProblem(segment) }
    if (problem != null) return VersionReading.Refused(problem)
    val numbers = segments.map(String::toInt) + List(CORE_SEGMENTS - segments.size) { 0 }
    val version = SemanticVersion(
        major = numbers[0],
        minor = numbers[1],
        patch = numbers[2],
        preRelease = preRelease,
        build = build,
    )
    return VersionReading.Read(version)
}

private fun shapeProblem(segments: List<String>, minimumCoreSegments: Int, qualified: Boolean): String? {
    val isCountFitting = segments.size in minimumCoreSegments..CORE_SEGMENTS
    val isShortenedQualified = qualified && segments.size < CORE_SEGMENTS
    return CORE_REASON.takeIf { !isCountFitting || isShortenedQualified }
}

private fun partAfter(text: String, separator: Char): String? {
    val at = text.indexOf(separator)
    if (at < 0) return null
    return text.substring(at + 1)
}

private fun identifiersOf(part: String?): List<String>? {
    if (part == null) return emptyList()
    return part.split(IDENTIFIER_SEPARATOR).takeIf { all -> all.all(::isIdentifier) }
}
