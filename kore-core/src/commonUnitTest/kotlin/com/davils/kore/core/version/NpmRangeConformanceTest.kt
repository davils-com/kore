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

import com.davils.kore.core.version.fixture.range
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

private const val MAJOR_ONE: String = ">=1.0.0 <2.0.0-0"
private const val MINOR_ONE_TWO: String = ">=1.2.0 <1.3.0-0"
private const val MINOR_ZERO_ZERO: String = ">=0.0.0 <0.1.0-0"
private const val MINOR_ZERO_TWO: String = ">=0.2.3 <0.3.0-0"
private const val CARET: String = "^1.2.3"
private const val PATCH: String = "1.2.3"
private const val WILDCARD_IN_THE_MIDDLE: String = "1.x.3"
private const val LARGEST_MAJOR: String = "${Int.MAX_VALUE}"
private const val LARGEST: String = "$LARGEST_MAJOR.$LARGEST_MAJOR.$LARGEST_MAJOR"
private const val EVERYTHING: String = "(,)"

private val carets: List<Pair<String, String>> = listOf(
    CARET to ">=1.2.3 <2.0.0-0",
    "^0.2.3" to MINOR_ZERO_TWO,
    "^0.0.3" to ">=0.0.3 <0.0.4-0",
    "^1.2.3-beta.2" to ">=1.2.3-beta.2 <2.0.0-0",
    "^0.0.3-beta" to ">=0.0.3-beta <0.0.4-0",
    "^1.2.x" to ">=1.2.0 <2.0.0-0",
    "^0.0.x" to MINOR_ZERO_ZERO,
    "^0.0" to MINOR_ZERO_ZERO,
    "^1.x" to MAJOR_ONE,
    "^0.x" to ">=0.0.0 <1.0.0-0",
)

private val tildes: List<Pair<String, String>> = listOf(
    "~1.2.3" to ">=1.2.3 <1.3.0-0",
    "~1.2" to MINOR_ONE_TWO,
    "~1" to MAJOR_ONE,
    "~0.2.3" to MINOR_ZERO_TWO,
    "~0.2" to ">=0.2.0 <0.3.0-0",
    "~0" to ">=0.0.0 <1.0.0-0",
    "~1.2.3-beta.2" to ">=1.2.3-beta.2 <1.3.0-0",
)

private val xRanges: List<Pair<String, String>> = listOf(
    "1.x" to MAJOR_ONE,
    "1.2.x" to MINOR_ONE_TWO,
    "1.X" to MAJOR_ONE,
    "1.2.*" to MINOR_ONE_TWO,
    "1" to MAJOR_ONE,
    "1.2" to MINOR_ONE_TWO,
    "=1.2" to MINOR_ONE_TWO,
)

private val primitives: List<Pair<String, String>> = listOf(
    ">1" to ">=2.0.0",
    ">1.2" to ">=1.3.0",
    ">=1.2" to ">=1.2.0",
    "<1.2" to "<1.2.0-0",
    "<=1.2" to "<1.3.0-0",
    "<1" to "<1.0.0-0",
    "<=1" to "<2.0.0-0",
    ">= 1.2.3" to ">=1.2.3",
    "=$PATCH" to PATCH,
    "$PATCH+build.5" to PATCH,
)

private val hyphens: List<Pair<String, String>> = listOf(
    "1.2.3 - 2.3.4" to ">=1.2.3 <=2.3.4",
    "1.2 - 2.3.4" to ">=1.2.0 <=2.3.4",
    "1.2.3 - 2.3" to ">=1.2.3 <2.4.0-0",
    "1.2.3 - 2" to ">=1.2.3 <3.0.0-0",
    "* - 2" to "<3.0.0-0",
    "1.2.3 - *" to ">=1.2.3",
)

private val canonical: List<Pair<String, String>> = listOf(
    CARET to "[1.2.3,2.0.0-0)",
    "^0.0.3" to "[0.0.3]",
    "~1.2" to "[1.2.0,1.3.0-0)",
    PATCH to "[$PATCH]",
    ">1.2.3" to "(1.2.3,)",
    "<=1.2.3" to "(,1.2.3]",
    ">1.2.3-alpha.3" to "(1.2.3-alpha.3,)",
    "*" to EVERYTHING,
    "x" to EVERYTHING,
    ">=*" to EVERYTHING,
    "<=*" to EVERYTHING,
    "~*" to EVERYTHING,
    "^x" to EVERYTHING,
    "1.2.7 || >=1.2.9 <2.0.0" to "[1.2.7],[1.2.9,2.0.0)",
    "^1.2 || ^2" to "[1.2.0,2.0.0-0),[2.0.0,3.0.0-0)",
    ">=1 <3 || >=2 <4" to "[1.0.0,4.0.0-0)",
    "<=1.0.0 || >1.0.0" to EVERYTHING,
    "[1.0,2.0) || ^3" to "[1.0.0,2.0.0),[3.0.0,4.0.0-0)",
)

