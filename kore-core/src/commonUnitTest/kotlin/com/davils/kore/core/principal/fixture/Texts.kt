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

package com.davils.kore.core.principal.fixture

import com.davils.kore.core.text.fixture.hostileText
import com.davils.kore.core.text.fixture.unsafeCodePointsIn
import com.davils.kore.core.text.internal.textOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.list

private const val LONGEST_MIDDLE: Int = 32
private const val HEXADECIMAL: Int = 16
private const val DEFAULT_IGNORABLE_RANGES: String =
    "00AD 034F 061C 115F-1160 17B4-17B5 180B-180F 200B-200F 202A-202E 2060-206F 3164 FE00-FE0F FEFF FFA0 " +
        "FFF0-FFF8 1BCA0-1BCA3 1D173-1D17A E0000-E0FFF"

internal const val BRAILLE_PATTERN_BLANK: String = "\u2800"
internal const val HANGUL_FILLER: String = "\u3164"
internal const val COMBINING_GRAPHEME_JOINER: String = "\u034F"
internal const val MONGOLIAN_FREE_VARIATION_SELECTOR: String = "\u180B"

private val visibleFragments: List<String> = listOf(
    "a", "Z", "7", ".", "-", "_", "|", "+", "@", ":", "=", ",", "{", "}", "(", ")", "[", "]", "\"", "\\", "ä", "日",
    "😀",
)

private val edges: Arb<String> = Arb.element(visibleFragments)

private val middles: Arb<List<String>> = Arb.list(Arb.element(visibleFragments + " "), 0..LONGEST_MIDDLE)

internal val visibleText: Arb<String> = Arb.bind(edges, middles, edges) { first, middle, last ->
    first + middle.joinToString(separator = "") + last
}

internal val unsafeText: Arb<String> = hostileText.filter { text -> unsafeCodePointsIn(text).isNotEmpty() }

internal val defaultIgnorableCodePoints: List<Int> =
    DEFAULT_IGNORABLE_RANGES.split(" ").flatMap(::codePointsOf)

private fun codePointsOf(range: String): List<Int> {
    val bounds = range.split("-").map { bound -> bound.toInt(HEXADECIMAL) }
    return (bounds.first()..bounds.last()).toList()
}

internal val textWithAnInvisibleCharacter: Arb<String> =
    Arb.bind(visibleText, Arb.element(defaultIgnorableCodePoints), visibleText) { before, codePoint, after ->
        before + textOf(codePoint) + after
    }
