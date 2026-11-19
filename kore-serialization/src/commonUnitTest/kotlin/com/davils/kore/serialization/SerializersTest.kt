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

package com.davils.kore.serialization

import com.davils.kore.core.Limits
import com.davils.kore.core.identity.Uuid
import com.davils.kore.core.identity.UuidV4
import com.davils.kore.core.identity.UuidV7
import com.davils.kore.core.size.DataSize
import com.davils.kore.core.size.mebibytes
import com.davils.kore.core.validation.violation.ViolationCode
import com.davils.kore.core.version.SemanticVersion
import com.davils.kore.core.version.SemanticVersionRange
import com.davils.kore.serialization.fixture.SecretSerializer
import com.davils.kore.serialization.fixture.hostileText
import com.davils.kore.serialization.fixture.shouldBeSafeToShow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

private const val SIXTY_FOUR: Int = 64
private const val FIVE: Int = 5
private const val V4: String = "0f1e2d3c-4b5a-4968-8776-655443322110"
private const val V7: String = "0190a8d4-1f3b-7c2e-9a4d-3b5e6f708192"
private const val SECRET: String = "hunter"

private fun <T> roundTrip(serializer: KSerializer<T>, value: T): T =
    Json.decodeFromString(serializer, Json.encodeToString(serializer, value))

private fun <T> refusalOf(serializer: KSerializer<T>, text: String): String {
    val failure = shouldThrow<SerializationException> { Json.decodeFromString(serializer, Json.encodeToString(text)) }
    return failure.message.orEmpty()
}

class SerializersTest : FunSpec({
    context("durations") {
        test("are written as Kotlin writes them and read back") {
            Json.encodeToString(DurationSerializer, 90.minutes) shouldBe "\"1h 30m\""
            roundTrip(DurationSerializer, 45.seconds) shouldBe 45.seconds
            Json.decodeFromString(DurationSerializer, "\"PT1H30M\"") shouldBe 90.minutes
            Json.decodeFromString(DurationSerializer, "\"-5s\"") shouldBe -FIVE.seconds
        }

        test("read back every finite duration they write") {
            checkAll(Arb.long()) { nanoseconds ->
                val duration = nanoseconds.nanoseconds

                roundTrip(DurationSerializer, duration) shouldBe duration
            }
        }

        test("refuse an infinite duration both ways") {
            shouldThrow<SerializationException> { Json.encodeToString(DurationSerializer, Duration.INFINITE) }
            refusalOf(DurationSerializer, "Infinity") shouldContain "is not a finite duration"
        }

        test("refuse text that is no duration, too long or outside printable ASCII") {
            refusalOf(DurationSerializer, "soon")
            refusalOf(DurationSerializer, "1".repeat(Limits.MAX_NUMERIC_TEXT_LENGTH) + "s")
            refusalOf(DurationSerializer, "1\u00A0s")
            refusalOf(DurationSerializer, "1\ts")
        }
    }

    context("the values of Kore") {
        test("are written as their text and read back") {
            roundTrip(DataSizeSerializer, SIXTY_FOUR.mebibytes) shouldBe SIXTY_FOUR.mebibytes
            Json.encodeToString(DataSizeSerializer, SIXTY_FOUR.mebibytes) shouldBe "\"64 MiB\""
            roundTrip(SemanticVersionSerializer, SemanticVersion.parse("2.1.0-rc.1")) shouldBe
                SemanticVersion.parse("2.1.0-rc.1")
            roundTrip(SemanticVersionRangeSerializer, SemanticVersionRange.parse(">=2.0.0 <3.0.0")) shouldBe
                SemanticVersionRange.parse(">=2.0.0 <3.0.0")
            roundTrip(UuidSerializer, Uuid.parse(V7)) shouldBe Uuid.parse(V7)
            roundTrip(UuidV4Serializer, UuidV4.parse(V4)) shouldBe UuidV4.parse(V4)
            roundTrip(UuidV7Serializer, UuidV7.parse(V7)) shouldBe UuidV7.parse(V7)
            roundTrip(ViolationCodeSerializer, ViolationCode.INVALID) shouldBe ViolationCode.INVALID
        }

        test("refuse text that is none of them") {
            refusalOf(DataSizeSerializer, "1 GB") shouldContain "is not a data size"
            refusalOf(SemanticVersionSerializer, "2.1") shouldContain "is not a semantic version"
            refusalOf(SemanticVersionRangeSerializer, ">=") shouldContain "is not a version range"
            refusalOf(UuidSerializer, "not-a-uuid") shouldContain "is not a UUID"
            refusalOf(UuidV4Serializer, V7) shouldContain "is not a version 4 UUID"
            refusalOf(UuidV7Serializer, V4) shouldContain "is not a version 7 UUID"
            refusalOf(ViolationCodeSerializer, "Kore") shouldContain "is not a violation code"
        }

        test("are found as contextual serializers") {
            coreSerializers.getContextual(Duration::class) shouldBe DurationSerializer
            coreSerializers.getContextual(DataSize::class) shouldBe DataSizeSerializer
            coreSerializers.getContextual(ViolationCode::class) shouldBe ViolationCodeSerializer
        }
    }

    context("a refused text") {
        test("is escaped in the message") {
            checkAll(hostileText) { text ->
                val message = refusalOf(ViolationCodeSerializer, text)

                message.shouldBeSafeToShow()
            }
        }

        test("of a sensitive serializer is never shown") {
            val message = refusalOf(SecretSerializer, "$SECRET!")

            message shouldNotContain SECRET
            message shouldBe "**** is not a secret"
            roundTrip(SecretSerializer, SECRET) shouldBe SECRET
        }

        test("longer than the limit is refused before it is read") {
            refusalOf(SecretSerializer, "abcdefghi") shouldBe "**** is not a secret"
        }
    }
})
