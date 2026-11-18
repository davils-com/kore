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

import com.davils.kore.core.identity.UuidV7
import com.davils.kore.core.identity.UuidVersion
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeStrictlyIncreasing
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlin.time.Instant

private const val BATCH: Int = 1_000
private const val ISSUED: Int = 3
private const val LAST_MILLISECOND: Long = 0xFFFF_FFFF_FFFFL

class SequentialUuidV7GeneratorTest : FunSpec({
    context("minting") {
        test("hands out the same identifiers in the same order every time") {
            val first = SequentialUuidV7Generator()
            val second = SequentialUuidV7Generator()
            val firstRun = List(BATCH) { first.next() }
            val secondRun = List(BATCH) { second.next() }

            firstRun shouldBe secondRun
            firstRun.shouldBeStrictlyIncreasing()
        }

        test("starts at the given instant and moves one millisecond per identifier") {
            val generator = SequentialUuidV7Generator(Instant.parse("2026-01-01T00:00:00Z"))

            generator.next().instant shouldBe Instant.parse("2026-01-01T00:00:00Z")
            generator.next().instant shouldBe Instant.parse("2026-01-01T00:00:00.001Z")
        }

        test("produces valid version 7 identifiers that survive a round trip through text") {
            val uuid = SequentialUuidV7Generator().next()

            uuid.version shouldBe UuidVersion.TimeOrdered
            UuidV7.parse(uuid.toString()) shouldBe uuid
        }

        test("starts every identifier at its start, whatever millisecond of the 48-bit range that is") {
            checkAll(Arb.long(min = 0L, max = LAST_MILLISECOND)) { milliseconds ->
                val uuid = SequentialUuidV7Generator(Instant.fromEpochMilliseconds(milliseconds)).next()

                uuid.epochMilliseconds shouldBe milliseconds
                UuidV7.parse(uuid.toString()) shouldBe uuid
            }
        }

        test("counts what it has issued") {
            val generator = SequentialUuidV7Generator()
            repeat(ISSUED) { generator.next() }

            generator.issued shouldBe ISSUED
        }
    }

    context("start") {
        test("refuses an instant before the epoch") {
            shouldThrow<IllegalArgumentException> {
                SequentialUuidV7Generator(Instant.fromEpochMilliseconds(-1))
            }
        }

        test("refuses an instant beyond the 48-bit range of milliseconds right away") {
            val message = shouldThrow<IllegalArgumentException> {
                SequentialUuidV7Generator(Instant.fromEpochMilliseconds(LAST_MILLISECOND + 1))
            }.message

            message shouldContain "48 bits"
        }

        test("refuses the distant future instead of wrapping around") {
            shouldThrow<IllegalArgumentException> { SequentialUuidV7Generator(Instant.DISTANT_FUTURE) }
        }
    }

    context("end of the range") {
        test("hands out the last millisecond of the range and then refuses with a clear reason") {
            val generator = SequentialUuidV7Generator(Instant.fromEpochMilliseconds(LAST_MILLISECOND))

            generator.next().epochMilliseconds shouldBe LAST_MILLISECOND
            shouldThrow<IllegalStateException> { generator.next() }.message shouldContain "end of the 48-bit range"
        }

        test("stays used up and never counts a refused identifier as issued") {
            val generator = SequentialUuidV7Generator(Instant.fromEpochMilliseconds(LAST_MILLISECOND - 1))
            repeat(2) { generator.next() }

            repeat(ISSUED) { shouldThrow<IllegalStateException> { generator.next() } }

            generator.issued shouldBe 2L
        }
    }
})
