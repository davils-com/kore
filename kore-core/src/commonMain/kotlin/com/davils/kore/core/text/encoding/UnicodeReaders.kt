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

private const val CONTINUATION_BITS: Int = 6
private const val CONTINUATION_MASK: Int = 0x3F
private const val CONTINUATION_TAG: Int = 0x80
private const val CONTINUATION_TAG_MASK: Int = 0xC0
private const val LAST_ONE_BYTE: Int = 0x7F
private const val FIRST_TWO_BYTES: Int = 0xC2
private const val FIRST_THREE_BYTES: Int = 0xE0
private const val FIRST_FOUR_BYTES: Int = 0xF0
private const val LAST_FOUR_BYTES: Int = 0xF4
private const val TWO_BYTES_PAYLOAD: Int = 0x1F
private const val THREE_BYTES_PAYLOAD: Int = 0x0F
private const val FOUR_BYTES_PAYLOAD: Int = 0x07
private const val TWO: Int = 2
private const val THREE: Int = 3
private const val FOUR: Int = 4
private const val BYTE_BITS: Int = 8
private const val SMALLEST_THREE_BYTES: Int = 0x800
private const val SMALLEST_FOUR_BYTES: Int = 0x10000
private const val FIRST_SURROGATE: Int = 0xD800
private const val LAST_HIGH_SURROGATE: Int = 0xDBFF
private const val FIRST_LOW_SURROGATE: Int = 0xDC00
private const val LAST_SURROGATE: Int = 0xDFFF
private const val LAST_CODE_POINT: Int = 0x10FFFF
private const val SURROGATE_BITS: Int = 10

private val widths: List<Pair<IntRange, Int>> = listOf(
    0..LAST_ONE_BYTE to 1,
    FIRST_TWO_BYTES until FIRST_THREE_BYTES to TWO,
    FIRST_THREE_BYTES until FIRST_FOUR_BYTES to THREE,
    FIRST_FOUR_BYTES..LAST_FOUR_BYTES to FOUR,
)

private val leadMasks: Map<Int, Int> =
    mapOf(1 to LAST_ONE_BYTE, TWO to TWO_BYTES_PAYLOAD, THREE to THREE_BYTES_PAYLOAD, FOUR to FOUR_BYTES_PAYLOAD)

private val smallest: Map<Int, Int> =
    mapOf(1 to 0, TWO to CONTINUATION_TAG, THREE to SMALLEST_THREE_BYTES, FOUR to SMALLEST_FOUR_BYTES)

internal fun utf8At(bytes: ByteArray, index: Int): CodePointRead {
    val lead = bytes[index].toInt() and BYTE_MASK
    val width = utf8WidthOf(lead)
    if (width == 0) return CodePointRead(codePoint = CodePointRead.INVALID, width = 1)
    return utf8Sequence(bytes = bytes, index = index, lead = lead, width = width)
}

private fun utf8WidthOf(lead: Int): Int = widths.firstOrNull { (range, _) -> lead in range }?.second ?: 0

private fun utf8Sequence(bytes: ByteArray, index: Int, lead: Int, width: Int): CodePointRead {
    if (index + width > bytes.size) return CodePointRead(codePoint = CodePointRead.TRUNCATED, width = width)
    val continuations = (1 until width).map { offset -> bytes[index + offset].toInt() and BYTE_MASK }
    val isContinued = continuations.all { byte -> byte and CONTINUATION_TAG_MASK == CONTINUATION_TAG }
    val payload = continuations.fold(lead and leadMaskOf(width)) { value, byte ->
        value shl CONTINUATION_BITS or (byte and CONTINUATION_MASK)
    }
    val isShortest = payload >= smallestOf(width)
    return checkedRead(codePoint = payload, width = width, isWellFormed = isContinued && isShortest)
}

private fun leadMaskOf(width: Int): Int = leadMasks.getValue(width)

private fun smallestOf(width: Int): Int = smallest.getValue(width)

internal fun utf16At(bytes: ByteArray, index: Int, isBigEndian: Boolean): CodePointRead {
    val high = unitAt(bytes = bytes, index = index, width = TWO, isBigEndian = isBigEndian)
    if (high < 0) return CodePointRead(codePoint = CodePointRead.TRUNCATED, width = TWO)
    return utf16From(bytes = bytes, index = index, high = high, isBigEndian = isBigEndian)
}

private fun utf16From(bytes: ByteArray, index: Int, high: Int, isBigEndian: Boolean): CodePointRead {
    val isHighSurrogate = high in FIRST_SURROGATE..LAST_HIGH_SURROGATE
    if (!isHighSurrogate) return checkedRead(codePoint = high, width = TWO, isWellFormed = true)
    return utf16Pair(bytes = bytes, index = index, high = high, isBigEndian = isBigEndian)
}

private fun utf16Pair(bytes: ByteArray, index: Int, high: Int, isBigEndian: Boolean): CodePointRead {
    val low = unitAt(bytes = bytes, index = index + TWO, width = TWO, isBigEndian = isBigEndian)
    if (low < 0) return CodePointRead(codePoint = CodePointRead.TRUNCATED, width = FOUR)
    val isPaired = low in FIRST_LOW_SURROGATE..LAST_SURROGATE
    val codePoint = SMALLEST_FOUR_BYTES + (high - FIRST_SURROGATE shl SURROGATE_BITS) + (low - FIRST_LOW_SURROGATE)
    return CodePointRead(codePoint = codePoint.takeIf { isPaired } ?: CodePointRead.INVALID, width = FOUR)
}

internal fun utf32At(bytes: ByteArray, index: Int, isBigEndian: Boolean): CodePointRead {
    val unit = unitAt(bytes = bytes, index = index, width = FOUR, isBigEndian = isBigEndian)
    if (unit < 0) return CodePointRead(codePoint = CodePointRead.TRUNCATED, width = FOUR)
    return checkedRead(codePoint = unit, width = FOUR, isWellFormed = true)
}

private fun unitAt(bytes: ByteArray, index: Int, width: Int, isBigEndian: Boolean): Int {
    if (index + width > bytes.size) return -1
    val ordered = (0 until width).map { offset -> bytes[index + offset].toInt() and BYTE_MASK }
    val significantFirst = ordered.takeIf { isBigEndian } ?: ordered.asReversed()
    val value = significantFirst.fold(0L) { unit, byte -> unit shl BYTE_BITS or byte.toLong() }
    return value.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}

private fun checkedRead(codePoint: Int, width: Int, isWellFormed: Boolean): CodePointRead {
    val isScalar = codePoint <= LAST_CODE_POINT && codePoint !in FIRST_SURROGATE..LAST_SURROGATE
    val read = codePoint.takeIf { isWellFormed && isScalar } ?: CodePointRead.INVALID
    return CodePointRead(codePoint = read, width = width)
}
