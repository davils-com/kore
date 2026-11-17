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

package com.davils.kore.core.arguments.conversion

import com.davils.kore.core.limit.Limits
import com.davils.kore.core.version.SemanticVersion
import com.davils.kore.core.version.SemanticVersionRange
import kotlin.time.Duration

private const val BOOLEAN_REASON: String = "is not a boolean, expected one of true, false, yes, no, on, off, 1, 0"
private const val DURATION_REASON: String = "is not a valid duration, expected something like 30s, 5m or PT1H"
private const val INFINITE_DURATION_REASON: String = "is not a finite duration"
private const val NEGATIVE_DURATION_REASON: String = "is a negative duration"
private const val VERSION_REASON: String = "is not a semantic version, expected something like 1.4.0 or 2.0.0-rc.1"
private const val RANGE_REASON: String = "is not a version range, expected something like ^1.2 or >=1.0.0 <2.0.0"

private val truthy: Set<String> = setOf("true", "yes", "on", "1")
private val falsy: Set<String> = setOf("false", "no", "off", "0")

internal val stringConverter: Converter<String> = NamedConverter(typeName = "string") { raw -> raw }

internal val flagConverter: Converter<Boolean> = NamedConverter(typeName = "flag", reading = ::readBoolean)

internal fun readBoolean(raw: String): Boolean {
    val normalised = asciiLowercase(raw)
    if (normalised !in truthy && normalised !in falsy) throw ConversionException(BOOLEAN_REASON)
    return normalised in truthy
}

internal fun readDuration(raw: String, shortest: Duration, longest: Duration): Duration {
    val duration = parsedDuration(raw) ?: throw ConversionException(DURATION_REASON)
    val problem = durationProblem(duration, shortest, longest)
    if (problem != null) throw ConversionException(problem)
    return duration
}

private fun parsedDuration(raw: String): Duration? {
    val isReadable = raw.length <= Limits.MAX_NUMERIC_TEXT_LENGTH && raw.all(::isPrintableAscii)
    if (!isReadable) return null
    return Duration.parseOrNull(raw)
}

private fun durationProblem(duration: Duration, shortest: Duration, longest: Duration): String? {
    if (duration.isInfinite()) return INFINITE_DURATION_REASON
    return finiteDurationProblem(duration, shortest, longest)
}

private fun finiteDurationProblem(duration: Duration, shortest: Duration, longest: Duration): String? {
    if (duration.isNegative()) return NEGATIVE_DURATION_REASON
    return boundsProblem(duration, shortest, longest)
}

private fun boundsProblem(duration: Duration, shortest: Duration, longest: Duration): String? {
    if (duration < shortest) return "is shorter than $shortest"
    return "is longer than $longest".takeIf { duration > longest }
}

internal fun readSemanticVersion(raw: String): SemanticVersion {
    val version = SemanticVersion.parseOrNull(raw)
    return version ?: throw ConversionException(VERSION_REASON)
}

internal fun readSemanticVersionRange(raw: String): SemanticVersionRange {
    val range = SemanticVersionRange.parseOrNull(raw)
    return range ?: throw ConversionException(RANGE_REASON)
}
