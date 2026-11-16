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

package com.davils.kore.core.version

import com.davils.kore.core.immutableList
import com.davils.kore.core.limit.KoreLimits
import com.davils.kore.core.version.grammar.RangeReading
import com.davils.kore.core.version.grammar.caretInterval
import com.davils.kore.core.version.grammar.partialOf
import com.davils.kore.core.version.grammar.readRange
import com.davils.kore.core.version.grammar.tildeInterval
import com.davils.kore.core.version.interval.EVERYTHING
import com.davils.kore.core.version.interval.KoreVersionInterval
import com.davils.kore.core.version.interval.boundedInterval
import com.davils.kore.core.version.interval.intersection
import com.davils.kore.core.version.interval.normalized

private const val RANGE_INTERVAL_SEPARATOR: String = ","
private const val NOTHING: String = "[]"
private const val EMPTY_RANGE_MESSAGE: String =
    "A version range must not be empty: its lower bound is not below its upper"

/**
 * A set of semantic versions, such as every version from `1.2.3` up to but excluding `2.0.0`, held as a union of
 * [KoreVersionInterval]s ordered by precedence.
 *
 * A range is read from text with [parse], [parseOrNull] or [toSemanticVersionRange], in the range notation of npm,
 * such as `^1.2 || >=2.1.0 <3`, or in the interval notation of Maven, such as `[1.2,2.0),[3.0,)`. It is stated in
 * code with [between], [exactly], [atLeast], [below], [caret], [tilde] and [bounded], and combined with [union] and
 * [intersect]. Build metadata plays no part in a range: it is dropped from every bound and ignored in every version
 * that is tested.
 *
 * Whether a range holds a pre-release is decided by a [KorePreReleasePolicy]. Every function that tests versions
 * takes one and defaults to [KorePreReleasePolicy.SameCoreOnly], under which a pre-release is admitted only when a
 * bound of an interval that holds it is a pre-release of the same three numbers, as npm does for each comparator set.
 *
 * A range keeps a canonical form: its [intervals] are merged and sorted, and its [toString] writes them in interval
 * notation, such as `[1.2.3,2.0.0-0)`. The canonical form depends only on the versions the range holds by precedence
 * and on the pre-releases it admits under [KorePreReleasePolicy.SameCoreOnly]. Where a part of the range names a
 * pre-release that the interval around it does not, that pre-release stays as an interval of its own nested inside
 * the wider one: `1.0.0-rc.1 || 0.9.0 - 1.0.0` is written `[0.9.0,1.0.0],[1.0.0-rc.1]`. No version lies below
 * `0.0.0-0`, so an interval that reaches down to the lowest version starts at `0.0.0-0` in the canonical form exactly
 * when the range admits every pre-release of `0.0.0` that the interval holds, and has no lower bound otherwise:
 * `<=0.0.0-alpha` and `<1.0.0 || >=0.0.0-0 <0.0.0` are written `[0.0.0-0,0.0.0-alpha]` and `[0.0.0-0,1.0.0)`,
 * while `<1.0.0` stays `(,1.0.0)`.
 *
 * Equality is representational: two ranges are equal when their canonical forms are equal, so `^1.2.3` equals
 * `>=1.2.3 <2.0.0-0`. Ranges that hold the same versions by precedence but name different pre-releases are not
 * equal: `>1.2.3` and `>=1.2.4-0` hold the same versions, yet only the second admits pre-releases of `1.2.4` under
 * [KorePreReleasePolicy.SameCoreOnly].
 *
 * Every range read from text reads back from its [toString] to an equal range. A range built in code through
 * [union] or [intersect] has no limit on the number of its intervals, so its text may exceed
 * [KoreLimits.MAX_RANGE_COMPARATORS] intervals or [KoreLimits.MAX_RANGE_LENGTH] characters, and [parse] then refuses
 * it. The time [union] and [intersect] take grows with the number of intervals of both ranges, a little faster than
 * that number. A range that collects the parts of many others holds all their intervals, so uniting many ranges one
 * after another takes time that grows with the square of their number; a caller that combines ranges taken from
 * untrusted input bounds how many it combines.
 *
 * A range is immutable and safe to share between threads; [intervals] cannot be changed, not even through a cast.
 * Its text consists only of versions, brackets and commas, so it is safe to show without escaping.
 *
 * @since 1.0.0
 */
