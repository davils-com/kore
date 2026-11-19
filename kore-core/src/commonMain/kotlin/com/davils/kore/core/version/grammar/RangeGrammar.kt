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
import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.version.interval.VersionInterval
import com.davils.kore.core.version.interval.normalized

internal const val MATCHES_NOTHING_REASON: String = "has an alternative that matches no version"
internal const val INTERVAL_OPENINGS: String = "[("

private const val RANGE_TOO_LONG_REASON: String = "is longer than ${Limits.MAX_RANGE_LENGTH} characters"
private const val CANONICAL_TOO_LARGE_REASON: String =
    "renders to more than ${Limits.MAX_RANGE_COMPARATORS} intervals or ${Limits.MAX_RANGE_LENGTH} characters"
private const val EMPTY_ALTERNATIVE_REASON: String = "has an empty alternative beside ||"
private const val ALTERNATIVE_SEPARATOR: String = "||"
private const val NOTHING: String = "[]"

internal fun readRange(text: String): RangeReading {
    val problem = rangeTextProblem(text)
    if (problem != null) return Outcome.Failure(problem)
    return readTrimmedRange(trimmedRangeSpace(text))
}

internal fun combined(
    readings: List<RangeReading>,
    initial: List<VersionInterval>,
    merge: (List<VersionInterval>, List<VersionInterval>) -> List<VersionInterval>,
): RangeReading {
    val start: RangeReading = Outcome.Success(initial)
    return readings.fold(start) { sofar, next -> joined(sofar, next, merge) }
}

internal fun readingOf(interval: VersionInterval?, emptyReason: String): RangeReading {
    if (interval == null) return Outcome.Failure(emptyReason)
    return Outcome.Success(listOf(interval))
}

private fun joined(
    sofar: RangeReading,
    next: RangeReading,
    merge: (List<VersionInterval>, List<VersionInterval>) -> List<VersionInterval>,
): RangeReading {
    if (sofar !is Outcome.Success) return sofar
    return when (next) {
        is Outcome.Success -> Outcome.Success(merge(sofar.value, next.value))
        is Outcome.Failure -> next
    }
}

private fun rangeTextProblem(text: String): String? {
    if (text.length > Limits.MAX_RANGE_LENGTH) return RANGE_TOO_LONG_REASON
    return EMPTY_REASON.takeIf { text.all(::isRangeSpace) }
}

private fun readTrimmedRange(text: String): RangeReading {
    if (text == NOTHING) return Outcome.Success(emptyList())
    val collector = RangeCollector()
    for (alternative in text.split(ALTERNATIVE_SEPARATOR)) {
        if (collector.isRefused) break
        readAlternative(alternative, collector)
    }
    return renderable(collector.reading())
}

private fun readAlternative(alternative: String, collector: RangeCollector) {
    val text = trimmedRangeSpace(alternative)
    if (text.isEmpty()) return collector.refuse(EMPTY_ALTERNATIVE_REASON)
    if (text.first() in INTERVAL_OPENINGS) return readIntervals(text, collector)
    readComparatorSet(text, collector)
}

private fun renderable(reading: RangeReading): RangeReading = when (reading) {
    is Outcome.Success -> renderableIntervals(normalized(reading.value))
    is Outcome.Failure -> reading
}

private fun renderableIntervals(intervals: List<VersionInterval>): RangeReading {
    val rendered = intervals.sumOf { interval -> interval.toString().length } + intervals.size - 1
    val isFitting = intervals.size <= Limits.MAX_RANGE_COMPARATORS && rendered <= Limits.MAX_RANGE_LENGTH
    if (!isFitting) return Outcome.Failure(CANONICAL_TOO_LARGE_REASON)
    return Outcome.Success(intervals)
}