private val refused: List<String> = listOf(
    "1.2.3 ||",
    "|| 1.2.3",
    ">",
    ">= ",
    WILDCARD_IN_THE_MIDDLE,
    "01.2",
    "1.2.3.4",
    ">2 <1",
    "<0.0.0-0",
    ">*",
    "<*",
    "1.2.3 -",
    "- 1.2",
    "1 - 2 - 3",
    ">=1 - 2",
    "v1.2.3",
    "~>1.2",
    "=>1.2",
    "1.2-rc.1",
    "^1.2.3-",
    "1.2.3 || >2 <1",
    "1.a",
    "1.2.3 - 1.a",
    "2 - 1",
    "1.a - 2",
    ">$LARGEST_MAJOR",
    ">$LARGEST",
)

private fun expectDesugaring(table: List<Pair<String, String>>) {
    for ((shorthand, desugared) in table) {
        withClue("$shorthand should mean $desugared") { range(shorthand) shouldBe range(desugared) }
    }
}

class NpmRangeConformanceTest : FunSpec({
    context("each shorthand means what node-semver makes of it") {
        test("for caret ranges") { expectDesugaring(carets) }

        test("for tilde ranges") { expectDesugaring(tildes) }

        test("for x-ranges and partial versions") { expectDesugaring(xRanges) }

        test("for primitive comparators with partial versions") { expectDesugaring(primitives) }

        test("for hyphen ranges") { expectDesugaring(hyphens) }
    }

    context("the canonical form") {
        test("renders each range as its one interval form") {
            for ((written, rendered) in canonical) {
                withClue(written) { range(written).toString() shouldBe rendered }
            }
        }

        test("reads back to an equal range") {
            for ((written, _) in canonical) {
                val range = range(written)
                withClue(written) { range(range.toString()) shouldBe range }
            }
        }
    }

    context("the largest numbers there are") {
        test("leave a caret range unbounded above") {
            range("^$LARGEST") shouldBe range(">=$LARGEST")
        }

        test("leave a tilde range unbounded above") {
            range("~$LARGEST_MAJOR.$LARGEST_MAJOR") shouldBe range(">=$LARGEST_MAJOR.$LARGEST_MAJOR.0")
        }

        test("make an inclusive upper bound hold every version") {
            range("<=$LARGEST") shouldBe SemanticVersionRange.ANY
        }
    }

    context("what is refused") {
        test("everything npm would not read, or reads as matching nothing") {
            for (text in refused) {
                withClue(text) { SemanticVersionRange.parseOrNull(text).shouldBeNull() }
            }
        }

        test("a comparator that is not a version says what was wrong with it") {
            val failure = shouldThrow<IllegalArgumentException> { range(">=01.2") }

            failure.message shouldBe
                "Not a version range: it has a comparator that is not a version: it has a number with a leading zero"
        }

        test("a range that cannot be met says so") {
            val failure = shouldThrow<IllegalArgumentException> { range(">=2 <1") }

            failure.message shouldBe "Not a version range: it has an alternative that matches no version"
        }

        test("a wildcard in the middle is refused rather than read as the line before it") {
            val failure = shouldThrow<IllegalArgumentException> { range(WILDCARD_IN_THE_MIDDLE) }

            failure.message shouldStartWith "Not a version range: it has a comparator that is not a version"
        }

        test("an operator without a version says so") {
            val failure = shouldThrow<IllegalArgumentException> { range(">=") }

            failure.message shouldBe "Not a version range: it has an operator with no version after it"
        }

        test("a malformed hyphen range says so") {
            val failure = shouldThrow<IllegalArgumentException> { range("1 - 2 - 3") }

            failure.message shouldBe "Not a version range: it has a hyphen range that is not written like 1.2 - 1.4"
        }

        test("an empty alternative says so") {
            val failure = shouldThrow<IllegalArgumentException> { range("1.2.3 ||") }

            failure.message shouldBe "Not a version range: it has an empty alternative beside ||"
        }
    }
})
