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

package com.davils.kore.core.naming

import com.davils.kore.core.text.isAsciiDigit

internal class NameGrammar(
    private val start: NameStart,
    val separators: String,
    val segmentSeparator: Char?,
    val minSegments: Int,
    private val isStrict: Boolean,
) {
    val isDigitStartAllowed: Boolean get() = start == NameStart.LetterOrDigit

    fun problemOf(text: CharSequence, maxLength: Int): NameProblem? {
        val shapeProblem = shapeProblemOf(text, maxLength)
        if (shapeProblem != null) return shapeProblem
        return segmentsProblemOf(text)
    }

    private fun shapeProblemOf(text: CharSequence, maxLength: Int): NameProblem? {
        if (text.isEmpty()) return NameProblem.Empty
        return NameProblem.TooLong.takeIf { text.length > maxLength }
    }

    private fun segmentsProblemOf(text: CharSequence): NameProblem? {
        var segments = 1
        var segmentStart = 0
        var problem: NameProblem? = null
        var index = 0
        while (problem == null && index <= text.length) {
            val isEnd = index == text.length
            val isBoundary = isEnd || text[index] == segmentSeparator
            if (isBoundary) {
                problem = segmentProblemOf(text = text, from = segmentStart, until = index)
                if (!isEnd) segments += 1
                segmentStart = index + 1
            }
            index += 1
        }
        return problem ?: NameProblem.TooFewSegments.takeIf { segments < minSegments }
    }

    private fun segmentProblemOf(text: CharSequence, from: Int, until: Int): NameProblem? {
        if (from == until) return NameProblem.EmptySegment
        val startProblem = NameProblem.InvalidStart.takeUnless { isStart(text[from]) }
        return startProblem ?: bodyProblemOf(text = text, from = from + 1, until = until)
    }

    private fun bodyProblemOf(text: CharSequence, from: Int, until: Int): NameProblem? {
        var problem: NameProblem? = null
        var isAfterSeparator = false
        var index = from
        while (problem == null && index < until) {
            val character = text[index]
            val isSeparator = character in separators
            problem = characterProblemOf(
                character = character,
                isSeparator = isSeparator,
                isAfterSeparator = isAfterSeparator,
            )
            isAfterSeparator = isSeparator
            index += 1
        }
        val isEndingOnSeparator = isStrict && isAfterSeparator
        return problem ?: NameProblem.TrailingSeparator.takeIf { isEndingOnSeparator }
    }

    private fun characterProblemOf(character: Char, isSeparator: Boolean, isAfterSeparator: Boolean): NameProblem? {
        val isAllowed = isSeparator || isLowerLetterOrDigit(character)
        if (!isAllowed) return NameProblem.InvalidCharacter
        return NameProblem.DoubledSeparator.takeIf { isStrict && isSeparator && isAfterSeparator }
    }

    private fun isStart(character: Char): Boolean {
        val isAllowedDigit = isDigitStartAllowed && isAsciiDigit(character)
        return isLowerLetter(character) || isAllowedDigit
    }
}

private fun isLowerLetter(character: Char): Boolean = character in 'a'..'z'

private fun isLowerLetterOrDigit(character: Char): Boolean = isLowerLetter(character) || isAsciiDigit(character)
