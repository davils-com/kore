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

package com.davils.kore.core.principal

import com.davils.kore.core.limit.KoreLimits
import com.davils.kore.core.text.escape.SafeDisplay

private const val COMBINING_GRAPHEME_JOINER: Int = 0x034F
private const val HANGUL_CHOSEONG_FILLER: Int = 0x115F
private const val HANGUL_JUNGSEONG_FILLER: Int = 0x1160
private const val KHMER_VOWEL_INHERENT_AQ: Int = 0x17B4
private const val KHMER_VOWEL_INHERENT_AA: Int = 0x17B5
private const val MONGOLIAN_FREE_VARIATION_SELECTOR_ONE: Int = 0x180B
private const val MONGOLIAN_FREE_VARIATION_SELECTOR_TWO: Int = 0x180C
private const val MONGOLIAN_FREE_VARIATION_SELECTOR_THREE: Int = 0x180D
private const val MONGOLIAN_FREE_VARIATION_SELECTOR_FOUR: Int = 0x180F
private const val HANGUL_FILLER: Int = 0x3164
private const val HALFWIDTH_HANGUL_FILLER: Int = 0xFFA0
private const val BRAILLE_PATTERN_BLANK: Char = '⠀'

private val problemsInCheckOrder: List<TextProblem> = listOf(
    TextProblem.TooLong,
    TextProblem.Blank,
    TextProblem.UnsafeCharacter,
    TextProblem.SurroundingWhitespace,
)

private val invisibleCodePoints: Set<Int> = setOf(
    COMBINING_GRAPHEME_JOINER,
    HANGUL_CHOSEONG_FILLER,
    HANGUL_JUNGSEONG_FILLER,
    KHMER_VOWEL_INHERENT_AQ,
    KHMER_VOWEL_INHERENT_AA,
    MONGOLIAN_FREE_VARIATION_SELECTOR_ONE,
    MONGOLIAN_FREE_VARIATION_SELECTOR_TWO,
    MONGOLIAN_FREE_VARIATION_SELECTOR_THREE,
    MONGOLIAN_FREE_VARIATION_SELECTOR_FOUR,
    HANGUL_FILLER,
    HALFWIDTH_HANGUL_FILLER,
)

internal enum class PrincipalTextRule(
    val maxLength: Int,
    private val isBlankAllowed: Boolean,
    private val isTrimRequired: Boolean,
) {
    Id(maxLength = KoreLimits.MAX_PRINCIPAL_ID_LENGTH, isBlankAllowed = false, isTrimRequired = true),
    Role(maxLength = KoreLimits.MAX_PRINCIPAL_TEXT_LENGTH, isBlankAllowed = false, isTrimRequired = true),
    Tenant(maxLength = KoreLimits.MAX_PRINCIPAL_TEXT_LENGTH, isBlankAllowed = false, isTrimRequired = true),
    DisplayName(maxLength = KoreLimits.MAX_PRINCIPAL_TEXT_LENGTH, isBlankAllowed = false, isTrimRequired = false),
    AttributeName(maxLength = KoreLimits.MAX_ATTRIBUTE_NAME_LENGTH, isBlankAllowed = false, isTrimRequired = false),
    AttributeValue(maxLength = KoreLimits.MAX_ATTRIBUTE_VALUE_LENGTH, isBlankAllowed = true, isTrimRequired = false),
    ;

    fun problemOf(value: String): TextProblem? =
        problemsInCheckOrder.firstOrNull { problem -> hasProblem(value, problem) }

    private fun hasProblem(value: String, problem: TextProblem): Boolean = when (problem) {
        TextProblem.TooLong -> value.length > maxLength
        TextProblem.Blank -> !isBlankAllowed && value.all(::isBlankCharacter)
        TextProblem.UnsafeCharacter -> !isSafe(value)
        TextProblem.SurroundingWhitespace -> isTrimRequired && hasSurroundingWhitespace(value)
    }
}

private fun isSafe(value: String): Boolean = value.codePoints().allMatch(::isShowable)

private fun isShowable(codePoint: Int): Boolean = SafeDisplay.isSafe(codePoint) && codePoint !in invisibleCodePoints

private fun isBlankCharacter(character: Char): Boolean = character.isWhitespace() || character == BRAILLE_PATTERN_BLANK

private fun hasSurroundingWhitespace(value: String): Boolean {
    val first = value.first()
    val last = value.last()
    return isBlankCharacter(first) || isBlankCharacter(last)
}
