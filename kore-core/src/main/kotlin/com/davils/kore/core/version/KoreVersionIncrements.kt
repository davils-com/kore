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

import com.davils.kore.core.limit.KoreLimits
import com.davils.kore.core.version.grammar.fitsVersionLength
import com.davils.kore.core.version.grammar.isNumeric
import com.davils.kore.core.version.grammar.isPreReleaseIdentifier
import com.davils.kore.core.version.interval.incremented

private const val FIRST_PRE_RELEASE: String = "1"
private const val EXHAUSTED: String = "cannot be raised past ${Int.MAX_VALUE}"
private const val PATCH_EXHAUSTED_MESSAGE: String = "A patch version $EXHAUSTED"
private const val NO_LINE_MESSAGE: String =
    "A release has no pre-release line to continue: name one with nextPreRelease(label)"
private const val LINE_TOO_LONG_MESSAGE: String =
    "A pre-release line cannot be continued past ${KoreLimits.MAX_VERSION_LENGTH} characters"
private const val LABEL_MESSAGE: String = "A pre-release label must be a pre-release identifier"
private const val BACKWARDS_MESSAGE: String = "A pre-release label must not move a version backwards"

/**
 * Returns the next major release: `1.4.2` gives `2.0.0`.
 *
 * A pre-release of a major release, such as `2.0.0-rc.2`, gives that release, `2.0.0`, rather than skipping past it.
 * Pre-release identifiers and build metadata are dropped.
 *
 * @receiver the version to step from.
 * @return the next major release.
 * @throws IllegalStateException if the major number is already [Int.MAX_VALUE] and the version is not a pre-release
 * of a major release.
 * @since 1.0.0
 */
public fun KoreSemanticVersion.nextMajor(): KoreSemanticVersion {
    if (isPreRelease && minor == 0 && patch == 0) return KoreSemanticVersion(major)
    check(major < Int.MAX_VALUE) { "A major version $EXHAUSTED" }
    return KoreSemanticVersion(major + 1)
}

/**
 * Returns the next minor release: `1.4.2` gives `1.5.0`.
 *
 * A pre-release of a minor release, such as `1.5.0-beta`, gives that release, `1.5.0`, rather than skipping past it.
 * Pre-release identifiers and build metadata are dropped.
 *
 * @receiver the version to step from.
 * @return the next minor release.
 * @throws IllegalStateException if the minor number is already [Int.MAX_VALUE] and the version is not a pre-release
 * of a minor release.
 * @since 1.0.0
 */
public fun KoreSemanticVersion.nextMinor(): KoreSemanticVersion {
    if (isPreRelease && patch == 0) return KoreSemanticVersion(major = major, minor = minor)
    check(minor < Int.MAX_VALUE) { "A minor version $EXHAUSTED" }
    return KoreSemanticVersion(major = major, minor = minor + 1)
}

/**
 * Returns the next patch release: `1.4.2` gives `1.4.3`.
 *
 * A pre-release, such as `1.5.1-beta`, gives its release, `1.5.1`, rather than skipping past it. Pre-release
 * identifiers and build metadata are dropped.
 *
 * @receiver the version to step from.
 * @return the next patch release.
 * @throws IllegalStateException if the patch number of a release is already [Int.MAX_VALUE].
 * @since 1.0.0
 */
public fun KoreSemanticVersion.nextPatch(): KoreSemanticVersion {
    if (isPreRelease) return release()
    check(patch < Int.MAX_VALUE) { PATCH_EXHAUSTED_MESSAGE }
    return KoreSemanticVersion(major = major, minor = minor, patch = patch + 1)
}

/**
 * Continues the pre-release line of this version: `1.0.0-rc.1` gives `1.0.0-rc.2`.
 *
 * The last numeric identifier is raised, however many digits it has and whatever follows it, so
 * `1.0.0-alpha.1.nightly` gives `1.0.0-alpha.2.nightly`. A line without a numeric identifier gets its first one:
 * `1.0.0-rc` gives `1.0.0-rc.1`. The numbers stay the same and build metadata is dropped.
 *
 * @receiver the pre-release to continue.
 * @return the next pre-release of the line.
 * @throws IllegalStateException if this version is a release, which has no line to continue, or if the next
 * pre-release would be longer than [KoreLimits.MAX_VERSION_LENGTH] characters.
 * @since 1.0.0
 */
public fun KoreSemanticVersion.nextPreRelease(): KoreSemanticVersion {
    check(isPreRelease) { NO_LINE_MESSAGE }
    val line = continued(preRelease)
    val isFitting = fitsVersionLength(
        major = major,
        minor = minor,
        patch = patch,
        preRelease = line,
        build = emptyList(),
    )
    check(isFitting) { LINE_TOO_LONG_MESSAGE }
    return KoreSemanticVersion(major = major, minor = minor, patch = patch, preRelease = line)
}

/**
 * Returns the next pre-release of the line named [label].
 *
 * When this version is a pre-release whose first identifier is [label], the line is continued as [nextPreRelease]
 * continues it: `1.0.0-rc.1` with `rc` gives `1.0.0-rc.2`. Otherwise a new line `label.1` is started. On a
 * pre-release it is started on the same numbers and must come later: `1.0.0-beta.3` with `rc` gives `1.0.0-rc.1`,
 * while `1.0.0-rc.1` with `alpha` is refused. On a release it is started on the next patch: `1.2.3` with `rc` gives
 * `1.2.4-rc.1`. Build metadata is dropped.
 *
 * The length of [label] is checked against [KoreLimits.MAX_VERSION_LENGTH] before its characters are read, and the
 * message of a refusal never quotes it.
 *
 * @receiver the version to step from.
 * @param label the first identifier of the line, such as `rc`.
 * @return the next pre-release of the line.
 * @throws IllegalArgumentException if [label] is not a pre-release identifier, if the new line would come before
 * this version, or if the new pre-release would be longer than [KoreLimits.MAX_VERSION_LENGTH] characters.
 * @throws IllegalStateException if the line is continued and would become too long, or if a line is started on a
 * release whose patch number is already [Int.MAX_VALUE].
 * @since 1.0.0
 */
public fun KoreSemanticVersion.nextPreRelease(label: String): KoreSemanticVersion {
    require(label.length <= KoreLimits.MAX_VERSION_LENGTH && isPreReleaseIdentifier(label)) { LABEL_MESSAGE }
    if (preRelease.firstOrNull() == label) return nextPreRelease()
    return startedPreRelease(label)
}

private fun KoreSemanticVersion.startedPreRelease(label: String): KoreSemanticVersion {
    val line = listOf(label, FIRST_PRE_RELEASE)
    if (!isPreRelease) {
        check(patch < Int.MAX_VALUE) { PATCH_EXHAUSTED_MESSAGE }
        return KoreSemanticVersion(major = major, minor = minor, patch = patch + 1, preRelease = line)
    }
    val started = KoreSemanticVersion(major = major, minor = minor, patch = patch, preRelease = line)
    require(started > this) { BACKWARDS_MESSAGE }
    return started
}

private fun continued(identifiers: List<String>): List<String> {
    val position = identifiers.indexOfLast(::isNumeric)
    if (position < 0) return identifiers + FIRST_PRE_RELEASE
    val raised = incremented(identifiers[position])
    return identifiers.take(position) + raised + identifiers.drop(position + 1)
}
