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

package com.davils.kore.core.version.interval

import com.davils.kore.core.version.SemanticVersion
import com.davils.kore.core.version.grammar.CORE_SEGMENTS
import kotlin.jvm.JvmSynthetic

private const val ZERO_IDENTIFIER: String = "0"
private const val NINE: Char = '9'
private const val CARRY: Char = '1'

internal val MINIMUM: SemanticVersion = SemanticVersion(major = 0, preRelease = listOf(ZERO_IDENTIFIER))

@JvmSynthetic
internal fun successor(version: SemanticVersion): SemanticVersion? {
    if (!version.isPreRelease) return floorAbove(listOf(version.major, version.minor, version.patch))
    return preReleaseSuccessor(version)
}

@JvmSynthetic
internal fun preReleaseSuccessor(version: SemanticVersion): SemanticVersion =
    SemanticVersion.preReleaseSuccessorOf(version)

internal fun predecessor(version: SemanticVersion): SemanticVersion? {
    if (version.preRelease.lastOrNull() != ZERO_IDENTIFIER) return null
    return preReleasePredecessor(version) ?: releasePredecessor(version)
}

internal fun preReleasePredecessor(version: SemanticVersion): SemanticVersion? {
    val identifiers = version.preRelease
    if (identifiers.size < 2 || identifiers.last() != ZERO_IDENTIFIER) return null
    return SemanticVersion(
        major = version.major,
        minor = version.minor,
        patch = version.patch,
        preRelease = identifiers.dropLast(1),
    )
}

internal fun releaseBelow(floor: SemanticVersion): SemanticVersion? {
    val numbers = listOf(floor.major, floor.minor, floor.patch)
    val position = numbers.indexOfLast { number -> number > 0 }
    val isFloor = floor.preRelease.singleOrNull() == ZERO_IDENTIFIER && floor.build.isEmpty()
    if (!isFloor || position < 0) return null
    val lowered = numbers.take(position) + (numbers[position] - 1)
    return releaseCeiling(lowered)
}

internal fun floorAbove(prefix: List<Int>): SemanticVersion? {
    val position = prefix.indexOfLast { number -> number < Int.MAX_VALUE }
    if (position < 0) return null
    return preReleaseFloor(prefix.take(position) + (prefix[position] + 1))
}

internal fun releaseFloor(prefix: List<Int>): SemanticVersion = versionOf(padded(prefix, 0), emptyList())

internal fun preReleaseFloor(prefix: List<Int>): SemanticVersion = versionOf(padded(prefix, 0), listOf(ZERO_IDENTIFIER))

internal fun incremented(numeral: String): String {
    val kept = numeral.trimEnd(NINE)
    val zeros = ZERO_IDENTIFIER.repeat(numeral.length - kept.length)
    if (kept.isEmpty()) return CARRY + zeros
    return kept.dropLast(1) + (kept.last() + 1) + zeros
}

private fun releasePredecessor(version: SemanticVersion): SemanticVersion? {
    if (version.patch == 0) return null
    return SemanticVersion(major = version.major, minor = version.minor, patch = version.patch - 1)
}

private fun releaseCeiling(prefix: List<Int>): SemanticVersion = versionOf(padded(prefix, Int.MAX_VALUE), emptyList())

private fun versionOf(numbers: List<Int>, preRelease: List<String>): SemanticVersion =
    SemanticVersion(major = numbers[0], minor = numbers[1], patch = numbers[2], preRelease = preRelease)

private fun padded(prefix: List<Int>, filler: Int): List<Int> = prefix + List(CORE_SEGMENTS - prefix.size) { filler }
