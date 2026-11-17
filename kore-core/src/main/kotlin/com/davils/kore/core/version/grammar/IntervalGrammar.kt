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
import com.davils.kore.core.version.interval.boundedInterval
import com.davils.kore.core.version.interval.interval

private const val INTERVAL_EMPTY_REASON: String =
    "has an interval that is empty, because its lower bound is not below its upper"
private const val INTERVAL_BOUND_REASON: String = "has a bound that is not a version: it"
private const val SHORTEST_BOUND: Int = 1
private const val BOUND_SEPARATOR: Char = ','
private const val INCLUSIVE_LOWER: Char = '['
private const val INCLUSIVE_UPPER: Char = ']'
private const val MOST_BOUNDS: Int = 2

internal fun readInterval(text: String): RangeReading {
    val bounds = text.substring(startIndex = 1, endIndex = text.length - 1).split(BOUND_SEPARATOR)
    if (bounds.size > MOST_BOUNDS) return RangeReading.Refused(INTERVAL_SHAPE_REASON)
    return readBoundList(
        bounds = bounds.map(::trimmedRangeSpace),
        lowerInclusive = text.first() == INCLUSIVE_LOWER,
        upperInclusive = text.last() == INCLUSIVE_UPPER,
    )
}

private fun readBoundList(bounds: List<String>, lowerInclusive: Boolean, upperInclusive: Boolean): RangeReading {
    if (bounds.size == 1) return readExact(bounds[0], lowerInclusive && upperInclusive)
    return readBounds(
        lower = bounds[0],
        lowerInclusive = lowerInclusive,
        upper = bounds[1],
        upperInclusive = upperInclusive,
    )
}

private fun readExact(bound: String, bothInclusive: Boolean): RangeReading {
    if (!bothInclusive) return RangeReading.Refused(INTERVAL_SHAPE_REASON)
    return when (val reading = readVersion(bound, SHORTEST_BOUND)) {
        is VersionReading.Read -> exactly(reading.version)
        is VersionReading.Refused -> boundRefusal(reading)
    }
}

private fun exactly(version: SemanticVersion): RangeReading {
    val interval = boundedInterval(lower = version, lowerInclusive = true, upper = version, upperInclusive = true)
    return RangeReading.Read(listOfNotNull(interval))
}

private fun readBounds(lower: String, lowerInclusive: Boolean, upper: String, upperInclusive: Boolean): RangeReading {
    val isLowerMissing = lower.isEmpty() && lowerInclusive
    val isUpperMissing = upper.isEmpty() && upperInclusive
    if (isLowerMissing || isUpperMissing) return RangeReading.Refused(INTERVAL_SHAPE_REASON)
    return readBoundVersions(
        from = boundOf(lower),
        lowerInclusive = lowerInclusive,
        to = boundOf(upper),
        upperInclusive = upperInclusive,
    )
}

private fun boundOf(text: String): VersionReading? {
    if (text.isEmpty()) return null
    return readVersion(text, SHORTEST_BOUND)
}

private fun readBoundVersions(
    from: VersionReading?,
    lowerInclusive: Boolean,
    to: VersionReading?,
    upperInclusive: Boolean,
): RangeReading {
    val refused = from as? VersionReading.Refused ?: to as? VersionReading.Refused
    if (refused != null) return boundRefusal(refused)
    val interval = boundedInterval(
        lower = versionOf(from),
        lowerInclusive = lowerInclusive,
        upper = versionOf(to),
        upperInclusive = upperInclusive,
    )
    return readingOf(interval, INTERVAL_EMPTY_REASON)
}

private fun versionOf(reading: VersionReading?): SemanticVersion? = (reading as? VersionReading.Read)?.version

private fun boundRefusal(reading: VersionReading.Refused): RangeReading =
    RangeReading.Refused("$INTERVAL_BOUND_REASON ${reading.reason}")
