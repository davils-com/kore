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

package com.davils.kore.core.identity.monotonic

import com.davils.kore.core.identity.KoreUuid
import com.davils.kore.core.identity.KoreUuidV7
import com.davils.kore.core.identity.generator.fixture.FallingClock
import com.davils.kore.core.identity.generator.fixture.ScriptedClock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeStrictlyIncreasing
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.time.Instant

private const val MAX_TIMESTAMP: Long = 0xFFFF_FFFF_FFFFL
private const val NOW: Long = 1_763_251_200_000L
private const val FAR_BACK: Long = 1_000L
private const val BATCH: Int = 10_000
private const val COUNTER_VALUES: Int = 4_096
private const val LONGEST_SCRIPT: Int = 64
private const val THREADS: Int = 8
private const val PER_THREAD: Int = 5_000
private const val VERSION: Int = 7
private const val RFC_VARIANT: Int = 2

private val timestamps: Arb<Long> = Arb.long(min = 0L, max = MAX_TIMESTAMP)

class MonotonicUuidV7GeneratorTest : FunSpec({
    context("timestamp") {
        test("is the millisecond the injected clock reads") {
            checkAll(timestamps) { milliseconds ->
                val uuid = MonotonicUuidV7Generator(ScriptedClock(listOf(milliseconds))).next()

                uuid.epochMilliseconds shouldBe milliseconds
                uuid.instant shouldBe Instant.fromEpochMilliseconds(milliseconds)
            }
        }

        test("stays at the last millisecond while the clock goes backwards") {
            val readings = listOf(NOW, NOW - 1, NOW - FAR_BACK, 0L, -1L)
            val generator = MonotonicUuidV7Generator(ScriptedClock(readings))

            val minted = List(readings.size) { generator.next() }

            minted.shouldBeStrictlyIncreasing()
            minted.map { uuid -> uuid.epochMilliseconds }.toSet() shouldBe setOf(NOW)
        }

        test("moves on once the clock passes the last millisecond again") {
            val readings = listOf(NOW, NOW - FAR_BACK, NOW + 1)
            val generator = MonotonicUuidV7Generator(ScriptedClock(readings))

            val minted = List(readings.size) { generator.next() }

            minted.shouldBeStrictlyIncreasing()
            minted.last().epochMilliseconds shouldBe NOW + 1
        }

        test("refuses a clock beyond the 48 bits of milliseconds instead of cutting it off") {
            val generator = MonotonicUuidV7Generator(ScriptedClock(listOf(MAX_TIMESTAMP + 1)))

            shouldThrow<IllegalStateException> { generator.next() }.message shouldContain "48 bits"
        }

        test("refuses to wrap around once every identifier of the last millisecond is used up") {
            val generator = MonotonicUuidV7Generator(ScriptedClock(listOf(MAX_TIMESTAMP)))

            val outcomes = List(COUNTER_VALUES + 1) { runCatching { generator.next() } }
            val minted = outcomes.mapNotNull { outcome -> outcome.getOrNull() }

            minted.shouldNotBeEmpty()
            minted.shouldBeStrictlyIncreasing()
            minted.map { uuid -> uuid.epochMilliseconds }.toSet() shouldBe setOf(MAX_TIMESTAMP)
            val refusal = outcomes.last().exceptionOrNull()
            refusal.shouldBeInstanceOf<IllegalStateException>()
        }
    }

    context("ordering") {
        test("never hands out a smaller identifier, however the clock jumps") {
            checkAll(Arb.list(timestamps, range = 1..LONGEST_SCRIPT)) { readings ->
                val generator = MonotonicUuidV7Generator(ScriptedClock(readings))

                List(readings.size) { generator.next() }.shouldBeStrictlyIncreasing()
            }
        }

        test("counts on within one millisecond, also beyond the room one millisecond has") {
            val generator = MonotonicUuidV7Generator(ScriptedClock(listOf(NOW)))

            val minted = List(BATCH) { generator.next() }

            minted.shouldBeStrictlyIncreasing()
            minted.first().epochMilliseconds shouldBe NOW
            minted.minOf { uuid -> uuid.epochMilliseconds } shouldBeGreaterThanOrEqual NOW
        }
    }

    context("bits") {
        test("carry version 7 and the variant of RFC 9562") {
            checkAll(timestamps) { milliseconds ->
                val uuid = MonotonicUuidV7Generator(ScriptedClock(listOf(milliseconds))).next()
                val java = uuid.toJavaUuid()

                java.version() shouldBe VERSION
                java.variant() shouldBe RFC_VARIANT
            }
        }

        test("keep the random part apart from the counter, so identifiers of one millisecond differ in it") {
            val generator = MonotonicUuidV7Generator(ScriptedClock(listOf(NOW)))

            val randomParts = List(BATCH) { generator.next().leastSignificantBits }

            randomParts.toSet() shouldHaveSize BATCH
        }
    }

    context("parsing") {
        test("reads every minted identifier back as itself") {
            checkAll(timestamps) { milliseconds ->
                val uuid = MonotonicUuidV7Generator(ScriptedClock(listOf(milliseconds))).next()

                KoreUuidV7.parse(uuid.toString()) shouldBe uuid
                KoreUuid.parse(uuid.toString()).shouldBeInstanceOf<KoreUuidV7>()
                KoreUuid.fromByteArray(uuid.toByteArray()) shouldBe uuid
                KoreUuid.fromJavaUuid(uuid.toJavaUuid()) shouldBe uuid
            }
        }
    }

    context("minting from several threads at once") {
        test("hands out distinct identifiers, in order per thread, even while the clock goes backwards") {
            val generator = MonotonicUuidV7Generator(FallingClock(NOW))
            val start = CountDownLatch(1)
            val batches = ConcurrentLinkedQueue<List<KoreUuidV7>>()

            val workers = List(THREADS) {
                thread {
                    start.await()
                    batches += List(PER_THREAD) { generator.next() }
                }
            }
            start.countDown()
            workers.forEach(Thread::join)

            batches.flatten().toSet() shouldHaveSize THREADS * PER_THREAD
            batches.forEach { batch -> batch.shouldBeStrictlyIncreasing() }
        }
    }
})
