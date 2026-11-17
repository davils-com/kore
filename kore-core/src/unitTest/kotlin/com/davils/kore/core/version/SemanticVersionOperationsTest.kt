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

import com.davils.kore.core.Limits
import com.davils.kore.core.version.fixture.version
import com.davils.kore.core.version.fixture.versions
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val CURRENT: String = "1.4.2"
private const val CANDIDATE: String = "2.0.0-rc.1"
private const val BUILT_CANDIDATE: String = "2.0.0-rc.1+sha.9f3c"
private const val RELEASE: String = "2.0.0"
private const val FIRST_CANDIDATE: String = "1.0.0-rc.1"
private const val MINOR_BETA: String = "1.5.0-beta"
private const val PATCH_BETA: String = "1.5.1-beta"
private const val BASELINE: String = "1.2.0"
private const val PATCH: String = "1.2.3"
private const val INITIAL: String = "0.0.3"
private const val FIRST_BUILD: String = "1.0.0+a"
private const val SECOND_BUILD: String = "1.0.0+b"
private const val FIRST_RELEASE: String = "1.0.0"
private const val NEXT_MINOR: String = "1.5.0"
private const val MINOR_START: String = "1.4.0"
private const val LINE_START: String = "1.0.0-"
private const val NUMBERED_LABEL_LENGTH: Int = 248
private const val LONG_LABEL_LENGTH: Int = 250
private const val HUGE_LABEL_LENGTH: Int = 1_000_000
private const val LONGEST: Int = Limits.MAX_VERSION_LENGTH

private val numberedLabel: String = "a".repeat(NUMBERED_LABEL_LENGTH)

private fun expectSteps(steps: List<Pair<String, String>>, step: (SemanticVersion) -> SemanticVersion) {
    for ((from, to) in steps) {
        withClue(from) { step(version(from)) shouldBe version(to) }
    }
}

