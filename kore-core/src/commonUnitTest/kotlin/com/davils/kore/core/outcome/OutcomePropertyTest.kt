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

package com.davils.kore.core.outcome

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

private const val SHIFT: Int = 3
private const val FACTOR: Int = 5

private val outcomes: Arb<Outcome<Int, String>> = Arb.bind(
    genA = Arb.boolean(),
    genB = Arb.int(),
    genC = Arb.string(),
    bindFn = ::outcomeOf,
)

private fun outcomeOf(isSuccess: Boolean, value: Int, problem: String): Outcome<Int, String> {
    if (isSuccess) return Outcome.Success(value)
    return Outcome.Failure(problem)
}

private fun shifted(value: Int): Outcome<Int, String> = Outcome.Success(value + SHIFT)

private fun checked(value: Int): Outcome<Int, String> {
    if (value % 2 == 0) return Outcome.Success(value * FACTOR)
    return Outcome.Failure("odd")
}

class OutcomePropertyTest : FunSpec({
    context("map") {
        test("keeps an outcome as it is under the identity") {
            checkAll(outcomes) { outcome -> outcome.map { value -> value } shouldBe outcome }
        }

        test("composes like the functions it applies") {
            checkAll(outcomes) { outcome ->
                val stepwise = outcome.map { value -> value + SHIFT }.map { value -> value * FACTOR }

                stepwise shouldBe outcome.map { value -> (value + SHIFT) * FACTOR }
            }
        }
    }

    context("flatMap") {
        test("continues a success exactly as the operation does") {
            checkAll(Arb.int()) { value -> Outcome.Success(value).flatMap(::checked) shouldBe checked(value) }
        }

        test("is associative") {
            checkAll(outcomes) { outcome ->
                val left = outcome.flatMap(::shifted).flatMap(::checked)
                val right = outcome.flatMap { value -> shifted(value).flatMap(::checked) }

                left shouldBe right
            }
        }
    }

    context("fold") {
        test("agrees with getOrNull and failureOrNull") {
            checkAll(outcomes) { outcome ->
                val folded = outcome.fold(onSuccess = { value -> value.toString() }, onFailure = { problem -> problem })
                val expected = outcome.getOrNull()?.toString() ?: outcome.failureOrNull()

                folded shouldBe expected
            }
        }
    }
})
