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

import com.davils.kore.core.version.KoreSemanticVersion
import com.davils.kore.core.version.KoreSemanticVersionRange

private const val INCLUSIVE_LOWER: Char = '['
private const val EXCLUSIVE_LOWER: Char = '('
private const val INCLUSIVE_UPPER: Char = ']'
private const val EXCLUSIVE_UPPER: Char = ')'
private const val BOUND_SEPARATOR: Char = ','
private const val INTERVAL_HASH_FACTOR: Int = 31
private const val MALFORMED_MESSAGE: String =
    "A version interval must hold a version and have bounds without build metadata, and only a release can be its " +
        "excluded lower bound"

/**
 * One contiguous interval of semantic versions, ordered by precedence, as a part of a [KoreSemanticVersionRange].
 *
 * Intervals have no public constructor; they are read from [KoreSemanticVersionRange.intervals]. An interval has an
 * optional [lower] and an optional [upper] bound, each inside or outside it. Its bounds carry no build metadata, and
 * [contains] ignores the build metadata of the version it tests. Its internal constructor, which Java code can
 * reach, checks every interval, so that each one holds at least one version and has bounds without build metadata.
 *
 * Besides the versions it holds, an interval keeps which pre-releases its bounds name, which the pre-release policy
 * of a range relies on. A lower bound therefore names the version written: `>1.2.3` keeps the lower bound `1.2.3`,
 * outside, and does not become `[1.2.4-0,)`, which holds the same versions but names the pre-releases of `1.2.4`.
 * An upper bound that is the lowest pre-release above a release is stated as that release, inside, so `<=1.2.3` and
 * `<1.2.4-0` both have the upper bound `1.2.3`, inside, while `<2.0.0-0` keeps its bound `2.0.0-0`, outside.
 *
 * An interval is immutable and safe to share between threads. Its text consists only of versions, brackets and a
 * comma, so it is safe to show without escaping.
 *
 * @since 1.0.0
 */