public class KoreSemanticVersionRange private constructor(given: List<KoreVersionInterval>) {
    /**
     * The intervals of the canonical form, ordered by their lowest version.
     *
     * The intervals neither overlap nor touch, except that an interval which names a pre-release the interval around
     * it does not name lies nested inside that interval. The list is empty for [NONE] and cannot be changed, not even
     * through a cast.
     *
     * @since 1.0.0
     */
    public val intervals: List<KoreVersionInterval> = normalized(given)

    /**
     * Whether this range holds no version at all, not even by precedence, as [NONE].
     *
     * A range that is not empty may still admit no version under a stricter policy: `[1.0.0-rc.1]` admits nothing
     * under [KorePreReleasePolicy.ReleasesOnly].
     *
     * @since 1.0.0
     */
    public val isEmpty: Boolean get() = intervals.isEmpty()

    /**
     * Whether this range equals [ANY]: one interval without a lower and without an upper bound.
     *
     * `>=0.0.0-0` holds the same versions by precedence but is not [ANY], because it names the pre-releases of
     * `0.0.0`.
     *
     * @since 1.0.0
     */
    public val isAny: Boolean get() = intervals.singleOrNull() == EVERYTHING

    /**
     * Whether [version] lies in this range and [policy] admits it.
     *
     * A release lies in the range when it lies in one of the [intervals] by precedence. A pre-release must also be
     * admitted by [policy]: under the default [KorePreReleasePolicy.SameCoreOnly], one interval that holds it must
     * have a bound that is a pre-release of the same three numbers. Build metadata of [version] is ignored.
     *
     * A range has no `in` operator, so the policy in force is always the default or named at the call. Use
     * [KorePreReleasePolicy.ByPrecedence] to ask whether [version] lies in the range by precedence alone.
     *
     * @param version the version to test.
     * @param policy decides whether a pre-release that lies in the range is admitted.
     * @return `true` if [version] lies in this range and [policy] admits it.
     * @since 1.0.0
     */
    public fun contains(
        version: KoreSemanticVersion,
        policy: KorePreReleasePolicy = KorePreReleasePolicy.SameCoreOnly,
    ): Boolean = intervals.any { interval -> version in interval && admits(interval, version, policy) }

    /**
     * Returns the range of the versions that lie in both this range and [other], as if each comparator set of this
     * range were joined with each comparator set of [other] into one comparator set.
     *
     * The result holds a version by precedence exactly when both ranges hold it. A pre-release that both hold is
     * admitted under [KorePreReleasePolicy.SameCoreOnly] exactly when either range alone admits it, so the result
     * can admit a pre-release that one of the two ranges alone does not: `>=1.0.0-rc.1 <2.0.0` intersected with
     * `>=0.9.0 <1.5.0` admits `1.0.0-rc.2`, which the second range alone does not. A pre-release that neither range
     * admits on its own stays refused, even where one of them names its three numbers in another of its parts:
     * `>=1.0.0-rc.1 <1.0.0-rc.2 || >=0.9.0 <2.0.0` intersected with `>=0.9.0 <2.0.0` refuses `1.0.0-rc.5`.
     *
     * The result is not checked against [KoreLimits]; see the class documentation for what that means for its text
     * and for the time that combining many ranges takes.
     *
     * @param other the range to intersect with.
     * @return the intersection, which is [NONE] when no version lies in both.
     * @since 1.0.0
     */
    public infix fun intersect(other: KoreSemanticVersionRange): KoreSemanticVersionRange =
        KoreSemanticVersionRange(intersection(left = intervals, right = other.intervals))

    /**
     * Returns the range of the versions that lie in this range, in [other] or in both.
     *
     * Under every [KorePreReleasePolicy], a version lies in the result exactly when it lies in one of the two ranges.
     * The result is not checked against [KoreLimits]; see the class documentation for what that means for its text
     * and for the time that combining many ranges takes.
     *
     * @param other the range to unite with.
     * @return the union of both ranges.
     * @since 1.0.0
     */
    public infix fun union(other: KoreSemanticVersionRange): KoreSemanticVersionRange =
        KoreSemanticVersionRange(intervals + other.intervals)

    /**
     * Returns the highest of [versions] that lies in this range, as [contains] decides it under [policy].
     *
     * Versions are ordered by [KoreSemanticVersion.TOTAL_ORDER], so among versions of the same precedence the one
     * with the last build metadata in that order is chosen, and the choice never depends on the order of [versions].
     * [versions] is read once and completely; no limit of [KoreLimits] applies to it, so a caller that passes an
     * untrusted collection bounds its size.
     *
     * @param versions the candidates.
     * @param policy decides whether a pre-release that lies in the range is admitted.
     * @return the highest candidate in this range, or `null` if there is none.
     * @since 1.0.0
     */
    public fun maxSatisfying(
        versions: Iterable<KoreSemanticVersion>,
        policy: KorePreReleasePolicy = KorePreReleasePolicy.SameCoreOnly,
    ): KoreSemanticVersion? = satisfying(versions, policy).maxWithOrNull(KoreSemanticVersion.TOTAL_ORDER)

