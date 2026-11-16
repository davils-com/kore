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

import com.davils.kore.core.limit.KoreLimits

internal const val VERSION_TOO_LONG_REASON: String = "is longer than ${KoreLimits.MAX_VERSION_LENGTH} characters"
internal const val PRE_RELEASE_REASON: String = "has an empty or malformed pre-release identifier"
internal const val BUILD_REASON: String = "has an empty or malformed build identifier"

private const val LEADING_ZERO_REASON: String = "has a number with a leading zero"
private const val TOO_LARGE_REASON: String = "has a number larger than ${Int.MAX_VALUE}"

internal fun partsProblem(major: Int, minor: Int, patch: Int, preRelease: List<String>, build: List<String>): String? {
    val isFitting = fitsVersionLength(
        major = major,
        minor = minor,
        patch = patch,
        preRelease = preRelease,
        build = build,
    )
    if (!isFitting) return VERSION_TOO_LONG_REASON
    return preReleaseProblem(preRelease) ?: buildProblem(build)
}

internal fun fitsVersionLength(
    major: Int,
    minor: Int,
    patch: Int,
    preRelease: List<String>,
    build: List<String>,
): Boolean {
    val core = "$major$IDENTIFIER_SEPARATOR$minor$IDENTIFIER_SEPARATOR$patch".length.toLong()
    return core + qualifierLength(preRelease) + qualifierLength(build) <= KoreLimits.MAX_VERSION_LENGTH
}

internal fun preReleaseProblem(identifiers: List<String>): String? =
    PRE_RELEASE_REASON.takeUnless { identifiers.all(::isPreReleaseIdentifier) }

internal fun buildProblem(identifiers: List<String>): String? =
    BUILD_REASON.takeUnless { identifiers.all(::isIdentifier) }

internal fun numberProblem(segment: String, malformedReason: String = CORE_REASON): String? {
    if (!isNumeric(segment)) return malformedReason
    return numeralProblem(segment)
}

private fun numeralProblem(numeral: String): String? {
    if (numeral.length > 1 && numeral.first() == ZERO) return LEADING_ZERO_REASON
    return TOO_LARGE_REASON.takeIf { numeral.toIntOrNull() == null }
}

private fun qualifierLength(identifiers: List<String>): Long =
    identifiers.sumOf { identifier -> identifier.length.toLong() + 1 }
