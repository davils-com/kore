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

package com.davils.kore.core.text.internal

import com.davils.kore.core.Limits
import com.davils.kore.core.text.TextRange

private const val TAB: Int = '\t'.code
private const val BACKSLASH: Int = '\\'.code
private const val QUOTE: Int = '"'.code
private const val HEX_WIDTH: Int = 4
private const val HEX_RADIX: Int = 16
private const val COMBINING_GRAPHEME_JOINER: Int = 0x034F
private const val HANGUL_CHOSEONG_FILLER: Int = 0x115F
private const val HANGUL_JUNGSEONG_FILLER: Int = 0x1160
private const val KHMER_VOWEL_INHERENT_AQ: Int = 0x17B4
private const val KHMER_VOWEL_INHERENT_AA: Int = 0x17B5
private const val FIRST_MONGOLIAN_VARIATION_SELECTOR: Int = 0x180B
private const val LAST_MONGOLIAN_VARIATION_SELECTOR: Int = 0x180F
private const val HANGUL_FILLER: Int = 0x3164
private const val FIRST_VARIATION_SELECTOR: Int = 0xFE00
private const val LAST_VARIATION_SELECTOR: Int = 0xFE0F
private const val HALFWIDTH_HANGUL_FILLER: Int = 0xFFA0
private const val FIRST_SUPPLEMENTARY_VARIATION_SELECTOR: Int = 0xE0100
private const val LAST_SUPPLEMENTARY_VARIATION_SELECTOR: Int = 0xE01EF

internal object SafeDisplay {
    const val REDACTED: String = "****"

    private val namedEscapes: Map<Int, String> = mapOf(
        '\n'.code to "\\n",
        '\r'.code to "\\r",
        TAB to "\\t",
    )

    private val invisibleCodePoints: List<IntRange> = listOf(
        COMBINING_GRAPHEME_JOINER..COMBINING_GRAPHEME_JOINER,
        HANGUL_CHOSEONG_FILLER..HANGUL_JUNGSEONG_FILLER,
        KHMER_VOWEL_INHERENT_AQ..KHMER_VOWEL_INHERENT_AA,
        FIRST_MONGOLIAN_VARIATION_SELECTOR..LAST_MONGOLIAN_VARIATION_SELECTOR,
        HANGUL_FILLER..HANGUL_FILLER,
        FIRST_VARIATION_SELECTOR..LAST_VARIATION_SELECTOR,
        HALFWIDTH_HANGUL_FILLER..HALFWIDTH_HANGUL_FILLER,
        FIRST_SUPPLEMENTARY_VARIATION_SELECTOR..LAST_SUPPLEMENTARY_VARIATION_SELECTOR,
    )

    fun text(value: CharSequence, maxLength: Int = Limits.MAX_DISPLAY_LENGTH): String {
        val rendering = render(value = value, maxLength = maxLength, style = Style.Plain, followsShown = false)
        return rendering.shown + rendering.note
    }

    fun quoted(value: CharSequence, maxLength: Int = Limits.MAX_DISPLAY_LENGTH): String {
        val rendering = render(value = value, maxLength = maxLength, style = Style.Quoted, followsShown = false)
        return "\"${rendering.shown}\"${rendering.note}"
    }

    fun lineParts(line: String, highlight: TextRange): List<String> {
        val start = line.substring(startIndex = 0, endIndex = highlight.start)
        val before = render(value = start, maxLength = Int.MAX_VALUE, style = Style.Line, followsShown = false)
        val marked = partAfter(before, line.substring(startIndex = highlight.start, endIndex = highlight.end))
        val after = partAfter(marked, line.substring(highlight.end))
        return listOf(before.shown, marked.shown, after.shown)
    }

    fun isSafe(codePoint: Int): Boolean = !isEscaped(codePoint)

    private fun partAfter(previous: Rendering, part: String): Rendering {
        val isAfterShown = previous.hasShownCharacterAtEnd
        return render(value = part, maxLength = Int.MAX_VALUE, style = Style.Plain, followsShown = isAfterShown)
    }

    private fun render(value: CharSequence, maxLength: Int, style: Style, followsShown: Boolean): Rendering {
        require(maxLength >= 0) { "The length a value is shown with cannot be negative, but was $maxLength" }
        val rendered = StringBuilder(minOf(a = value.length, b = maxLength))
        var isAttached = followsShown
        var index = 0
        while (index < value.length) {
            val codePoint = value.codePointAt(index)
            val piece = pieceOf(codePoint, style, isAttached)
            if (rendered.length + piece.length > maxLength) return Rendering(rendered.toString(), value.length - index)
            rendered.append(piece)
            isAttached = isSafe(codePoint) && piece.length == charCountOf(codePoint)
            index += charCountOf(codePoint)
        }
        return Rendering(rendered.toString(), omitted = 0, hasShownCharacterAtEnd = isAttached)
    }

    private fun pieceOf(codePoint: Int, style: Style, isAttached: Boolean): String {
        val isLooseMark = !isAttached && isCombiningMark(codePoint)
        if (isLooseMark) return unicodeEscape(codePoint)
        return style.pieces[codePoint] ?: commonPieceOf(codePoint)
    }

    private fun commonPieceOf(codePoint: Int): String {
        if (!isEscaped(codePoint)) return textOf(codePoint)
        return namedEscapes[codePoint] ?: unicodeEscape(codePoint)
    }

    private fun isEscaped(codePoint: Int): Boolean {
        if (isEscapedCategory(codePoint)) return true
        return invisibleCodePoints.any { range -> codePoint in range }
    }

    private fun unicodeEscape(codePoint: Int): String {
        val hex = codePoint.toString(HEX_RADIX).uppercase()
        return "\\u{${hex.padStart(HEX_WIDTH, '0')}}"
    }

    private class Rendering(val shown: String, val omitted: Int, val hasShownCharacterAtEnd: Boolean = false) {
        val note: String get() = "…(+$omitted chars)".takeIf { omitted > 0 }.orEmpty()
    }

    private enum class Style(val pieces: Map<Int, String>) {
        Plain(pieces = emptyMap()),
        Line(pieces = mapOf(TAB to "\t")),
        Quoted(pieces = mapOf(BACKSLASH to "\\\\", QUOTE to "\\\"")),
    }
}
