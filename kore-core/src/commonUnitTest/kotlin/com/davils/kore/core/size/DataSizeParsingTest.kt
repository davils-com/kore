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

package com.davils.kore.core.size

import com.davils.kore.core.Limits
import com.davils.kore.core.text.fixture.hostileText
import com.davils.kore.core.text.fixture.shouldBeSafeToShow
import com.davils.kore.core.validation.validate
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val SIXTY_FOUR: Int = 64
private const val HALF: Int = 512
private const val SEVEN: Long = 7L
private const val PARTS: Long = 3L
private const val LONGEST_HOSTILE: Int = 12
private const val NO_NUMBER: String = "does not start with a number of bytes"
private const val NO_UNIT: String = "has no unit"
private const val TOO_LARGE: String = "is larger than the largest data size"

private val pieces: List<String> =
    listOf("1", "0", "9", " ", "\t", "\u00A0", "KiB", "kib", "MB", "B", "-", ".", "e", "\u0663")

private val adversarial: Arb<String> =
    Arb.list(Arb.element(pieces), 0..LONGEST_HOSTILE).map { parts -> parts.joinToString(separator = "") }

private fun reasonOf(text: String): String {
    val failure = shouldThrow<IllegalArgumentException> { DataSize.parse(text) }
    return failure.message.orEmpty()
}

class DataSizeParsingTest : FunSpec({
    context("parse") {
        test("reads every binary unit with and without a space") {
            DataSize.parse("64 MiB") shouldBe SIXTY_FOUR.mebibytes
            DataSize.parse("512KiB") shouldBe HALF.kibibytes
            DataSize.parse("0 B") shouldBe DataSize.ZERO
            DataSize.parse("1 TiB") shouldBe 1.tebibytes
            DataSize.parse("1 PiB") shouldBe DataSize(DataSize.PEBIBYTE)
            DataSize.parse("7 EiB") shouldBe DataSize(SEVEN * DataSize.EXBIBYTE)
        }

        test("reads back every size it shows") {
            checkAll(Arb.long(min = 0L, max = Long.MAX_VALUE)) { bytes ->
                val size = DataSize(bytes)

                DataSize.parse(size.toString()) shouldBe size
            }
        }

        test("refuses decimal, abbreviated and wrongly cased units") {
            listOf("1 GB", "1 kB", "1 k", "1 kib", "1 mb").forEach { text ->
                reasonOf(text) shouldContain "decimal, abbreviated or in the wrong case"
            }
        }

        test("says why it refuses a text") {
            reasonOf("MiB") shouldContain NO_NUMBER
            reasonOf("-1 B") shouldContain NO_NUMBER
            reasonOf("01 B") shouldContain "has a number with a leading zero"
            reasonOf("1") shouldContain NO_UNIT
            reasonOf("1  B") shouldContain NO_UNIT
            reasonOf("1.5 MiB") shouldContain NO_UNIT
            reasonOf("8 EiB") shouldContain TOO_LARGE
            reasonOf("99999999999999999999 B") shouldContain TOO_LARGE
            reasonOf("1".repeat(Limits.MAX_DATA_SIZE_TEXT_LENGTH + 1)) shouldContain "is longer than 32 characters"
        }

        test("never throws from parseOrNull, whatever the text") {
            checkAll(adversarial) { text -> DataSize.parseOrNull(text) }
            DataSize.parseOrNull("1 GB").shouldBeNull()
            DataSize.parseOrNull("1 GiB") shouldBe 1.gibibytes
        }

        test("escapes a hostile text in its message") {
            checkAll(hostileText) { text ->
                val failure = shouldThrow<IllegalArgumentException> { DataSize.parse("x$text") }

                failure.message.orEmpty().shouldBeSafeToShow()
            }
        }
    }

    context("division") {
        test("divides into parts, rounding down") {
            1.kibibytes / PARTS shouldBe DataSize(DataSize.KIBIBYTE / PARTS)
            shouldThrow<IllegalArgumentException> { 1.kibibytes / 0L }
        }

        test("measures one size in another") {
            1.mebibytes / 1.kibibytes shouldBe DataSize.KIBIBYTE
            shouldThrow<ArithmeticException> { 1.mebibytes / DataSize.ZERO }
        }
    }

    context("long units") {
        test("convert without overflow and refuse what does not fit") {
            1L.kibibytes shouldBe 1.kibibytes
            1L.mebibytes shouldBe 1.mebibytes
            1L.gibibytes shouldBe 1.gibibytes
            1L.tebibytes shouldBe 1.tebibytes
            shouldThrow<ArithmeticException> { Long.MAX_VALUE.kibibytes }
            shouldThrow<IllegalArgumentException> { (-1L).kibibytes }
        }
    }

    context("validation") {
        test("reads a valid size and passes a positive one") {
            val upload = Upload(limit = "8 MiB", chunk = 1.kibibytes)

            upload.validate("upload").isValid shouldBe true
            upload.read shouldBe 8.mebibytes
        }

        test("reports a text that is no size and a size of nothing") {
            val upload = Upload(limit = "8 MB", chunk = DataSize.ZERO)

            val codes = upload.validate("upload").violations.map { violation -> violation.code }

            upload.read.shouldBeNull()
            codes shouldBe listOf(SizeViolationCodes.INVALID_SIZE, ViolationCode.OUT_OF_RANGE)
        }
    }
})
