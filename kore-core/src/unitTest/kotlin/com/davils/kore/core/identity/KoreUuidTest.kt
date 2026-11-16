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

import com.davils.kore.core.identity.fixture.NIL_TEXT
import com.davils.kore.core.identity.fixture.V1_TEXT
import com.davils.kore.core.identity.fixture.V4_TEXT
import com.davils.kore.core.identity.fixture.V7_TEXT
import com.davils.kore.core.identity.fixture.WRONG_VARIANT_TEXT
import com.davils.kore.core.identity.fixture.v4Of
import com.davils.kore.core.text.fixture.hostileText
import com.davils.kore.core.text.fixture.shouldBeSafeToShow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.checkAll
import java.util.UUID
import kotlin.uuid.Uuid

private const val LONG_INPUT_LENGTH: Int = 200
private const val SHORT_BYTES: Int = 15
private const val HUGE_TAIL_LENGTH: Int = 1_048_576

class KoreUuidTest : FunSpec({
    context("parsing") {
        test("reads a version 4 text form as a version 4 identifier") {
            KoreUuid.parse(V4_TEXT).shouldBeInstanceOf<KoreUuidV4>()
        }

        test("reads a version 7 text form as a version 7 identifier") {
            KoreUuid.parse(V7_TEXT).shouldBeInstanceOf<KoreUuidV7>()
        }

        test("reads uppercase hexadecimal as the same identifier and writes it in lowercase") {
            val parsed = KoreUuid.parse(V7_TEXT.uppercase())

            parsed shouldBe KoreUuid.parse(V7_TEXT)
            parsed.toString() shouldBe V7_TEXT
        }

        context("refuses every text that is not a canonical version 4 or 7 identifier") {
            listOf(
                V1_TEXT,
                WRONG_VARIANT_TEXT,
                NIL_TEXT,
                V4_TEXT.replace(oldValue = "-", newValue = ""),
                V4_TEXT.replace(oldChar = 'a', newChar = 'g'),
                "{$V4_TEXT}",
                "",
            ).forEach { text ->
                test("refuses '$text'") {
                    KoreUuid.parseOrNull(text) shouldBe null
                    shouldThrow<IllegalArgumentException> { KoreUuid.parse(text) }
                }
            }
        }
    }

    context("refusal messages") {
        test("give the length of an input that is too long without quoting it") {
            val input = "x".repeat(LONG_INPUT_LENGTH)

            val message = shouldThrow<IllegalArgumentException> { KoreUuid.parse(input) }.message

            message shouldBe "Not a UUID: 200 characters instead of 36"
            message shouldNotContain "xxx"
        }

        test("name the expected versions for a token of the right length without quoting it") {
            val message = shouldThrow<IllegalArgumentException> { KoreUuid.parse(V1_TEXT) }.message

            message shouldBe "Not a version 4 or version 7 UUID in canonical form"
        }

        test("refuse an identifier with a huge tail by its length alone") {
            val input = V4_TEXT + "0".repeat(HUGE_TAIL_LENGTH)

            KoreUuid.parseOrNull(input) shouldBe null
            shouldThrow<IllegalArgumentException> { KoreUuid.parse(input) }
                .message shouldBe "Not a UUID: ${input.length} characters instead of 36"
        }

        test("never repeat any part of hostile input and stay safe to show") {
            checkAll(hostileText) { text ->
                val message = shouldThrow<IllegalArgumentException> { KoreUuid.parse(text) }.message.orEmpty()

                message shouldBeIn listOf(
                    "Not a UUID: ${text.length} characters instead of 36",
                    "Not a version 4 or version 7 UUID in canonical form",
                )
                message.shouldBeSafeToShow()
            }
        }
    }

    context("bytes") {
        test("round-trip through the text form") {
            val uuid = KoreUuid.parse(V7_TEXT)

            KoreUuid.fromByteArray(uuid.toByteArray()) shouldBe uuid
        }

        test("are a copy, so changing them changes nothing") {
            val uuid = KoreUuid.parse(V4_TEXT)

            uuid.toByteArray().fill(0)

            uuid.toString() shouldBe V4_TEXT
        }

        test("of the wrong length are refused with their length") {
            KoreUuid.fromByteArrayOrNull(ByteArray(SHORT_BYTES)) shouldBe null
            shouldThrow<IllegalArgumentException> { KoreUuid.fromByteArray(ByteArray(SHORT_BYTES)) }
                .message shouldBe "Not a UUID: 15 bytes instead of 16"
        }

        test("of an unsupported version are refused") {
            val bytes = Uuid.parse(V1_TEXT).toByteArray()

            KoreUuid.fromByteArrayOrNull(bytes) shouldBe null
            shouldThrow<IllegalArgumentException> { KoreUuid.fromByteArray(bytes) }
                .message shouldBe "Not a version 4 or version 7 UUID"
        }
    }

    context("interoperability") {
        test("hands out the standard library identifier and reads it back") {
            val uuid = KoreUuid.parse(V7_TEXT)

            KoreUuid.fromKotlinUuid(uuid.toKotlinUuid()) shouldBe uuid
        }

        test("hands out the Java identifier and reads it back") {
            val uuid = KoreUuid.parse(V4_TEXT)

            uuid.toJavaUuid() shouldBe UUID.fromString(V4_TEXT)
            KoreUuid.fromJavaUuid(uuid.toJavaUuid()) shouldBe uuid
        }

        test("refuses a foreign identifier of an unsupported version") {
            shouldThrow<IllegalArgumentException> { KoreUuid.fromJavaUuid(UUID.fromString(V1_TEXT)) }
        }

        test("exposes both halves of the 128 bits") {
            val uuid = KoreUuid.parse(V4_TEXT)
            val java = UUID.fromString(V4_TEXT)

            uuid.mostSignificantBits shouldBe java.mostSignificantBits
            uuid.leastSignificantBits shouldBe java.leastSignificantBits
        }
    }

    context("equality") {
        test("compares the whole value and agrees on the hash") {
            val left = KoreUuid.parse(V4_TEXT)
            val right = KoreUuid.parse(V4_TEXT.uppercase())

            left shouldBe right
            left.hashCode() shouldBe right.hashCode()
        }

        test("never matches an identifier of the other version") {
            KoreUuid.parse(V4_TEXT) shouldNotBe KoreUuid.parse(V7_TEXT)
        }

        test("never matches something that is not an identifier") {
            KoreUuid.parse(V4_TEXT).equals(V4_TEXT) shouldBe false
        }
    }

    context("ordering") {
        test("compares the upper half as unsigned bits") {
            v4Of(high = Long.MIN_VALUE, low = 0) shouldBeGreaterThan v4Of(high = Long.MAX_VALUE, low = 0)
        }

        test("falls back to the lower half as unsigned bits") {
            v4Of(high = 0, low = 1) shouldBeLessThan v4Of(high = 0, low = Long.MAX_VALUE)
        }

        test("treats an identifier as equal to itself") {
            KoreUuid.parse(V7_TEXT).compareTo(KoreUuid.parse(V7_TEXT)) shouldBe 0
        }
    }
})
