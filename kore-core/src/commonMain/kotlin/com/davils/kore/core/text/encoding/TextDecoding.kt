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

package com.davils.kore.core.text.encoding

import com.davils.kore.core.Limits
import com.davils.kore.core.annotation.DavilsExperimental
import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.text.internal.textOf

private const val INVALID_REASON: String = "holds a sequence that is no character"
private const val TRUNCATED_REASON: String = "ends in the middle of a character"

/**
 * Reads [bytes] as text in [encoding], strictly, with the same result on every platform.
 *
 * A byte order mark of [encoding] at the start is skipped and reported in [DecodedText.hasByteOrderMark]. Every
 * byte must belong to a well-formed character: an overlong UTF-8 sequence, an encoded surrogate, an unpaired
 * surrogate in UTF-16, a value beyond U+10FFFF and bytes that end in the middle of a character are refused with the
 * offset of the first byte that could not be read, never replaced. Bytes beyond [Limits.MAX_DECODED_TEXT_BYTES] are
 * refused before any is read. The function is pure and safe to call from any thread.
 *
 * It is experimental: its shape may still change.
 *
 * @param bytes the encoded text, which may come from an attacker.
 * @param encoding the encoding to read; detected by [TextEncoding.detect] by default.
 * @return the text, or why it could not be read.
 * @since 2.1.0
 */
@DavilsExperimental
public fun decodeText(
    bytes: ByteArray,
    encoding: TextEncoding = TextEncoding.detect(bytes),
): Outcome<DecodedText, DecodingFailure> {
    if (bytes.size > Limits.MAX_DECODED_TEXT_BYTES) {
        val reason = "is longer than ${Limits.MAX_DECODED_TEXT_BYTES} bytes"
        return Outcome.Failure(DecodingFailure(offset = 0L, encoding = encoding, reason = reason))
    }
    val hasMark = encoding.isMarkedIn(bytes)
    val start = encoding.byteOrderMarkLength.takeIf { hasMark } ?: 0
    return decodeFrom(bytes = bytes, start = start, encoding = encoding, hasMark = hasMark)
}

@OptIn(DavilsExperimental::class)
private fun decodeFrom(
    bytes: ByteArray,
    start: Int,
    encoding: TextEncoding,
    hasMark: Boolean,
): Outcome<DecodedText, DecodingFailure> {
    val text = StringBuilder()
    var failure: DecodingFailure? = null
    var index = start
    while (failure == null && index < bytes.size) {
        val read = readAt(bytes = bytes, index = index, encoding = encoding)
        if (read.isValid) text.append(textOf(read.codePoint))
        failure = failureOf(read = read, index = index, encoding = encoding)
        index += read.width
    }
    val decoded = DecodedText(text = text.toString(), encoding = encoding, hasByteOrderMark = hasMark)
    return failure?.let { found -> Outcome.Failure(found) } ?: Outcome.Success(decoded)
}

@OptIn(DavilsExperimental::class)
private fun readAt(bytes: ByteArray, index: Int, encoding: TextEncoding): CodePointRead = when (encoding) {
    TextEncoding.Utf8 -> utf8At(bytes = bytes, index = index)
    TextEncoding.Utf16Be -> utf16At(bytes = bytes, index = index, isBigEndian = true)
    TextEncoding.Utf16Le -> utf16At(bytes = bytes, index = index, isBigEndian = false)
    TextEncoding.Utf32Be -> utf32At(bytes = bytes, index = index, isBigEndian = true)
    TextEncoding.Utf32Le -> utf32At(bytes = bytes, index = index, isBigEndian = false)
}

@OptIn(DavilsExperimental::class)
private fun failureOf(read: CodePointRead, index: Int, encoding: TextEncoding): DecodingFailure? {
    if (read.isValid) return null
    val reason = TRUNCATED_REASON.takeIf { read.codePoint == CodePointRead.TRUNCATED } ?: INVALID_REASON
    return DecodingFailure(offset = index.toLong(), encoding = encoding, reason = reason)
}
