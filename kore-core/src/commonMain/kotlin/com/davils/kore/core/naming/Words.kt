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

import com.davils.kore.core.Limits
import com.davils.kore.core.annotation.DavilsExperimental
import com.davils.kore.core.immutableList
import com.davils.kore.core.text.asciiLowercase
import com.davils.kore.core.text.asciiUppercase
import com.davils.kore.core.text.isAsciiDigit

private const val WORD_SEPARATORS: String = "-_. "
private const val NO_LETTER: Char = ' '

/**
 * Splits [text] into its words, in lower case, such as `HTTPServerError2` into `http`, `server` and `error2`.
 *
 * A word ends at a dash, an underscore, a dot or a space, before an upper-case letter that follows a lower-case
 * letter, also across digits, and before the last upper-case letter of an acronym that a lower-case letter follows,
 * as in `HTTPServer`. Digits stay with the word before them, so `version2Beta` splits into `version2` and `beta`, but
 * `HTTP2SERVER` stays one word. Only ASCII letters are told apart by case and lowered: every other character is kept
 * as it is and never ends a word, so the result is the same on every platform and in every locale. Empty words, such
 * as those between two separators, are dropped.
 *
 * @param text the text to split.
 * @return the words in order, in lower case, as a list that no caller can change; empty for a text without words.
 * @throws IllegalArgumentException if [text] is longer than [Limits.MAX_WORDS_TEXT_LENGTH] UTF-16 code units; it is
 * refused before it is read.
 * @since 2.1.0
 */
@DavilsExperimental
public fun words(text: String): List<String> {
    require(text.length <= Limits.MAX_WORDS_TEXT_LENGTH) {
        "Words are split from at most ${Limits.MAX_WORDS_TEXT_LENGTH} characters, but the text has ${text.length}"
    }
    val found = mutableListOf<String>()
    val current = StringBuilder()
    for (index in text.indices) {
        val character = text[index]
        val isSeparator = character in WORD_SEPARATORS
        val isWordEnd = isSeparator || startsWord(text = text, index = index)
        if (isWordEnd && current.isNotEmpty()) found += asciiLowercase(current.toString())
        if (isWordEnd) current.clear()
        if (!isSeparator) current.append(character)
    }
    if (current.isNotEmpty()) found += asciiLowercase(current.toString())
    return found.immutableList()
}

/**
 * Writes this text in [style], such as `max-retry-count` as `maxRetryCount`.
 *
 * The text is split as [words] splits it, so a text in any style can be written in any other, and only ASCII letters
 * change case.
 *
 * @receiver the text to write in another style.
 * @param style the style to write it in.
 * @return the text in [style]; empty for a text without words.
 * @throws IllegalArgumentException if this text is longer than [Limits.MAX_WORDS_TEXT_LENGTH] UTF-16 code units.
 * @since 2.1.0
 */
@DavilsExperimental
public fun String.toCase(style: CaseStyle): String {
    val parts = words(this)
    return when (style) {
        CaseStyle.Camel -> camel(parts)
        CaseStyle.Pascal -> parts.joinToString(separator = "", transform = ::capitalised)
        CaseStyle.Kebab -> parts.joinToString(separator = "-")
        CaseStyle.Snake -> parts.joinToString(separator = "_")
        CaseStyle.ScreamingSnake -> asciiUppercase(parts.joinToString(separator = "_"))
        CaseStyle.Words -> parts.joinToString(separator = " ")
    }
}

private fun startsWord(text: String, index: Int): Boolean {
    val previous = text.getOrNull(index - 1)
    val next = text.getOrNull(index + 1)
    val isUpper = isAsciiUpper(text[index])
    val isAfterLower = isAsciiLower(lastLetterBefore(text = text, index = index))
    val isAcronymEnd = previous != null && isAsciiUpper(previous) && next != null && isAsciiLower(next)
    return isUpper && (isAfterLower || isAcronymEnd)
}

private fun lastLetterBefore(text: String, index: Int): Char {
    val before = text.substring(startIndex = 0, endIndex = index)
    return before.trimEnd(::isAsciiDigit).lastOrNull() ?: NO_LETTER
}

private fun camel(parts: List<String>): String {
    val written = parts.mapIndexed { index, word -> capitalisedAfterFirst(index = index, word = word) }
    return written.joinToString(separator = "")
}

private fun capitalisedAfterFirst(index: Int, word: String): String {
    if (index == 0) return word
    return capitalised(word)
}

private fun capitalised(word: String): String = asciiUppercase(word.take(1)) + word.drop(1)

private fun isAsciiUpper(character: Char): Boolean = character in 'A'..'Z'

private fun isAsciiLower(character: Char): Boolean = character in 'a'..'z'
