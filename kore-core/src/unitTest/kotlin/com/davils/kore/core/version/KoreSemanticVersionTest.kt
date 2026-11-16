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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain

private const val LONG_TEXT_LENGTH: Int = 10_000
private const val CORE: String = "1.0.0"
private const val CORE_WITH_SEPARATOR_LENGTH: Int = 6
private const val TOO_LONG_MESSAGE: String = "A version is longer than ${KoreLimits.MAX_VERSION_LENGTH} characters"
private const val MOST_SINGLE_LETTER_IDENTIFIERS: Int = 125
private const val CANDIDATE: String = "1.0.0-rc.1"

private val longestLabel: String = "a".repeat(KoreLimits.MAX_VERSION_LENGTH - CORE_WITH_SEPARATOR_LENGTH)

private val specificationOrder: List<String> = listOf(
    "1.0.0-alpha",
    "1.0.0-alpha.1",
    "1.0.0-alpha.beta",
    "1.0.0-beta",
    "1.0.0-beta.2",
    "1.0.0-beta.11",
    "1.0.0-rc.1",
    "1.0.0",
    "2.0.0",
    "2.1.0",
    "2.1.1",
)

private val valid: List<String> = listOf(
    "0.0.0",
    "1.0.0-0.3.7",
    "1.0.0-x.7.z.92",
    "1.0.0-x-y-z.--",
    "1.0.0-alpha+001",
    "1.0.0+20130313144700",
    "1.0.0-beta+exp.sha.5114f85",
    "1.0.0+21AF26D3----117B344092BD",
)

private val invalid: List<String> = listOf(
    "",
    "1",
    "1.2",
    "1.2.3.4",
    "v1.0.0",
    " 1.0.0",
    "1.0.0 ",
    "01.0.0",
    "1.00.0",
    "1.0.0-01",
    "1.0.0-",
    "1.0.0+",
    "1.0.0-alpha..1",
    "1.0.0+build..1",
    "1.0.0-al_pha",
    "1.0.0-r~c",
    "99999999999.0.0",
    "a.b.c",
)

