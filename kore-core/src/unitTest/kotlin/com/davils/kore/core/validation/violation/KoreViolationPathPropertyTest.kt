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

package com.davils.kore.core.validation.violation

import com.davils.kore.core.validation.violation.fixture.PathStep
import com.davils.kore.core.validation.violation.fixture.pathOf
import com.davils.kore.core.validation.violation.fixture.stepsOf
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

private const val CASES: Int = 300
private const val MAX_LENGTH: Int = 10
private const val MAX_INDEX: Int = 999
private const val MAX_STEPS: Int = 8

private val names: Arb<String> = Arb.string(minSize = 1, maxSize = MAX_LENGTH, codepoints = Codepoint.az())

private val keys: Arb<String> = Arb.string(minSize = 1, maxSize = MAX_LENGTH, codepoints = Codepoint.alphanumeric())

private val steps: Arb<PathStep> = Arb.choice(
    names.map(PathStep::Named),
    Arb.int(min = 0, max = MAX_INDEX).map(PathStep::At),
    keys.map(PathStep::Keyed),
)

class KoreViolationPathPropertyTest : FunSpec({
    context("building") {
        test("keeps every appended segment in the order it was appended") {
            checkAll(CASES, Arb.list(steps, 0..MAX_STEPS)) { appended ->
                val path = pathOf(appended)

                withClue(path.toString()) {
                    path.segments.map(Any::toString) shouldBe appended.map { it.rendered }
                }
            }
        }

        test("is the root exactly when nothing was appended") {
            checkAll(CASES, Arb.list(steps, 0..MAX_STEPS)) { appended ->
                pathOf(appended).isRoot shouldBe appended.isEmpty()
            }
        }

        test("appends a whole path as its segments") {
            checkAll(CASES, Arb.list(steps, 0..MAX_STEPS), Arb.list(steps, 0..MAX_STEPS)) { outer, inner ->
                pathOf(outer) + pathOf(inner) shouldBe pathOf(outer + inner)
            }
        }
    }

    context("rendering") {
        test("reads back as the segments that went into it") {
            checkAll(CASES, Arb.list(steps, 0..MAX_STEPS)) { appended ->
                val rendered = pathOf(appended).toString()

                withClue(rendered) { stepsOf(rendered) shouldBe appended }
            }
        }

        test("never leads with a separator and never doubles one") {
            checkAll(CASES, Arb.list(steps, 0..MAX_STEPS)) { appended ->
                val rendered = pathOf(appended).toString()

                withClue(rendered) {
                    rendered.startsWith(".") shouldBe false
                    rendered.contains("..") shouldBe false
                    rendered.contains(".[") shouldBe false
                }
            }
        }
    }
})
