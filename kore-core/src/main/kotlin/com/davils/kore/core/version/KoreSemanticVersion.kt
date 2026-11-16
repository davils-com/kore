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
import com.davils.kore.core.version.grammar.BUILD_SEPARATOR
import com.davils.kore.core.version.grammar.IDENTIFIER_SEPARATOR
import com.davils.kore.core.version.grammar.PRE_RELEASE_SEPARATOR
import com.davils.kore.core.version.grammar.VERSION_TOO_LONG_REASON
import com.davils.kore.core.version.grammar.VersionReading
import com.davils.kore.core.version.grammar.compareBuilds
import com.davils.kore.core.version.grammar.comparePreReleases
import com.davils.kore.core.version.grammar.fitsVersionLength
import com.davils.kore.core.version.grammar.partsProblem
import com.davils.kore.core.version.grammar.readVersion

private const val HASH_FACTOR: Int = 31
private const val SUCCESSOR_IDENTIFIER: String = "0"
private const val VERSION_SUBJECT: String = "A version"
private const val NEGATIVE_NUMBER_MESSAGE: String = "$VERSION_SUBJECT number must not be negative"
private const val TOO_LONG_MESSAGE: String = "$VERSION_SUBJECT $VERSION_TOO_LONG_REASON"
private const val SUCCESSOR_MESSAGE: String =
    "Only a pre-release of at most ${KoreLimits.MAX_VERSION_LENGTH} characters has a pre-release successor"

private val coreOrder: Comparator<KoreSemanticVersion> = compareBy({ it.major }, { it.minor }, { it.patch })

/**
 * A version as Semantic Versioning 2.0.0 defines it: three numbers, optional pre-release identifiers and optional
 * build metadata, such as `2.1.0-rc.1+sha.9f3c`.
 *
 * A version is read from text with [parse], [parseOrNull] or [toSemanticVersion], or stated in code through
 * [invoke], as in `KoreSemanticVersion(1, 4, 0)`. Both refuse what the specification refuses, and both refuse a
 * version whose text would be longer than [KoreLimits.MAX_VERSION_LENGTH] characters. Every version that a function
 * of Kore returns meets the same rules, so it reads back from its [toString].
 *
 * The grammar is stricter than npm: a leading `v` or `=`, surrounding whitespace, a number with a leading zero and
 * every character outside ASCII letters, digits, `-`, `.` and `+` are refused. Each of the three numbers is an
 * [Int], so a number above [Int.MAX_VALUE] is refused, although the specification sets no upper bound. Numeric
 * pre-release identifiers are compared as numbers of any length and are limited only by the length of the version.
 *
 * [compareTo] orders versions by precedence, which ignores build metadata, while [equals] compares the build
 * metadata as well: `1.0.0+a` and `1.0.0+b` have the same precedence but are not equal. A sorted set or map built on
 * the natural order therefore keeps only one of them; build it on [TOTAL_ORDER] to keep both.
 *
 * A version is immutable and safe to share between threads; [preRelease] and [build] cannot be changed, not even
 * through a cast. Its text consists only of ASCII letters, digits, `-`, `.` and `+`, so it is safe to show without
 * escaping; Kore checks the numbers and the identifiers wherever it creates a version, in its internal functions as
 * well.
 *
 * @property major the major number, raised for a change that breaks compatibility; never negative.
 * @property minor the minor number, raised for a compatible addition; never negative.
 * @property patch the patch number, raised for a compatible fix; never negative.
 * @property preRelease the pre-release identifiers, such as `rc` and `1` of `2.0.0-rc.1`; empty for a release.
 * @property build the build metadata identifiers, such as `sha` and `9f3c` of `1.0.0+sha.9f3c`; empty when the
 * version has none.
 * @since 1.0.0
 */