    /**
     * Returns the lowest of [versions] that lies in this range, as [contains] decides it under [policy].
     *
     * Versions are ordered by [KoreSemanticVersion.TOTAL_ORDER], so among versions of the same precedence the one
     * without build metadata, or with the first in that order, is chosen, and the choice never depends on the order
     * of [versions]. [versions] is read once and completely; no limit of [KoreLimits] applies to it, so a caller that
     * passes an untrusted collection bounds its size.
     *
     * @param versions the candidates.
     * @param policy decides whether a pre-release that lies in the range is admitted.
     * @return the lowest candidate in this range, or `null` if there is none.
     * @since 1.0.0
     */
    public fun minSatisfying(
        versions: Iterable<KoreSemanticVersion>,
        policy: KorePreReleasePolicy = KorePreReleasePolicy.SameCoreOnly,
    ): KoreSemanticVersion? = satisfying(versions, policy).minWithOrNull(KoreSemanticVersion.TOTAL_ORDER)

    /**
     * Returns those of [versions] that lie in this range, as [contains] decides it under [policy].
     *
     * The result keeps the order and the duplicates of [versions] and cannot be changed, not even through a cast.
     * [versions] is read once and completely; no limit of [KoreLimits] applies to it, so a caller that passes an
     * untrusted collection bounds its size.
     *
     * @param versions the candidates.
     * @param policy decides whether a pre-release that lies in the range is admitted.
     * @return the candidates in this range, in their order.
     * @since 1.0.0
     */
    public fun satisfying(
        versions: Iterable<KoreSemanticVersion>,
        policy: KorePreReleasePolicy = KorePreReleasePolicy.SameCoreOnly,
    ): List<KoreSemanticVersion> {
        val inside = versions.filter { version -> contains(version, policy) }
        return inside.immutableList()
    }

    /**
     * Whether [other] is a range with the same canonical form.
     *
     * Equality is representational, as the class documentation describes: ranges that hold the same versions by
     * precedence but name different pre-releases are not equal.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is a range with equal [intervals].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is KoreSemanticVersionRange && intervals == other.intervals

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code of the [intervals].
     * @since 1.0.0
     */
    override fun hashCode(): Int = intervals.hashCode()

    /**
     * Returns the canonical form in interval notation, such as `[1.2.3,2.0.0-0)` or `[0.9.0,1.0.0],[1.0.0-rc.1]`.
     *
     * [NONE] is written `[]` and [ANY] `(,)`. For every range read from text, [parse] reads this text back to an equal
     * range.
     *
     * @return the canonical text of this range.
     * @since 1.0.0
     */
    override fun toString(): String {
        if (intervals.isEmpty()) return NOTHING
        return intervals.joinToString(RANGE_INTERVAL_SEPARATOR)
    }

    /**
     * Reads ranges from text, states them in code and holds the range of every version and the range of none.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The range of every version, written `(,)`.
         *
         * It holds every version by precedence. Under the default [KorePreReleasePolicy.SameCoreOnly] it admits
         * every release but no pre-release, because it has no bound that names one; pass
         * [KorePreReleasePolicy.ByPrecedence] to admit every version.
         *
         * @since 1.0.0
         */
        public val ANY: KoreSemanticVersionRange = KoreSemanticVersionRange(listOf(EVERYTHING))

        /**
         * The range of no version, written `[]`, which [parse] reads back to this range.
         *
         * @since 1.0.0
         */
        public val NONE: KoreSemanticVersionRange = KoreSemanticVersionRange(emptyList())

        /**
         * Returns the range from [from] up to but excluding [until], such as `[1.0.0,2.0.0)`.
         *
         * A pre-release given as a bound is named by the range, so its pre-releases of the same three numbers are
         * admitted under [KorePreReleasePolicy.SameCoreOnly]. [toString] writes the canonical form, which states a
         * bound by an equal one where the class documentation says so: `between(1.0.0, 1.2.4-0)` is written
         * `[1.0.0,1.2.3]`, and `between(1.0.0-rc.0, 2.0.0)` is written `(1.0.0-rc,2.0.0)`.
         *
         * @param from the lowest version of the range; its build metadata is ignored.
         * @param until the first version above the range, or `null` for a range without an upper bound; its build
         * metadata is ignored.
         * @return the range.
         * @throws IllegalArgumentException if [until] does not have a higher precedence than [from].
         * @since 1.0.0
         */
        public fun between(from: KoreSemanticVersion, until: KoreSemanticVersion?): KoreSemanticVersionRange =
            bounded(lower = from, lowerInclusive = true, upper = until, upperInclusive = false)

