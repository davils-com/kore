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

package com.davils.kore.testing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.TestTimeSource

private const val START_MILLISECONDS: Long = 1_700_000_000_000L
private const val CENTURIES: Long = 300L * 365L * 24L * 60L

private val start: Instant = Instant.fromEpochMilliseconds(START_MILLISECONDS)

class TestClockTest : FunSpec({
    context("a test clock") {
        test("stands still at its start until it is advanced") {
            val clock = TestClock(start)

            clock.now() shouldBe start
            clock.now() shouldBe start
        }

        test("starts at the epoch by default") {
            TestClock().now() shouldBe Instant.fromEpochMilliseconds(0L)
        }

        test("moves its time and its time source together") {
            val clock = TestClock(start)
            val mark = clock.timeSource.markNow()

            clock.advanceBy(1.seconds)
            clock.advanceBy(Duration.ZERO)

            clock.now() shouldBe start + 1.seconds
            mark.elapsedNow() shouldBe 1.seconds
        }

        test("refuses to go back, to stop and to pass its range") {
            val clock = TestClock(start)

            shouldThrow<IllegalArgumentException> { clock.advanceBy((-1).seconds) }
            shouldThrow<IllegalArgumentException> { clock.advanceBy(Duration.INFINITE) }
            shouldThrow<ArithmeticException> {
                clock.advanceBy((CENTURIES / 2).minutes)
                clock.advanceBy((CENTURIES / 2).minutes)
            }
        }
    }

    context("a clock from a time source") {
        test("shows its origin plus the time the source measured") {
            val source = TestTimeSource()
            val clock = source.asClock(start)

            source += 1.seconds

            clock.now() shouldBe start + 1.seconds
        }
    }
})
