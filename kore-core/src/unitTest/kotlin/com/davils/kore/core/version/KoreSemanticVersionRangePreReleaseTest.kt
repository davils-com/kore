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
import io.kotest.matchers.shouldBe

private const val CANDIDATE: String = "1.0.0-rc.1"
private const val LATER_CANDIDATE: String = "1.0.0-rc.5"
private const val NAMED_AND_WIDE: String = "$CANDIDATE || 0.9.0 - 1.0.0"
private const val NARROW_AND_WIDE: String = ">=$CANDIDATE <1.0.0-rc.2 || >=0.9.0 <2.0.0"
private const val WIDE: String = "0.9.0 - 1.0.0"
private const val FLOOR_AND_CANDIDATE: String = ">1.2.3 || 1.2.4-rc.1"

class KoreSemanticVersionRangePreReleaseTest : FunSpec({
    context("alternatives that name different pre-releases") {
        test("keep the pre-release one of them names, after they are merged") {
            val range = range(NAMED_AND_WIDE)

            range.contains(version(CANDIDATE)) shouldBe true
            range.contains(version("1.0.0")) shouldBe true
            range.contains(version("1.0.0-rc.2")) shouldBe false
        }

        test("keep that pre-release as an interval of its own") {
            range(NAMED_AND_WIDE).toString() shouldBe "[0.9.0,1.0.0],[$CANDIDATE]"
        }

        test("admit only the pre-releases the alternative that holds them names") {
            val range = range(NARROW_AND_WIDE)

            range.contains(version(CANDIDATE)) shouldBe true
            range.contains(version(LATER_CANDIDATE)) shouldBe false
            range.contains(version(LATER_CANDIDATE), KorePreReleasePolicy.ByPrecedence) shouldBe true
            range.toString() shouldBe "[0.9.0,2.0.0),[$CANDIDATE,1.0.0-rc.2)"
        }

        test("choose a named pre-release as the newest version inside") {
            range(NAMED_AND_WIDE).maxSatisfying(versions("0.9.5", CANDIDATE, "1.0.0-rc.2")) shouldBe version(CANDIDATE)
        }

        test("write a pre-release floor that only part of the range names as an exclusive release") {
            val range = range(FLOOR_AND_CANDIDATE)

            range.toString() shouldBe "(1.2.3,),[1.2.4-rc.1]"
            range.contains(version("1.2.4-rc.1")) shouldBe true
            range.contains(version("1.2.4-beta")) shouldBe false
        }

        test("write that floor as the largest release below it, even where the release below it carries") {
            val largest = Int.MAX_VALUE

            range(">1.2.$largest || 1.3.0-rc.1").toString() shouldBe "(1.2.$largest,),[1.3.0-rc.1]"
            range(">1.$largest.$largest || 2.0.0-rc.1").toString() shouldBe "(1.$largest.$largest,),[2.0.0-rc.1]"
        }
    }

    context("uniting") {
        test("keeps a pre-release either side names") {
            val united = range(CANDIDATE) union range(WIDE)

            united shouldBe range(NAMED_AND_WIDE)
            united.contains(version(CANDIDATE)) shouldBe true
        }

        test("gives the same range however the parts are grouped") {
            val first = range("[0.9.0,1.0.0-rc.2)")
            val second = range("[$CANDIDATE,$LATER_CANDIDATE)")
            val third = range("[1.0.0-rc.3,2.0.0)")

            val unitedFirst = first union second
            val unitedLast = second union third

            val united = unitedFirst union third

            united shouldBe (first union unitedLast)
            united.toString() shouldBe "[0.9.0,2.0.0),[1.0.0-0,1.0.0)"
        }
    }

    context("intersecting") {
        test("keeps a pre-release either side names, as one comparator set does") {
            val shared = range(">=$CANDIDATE <2") intersect range(">=0.5 <3")

            shared shouldBe range(">=$CANDIDATE <2 >=0.5 <3")
            shared.contains(version(LATER_CANDIDATE)) shouldBe true
        }
    }
})