public class KoreVersionInterval internal constructor(
    @get:JvmSynthetic internal val start: KoreSemanticVersion?,
    internal val isStartExclusive: Boolean,
    @get:JvmSynthetic internal val end: KoreSemanticVersion?,
) {
    @get:JvmSynthetic
    internal val first: KoreSemanticVersion? = firstOf(start, isStartExclusive)

    init {
        require(isWellFormed(start = start, isStartExclusive = isStartExclusive, end = end, first = first)) {
            MALFORMED_MESSAGE
        }
    }

    /**
     * The lower bound, or `null` when the interval has none and reaches down to the lowest version there is.
     *
     * @since 1.0.0
     */
    public val lower: KoreSemanticVersion? = start?.let { bound -> preReleasePredecessor(bound) ?: bound }

    /**
     * Whether [lower] lies in the interval; `false` when there is no lower bound.
     *
     * @since 1.0.0
     */
    public val isLowerInclusive: Boolean = start != null && !isStartExclusive && preReleasePredecessor(start) == null

    /**
     * The upper bound, or `null` when the interval has none and holds every version above its lower bound.
     *
     * @since 1.0.0
     */
    public val upper: KoreSemanticVersion? = end?.let { bound -> predecessor(bound) ?: bound }

    /**
     * Whether [upper] lies in the interval; `false` when there is no upper bound.
     *
     * @since 1.0.0
     */
    public val isUpperInclusive: Boolean = end != null && predecessor(end) != null

    /**
     * Whether [version] lies in this interval by precedence.
     *
     * No pre-release policy applies here: a pre-release between the bounds lies in the interval like any release.
     * Build metadata of [version] is ignored. To test a version against a whole range under a policy, use
     * [KoreSemanticVersionRange.contains].
     *
     * @param version the version to test.
     * @return `true` if [version] lies between the bounds of this interval.
     * @since 1.0.0
     */
    public operator fun contains(version: KoreSemanticVersion): Boolean {
        val isAboveStart = first == null || version >= first
        val isBelowEnd = end == null || version < end
        return isAboveStart && isBelowEnd
    }

    internal fun namesPreReleaseOf(version: KoreSemanticVersion): Boolean =
        isPreReleaseOfSameCore(lower, version) || isPreReleaseOfSameCore(upper, version)

    /**
     * Whether [other] is an interval with the same bounds, each inside or outside it in the same way.
     *
     * Two intervals are equal when they hold the same versions by precedence and their bounds name the same
     * pre-releases: `(1.2.3,)` and `[1.2.4-0,)` hold the same versions but are not equal, because only the second
     * names the pre-releases of `1.2.4`.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is an equal interval.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is KoreVersionInterval) return false
        return start == other.start && isStartExclusive == other.isStartExclusive && end == other.end
    }

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code of the bounds.
     * @since 1.0.0
     */
    override fun hashCode(): Int {
        val lowerHash = start.hashCode() * INTERVAL_HASH_FACTOR + isStartExclusive.hashCode()
        return lowerHash * INTERVAL_HASH_FACTOR + end.hashCode()
    }

    /**
     * Returns this interval in interval notation, such as `[1.2.3,2.0.0-0)`, `(,1.2.3]` or `[1.2.3]` for an interval
     * of exactly one version.
     *
     * `[` and `]` mark a bound inside the interval, `(` and `)` a bound outside it, and a missing bound is left
     * empty.
     *
     * @return the text of this interval.
     * @since 1.0.0
     */
    override fun toString(): String {
        val isClosed = isLowerInclusive && isUpperInclusive
        val exact = lower?.takeIf { bound -> isClosed && bound == upper }
        if (exact != null) return "$INCLUSIVE_LOWER$exact$INCLUSIVE_UPPER"
        val opening = INCLUSIVE_LOWER.takeIf { isLowerInclusive } ?: EXCLUSIVE_LOWER
        val closing = INCLUSIVE_UPPER.takeIf { isUpperInclusive } ?: EXCLUSIVE_UPPER
        val from = lower?.toString().orEmpty()
        val until = upper?.toString().orEmpty()
        return "$opening$from$BOUND_SEPARATOR$until$closing"
    }
}

internal fun intervalHoldingAny(
    start: KoreSemanticVersion?,
    isStartExclusive: Boolean,
    end: KoreSemanticVersion?,
): KoreVersionInterval? {
    val first = firstOf(start, isStartExclusive)
    val isFitting = isWellFormed(start = start, isStartExclusive = isStartExclusive, end = end, first = first)
    if (!isFitting) return null
    return KoreVersionInterval(start = start, isStartExclusive = isStartExclusive, end = end)
}

private fun isWellFormed(
    start: KoreSemanticVersion?,
    isStartExclusive: Boolean,
    end: KoreSemanticVersion?,
    first: KoreSemanticVersion?,
): Boolean {
    val isExclusionFitting = !isStartExclusive || start?.isPreRelease == false
    val isBuildFree = hasNoBuild(start) && hasNoBuild(end)
    return isExclusionFitting && isBuildFree && holdsAny(start = start, first = first, end = end)
}

private fun holdsAny(start: KoreSemanticVersion?, first: KoreSemanticVersion?, end: KoreSemanticVersion?): Boolean {
    val lowest = first ?: MINIMUM.takeIf { start == null } ?: return false
    return end == null || lowest < end
}

private fun hasNoBuild(bound: KoreSemanticVersion?): Boolean = bound == null || bound.build.isEmpty()

private fun firstOf(start: KoreSemanticVersion?, isStartExclusive: Boolean): KoreSemanticVersion? {
    if (start == null || !isStartExclusive) return start
    return successor(start)
}

private fun isPreReleaseOfSameCore(bound: KoreSemanticVersion?, version: KoreSemanticVersion): Boolean {
    if (bound == null || !bound.isPreRelease) return false
    val isSameRelease = bound.major == version.major && bound.minor == version.minor
    return isSameRelease && bound.patch == version.patch
}