public class KoreSemanticVersion private constructor(
    public val major: Int,
    public val minor: Int,
    public val patch: Int,
    public val preRelease: List<String>,
    public val build: List<String>,
) : Comparable<KoreSemanticVersion> {
    /**
     * Whether this version has pre-release identifiers, such as `2.0.0-rc.1`.
     *
     * @since 1.0.0
     */
    public val isPreRelease: Boolean get() = preRelease.isNotEmpty()

    /**
     * Whether this version is a release of a stable public API: a release with a major number above zero.
     *
     * A version of the initial development, `0.y.z`, and every pre-release are not stable. Build metadata plays no
     * part.
     *
     * @since 1.0.0
     */
    public val isStable: Boolean get() = major > 0 && !isPreRelease

    /**
     * Returns the release this version leads to, without pre-release identifiers and without build metadata.
     *
     * `2.0.0-rc.1+sha.9f3c` gives `2.0.0`. A release without build metadata returns itself.
     *
     * @return the release of the same three numbers.
     * @since 1.0.0
     */
    public fun release(): KoreSemanticVersion {
        if (!isPreRelease && build.isEmpty()) return this
        return invoke(major = major, minor = minor, patch = patch)
    }

    /**
     * Returns this version without its build metadata, keeping its pre-release identifiers.
     *
     * A version without build metadata returns itself.
     *
     * @return the version of the same precedence without build metadata.
     * @since 1.0.0
     */
    public fun withoutBuild(): KoreSemanticVersion {
        if (build.isEmpty()) return this
        return KoreSemanticVersion(
            major = major,
            minor = minor,
            patch = patch,
            preRelease = preRelease,
            build = emptyList(),
        )
    }

    /**
     * Returns this version with [identifiers] as its pre-release identifiers, keeping its numbers and build metadata.
     *
     * Without identifiers the result is the release of this version with the same build metadata. The identifiers
     * are checked as [invoke] checks them; the message of a refusal never quotes them.
     *
     * @param identifiers the new pre-release identifiers, such as `"rc", "2"`.
     * @return the version with the new pre-release identifiers.
     * @throws IllegalArgumentException if an identifier is empty, holds a character other than an ASCII letter, digit
     * or `-`, or is a number with a leading zero, or if the version would be longer than
     * [KoreLimits.MAX_VERSION_LENGTH] characters.
     * @since 1.0.0
     */
    public fun withPreRelease(vararg identifiers: String): KoreSemanticVersion =
        invoke(major = major, minor = minor, patch = patch, preRelease = identifiers.asList(), build = build)

    /**
     * Returns this version with [identifiers] as its build metadata, keeping its numbers and pre-release identifiers.
     *
     * Without identifiers the result has no build metadata. The identifiers are checked as [invoke] checks them; the
     * message of a refusal never quotes them.
     *
     * @param identifiers the new build metadata identifiers, such as `"sha", "9f3c"`.
     * @return the version with the new build metadata.
     * @throws IllegalArgumentException if an identifier is empty or holds a character other than an ASCII letter,
     * digit or `-`, or if the version would be longer than [KoreLimits.MAX_VERSION_LENGTH] characters.
     * @since 1.0.0
     */
    public fun withBuild(vararg identifiers: String): KoreSemanticVersion =
        invoke(major = major, minor = minor, patch = patch, preRelease = preRelease, build = identifiers.asList())

    /**
     * Whether this version can stand in for [baseline], as the caret range `^baseline` of npm decides it.
     *
     * From `1.0.0` on, a version of the same major number that is not below [baseline] is compatible. Below `1.0.0`
     * the minor number is the line of compatibility, and below `0.1.0` only versions of the same patch number are
     * compatible. A pre-release is compatible only when [baseline] is a pre-release of the same three numbers, as
     * [KorePreReleasePolicy.SameCoreOnly] decides it. Build metadata plays no part.
     *
     * @param baseline the version that is required.
     * @return `true` if this version satisfies `^baseline`.
     * @since 1.0.0
     */
    public fun isCompatibleWith(baseline: KoreSemanticVersion): Boolean {
        val compatible = KoreSemanticVersionRange.caret(baseline)
        return compatible.contains(this, KorePreReleasePolicy.SameCoreOnly)
    }

    /**
     * Whether this version has the same precedence as [other], which is when they differ at most in build metadata.
     *
     * Unlike [equals], `1.0.0+a` and `1.0.0+b` have the same precedence; a pre-release never has the precedence of
     * its release.
     *
     * @param other the version to compare with.
     * @return `true` if [compareTo] returns zero for [other].
     * @since 1.0.0
     */
    public fun isSamePrecedence(other: KoreSemanticVersion): Boolean = compareTo(other) == 0

    /**
     * Compares this version with [other] by the precedence of Semantic Versioning 2.0.0.
     *
     * The three numbers are compared numerically. A pre-release precedes the release of the same numbers. Pre-release
     * identifiers are compared one by one: numeric identifiers as numbers of any length, other identifiers by their
     * ASCII order, a numeric identifier before a non-numeric one, and a shorter list before a longer one that starts
     * with it. Build metadata is ignored, so this order is not consistent with [equals]; [TOTAL_ORDER] is.
     *
     * @param other the version to compare with.
     * @return a negative number, zero or a positive number as this version precedes, shares or follows the
     * precedence of [other].
     * @since 1.0.0
     */
    override fun compareTo(other: KoreSemanticVersion): Int {
        val byCore = coreOrder.compare(this, other)
        if (byCore != 0) return byCore
        return comparePreReleases(left = preRelease, right = other.preRelease)
    }

    /**
     * Whether [other] is a version with the same numbers, the same pre-release identifiers and the same build
     * metadata.
     *
     * Equality compares build metadata, which precedence ignores; use [isSamePrecedence] to ignore it.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is an equal version.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is KoreSemanticVersion) return false
        val isSameCore = major == other.major && minor == other.minor && patch == other.patch
        return isSameCore && preRelease == other.preRelease && build == other.build
    }

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code of the numbers, the pre-release identifiers and the build metadata.
     * @since 1.0.0
     */
    override fun hashCode(): Int {
        val core = (major * HASH_FACTOR + minor) * HASH_FACTOR + patch
        val qualified = core * HASH_FACTOR + preRelease.hashCode()
        return qualified * HASH_FACTOR + build.hashCode()
    }

    /**
     * Returns the canonical text of this version, such as `2.1.0-rc.1+sha.9f3c`, which [parse] reads back to an equal
     * version.
     *
     * @return the text of this version.
     * @since 1.0.0
     */
    override fun toString(): String {
        val core = "$major.$minor.$patch"
        val released = qualified(core, PRE_RELEASE_SEPARATOR, preRelease)
        return qualified(released, BUILD_SEPARATOR, build)
    }

    /**
     * Reads versions from text, states them in code and holds the two orders of versions.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Orders versions by precedence, as [compareTo] does, so versions that differ only in build metadata compare
         * as equal.
         *
         * @since 1.0.0
         */
        public val PRECEDENCE: Comparator<KoreSemanticVersion> = Comparator { left, right -> left.compareTo(right) }

        /**
         * Orders versions by precedence and then by build metadata, so that it is consistent with [equals].
         *
         * A version without build metadata comes first; build identifiers are then compared as pre-release
         * identifiers are. Use this order for a sorted set or map of versions, and wherever versions of the same
         * precedence must be told apart in a stable way.
         *
         * @since 1.0.0
         */
        public val TOTAL_ORDER: Comparator<KoreSemanticVersion> =
            PRECEDENCE.thenComparator { left, right -> compareBuilds(left = left.build, right = right.build) }

        /**
         * States a version in code, such as `KoreSemanticVersion(2, 0, 0, preRelease = listOf("rc", "1"))`.
         *
         * The checks run in this order: the number of identifiers in each list against
         * [KoreLimits.MAX_VERSION_LENGTH], so that a huge list is refused before it is copied; then both lists are
         * copied, so that a list another caller changes afterwards cannot change the version; then the length of the
         * whole text against the same limit; and only then every identifier. The message of a refusal never quotes an
         * identifier.
         *
         * @param major the major number.
         * @param minor the minor number.
         * @param patch the patch number.
         * @param preRelease the pre-release identifiers; each is non-empty, consists of ASCII letters, digits and
         * `-`, and has no leading zero when it is numeric.
         * @param build the build metadata identifiers; each is non-empty and consists of ASCII letters, digits and
         * `-`.
         * @return the version.
         * @throws IllegalArgumentException if a number is negative, if an identifier is malformed, or if the text of
         * the version would be longer than [KoreLimits.MAX_VERSION_LENGTH] characters.
         * @since 1.0.0
         */
        public operator fun invoke(
            major: Int,
            minor: Int = 0,
            patch: Int = 0,
            preRelease: List<String> = emptyList(),
            build: List<String> = emptyList(),
        ): KoreSemanticVersion {
            require(major >= 0 && minor >= 0 && patch >= 0) { NEGATIVE_NUMBER_MESSAGE }
            val isCountFitting = preRelease.size <= KoreLimits.MAX_VERSION_LENGTH
            require(isCountFitting && build.size <= KoreLimits.MAX_VERSION_LENGTH) { TOO_LONG_MESSAGE }
            val identifiers = preRelease.immutableList()
            val metadata = build.immutableList()
            val problem = partsProblem(
                major = major,
                minor = minor,
                patch = patch,
                preRelease = identifiers,
                build = metadata,
            )
            problem?.let { reason -> throw IllegalArgumentException("$VERSION_SUBJECT $reason") }
            return KoreSemanticVersion(
                major = major,
                minor = minor,
                patch = patch,
                preRelease = identifiers,
                build = metadata,
            )
        }

        /**
         * Reads [text] as a semantic version, such as `2.1.0-rc.1+sha.9f3c`.
         *
         * The text must be exactly a version: three numbers without leading zeros, optional pre-release identifiers
         * after `-` and optional build metadata after `+`. A leading `v` or `=` and surrounding whitespace are
         * refused, as is a number above [Int.MAX_VALUE]. Text longer than [KoreLimits.MAX_VERSION_LENGTH] characters
         * is refused before it is split.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the version.
         * @throws IllegalArgumentException if [text] is not a semantic version. The message names the problem in
         * fixed words and never quotes [text], so it is safe to log and to show.
         * @since 1.0.0
         */
        public fun parse(text: String): KoreSemanticVersion = when (val reading = readVersion(text)) {
            is VersionReading.Read -> reading.version
            is VersionReading.Refused -> throw IllegalArgumentException(versionRefusal(reading.reason))
        }

        /**
         * Reads [text] as a semantic version, as [parse] does, and returns `null` where [parse] throws.
         *
         * @param text the text to read; it may come from an attacker.
         * @return the version, or `null` if [text] is not a semantic version.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): KoreSemanticVersion? = when (val reading = readVersion(text)) {
            is VersionReading.Read -> reading.version
            is VersionReading.Refused -> null
        }

        @JvmSynthetic
        internal fun preReleaseSuccessorOf(version: KoreSemanticVersion): KoreSemanticVersion {
            val identifiers = version.preRelease
            val isFitting = fitsVersionLength(
                major = version.major,
                minor = version.minor,
                patch = version.patch,
                preRelease = identifiers,
                build = emptyList(),
            )
            require(version.isPreRelease && isFitting) { SUCCESSOR_MESSAGE }
            val following = identifiers + SUCCESSOR_IDENTIFIER
            return KoreSemanticVersion(
                major = version.major,
                minor = version.minor,
                patch = version.patch,
                preRelease = following.immutableList(),
                build = emptyList(),
            )
        }
    }
}

private fun versionRefusal(reason: String): String = "Not a semantic version: it $reason"

private fun qualified(text: String, separator: Char, identifiers: List<String>): String {
    if (identifiers.isEmpty()) return text
    return text + separator + identifiers.joinToString(separator = IDENTIFIER_SEPARATOR.toString())
}