        /**
         * Returns the range of exactly [version], such as `[1.4.0]`.
         *
         * Build metadata is ignored, so every build of [version] lies in the range. [toString] writes the canonical
         * form, which states a bound by an equal one where the class documentation says so: `exactly(1.0.0-rc.0)`
         * is written `(1.0.0-rc,1.0.0-rc.0]`, `exactly(1.2.2147483647)` is written `[1.2.2147483647,1.3.0-0)` and
         * `exactly(2147483647.2147483647.2147483647)` is written `[2147483647.2147483647.2147483647,)`, because no
         * version lies above it.
         *
         * @param version the only version of the range.
         * @return the range.
         * @since 1.0.0
         */
        public fun exactly(version: KoreSemanticVersion): KoreSemanticVersionRange =
            bounded(lower = version, lowerInclusive = true, upper = version, upperInclusive = true)

        /**
         * Returns the range of [version] and every version above it, such as `[1.2.0,)`.
         *
         * [toString] writes the canonical form, which states a bound by an equal one where the class documentation
         * says so: `atLeast(1.0.0-rc.0)` is written `(1.0.0-rc,)`.
         *
         * @param version the lowest version of the range; its build metadata is ignored.
         * @return the range.
         * @since 1.0.0
         */
        public fun atLeast(version: KoreSemanticVersion): KoreSemanticVersionRange =
            bounded(lower = version, lowerInclusive = true, upper = null, upperInclusive = false)

        /**
         * Returns the range of every version below [version], such as `(,2.0.0)`.
         *
         * [toString] writes the canonical form, which states a bound by an equal one where the class documentation
         * says so: `below(1.2.4-0)` is written `(,1.2.3]`, and `below(0.0.0-alpha)` is written `[0.0.0-0,0.0.0-alpha)`,
         * because it admits every pre-release of `0.0.0` it holds.
         *
         * @param version the first version above the range; its build metadata is ignored.
         * @return the range.
         * @throws IllegalArgumentException if [version] is `0.0.0-0`, the lowest version there is, below which no
         * version lies.
         * @since 1.0.0
         */
        public fun below(version: KoreSemanticVersion): KoreSemanticVersionRange =
            bounded(lower = null, lowerInclusive = false, upper = version, upperInclusive = false)

        /**
         * Returns the range of the versions compatible with [version], as the caret range `^version` of npm.
         *
         * The upper bound raises the leftmost number that is not zero, or the patch number when all three are zero,
         * and excludes every pre-release of the result: `^1.2.3` is written `[1.2.3,2.0.0-0)` and `^0.2.3`
         * `[0.2.3,0.3.0-0)`, while `^0.0.3`, which holds `0.0.3` only, is written `[0.0.3]`. Where that number is
         * already [Int.MAX_VALUE], the number to its left is raised instead, and where there is none, the range has
         * no upper bound.
         *
         * @param version the lowest version of the range; its build metadata is ignored.
         * @return the range.
         * @since 1.0.0
         */
        public fun caret(version: KoreSemanticVersion): KoreSemanticVersionRange =
            KoreSemanticVersionRange(listOfNotNull(caretInterval(partialOf(version))))

        /**
         * Returns the range of the versions of the same minor line from [version] on, as the tilde range `~version`
         * of npm.
         *
         * `~1.2.3` is `[1.2.3,1.3.0-0)`. Where the minor number is already [Int.MAX_VALUE], the major number is
         * raised, and where both are, the range has no upper bound.
         *
         * @param version the lowest version of the range; its build metadata is ignored.
         * @return the range.
         * @since 1.0.0
         */
        public fun tilde(version: KoreSemanticVersion): KoreSemanticVersionRange =
            KoreSemanticVersionRange(listOfNotNull(tildeInterval(partialOf(version))))

