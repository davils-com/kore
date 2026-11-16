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

import com.davils.kore.core.version.KoreSemanticVersionRange.Companion.atLeast
import com.davils.kore.core.version.KoreSemanticVersionRange.Companion.below
import com.davils.kore.core.version.KoreSemanticVersionRange.Companion.between
import com.davils.kore.core.version.KoreSemanticVersionRange.Companion.caret
import com.davils.kore.core.version.KoreSemanticVersionRange.Companion.exactly
import com.davils.kore.core.version.KoreSemanticVersionRange.Companion.tilde
import com.davils.kore.core.version.fixture.range
import com.davils.kore.core.version.fixture.version
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll

private const val CASES: Int = 300
private const val HIGHEST_NUMBER: Int = 2
private const val MOST_IDENTIFIERS: Int = 2
private const val LOWEST: String = "0.0.0-0"
private const val LOWEST_PRE_RELEASES: String = ">=0.0.0-0 <0.0.0"
private const val UP_TO_ALPHA: String = "<=0.0.0-alpha"
private const val BELOW_ONE: String = "<1.0.0"
private const val HIGHEST: String = "2147483647.2147483647.2147483647"
private const val CANDIDATE: String = "1.0.0-rc.0"
private const val BEFORE_FLOOR: String = "1.2.4-0"
private const val NEXT_MAJOR: String = "2.0.0"
private const val PATCH: String = "1.2.3"

private val preRelease: Arb<List<String>> = Arb.list(Arb.element("alpha", "0", "1"), 0..MOST_IDENTIFIERS)

private val versions: Arb<KoreSemanticVersion> = arbitrary {
    KoreSemanticVersion(
        major = Arb.int(0..HIGHEST_NUMBER).bind(),
        minor = Arb.int(0..HIGHEST_NUMBER).bind(),
        patch = Arb.int(0..HIGHEST_NUMBER).bind(),
        preRelease = preRelease.bind(),
    )
}

private fun written(range: KoreSemanticVersionRange): String = range.toString()

class KoreSemanticVersionRangeCanonicalTest : FunSpec({
    context("the lowest version there is") {
        test("makes a range without a lower bound equal to one from 0.0.0-0 that holds and admits the same") {
            range("<=$LOWEST") shouldBe range(LOWEST)
            range(UP_TO_ALPHA) shouldBe range(">=0.0.0-0 <=0.0.0-alpha")
            range("$BELOW_ONE || $LOWEST_PRE_RELEASES") shouldBe range(">=0.0.0-0 <1.0.0")
            range("* || $LOWEST_PRE_RELEASES") shouldBe range(">=0.0.0-0")
        }

        test("is the lower bound written when the range admits every pre-release of 0.0.0 it holds") {
            written(range("<=$LOWEST")) shouldBe "[0.0.0-0]"
            written(range(UP_TO_ALPHA)) shouldBe "[0.0.0-0,0.0.0-alpha]"
            written(range("$BELOW_ONE || $LOWEST_PRE_RELEASES")) shouldBe "[0.0.0-0,1.0.0)"
            written(KoreSemanticVersionRange.ANY union range(LOWEST_PRE_RELEASES)) shouldBe "[0.0.0-0,)"
        }

        test("is not written when the range admits only some of the pre-releases of 0.0.0 it holds") {
            written(range(BELOW_ONE)) shouldBe "(,1.0.0)"
            written(range("$BELOW_ONE || $LOWEST")) shouldBe "(,1.0.0),[0.0.0-0]"
            written(KoreSemanticVersionRange.ANY) shouldBe "(,)"
        }

        test("leaves a range that holds and admits the same versions only one canonical form") {
            checkAll(CASES, versions) { upper ->
                val unbounded = range("<=$upper || $LOWEST_PRE_RELEASES")
                val fromLowest = range(">=0.0.0-0 <=$upper || $LOWEST_PRE_RELEASES")

                withClue(upper.toString()) {
                    unbounded shouldBe fromLowest
                    written(unbounded) shouldBe written(fromLowest)
                }
            }
        }

        test("reads back from the canonical form") {
            checkAll(CASES, versions) { upper ->
                val unbounded = range("<=$upper || $LOWEST_PRE_RELEASES")

                withClue(upper.toString()) { range(written(unbounded)) shouldBe unbounded }
            }
        }
    }

    context("the functions that state a range") {
        test("write a plain bound as their documentation shows it") {
            written(between(from = version("1.0.0"), until = version(NEXT_MAJOR))) shouldBe "[1.0.0,2.0.0)"
            written(exactly(version("1.4.0"))) shouldBe "[1.4.0]"
            written(atLeast(version("1.2.0"))) shouldBe "[1.2.0,)"
            written(below(version(NEXT_MAJOR))) shouldBe "(,2.0.0)"
            written(caret(version(PATCH))) shouldBe "[1.2.3,2.0.0-0)"
            written(caret(version("0.2.3"))) shouldBe "[0.2.3,0.3.0-0)"
            written(tilde(version(PATCH))) shouldBe "[1.2.3,1.3.0-0)"
        }

        test("write the canonical form, which states some bounds by an equal one") {
            written(between(from = version("1.0.0"), until = version(BEFORE_FLOOR))) shouldBe "[1.0.0,1.2.3]"
            written(between(from = version(CANDIDATE), until = version(NEXT_MAJOR))) shouldBe "(1.0.0-rc,2.0.0)"
            written(exactly(version(CANDIDATE))) shouldBe "(1.0.0-rc,1.0.0-rc.0]"
            written(exactly(version("1.2.2147483647"))) shouldBe "[1.2.2147483647,1.3.0-0)"
            written(exactly(version(HIGHEST))) shouldBe "[$HIGHEST,)"
            written(atLeast(version(CANDIDATE))) shouldBe "(1.0.0-rc,)"
            written(below(version(BEFORE_FLOOR))) shouldBe "(,1.2.3]"
            written(below(version("0.0.0-alpha"))) shouldBe "[0.0.0-0,0.0.0-alpha)"
            written(caret(version("0.0.3"))) shouldBe "[0.0.3]"
        }

        test("hold the same versions as the bounds they were given") {
            exactly(version(CANDIDATE)).contains(version(CANDIDATE)) shouldBe true
            exactly(version(CANDIDATE)).contains(version("1.0.0-rc")) shouldBe false
            below(version(BEFORE_FLOOR)).contains(version(PATCH)) shouldBe true
            below(version(BEFORE_FLOOR)).contains(version(BEFORE_FLOOR)) shouldBe false
        }
    }

    context("intersecting") {
        test("admits a pre-release that one range alone admits and the other only holds") {
            val narrow = range(">=0.9.0 <1.5.0")
            val shared = range(">=1.0.0-rc.1 <2.0.0") intersect narrow

            shared.contains(version("1.0.0-rc.2")) shouldBe true
            narrow.contains(version("1.0.0-rc.2")) shouldBe false
        }

        test("refuses a pre-release that neither range alone admits, though one names its numbers elsewhere") {
            val named = range(">=1.0.0-rc.1 <1.0.0-rc.2 || >=0.9.0 <2.0.0")
            val shared = named intersect range(">=0.9.0 <2.0.0")
            val later = version("1.0.0-rc.5")

            named.contains(later) shouldBe false
            shared.contains(later) shouldBe false
            shared.contains(later, KorePreReleasePolicy.ByPrecedence) shouldBe true
            shared.contains(version("1.0.0-rc.1")) shouldBe true
        }
    }
})