class SemanticVersionOperationsTest : FunSpec({
    context("the next major, minor and patch") {
        test("raise the number and reset what follows it") {
            expectSteps(listOf(CURRENT to RELEASE, "0.9.9" to FIRST_RELEASE), SemanticVersion::nextMajor)
            expectSteps(listOf(CURRENT to NEXT_MINOR, FIRST_RELEASE to "1.1.0"), SemanticVersion::nextMinor)
            expectSteps(listOf(CURRENT to "1.4.3", MINOR_START to "1.4.1"), SemanticVersion::nextPatch)
        }

        test("release a pre-release of that step rather than skipping past it") {
            expectSteps(listOf("2.0.0-rc.2" to RELEASE, MINOR_BETA to RELEASE), SemanticVersion::nextMajor)
            expectSteps(listOf(MINOR_BETA to NEXT_MINOR, PATCH_BETA to "1.6.0"), SemanticVersion::nextMinor)
            expectSteps(listOf(PATCH_BETA to "1.5.1", "1.5.0-rc.1" to NEXT_MINOR), SemanticVersion::nextPatch)
        }

        test("drop build metadata, because a new version is a new build") {
            version("$CURRENT+sha.9f3c").nextPatch() shouldBe version("1.4.3")
        }

        test("refuse to raise a number past the largest one there is") {
            val largest = SemanticVersion(major = Int.MAX_VALUE, minor = Int.MAX_VALUE, patch = Int.MAX_VALUE)

            shouldThrow<IllegalStateException> { largest.nextMajor() }
                .message shouldBe "A major version cannot be raised past 2147483647"
            shouldThrow<IllegalStateException> { largest.nextMinor() }
                .message shouldBe "A minor version cannot be raised past 2147483647"
            shouldThrow<IllegalStateException> { largest.nextPatch() }
                .message shouldBe "A patch version cannot be raised past 2147483647"
        }
    }

    context("the next pre-release") {
        test("raises the last numeric identifier") {
            expectSteps(
                listOf(FIRST_CANDIDATE to "1.0.0-rc.2", "1.0.0-beta.9" to "1.0.0-beta.10", "1.0.0-0" to "1.0.0-1"),
                SemanticVersion::nextPreRelease,
            )
        }

        test("raises a number of any length without overflowing") {
            val raised = version("1.0.0-rc.99999999999999999999").nextPreRelease()

            raised shouldBe version("1.0.0-rc.100000000000000000000")
        }

        test("raises the last numeric identifier even when a word follows it") {
            version("1.0.0-alpha.1.nightly").nextPreRelease() shouldBe version("1.0.0-alpha.2.nightly")
        }

        test("gives a line without a number its first one") {
            version("1.0.0-rc").nextPreRelease() shouldBe version("1.0.0-rc.1")
        }

        test("refuses to continue a line a release does not have") {
            shouldThrow<IllegalStateException> { version(FIRST_RELEASE).nextPreRelease() }
        }

        test("continues the line when given the label the line already has") {
            version(FIRST_CANDIDATE).nextPreRelease("rc") shouldBe version("1.0.0-rc.2")
        }

        test("moves to a later label on the same version") {
            version("1.0.0-beta.3").nextPreRelease("rc") shouldBe version(FIRST_CANDIDATE)
        }

        test("refuses an earlier label, because it would go backwards") {
            shouldThrow<IllegalArgumentException> { version(FIRST_CANDIDATE).nextPreRelease("alpha") }
        }

        test("starts the line on the next patch of a release") {
            version(PATCH).nextPreRelease("rc") shouldBe version("1.2.4-rc.1")
        }

        test("refuses to start a line past the largest patch there is") {
            val largest = SemanticVersion(major = 1, minor = 0, patch = Int.MAX_VALUE)

            shouldThrow<IllegalStateException> { largest.nextPreRelease("rc") }
        }

        test("continues a line up to the longest version there is") {
            val almost = version("$LINE_START$numberedLabel.8")

            val continued = almost.nextPreRelease()

            continued.toString() shouldBe "$LINE_START$numberedLabel.9"
            continued.toString().length shouldBe LONGEST
        }

        test("refuses to continue a line past the longest version there is") {
            val message = "A pre-release line cannot be continued past $LONGEST characters"

            shouldThrow<IllegalStateException> { version("$LINE_START$numberedLabel.9").nextPreRelease() }
                .message shouldBe message
            val unnumbered = version(LINE_START + "a".repeat(LONG_LABEL_LENGTH))

            shouldThrow<IllegalStateException> { unnumbered.nextPreRelease() }.message shouldBe message
        }

        test("refuses a label too long for any version before it reads the label") {
            shouldThrow<IllegalArgumentException> { version(PATCH).nextPreRelease("a".repeat(HUGE_LABEL_LENGTH)) }
                .message shouldBe "A pre-release label must be a pre-release identifier"
        }

        test("refuses a label that would make the version too long") {
            shouldThrow<IllegalArgumentException> { version(PATCH).nextPreRelease("a".repeat(LONG_LABEL_LENGTH)) }
                .message shouldBe "A version is longer than $LONGEST characters"
        }

        test("refuses a label that is not a pre-release identifier") {
            shouldThrow<IllegalArgumentException> { version(PATCH).nextPreRelease("r c") }
            shouldThrow<IllegalArgumentException> { version(PATCH).nextPreRelease("01") }
        }
    }

    context("reshaping") {
        test("release drops the pre-release and the build") {
            version(BUILT_CANDIDATE).release() shouldBe version(RELEASE)
        }

        test("release drops the build of a release") {
            version("$RELEASE+sha.9f3c").release() shouldBe version(RELEASE)
        }

        test("release of a plain release is that release") {
            val plain = version(RELEASE)

            plain.release() shouldBeSameInstanceAs plain
        }

        test("withoutBuild keeps the pre-release") {
            version(BUILT_CANDIDATE).withoutBuild() shouldBe version(CANDIDATE)
        }

        test("withoutBuild of a version without build metadata is that version") {
            val plain = version(CANDIDATE)

            plain.withoutBuild() shouldBeSameInstanceAs plain
        }

        test("withPreRelease replaces it and keeps the build") {
            version(BUILT_CANDIDATE).withPreRelease("rc", "2") shouldBe version("2.0.0-rc.2+sha.9f3c")
            version(CANDIDATE).withPreRelease() shouldBe version(RELEASE)
        }

        test("withBuild replaces it and keeps the pre-release") {
            version(CANDIDATE).withBuild("sha", "1a2b") shouldBe version("$CANDIDATE+sha.1a2b")
        }

        test("refuses an identifier that is not one") {
            shouldThrow<IllegalArgumentException> { version(RELEASE).withPreRelease("01") }
            shouldThrow<IllegalArgumentException> { version(RELEASE).withBuild("") }
        }
    }

    context("stability") {
        test("a release past 0.y.z is stable") {
            version(FIRST_RELEASE).isStable shouldBe true
        }

        test("initial development and pre-releases are not") {
            version("0.9.0").isStable shouldBe false
            version(FIRST_CANDIDATE).isStable shouldBe false
        }
    }

    context("compatibility") {
        test("a later version of the same major stands in for an earlier one") {
            version("1.9.0").isCompatibleWith(version(BASELINE)) shouldBe true
            version(BASELINE).isCompatibleWith(version("1.9.0")) shouldBe false
            version(RELEASE).isCompatibleWith(version(BASELINE)) shouldBe false
        }

        test("below 1.0.0 the minor is the compatibility line") {
            version("0.2.5").isCompatibleWith(version("0.2.3")) shouldBe true
            version("0.3.0").isCompatibleWith(version("0.2.3")) shouldBe false
        }

        test("below 0.1.0 a version stands in only for itself") {
            version(INITIAL).isCompatibleWith(version(INITIAL)) shouldBe true
            version("0.0.4").isCompatibleWith(version(INITIAL)) shouldBe false
        }

        test("a pre-release stands in only for a pre-release of the same version") {
            version("$PATCH-rc.2").isCompatibleWith(version("$PATCH-rc.1")) shouldBe true
            version("1.3.0-beta").isCompatibleWith(version(BASELINE)) shouldBe false
        }

        test("a build of a version stands in for another build of it") {
            version("$BASELINE+b").isCompatibleWith(version("$BASELINE+a")) shouldBe true
        }
    }

    context("the two orderings") {
        test("precedence does not tell builds apart") {
            SemanticVersion.PRECEDENCE.compare(version(FIRST_BUILD), version(SECOND_BUILD)) shouldBe 0
        }

        test("the total order does, and agrees with equality") {
            SemanticVersion.TOTAL_ORDER.compare(version(FIRST_BUILD), version(SECOND_BUILD)) shouldBe -1
            SemanticVersion.TOTAL_ORDER.compare(version(FIRST_BUILD), version(FIRST_BUILD)) shouldBe 0
        }

        test("the total order puts no build first, and numeric identifiers numerically") {
            val sorted = versions("1.0.0+10", SECOND_BUILD, FIRST_RELEASE, "1.0.0+9", "0.9.0+z")
                .sortedWith(SemanticVersion.TOTAL_ORDER)

            sorted shouldContainExactly versions("0.9.0+z", FIRST_RELEASE, "1.0.0+9", "1.0.0+10", SECOND_BUILD)
        }
    }

    context("reading from a string") {
        test("reads a version and a range") {
            MINOR_START.toSemanticVersion() shouldBe SemanticVersion(major = 1, minor = 4)
            "^1.2".toSemanticVersionRange() shouldBe SemanticVersionRange.parse(">=1.2.0 <2.0.0-0")
            MINOR_START.toSemanticVersionOrNull() shouldBe SemanticVersion(major = 1, minor = 4)
            "^1.2".toSemanticVersionRangeOrNull() shouldBe SemanticVersionRange.parse(">=1.2.0 <2.0.0-0")
        }

        test("answers null for what is not one") {
            "v1.4.0".toSemanticVersionOrNull().shouldBeNull()
            ">2 <1".toSemanticVersionRangeOrNull().shouldBeNull()
        }

        test("throws for what is not one") {
            shouldThrow<IllegalArgumentException> { "v1.4.0".toSemanticVersion() }
            shouldThrow<IllegalArgumentException> { "".toSemanticVersionRange() }
        }
    }
})