        /**
         * Returns the range between [lower] and [upper], each inside or outside as stated.
         *
         * A pre-release given as a bound is named by the range, so its pre-releases of the same three numbers are
         * admitted under [KorePreReleasePolicy.SameCoreOnly].
         *
         * @param lower the lower bound, or `null` for a range without one; its build metadata is ignored.
         * @param lowerInclusive whether [lower] lies in the range; ignored when [lower] is `null`.
         * @param upper the upper bound, or `null` for a range without one; its build metadata is ignored.
         * @param upperInclusive whether [upper] lies in the range; ignored when [upper] is `null`.
         * @return the range.
         * @throws IllegalArgumentException if no version lies between the bounds, such as for `[2.0.0,1.0.0]` or
         * `(1.0.0,1.0.0]`.
         * @since 1.0.0
         */
        public fun bounded(
            lower: KoreSemanticVersion?,
            lowerInclusive: Boolean,
            upper: KoreSemanticVersion?,
            upperInclusive: Boolean,
        ): KoreSemanticVersionRange {
            val interval = boundedInterval(
                lower = lower,
                lowerInclusive = lowerInclusive,
                upper = upper,
                upperInclusive = upperInclusive,
            )
            return KoreSemanticVersionRange(listOf(requireNotNull(interval) { EMPTY_RANGE_MESSAGE }))
        }

        /**
         * Reads [text] as a range of semantic versions.
         *
         * Alternatives are separated by `||`; a version lies in the range when it lies in one of them. An
         * alternative is either a comparator set in the notation of npm or a list of intervals in the notation of
         * Maven, and `[]` on its own is [NONE]:
         *
         * - A comparator set is a list of comparators separated by spaces, all of which a version must meet, such as
         *   `>=1.2.0 <2`. A comparator is a version or a partial version such as `1.2` or `1.x`, where `x`, `X` and
         *   `*` stand for any number, after one of the operators `=`, `<`, `<=`, `>`, `>=`, `~` and `^`, or after
         *   none, which means `=`. A hyphen range such as `1.2 - 1.4` is a comparator set of its own.
         * - An interval list is a list of intervals separated by commas, such as `[1.2,2.0),[3.0,)`: `[` and `]`
         *   include a bound, `(` and `)` exclude it, an excluded bound may be left out to leave that side open, and
         *   `[1.2.3]` is exactly one version. A bound may be shortened to `1` or `1.2` when it has no pre-release and
         *   no build metadata.
         *
         * The grammar is stricter than npm. Only the space and the tab separate parts; every other whitespace, such
         * as a no-break space or a line break, is refused rather than trimmed away. An operator written apart from
         * its version joins only a version that does not start with an operator of its own, so `>= 1.2` is read but
         * `> = 1.2` is refused. Empty text, a leading `v`, the operator `~>` and a number above [Int.MAX_VALUE] are
         * refused, as is a comparator set or an interval that holds no version.
         *
         * Before anything else, text longer than [KoreLimits.MAX_RANGE_LENGTH] characters is refused. While it is
         * read, each comparator of a comparator set, each hyphen range and each interval counts once against
         * [KoreLimits.MAX_RANGE_COMPARATORS], across all alternatives, and every version against
         * [KoreLimits.MAX_VERSION_LENGTH]. A range whose canonical form would have more than
         * [KoreLimits.MAX_RANGE_COMPARATORS] intervals or more than [KoreLimits.MAX_RANGE_LENGTH] characters is
         * refused too, so that every range read from text reads back from its [toString].
         *
         * @param text the text to read; it may come from an attacker.
         * @return the range.
         * @throws IllegalArgumentException if [text] is not a range. The message names the first problem found in
         * fixed words and never quotes [text], so it is safe to log and to show.
         * @since 1.0.0
         */
        public fun parse(text: String): KoreSemanticVersionRange = read(
            text = text,
            read = { range -> range },
            refused = { reason -> throw IllegalArgumentException(rangeRefusal(reason)) },
        )

        /**
         * Reads [text] as a range of semantic versions, as [parse] does, and returns `null` where [parse] throws.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the range, or `null` if [text] is not a range.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): KoreSemanticVersionRange? =
            read(text = text, read = { range -> range }, refused = { null })

        @JvmSynthetic
        internal fun <T> read(text: String, read: (KoreSemanticVersionRange) -> T, refused: (String) -> T): T =
            when (val reading = readRange(text)) {
                is RangeReading.Read -> read(KoreSemanticVersionRange(reading.intervals))
                is RangeReading.Refused -> refused(reading.reason)
            }
    }
}

private fun admits(holder: KoreVersionInterval, version: KoreSemanticVersion, policy: KorePreReleasePolicy): Boolean {
    if (!version.isPreRelease) return true
    return when (policy) {
        KorePreReleasePolicy.ByPrecedence -> true
        KorePreReleasePolicy.SameCoreOnly -> holder.namesPreReleaseOf(version)
        KorePreReleasePolicy.ReleasesOnly -> false
    }
}

private fun rangeRefusal(reason: String): String = "Not a version range: it $reason"
