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

@file:OptIn(DavilsExperimental::class)

package com.davils.kore.core.text.encoding

import com.davils.kore.core.annotation.DavilsExperimental
import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.outcome.failureOrNull
import com.davils.kore.core.outcome.getOrNull
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

private const val SAMPLE: String = "aé€😀"
private const val LONGEST: Int = 24
private const val HEX_RADIX: Int = 16
private const val INVALID: String = "holds a sequence that is no character"
private const val TRUNCATED: String = "ends in the middle of a character"

private fun hex(text: String): ByteArray {
    val pairs = text.split(" ").filter { pair -> pair.isNotEmpty() }
    return ByteArray(pairs.size) { index -> pairs[index].toInt(HEX_RADIX).toByte() }
}

private fun failureOf(bytes: ByteArray, encoding: TextEncoding): DecodingFailure {
    val failure = decodeText(bytes, encoding).failureOrNull()
    return failure.shouldNotBeNull()
}

private val utf8Sample: ByteArray = hex("61 C3 A9 E2 82 AC F0 9F 98 80")
private val utf16BeSample: ByteArray = hex("00 61 00 E9 20 AC D8 3D DE 00")
private val utf16LeSample: ByteArray = hex("61 00 E9 00 AC 20 3D D8 00 DE")
private val utf32BeSample: ByteArray = hex("00 00 00 61 00 00 00 E9 00 00 20 AC 00 01 F6 00")
private val utf32LeSample: ByteArray = hex("61 00 00 00 E9 00 00 00 AC 20 00 00 00 F6 01 00")

private val marks: Map<TextEncoding, ByteArray> = mapOf(
    TextEncoding.Utf8 to hex("EF BB BF"),
    TextEncoding.Utf16Be to hex("FE FF"),
    TextEncoding.Utf16Le to hex("FF FE"),
    TextEncoding.Utf32Be to hex("00 00 FE FF"),
    TextEncoding.Utf32Le to hex("FF FE 00 00"),
)

class TextDecodingTest : FunSpec({
    context("detect") {
        test("finds every byte order mark, UTF-32 before UTF-16") {
            TextEncoding.detect(hex("EF BB BF 61")) shouldBe TextEncoding.Utf8
            TextEncoding.detect(hex("FE FF 00 61")) shouldBe TextEncoding.Utf16Be
            TextEncoding.detect(hex("FF FE 61 00")) shouldBe TextEncoding.Utf16Le
            TextEncoding.detect(hex("00 00 FE FF")) shouldBe TextEncoding.Utf32Be
            TextEncoding.detect(hex("FF FE 00 00")) shouldBe TextEncoding.Utf32Le
        }

        test("reads the zero bytes around a first ASCII character without a mark") {
            TextEncoding.detect(hex("00 00 00 61")) shouldBe TextEncoding.Utf32Be
            TextEncoding.detect(hex("61 00 00 00")) shouldBe TextEncoding.Utf32Le
            TextEncoding.detect(hex("00 61")) shouldBe TextEncoding.Utf16Be
            TextEncoding.detect(hex("61 00 62 00")) shouldBe TextEncoding.Utf16Le
            TextEncoding.detect(hex("61 62")) shouldBe TextEncoding.Utf8
            TextEncoding.detect(ByteArray(0)) shouldBe TextEncoding.Utf8
        }
    }

    context("decodeText") {
        test("reads every encoding, with and without its mark") {
            val samples = mapOf(
                TextEncoding.Utf8 to utf8Sample,
                TextEncoding.Utf16Be to utf16BeSample,
                TextEncoding.Utf16Le to utf16LeSample,
                TextEncoding.Utf32Be to utf32BeSample,
                TextEncoding.Utf32Le to utf32LeSample,
            )
            samples.forEach { (encoding, bytes) ->
                val marked = decodeText(marks.getValue(encoding) + bytes)

                decodeText(bytes, encoding) shouldBe Outcome.Success(DecodedText(SAMPLE, encoding, false))
                marked shouldBe Outcome.Success(DecodedText(SAMPLE, encoding, true))
            }
        }

        test("refuses malformed UTF-8 at the first byte it cannot read") {
            failureOf(hex("61 C0 80"), TextEncoding.Utf8).offset shouldBe 1L
            failureOf(hex("E0 80 80"), TextEncoding.Utf8).reason shouldBe INVALID
            failureOf(hex("ED A0 80"), TextEncoding.Utf8).reason shouldBe INVALID
            failureOf(hex("F4 90 80 80"), TextEncoding.Utf8).reason shouldBe INVALID
            failureOf(hex("FF"), TextEncoding.Utf8).reason shouldBe INVALID
            failureOf(hex("E2 41 41"), TextEncoding.Utf8).reason shouldBe INVALID
            failureOf(hex("61 E2 82"), TextEncoding.Utf8).reason shouldBe TRUNCATED
        }

        test("refuses malformed UTF-16") {
            failureOf(hex("00 61 00"), TextEncoding.Utf16Be).reason shouldBe TRUNCATED
            failureOf(hex("D8 00 00 41"), TextEncoding.Utf16Be).reason shouldBe INVALID
            failureOf(hex("DC 00"), TextEncoding.Utf16Be).reason shouldBe INVALID
            failureOf(hex("D8 00"), TextEncoding.Utf16Be).reason shouldBe TRUNCATED
        }

        test("refuses malformed UTF-32") {
            failureOf(hex("00 11 00 00"), TextEncoding.Utf32Be).reason shouldBe INVALID
            failureOf(hex("00 D8 00 00"), TextEncoding.Utf32Le).reason shouldBe INVALID
            failureOf(hex("FF FF FF FF"), TextEncoding.Utf32Be).reason shouldBe INVALID
            failureOf(hex("00 00 00"), TextEncoding.Utf32Be).reason shouldBe TRUNCATED
        }

        test("never throws and points inside the bytes, whatever they are") {
            checkAll(Arb.byteArray(Arb.int(0..LONGEST), Arb.byte()), Arb.enum<TextEncoding>()) { bytes, encoding ->
                val failure = decodeText(bytes, encoding).failureOrNull()

                if (failure != null) failure.offset shouldBeLessThan bytes.size.toLong()
            }
        }
    }

    context("the results") {
        test("describe themselves without the text and compare by content") {
            val decoding = decodeText(utf8Sample)
            val decoded = decoding.getOrNull().shouldNotBeNull()
            val failure = failureOf(hex("FF"), TextEncoding.Utf8)

            decoded.toString() shouldBe "DecodedText(Utf8, 5 characters, without mark)"
            val marked = DecodedText(SAMPLE, TextEncoding.Utf8, true)

            marked.toString() shouldBe "DecodedText(Utf8, 5 characters, with mark)"
            decoded.hashCode() shouldBe DecodedText(SAMPLE, TextEncoding.Utf8, false).hashCode()
            decoded shouldNotBe SAMPLE
            failure.toString() shouldBe "Utf8 at byte 0: $INVALID"
            failure shouldBe DecodingFailure(0L, TextEncoding.Utf8, INVALID)
            failure.hashCode() shouldBe DecodingFailure(0L, TextEncoding.Utf8, INVALID).hashCode()
            failure shouldNotBe INVALID
        }
    }
})
