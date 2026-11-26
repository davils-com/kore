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

package com.davils.kore.core.budget

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class DeadlineTest : FunSpec({
    context("a deadline") {
        test("counts down from its budget") {
            val time = TestTimeSource()
            val deadline = Deadline.after(budget = 10.seconds, timeSource = time)

            time += 3.seconds

            deadline.remaining shouldBe 7.seconds
            deadline.isExpired shouldBe false
        }

        test("expires and never goes below zero") {
            val time = TestTimeSource()
            val deadline = Deadline.after(budget = 2.seconds, timeSource = time)

            time += 5.seconds

            deadline.remaining shouldBe Duration.ZERO
            deadline.isExpired shouldBe true
        }

        test("gives a step the shorter of its timeout and the time left") {
            val time = TestTimeSource()
            val deadline = Deadline.after(budget = 10.seconds, timeSource = time)

            deadline.within(4.seconds) shouldBe 4.seconds
            time += 8.seconds
            deadline.within(4.seconds) shouldBe 2.seconds
            deadline.within((-1).seconds) shouldBe Duration.ZERO
        }

        test("keeps a margin back without going below zero") {
            val time = TestTimeSource()
            val deadline = Deadline.after(budget = 10.seconds, timeSource = time)

            val margin = 3.seconds

            deadline.remainingLess(margin) shouldBe 7.seconds
            deadline.remainingLess(30.seconds) shouldBe Duration.ZERO
            deadline.remainingLess(-margin) shouldBe 10.seconds
        }

        test("never ends with an infinite budget") {
            val time = TestTimeSource()
            val deadline = Deadline.after(budget = Duration.INFINITE, timeSource = time)

            time += 1_000.seconds

            deadline.isExpired shouldBe false
            deadline.remaining shouldBe Duration.INFINITE
        }

        test("refuses a negative budget") {
            shouldThrow<IllegalArgumentException> { Deadline.after((-1).seconds) }
        }

        test("shows its budget only") {
            Deadline.after(5.seconds).toString() shouldBe "Deadline(budget=5s)"
        }
    }
})
