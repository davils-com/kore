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
import com.davils.kore.core.version.fixture.version
import com.davils.kore.core.version.fixture.versions
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val MINOR_LINE: String = "^1.2"
private const val MAJOR_LINE: String = "^1"
private const val BETA: String = "1.3.0-beta"
private const val NEWEST: String = "1.2.4"
private const val OLDEST: String = "1.2.3"
private const val ABOVE_OLDEST: String = ">1.2.3"
private const val CANDIDATE: String = "2.0.0-rc.1"
private const val LAST_BUILD: String = "$NEWEST+c"

private val published: List<SemanticVersion> = versions(OLDEST, NEWEST, BETA, "2.0.0", "1.1.0")

class SemanticVersionRangeAlgebraTest : FunSpec({
    context("intersecting") {
        test("keeps what both ranges allow") {
            range(MINOR_LINE) intersect range("<1.5") shouldBe range(">=1.2.0 <1.5")
        }

        test("of two ranges nothing satisfies is empty") {
            val both = range(MAJOR_LINE) intersect range("^2")

            both.isEmpty shouldBe true
            both shouldBe SemanticVersionRange.NONE
        }

        test("with ANY changes nothing") {
            range("^1.2 || ^3") intersect SemanticVersionRange.ANY shouldBe range("^1.2 || ^3")
        }

        test("cuts every interval of a union") {
            val cut = range("^1 || ^3") intersect range(">=1.5 <3.5")

            cut shouldBe range(">=1.5.0 <2.0.0-0 || >=3.0.0 <3.5")
        }

        test("keeps the exclusive one of two lower bounds at the same version") {
            range(ABOVE_OLDEST) intersect range(">=1.2.3") shouldBe range(ABOVE_OLDEST)
            range(">=1.2.3") intersect range(ABOVE_OLDEST) shouldBe range(ABOVE_OLDEST)
        }
    }

    context("uniting") {
        test("merges intervals that overlap") {
            range(MINOR_LINE) union range("~1.9 || <1.3") shouldBe range("<2.0.0-0")
        }

        test("merges intervals that only touch") {
            range("[1,2)") union range("[2,3)") shouldBe range("[1,3)")
        }

        test("keeps intervals with versions between them apart") {
            val united = range(MAJOR_LINE) union range("^2")

            united.intervals.size shouldBe 2
            united.contains(version(CANDIDATE), PreReleasePolicy.ByPrecedence) shouldBe false
        }

        test("with NONE changes nothing") {
            range(MINOR_LINE) union SemanticVersionRange.NONE shouldBe range(MINOR_LINE)
        }

        test("with a range unbounded above is unbounded above") {
            range(MINOR_LINE) union range(">=1.5") shouldBe range(">=1.2")
        }
    }

    context("pre-release policies") {
        test("by precedence, a pre-release is wherever its precedence puts it") {
            range("^1.2.0").contains(version("1.5.0-beta"), PreReleasePolicy.ByPrecedence) shouldBe true
        }

        test("same core only admits a pre-release a bound names, as npm does") {
            val range = range(">1.2.3-alpha.3")

            range.contains(version("1.2.3-alpha.7"), PreReleasePolicy.SameCoreOnly) shouldBe true
            range.contains(version("3.4.5-alpha.9"), PreReleasePolicy.SameCoreOnly) shouldBe false
            range.contains(version("3.4.5"), PreReleasePolicy.SameCoreOnly) shouldBe true
        }

        test("same core only is decided by the interval that holds the version") {
            val range = range(">=1.0.0-rc.1 <1.1 || >=2.0.0 <3")

            range.contains(version("1.0.0-rc.2"), PreReleasePolicy.SameCoreOnly) shouldBe true
            range.contains(version("2.1.0-rc.2"), PreReleasePolicy.SameCoreOnly) shouldBe false
        }

        test("same core only tells versions apart by every number of the core") {
            val range = range(">=1.2.3-rc.1")

            range.contains(version("1.3.3-rc.2"), PreReleasePolicy.SameCoreOnly) shouldBe false
            range.contains(version("1.2.4-rc.2"), PreReleasePolicy.SameCoreOnly) shouldBe false
            range.contains(version("2.2.3-rc.2"), PreReleasePolicy.SameCoreOnly) shouldBe false
        }

        test("an upper bound names a pre-release as well") {
            range("<1.2.3-rc.1").contains(version("1.2.3-beta"), PreReleasePolicy.SameCoreOnly) shouldBe true
        }

        test("the -0 floor a shorthand adds admits nothing") {
            range("^1.2.3").contains(version(CANDIDATE), PreReleasePolicy.SameCoreOnly) shouldBe false
        }

        test("releases only never admits a pre-release") {
            range(">=1.2.3-rc.1").contains(version("1.2.3-rc.2"), PreReleasePolicy.ReleasesOnly) shouldBe false
        }

        test("a version outside is outside under every policy") {
            for (policy in PreReleasePolicy.entries) {
                range(MINOR_LINE).contains(version(CANDIDATE), policy) shouldBe false
            }
        }

        test("a release is placed by precedence under every policy") {
            for (policy in PreReleasePolicy.entries) {
                range(MINOR_LINE).contains(version("1.4.0"), policy) shouldBe true
                range(MINOR_LINE).contains(version("2.0.0"), policy) shouldBe false
            }
        }
    }

    context("selecting") {
        test("chooses the newest version inside, without a pre-release nobody asked for") {
            range(MINOR_LINE).maxSatisfying(published) shouldBe version(NEWEST)
        }

        test("chooses the newest version inside by precedence, when asked") {
            range(MINOR_LINE).maxSatisfying(published, PreReleasePolicy.ByPrecedence) shouldBe version(BETA)
        }

        test("chooses the oldest version inside") {
            range(MINOR_LINE).minSatisfying(published) shouldBe version(OLDEST)
        }

        test("chooses nothing when no version fits") {
            val later = range("^3")

            later.maxSatisfying(published).shouldBeNull()
            later.minSatisfying(published).shouldBeNull()
        }

        test("keeps every version inside, in the order given") {
            range(MAJOR_LINE).satisfying(published) shouldContainExactly versions(OLDEST, NEWEST, "1.1.0")
        }

        test("gives the same answer among builds of one version, whatever order they come in") {
            val builds = versions("$NEWEST+b", "$NEWEST+a", LAST_BUILD)

            range(MAJOR_LINE).maxSatisfying(builds) shouldBe version(LAST_BUILD)
            range(MAJOR_LINE).maxSatisfying(builds.reversed()) shouldBe version(LAST_BUILD)
            range(MAJOR_LINE).minSatisfying(builds) shouldBe version("$NEWEST+a")
        }
    }
})
