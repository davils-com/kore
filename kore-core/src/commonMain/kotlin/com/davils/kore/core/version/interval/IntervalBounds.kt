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

internal val EVERYTHING: VersionInterval = VersionInterval(start = null, isStartExclusive = false, end = null)

internal fun interval(
    start: SemanticVersion?,
    end: SemanticVersion?,
    isStartExclusive: Boolean = false,
): VersionInterval? {
    val until = end?.withoutBuild()
    if (start == null) return intervalHoldingAny(start = null, isStartExclusive = false, end = until)
    return startingAt(start.withoutBuild(), isStartExclusive, until)
}

internal fun boundedInterval(
    lower: SemanticVersion?,
    lowerInclusive: Boolean,
    upper: SemanticVersion?,
    upperInclusive: Boolean,
): VersionInterval? = interval(
    start = lower,
    end = exclusiveEnd(upper, upperInclusive),
    isStartExclusive = !lowerInclusive,
)

private fun startingAt(from: SemanticVersion, isStartExclusive: Boolean, end: SemanticVersion?): VersionInterval? =
    intervalHoldingAny(
        start = storedStart(from, isStartExclusive),
        isStartExclusive = isStartExclusive && !from.isPreRelease,
        end = end,
    )

private fun storedStart(from: SemanticVersion, isStartExclusive: Boolean): SemanticVersion {
    if (isStartExclusive && from.isPreRelease) return preReleaseSuccessor(from)
    return from
}

private fun exclusiveEnd(upper: SemanticVersion?, upperInclusive: Boolean): SemanticVersion? {
    if (upper == null || !upperInclusive) return upper
    return successor(upper)
}