class KoreSemanticVersionTest : FunSpec({
    context("reading") {
        test("reads every version the specification lists as valid back as written") {
            for (text in valid) {
                withClue(text) { KoreSemanticVersion.parse(text).toString() shouldBe text }
            }
        }

        test("refuses every text the specification refuses") {
            for (text in invalid) {
                withClue(text) { KoreSemanticVersion.parseOrNull(text).shouldBeNull() }
            }
        }

        test("splits a version into its parts") {
            val version = KoreSemanticVersion.parse("2.0.0-rc.1+sha.9f3c")

            version.major shouldBe 2
            version.minor shouldBe 0
            version.patch shouldBe 0
            version.preRelease shouldContainExactly listOf("rc", "1")
            version.build shouldContainExactly listOf("sha", "9f3c")
            version.isPreRelease shouldBe true
        }

        test("says why a text is refused without quoting it") {
            val failure = shouldThrow<IllegalArgumentException> { KoreSemanticVersion.parse("01.2.3") }

            failure.message shouldBe "Not a semantic version: it has a number with a leading zero"
            failure.message shouldNotContain "01.2.3"
        }

        test("refuses a very long text before it is split") {
            val failure = shouldThrow<IllegalArgumentException> {
                KoreSemanticVersion.parse("1".repeat(LONG_TEXT_LENGTH))
            }

            failure.message shouldBe "Not a semantic version: it is longer than 256 characters"
        }
    }

    context("precedence") {
        test("orders the example list of the specification exactly as the specification does") {
            val shuffled = specificationOrder.reversed().map(KoreSemanticVersion::parse)

            shuffled.sorted().map { it.toString() } shouldContainExactly specificationOrder
        }

        test("puts a pre-release below the release it leads to") {
            (KoreSemanticVersion.parse("2.0.0-rc.1") < KoreSemanticVersion(2)) shouldBe true
        }

        test("compares numeric pre-release identifiers as numbers, however long they are") {
            val short = KoreSemanticVersion.parse("1.0.0-9")
            val long = KoreSemanticVersion.parse("1.0.0-100000000000000000000")

            (short < long) shouldBe true
        }

        test("ignores build metadata, while equality does not") {
            val one = KoreSemanticVersion.parse("1.0.0+a")
            val other = KoreSemanticVersion.parse("1.0.0+b")

            one.compareTo(other) shouldBe 0
            one.isSamePrecedence(other) shouldBe true
            one shouldNotBe other
        }

        test("tells a pre-release apart from its release by the same precedence") {
            val candidate = KoreSemanticVersion.parse(CANDIDATE)

            candidate.isSamePrecedence(KoreSemanticVersion.parse(CORE)) shouldBe false
            candidate.isSamePrecedence(KoreSemanticVersion.parse("$CANDIDATE+b")) shouldBe true
        }
    }

    context("stating one") {
        test("fills in what is not given with zeros and nothing") {
            val version = KoreSemanticVersion(2)

            version.toString() shouldBe "2.0.0"
            version.isPreRelease shouldBe false
        }

        test("equals the same version read from text") {
            val stated = KoreSemanticVersion(major = 1, minor = 4, patch = 0, preRelease = listOf("rc", "1"))

            stated shouldBe KoreSemanticVersion.parse("1.4.0-rc.1")
            KoreSemanticVersion(major = 1, minor = 4).hashCode() shouldBe KoreSemanticVersion.parse("1.4.0").hashCode()
        }

        test("refuses a negative number") {
            shouldThrow<IllegalArgumentException> { KoreSemanticVersion(major = 1, minor = -1) }
            shouldThrow<IllegalArgumentException> { KoreSemanticVersion(major = -1) }
            shouldThrow<IllegalArgumentException> { KoreSemanticVersion(major = 1, patch = -1) }
        }

        test("refuses a malformed identifier and says which part holds it") {
            shouldThrow<IllegalArgumentException> { KoreSemanticVersion(major = 1, preRelease = listOf("01")) }
                .message shouldBe "A version has an empty or malformed pre-release identifier"
            shouldThrow<IllegalArgumentException> { KoreSemanticVersion(major = 1, build = listOf("")) }
                .message shouldBe "A version has an empty or malformed build identifier"
        }

        test("reads back from its text when it is as long as a version may be") {
            val longest = KoreSemanticVersion(major = 1, preRelease = listOf(longestLabel))

            longest.toString().length shouldBe KoreLimits.MAX_VERSION_LENGTH
            KoreSemanticVersion.parse(longest.toString()) shouldBe longest
        }

        test("refuses to be longer than reading would allow") {
            shouldThrow<IllegalArgumentException> {
                KoreSemanticVersion(major = 1, preRelease = listOf(longestLabel + "a"))
            }.message shouldBe TOO_LONG_MESSAGE
            shouldThrow<IllegalArgumentException> {
                KoreSemanticVersion(major = 1, build = List(MOST_SINGLE_LETTER_IDENTIFIERS + 1) { "b" })
            }.message shouldBe TOO_LONG_MESSAGE
            shouldThrow<IllegalArgumentException> {
                KoreSemanticVersion(major = 1, preRelease = listOf("rc"), build = listOf(longestLabel))
            }.message shouldBe TOO_LONG_MESSAGE
        }

        test("refuses a huge list of identifiers before it copies it") {
            shouldThrow<IllegalArgumentException> {
                KoreSemanticVersion(major = 1, preRelease = List(LONG_TEXT_LENGTH) { "a" })
            }.message shouldBe TOO_LONG_MESSAGE
            shouldThrow<IllegalArgumentException> {
                KoreSemanticVersion(major = 1, build = List(LONG_TEXT_LENGTH) { "b" })
            }.message shouldBe TOO_LONG_MESSAGE
        }

        test("refuses an identifier that is too long, given through withPreRelease or withBuild") {
            val version = KoreSemanticVersion(1)

            shouldThrow<IllegalArgumentException> { version.withPreRelease("a".repeat(LONG_TEXT_LENGTH)) }
                .message shouldBe TOO_LONG_MESSAGE
            shouldThrow<IllegalArgumentException> { version.withBuild("a".repeat(LONG_TEXT_LENGTH)) }
                .message shouldBe TOO_LONG_MESSAGE
        }

        test("keeps no reference to the list it was given") {
            val identifiers = mutableListOf("rc")
            val version = KoreSemanticVersion(major = 1, preRelease = identifiers)

            identifiers += "2"

            version.preRelease shouldContainExactly listOf("rc")
        }
    }

    context("the identifiers") {
        test("of a version read from text cannot be changed through a cast") {
            val version = KoreSemanticVersion.parse("1.0.0-rc.1+sha.9f3c")

            shouldThrow<UnsupportedOperationException> { (version.preRelease as MutableList<String>).add("x") }
            shouldThrow<UnsupportedOperationException> { (version.build as MutableList<String>).clear() }
            version.toString() shouldBe "1.0.0-rc.1+sha.9f3c"
        }

        test("of a version stated in code cannot be changed through a cast") {
            val version = KoreSemanticVersion(major = 1, preRelease = listOf("rc", "1"), build = listOf("sha", "1"))
            val reshaped = version.withPreRelease("beta", "2")
            val plain = KoreSemanticVersion(1)

            shouldThrow<UnsupportedOperationException> { (version.preRelease as MutableList<String>).add("x") }
            shouldThrow<UnsupportedOperationException> { (version.build as MutableList<String>).add("x") }
            shouldThrow<UnsupportedOperationException> { (reshaped.preRelease as MutableList<String>).add("x") }
            shouldThrow<UnsupportedOperationException> { (plain.build as MutableList<String>).add("x") }
        }
    }

    context("equality") {
        test("never matches something that is not a version") {
            KoreSemanticVersion(1).equals(CORE) shouldBe false
        }

        test("tells versions apart by every part") {
            val base = KoreSemanticVersion.parse("1.2.3-rc+b")

            base shouldNotBe KoreSemanticVersion.parse("2.2.3-rc+b")
            base shouldNotBe KoreSemanticVersion.parse("1.3.3-rc+b")
            base shouldNotBe KoreSemanticVersion.parse("1.2.4-rc+b")
            base shouldNotBe KoreSemanticVersion.parse("1.2.3-rd+b")
            base shouldBe KoreSemanticVersion.parse("1.2.3-rc+b")
        }
    }
})
