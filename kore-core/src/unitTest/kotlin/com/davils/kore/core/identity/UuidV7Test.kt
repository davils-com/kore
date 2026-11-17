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

package com.davils.kore.core.identity

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeStrictlyIncreasing
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid as KotlinUuid

private const val BATCH: Int = 10_000
private const val TIME_ORDERED: Int = 7
private const val RFC_VARIANT: Int = 2
private const val VERSION_1_HIGH: Long = 0x1000L
private const val VERSION_7_HIGH: Long = 0x7000L
private const val NCS_VARIANT_LOW: Long = 0x0123_4567_89AB_CDEFL

class UuidV7Test : FunSpec({
    context("minting") {
        test("produces the canonical version 7 text form") {
            UuidV7.random().toString() shouldMatch
                Regex("[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
        }

        test("reports the time-ordered version") {
            UuidV7.random().version shouldBe UuidVersion.TimeOrdered
        }

        test("records the moment it was minted") {
            val before = Clock.System.now().toEpochMilliseconds()
            val uuid = UuidV7.random()
            val after = Clock.System.now().toEpochMilliseconds()

            uuid.epochMilliseconds shouldBeInRange before..after
        }

        test("orders every identifier after the ones minted before it, also within one millisecond") {
            List(BATCH) { UuidV7.random() }.shouldBeStrictlyIncreasing()
        }
    }

    context("timestamp") {
        test("reads the milliseconds from the leading 48 bits") {
            UuidV7.parse(V7_TEXT).epochMilliseconds shouldBe V7_EPOCH_MILLISECONDS
        }

        test("is available as an instant") {
            UuidV7.parse(V7_TEXT).instant shouldBe Instant.parse("2022-02-22T19:22:22Z")
        }
    }

    context("construction") {
        test("refuses the bits of every other version or variant, also through the constructor Java can call") {
            listOf(
                KotlinUuid.NIL,
                KotlinUuid.fromLongs(mostSignificantBits = VERSION_1_HIGH, leastSignificantBits = Long.MIN_VALUE),
                KotlinUuid.fromLongs(mostSignificantBits = VERSION_7_HIGH, leastSignificantBits = NCS_VARIANT_LOW),
                Uuid.parse(V4_TEXT).toKotlinUuid(),
            ).forEach { bits ->
                val failure = shouldThrow<IllegalArgumentException> { UuidV7(bits) }

                failure.message shouldBe "Not a version 7 UUID"
                failure.message.orEmpty() shouldNotContain bits.toString()
            }
        }

        test("holds version 7 in the variant of RFC 9562 whatever bits the constructor is handed") {
            checkAll(Arb.long(), Arb.long()) { high, low ->
                val oracle = UUID(high, low)
                val isVersion7 = oracle.version() == TIME_ORDERED && oracle.variant() == RFC_VARIANT

                val built = runCatching {
                    UuidV7(KotlinUuid.fromLongs(mostSignificantBits = high, leastSignificantBits = low))
                }

                built.isSuccess shouldBe isVersion7
                built.onSuccess { uuid -> uuid.toJavaUuid().version() shouldBe TIME_ORDERED }
            }
        }
    }

    context("parsing") {
        test("reads its own text form back as itself") {
            val uuid = UuidV7.random()

            UuidV7.parse(uuid.toString()) shouldBe uuid
        }

        test("refuses a version 4 identifier") {
            UuidV7.parseOrNull(V4_TEXT) shouldBe null
            shouldThrow<IllegalArgumentException> { UuidV7.parse(V4_TEXT) }
                .message shouldBe "Not a version 7 UUID in canonical form"
        }
    }
})
