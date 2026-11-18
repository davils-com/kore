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

private const val TAB: Int = '\t'.code
private const val HEX: Int = 16
private const val HEX_DIGITS: Int = 4
private const val SUPPLEMENTARY_START: Int = 0x10000
private const val HIGH_SURROGATE_START: Int = 0xD800
private const val LOW_SURROGATE_START: Int = 0xDC00
private const val LAST_SURROGATE: Int = 0xDFFF
private const val SURROGATE_BITS: Int = 10
private const val RANGE_SEPARATOR: String = ".."
private const val DEFAULT_IGNORABLE_CODE_POINTS: String = "00AD 034F 061C 115F..1160 17B4..17B5 180B..180F " +
    "200B..200F 202A..202E 2060..206F 3164 FE00..FE0F FEFF FFA0 FFF0..FFF8 1BCA0..1BCA3 1D173..1D17A E0000..E0FFF"
private const val VARIATION_SELECTORS: String = "180B..180D 180F FE00..FE0F E0100..E01EF"

private val invisibleCodePoints: List<IntRange> =
    codePointRangesOf(DEFAULT_IGNORABLE_CODE_POINTS) + codePointRangesOf(VARIATION_SELECTORS)

internal fun isUnsafeToShow(codePoint: Int, allowTabs: Boolean): Boolean {
    val isAllowedTab = allowTabs && codePoint == TAB
    if (isAllowedTab) return false
    val isInvisible = invisibleCodePoints.any { range -> codePoint in range }
    val isSurrogate = codePoint in HIGH_SURROGATE_START..LAST_SURROGATE
    return isInvisible || isSurrogate || hasUnsafeCategory(codePoint)
}

internal fun codePointsOf(text: String): List<Int> = buildList {
    var index = 0
    while (index < text.length) {
        val codePoint = codePointAt(text, index)
        add(codePoint)
        index += charCountOf(codePoint)
    }
}

internal fun notationOf(codePoint: Int): String {
    val hex = codePoint.toString(HEX).uppercase()
    return "U+" + hex.padStart(HEX_DIGITS, '0')
}

private fun charCountOf(codePoint: Int): Int {
    if (codePoint >= SUPPLEMENTARY_START) return 2
    return 1
}

private fun codePointAt(text: String, index: Int): Int {
    val high = text[index]
    val low = text.getOrNull(index + 1)
    if (low == null || !high.isHighSurrogate() || !low.isLowSurrogate()) return high.code
    val upper = high.code - HIGH_SURROGATE_START shl SURROGATE_BITS
    return SUPPLEMENTARY_START + upper + (low.code - LOW_SURROGATE_START)
}

private fun codePointRangesOf(listing: String): List<IntRange> = listing.split(" ").map { entry ->
    val bounds = entry.split(RANGE_SEPARATOR).map { bound -> bound.toInt(HEX) }
    bounds.first()..bounds.last()
}
