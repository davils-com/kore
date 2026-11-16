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

import com.davils.kore.core.identity.fixture.V4_TEXT
import com.davils.kore.core.identity.fixture.V7_TEXT
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import java.util.UUID
import kotlin.uuid.Uuid

private const val BATCH: Int = 10_000
private const val VERSION_POSITION: Int = 14
private const val RANDOM: Int = 4
private const val RFC_VARIANT: Int = 2
private const val VERSION_1_HIGH: Long = 0x1000L
private const val VERSION_4_HIGH: Long = 0x4000L
private const val NCS_VARIANT_LOW: Long = 0x0123_4567_89AB_CDEFL

class KoreUuidV4Test : FunSpec({
    context("minting") {
        test("produces the canonical version 4 text form") {
            KoreUuidV4.random().toString() shouldMatch
                Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
        }

        test("reports the random version") {
            KoreUuidV4.random().version shouldBe KoreUuidVersion.Random
            KoreUuidVersion.Random.number shouldBe V4_TEXT[VERSION_POSITION].digitToInt()
        }

        test("never repeats an identifier") {
            List(BATCH) { KoreUuidV4.random() }.toSet() shouldHaveSize BATCH
        }
    }

    context("construction") {
        test("refuses the bits of every other version or variant, also through the constructor Java can call") {
            listOf(
                Uuid.NIL,
                Uuid.fromLongs(mostSignificantBits = VERSION_1_HIGH, leastSignificantBits = Long.MIN_VALUE),
                Uuid.fromLongs(mostSignificantBits = VERSION_4_HIGH, leastSignificantBits = NCS_VARIANT_LOW),
                KoreUuid.parse(V7_TEXT).toKotlinUuid(),
            ).forEach { bits ->
                val failure = shouldThrow<IllegalArgumentException> { KoreUuidV4(bits) }

                failure.message shouldBe "Not a version 4 UUID"
                failure.message.orEmpty() shouldNotContain bits.toString()
            }
        }

        test("holds version 4 in the variant of RFC 9562 whatever bits the constructor is handed") {
            checkAll(Arb.long(), Arb.long()) { high, low ->
                val oracle = UUID(high, low)
                val isVersion4 = oracle.version() == RANDOM && oracle.variant() == RFC_VARIANT

                val built = runCatching {
                    KoreUuidV4(Uuid.fromLongs(mostSignificantBits = high, leastSignificantBits = low))
                }

                built.isSuccess shouldBe isVersion4
                built.onSuccess { uuid -> uuid.toJavaUuid().version() shouldBe RANDOM }
            }
        }
    }

    context("parsing") {
        test("reads its own text form back as itself") {
            val uuid = KoreUuidV4.random()

            KoreUuidV4.parse(uuid.toString()) shouldBe uuid
        }

        test("refuses a version 7 identifier") {
            KoreUuidV4.parseOrNull(V7_TEXT) shouldBe null
            shouldThrow<IllegalArgumentException> { KoreUuidV4.parse(V7_TEXT) }
                .message shouldBe "Not a version 4 UUID in canonical form"
        }
    }
})
