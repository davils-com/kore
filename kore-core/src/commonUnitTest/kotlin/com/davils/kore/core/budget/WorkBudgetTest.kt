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

@file:OptIn(DavilsInternal::class)

package com.davils.kore.core.budget

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.fixture.hostileText
import com.davils.kore.core.text.fixture.shouldBeSafeToShow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll

private const val LIMIT: String = "maximumQuerySteps"
private const val MAXIMUM: Long = 10L
private const val SEVEN: Long = 7L
private const val THREE: Long = 3L
private const val LARGEST_CHARGE: Long = 20L
private const val MOST_CHARGES: Int = 30

class WorkBudgetTest : FunSpec({
    context("charge") {
        test("takes steps up to the maximum") {
            val budget = WorkBudget(MAXIMUM, LIMIT)
            budget.charge(SEVEN)
            budget.charge(THREE)

            budget.spent shouldBe MAXIMUM
            budget.remaining shouldBe 0L
            budget.isExhausted shouldBe false
        }

        test("charges one step by default") {
            val budget = WorkBudget(MAXIMUM, LIMIT)
            budget.charge()

            budget.spent shouldBe 1L
            budget.remaining shouldBe MAXIMUM - 1L
        }

        test("throws an exception that names the limit and its maximum at the first excess") {
            val budget = WorkBudget(MAXIMUM, LIMIT)
            budget.charge(MAXIMUM)

            val exception = shouldThrow<WorkLimitException> { budget.charge() }

            exception.limit shouldBe LIMIT
            exception.maximum shouldBe MAXIMUM
            exception.message shouldBe "The work needs more than the 10 steps that the limit maximumQuerySteps allows"
            budget.spent shouldBe MAXIMUM
            budget.isExhausted shouldBe true
        }

        test("allows no step for a maximum of zero but a charge of nothing") {
            val budget = WorkBudget(0L, LIMIT)
            budget.charge(0L)

            shouldThrow<WorkLimitException> { budget.charge() }
        }

        test("escapes the name of the limit in its message") {
            checkAll(hostileText) { limit ->
                val budget = WorkBudget(0L, limit)

                val exception = shouldThrow<WorkLimitException> { budget.charge() }

                exception.limit shouldBe limit
                exception.message.orEmpty().shouldBeSafeToShow()
            }
        }
    }

    context("tryCharge") {
        test("reports an excess without throwing and fails every later charge") {
            val budget = WorkBudget(MAXIMUM, LIMIT)

            budget.tryCharge(SEVEN) shouldBe true
            budget.tryCharge(SEVEN) shouldBe false
            budget.tryCharge(1L) shouldBe false
            budget.tryCharge(0L) shouldBe false
            budget.spent shouldBe SEVEN
            budget.remaining shouldBe 0L
            budget.isExhausted shouldBe true
        }

        test("charges one step by default") {
            val budget = WorkBudget(1L, LIMIT)

            budget.tryCharge() shouldBe true
            budget.tryCharge() shouldBe false
            budget.spent shouldBe 1L
        }

        test("never overflows the sum of the steps") {
            val budget = WorkBudget(Long.MAX_VALUE, LIMIT)

            budget.tryCharge(Long.MAX_VALUE) shouldBe true
            budget.tryCharge(Long.MAX_VALUE) shouldBe false
            budget.spent shouldBe Long.MAX_VALUE
        }

        test("refuses a negative number of steps") {
            val budget = WorkBudget(MAXIMUM, LIMIT)

            val exception = shouldThrow<IllegalArgumentException> { budget.tryCharge(-1L) }

            exception.message.orEmpty() shouldContain "-1"
            budget.spent shouldBe 0L
        }

        test("charges exactly as long as the sum stays within the maximum") {
            checkAll(Arb.list(Arb.long(0L..LARGEST_CHARGE), 0..MOST_CHARGES)) { charges ->
                val budget = WorkBudget(MAXIMUM * MAXIMUM, LIMIT)
                var sum = 0L
                var isWithin = true
                charges.forEach { steps ->
                    sum += steps
                    isWithin = isWithin && sum <= budget.maximum
                    budget.tryCharge(steps) shouldBe isWithin
                }

                budget.isExhausted shouldBe !isWithin
                (budget.spent <= budget.maximum) shouldBe true
            }
        }
    }

    context("the constructor") {
        test("refuses a negative maximum") {
            shouldThrow<IllegalArgumentException> { WorkBudget(-1L, LIMIT) }
        }
    }
})
