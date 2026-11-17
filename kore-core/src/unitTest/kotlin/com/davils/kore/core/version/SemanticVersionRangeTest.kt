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

import com.davils.kore.core.limit.Limits
import com.davils.kore.core.version.fixture.range
import com.davils.kore.core.version.fixture.version
import com.davils.kore.core.version.interval.VersionInterval
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val HALF_OPEN: String = "[1.2, 2.0)"
private const val LOWER: String = "1.2.0"
private const val UPPER: String = "2.0.0"
private const val EXACT: String = "1.4.0"
private const val PATCH: String = "1.2.3"
private const val CARET: String = "^1.2.3"
private const val ABOVE_PATCH: String = ">1.2.3"
private const val REPETITIONS: Int = 200
private const val EXACT_INTERVAL: String = "[$EXACT]"
private const val CARET_INTERVAL: String = "[$PATCH, 2.0.0-0)"
private const val LONGEST_LABEL_LENGTH: Int = Limits.MAX_VERSION_LENGTH - 6

private val longest: String = "1.0.0-${"a".repeat(LONGEST_LABEL_LENGTH)}"

private fun intervalOf(text: String): VersionInterval = range(text).intervals.single()

class SemanticVersionRangeTest : FunSpec({
    context("the interval form") {
        test("closed below and open above holds its lower bound and not its upper") {
            val range = range(HALF_OPEN)

            range.contains(version(LOWER)) shouldBe true
            range.contains(version("1.9.3")) shouldBe true
            range.contains(version(UPPER)) shouldBe false
            range.contains(version("1.1.9")) shouldBe false
        }

        test("open below and closed above holds its upper bound and not its lower") {
            val range = range("(1.0,2.0]")

            range.contains(version("1.0.0")) shouldBe false
            range.contains(version(UPPER)) shouldBe true
        }

        test("a missing upper bound is unbounded above") {
            val range = range("[1.2,)")

            range.contains(version("99.0.0")) shouldBe true
            range.contains(version("1.1.0")) shouldBe false
        }

        test("a missing lower bound is unbounded below") {
            val range = range("(,2.0]")

            range.contains(version("0.0.1")) shouldBe true
            range.contains(version("2.0.1")) shouldBe false
        }

        test("a single bound in brackets is exactly that version") {
            val range = range(EXACT_INTERVAL)

            range.contains(version(EXACT)) shouldBe true
            range.contains(version("1.4.1")) shouldBe false
            range shouldBe SemanticVersionRange.exactly(version(EXACT))
        }

        test("shortened bounds mean the same as written out") {
            range("[1.2, 2)") shouldBe range("[$LOWER,$UPPER)")
        }

        test("several intervals separated by commas are a union, as Maven writes them") {
            val range = range("[1.0,2.0), [3.0,4.0)")

            range.contains(version("1.5.0")) shouldBe true
            range.contains(version("2.5.0")) shouldBe false
            range.contains(version("3.5.0")) shouldBe true
            range.intervals.size shouldBe 2
        }

        test("surrounding whitespace is tolerated") {
            range("  $HALF_OPEN  ") shouldBe range(HALF_OPEN)
        }

        test("renders in the canonical form") {
            range(HALF_OPEN).toString() shouldBe "[$LOWER,$UPPER)"
            range("(,2]").toString() shouldBe "(,$UPPER]"
            range(EXACT_INTERVAL).toString() shouldBe EXACT_INTERVAL
            range("[3,4), [1,2)").toString() shouldBe "[1.0.0,2.0.0),[3.0.0,4.0.0)"
            SemanticVersionRange.ANY.toString() shouldBe "(,)"
            SemanticVersionRange.NONE.toString() shouldBe "[]"
        }

        test("places a pre-release by precedence, so it precedes its release, only when asked to") {
            val candidate = version("2.0.0-rc.1")

            range(HALF_OPEN).contains(candidate, PreReleasePolicy.ByPrecedence) shouldBe true
            range(HALF_OPEN).contains(candidate) shouldBe false
        }

        test("ignores build metadata in a bound, as precedence does") {
            range("[1.2.0+sha.1, 2.0.0+sha.2)") shouldBe range(HALF_OPEN)
        }
    }

    context("what is refused") {
        test("text that is not in interval form") {
            val refused = listOf(
                "[1.2",
                "1.2)",
                "[1.0,2.0,3.0)",
                "[1.0)",
                "(1.0]",
                "[,1.0]",
                "[1.0,]",
                "[1,2) [3,4)",
                "[1,2),",
                "[1,2), 3",
            )
            for (text in refused) {
                withClue(text) { SemanticVersionRange.parseOrNull(text).shouldBeNull() }
            }
        }

        test("nothing at all") {
            for (text in listOf("", "   ")) {
                withClue(text) { SemanticVersionRange.parseOrNull(text).shouldBeNull() }
            }
        }

        test("a bound that is not a version, saying why") {
            val failure = shouldThrow<IllegalArgumentException> { range("[1.x, 2.0)") }

            failure.message shouldBe
                "Not a version range: it has a bound that is not a version: it must start with three " +
                "dot-separated numbers, such as 1.4.0"
        }

        test("an exact bound that is not a version") {
            SemanticVersionRange.parseOrNull("[1.x]").shouldBeNull()
        }

        test("an upper bound that is not a version") {
            SemanticVersionRange.parseOrNull("[1.2, 2.x)").shouldBeNull()
        }

        test("a shortened bound carrying a pre-release") {
            SemanticVersionRange.parseOrNull("[1.2-rc.1, 2.0)").shouldBeNull()
        }

        test("an interval with nothing in it") {
            SemanticVersionRange.parseOrNull("[2.0, 1.0)").shouldBeNull()
            SemanticVersionRange.parseOrNull("(1.0, 1.0)").shouldBeNull()
        }

        test("an interval with no version between its exclusive bounds") {
            SemanticVersionRange.parseOrNull("($PATCH, 1.2.4-0)").shouldBeNull()
        }

        test("text longer than the limit, before it is split") {
            val long = "^1.2 || ".repeat(REPETITIONS) + "^1.2"

            val failure = shouldThrow<IllegalArgumentException> { range(long) }

            failure.message shouldBe "Not a version range: it is longer than 1024 characters"
        }
    }

    context("stating one") {
        test("between is closed below and open above") {
            val range = SemanticVersionRange.between(from = version(LOWER), until = version(UPPER))

            range shouldBe range(HALF_OPEN)
        }

        test("between with no upper bound is open-ended") {
            val range = SemanticVersionRange.between(version(LOWER), until = null)

            range.contains(version("50.0.0")) shouldBe true
        }

        test("atLeast and below are the two halves") {
            SemanticVersionRange.atLeast(version(LOWER)) shouldBe range(">=$LOWER")
            SemanticVersionRange.below(version(UPPER)) shouldBe range("<$UPPER")
        }

        test("bounded states each bound and whether it is inside") {
            val range = SemanticVersionRange.bounded(
                lower = version(LOWER),
                lowerInclusive = false,
                upper = version(UPPER),
                upperInclusive = true,
            )

            range shouldBe range("($LOWER, $UPPER]")
        }

        test("caret and tilde mean what their notation means") {
            SemanticVersionRange.caret(version(PATCH)) shouldBe range(CARET)
            SemanticVersionRange.caret(version("0.0.3")) shouldBe range("^0.0.3")
            SemanticVersionRange.tilde(version(PATCH)) shouldBe range("~$PATCH")
        }

        test("an empty range is refused") {
            val failure = shouldThrow<IllegalArgumentException> {
                SemanticVersionRange.between(version(UPPER), version(UPPER))
            }

            failure.message shouldBe "A version range must not be empty: its lower bound is not below its upper"
        }

        test("nothing lies below the lowest version there is") {
            shouldThrow<IllegalArgumentException> { SemanticVersionRange.below(version("0.0.0-0")) }
        }

        test("any version is in ANY by precedence, and any release under the default policy") {
            val alpha = version("0.0.0-alpha")

            SemanticVersionRange.ANY.contains(alpha, PreReleasePolicy.ByPrecedence) shouldBe true
            SemanticVersionRange.ANY.contains(alpha) shouldBe false
            SemanticVersionRange.ANY.contains(version(UPPER)) shouldBe true
            SemanticVersionRange.ANY.isAny shouldBe true
            SemanticVersionRange.NONE.isAny shouldBe false
        }

        test("a lower bound of the lowest version there is holds every version and still names it") {
            val lowest = range(">=0.0.0-0")
            val alpha = version("0.0.0-alpha")

            lowest.contains(version("0.0.0-0")) shouldBe true
            lowest.contains(alpha, PreReleasePolicy.SameCoreOnly) shouldBe true
            SemanticVersionRange.ANY.contains(alpha, PreReleasePolicy.SameCoreOnly) shouldBe false
        }
    }

    context("the parts") {
        test("cannot be changed through a cast") {
            val written = range("^1.2 || ^3 || 1.0.0-rc.1")
            val chosen = written.satisfying(listOf(version(LOWER), version(PATCH)))
            val intervals = written.intervals

            shouldThrow<UnsupportedOperationException> { (intervals as MutableList<VersionInterval>).clear() }
            shouldThrow<UnsupportedOperationException> { (chosen as MutableList<SemanticVersion>).clear() }
            written shouldBe range("1.0.0-rc.1 || ^3 || ^1.2")
        }

        test("around the longest version there is read back from their text") {
            for (text in listOf("[$longest]", ">$longest", "<=$longest", "[0.1.0,$longest]", "($longest,)")) {
                val read = range(text)

                withClue(text) { range(read.toString()) shouldBe read }
            }
        }
    }

    context("equality") {
        test("two ranges holding the same versions are equal however they were written") {
            range(CARET) shouldBe range(">=$PATCH <2.0.0-0")
            range(CARET) shouldBe range(CARET_INTERVAL)
            range("<=$PATCH") shouldBe range("<1.2.4-0")
            range(PATCH) shouldBe range("[$PATCH]")
        }

        test("a lower bound just above a release stays apart from the pre-release floor after it") {
            val above = range(ABOVE_PATCH)
            val floor = range(">=1.2.4-0")
            val preRelease = version("1.2.4-beta")

            (above == floor) shouldBe false
            above.contains(preRelease, PreReleasePolicy.ByPrecedence) shouldBe
                floor.contains(preRelease, PreReleasePolicy.ByPrecedence)
            above.contains(preRelease, PreReleasePolicy.SameCoreOnly) shouldBe false
            floor.contains(preRelease, PreReleasePolicy.SameCoreOnly) shouldBe true
            floor.toString() shouldBe "[1.2.4-0,)"
        }

        test("equal ranges hash alike") {
            range(CARET).hashCode() shouldBe range(CARET_INTERVAL).hashCode()
        }

        test("a range never equals something that is not a range") {
            range(CARET).equals(CARET) shouldBe false
        }

        test("a bound is stated from the side that names the version written") {
            val interval = intervalOf(ABOVE_PATCH)

            interval.lower shouldBe version(PATCH)
            interval.isLowerInclusive shouldBe false
            interval.upper.shouldBeNull()
            interval.isUpperInclusive shouldBe false
        }

        test("an exclusive upper bound that is a pre-release floor stays as written") {
            val interval = intervalOf(CARET)

            interval.upper shouldBe version("2.0.0-0")
            interval.isUpperInclusive shouldBe false
        }

        test("intervals are equal exactly when they hold and name the same versions") {
            val interval = intervalOf(CARET)

            interval shouldBe intervalOf(CARET_INTERVAL)
            interval.hashCode() shouldBe intervalOf(CARET_INTERVAL).hashCode()
            interval.equals(CARET) shouldBe false
            intervalOf(ABOVE_PATCH) shouldBe intervalOf("($PATCH,)")
            (intervalOf(ABOVE_PATCH) == intervalOf(">=1.2.4-0")) shouldBe false
            (interval == intervalOf("[$PATCH, 3.0.0-0)")) shouldBe false
            (interval == intervalOf("[1.2.2, 2.0.0-0)")) shouldBe false
            (intervalOf(ABOVE_PATCH) == intervalOf(">=$PATCH")) shouldBe false
        }
    }
})
