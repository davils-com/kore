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

import com.davils.kore.core.Limits
import com.davils.kore.core.annotation.DavilsExperimental
import com.davils.kore.core.outcome.failureOrNull
import com.davils.kore.core.outcome.getOrNull
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

private const val LONGEST: Int = 24
private const val ITERATIONS: Int = 5_000

private val charsets: Map<TextEncoding, Charset> = mapOf(
    TextEncoding.Utf8 to Charsets.UTF_8,
    TextEncoding.Utf16Be to Charsets.UTF_16BE,
    TextEncoding.Utf16Le to Charsets.UTF_16LE,
    TextEncoding.Utf32Be to Charset.forName("UTF-32BE"),
    TextEncoding.Utf32Le to Charset.forName("UTF-32LE"),
)

private fun strictlyDecoded(bytes: ByteArray, charset: Charset): String? {
    val decoder = charset.newDecoder()
    decoder.onMalformedInput(CodingErrorAction.REPORT)
    decoder.onUnmappableCharacter(CodingErrorAction.REPORT)
    return try {
        decoder.decode(ByteBuffer.wrap(bytes)).toString()
    } catch (_: CharacterCodingException) {
        null
    }
}

class TextDecodingJvmTest : FunSpec({
    context("decodeText against the decoders of the JDK") {
        test("reads back any text that the JDK encodes") {
            checkAll(Arb.string(0..LONGEST)) { text ->
                charsets.forEach { (encoding, charset) ->
                    val wellFormed = strictlyDecoded(text.toByteArray(charset), charset) ?: return@forEach
                    val decoded = decodeText(text.toByteArray(charset), encoding).getOrNull()

                    decoded?.text shouldBe wellFormed
                }
            }
        }

        test("accepts exactly the bytes that the strict decoder of the JDK accepts") {
            checkAll(iterations = ITERATIONS, genA = Arb.byteArray(Arb.int(0..LONGEST), Arb.byte())) { bytes ->
                charsets.forEach { (encoding, charset) ->
                    val expected = strictlyDecoded(bytes, charset)
                    val isMarked = encoding.isMarkedIn(bytes) && expected?.startsWith("\uFEFF") == true
                    val decoded = decodeText(bytes, encoding).getOrNull()?.text

                    decoded?.let { text -> text.takeUnless { isMarked } ?: "\uFEFF$text" } shouldBe expected
                }
            }
        }
    }

    context("too many bytes") {
        test("are refused before any is read") {
            val bytes = ByteArray(Limits.MAX_DECODED_TEXT_BYTES + 1)

            val failure = decodeText(bytes, TextEncoding.Utf8).failureOrNull()

            failure.shouldNotBeNull().offset shouldBe 0L
        }
    }
})
