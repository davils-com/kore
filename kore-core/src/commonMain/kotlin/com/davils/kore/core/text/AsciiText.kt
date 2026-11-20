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

package com.davils.kore.core.text

private const val CASE_DISTANCE: Int = 'a' - 'A'
private const val FIRST_PRINTABLE: Char = ' '
private const val LAST_PRINTABLE: Char = '~'

internal fun isAsciiDigit(character: Char): Boolean = character in '0'..'9'

internal fun isAsciiLetter(character: Char): Boolean = character in 'a'..'z' || character in 'A'..'Z'

internal fun isAsciiLetterOrDigit(character: Char): Boolean = isAsciiLetter(character) || isAsciiDigit(character)

internal fun isPrintableAscii(character: Char): Boolean = character in FIRST_PRINTABLE..LAST_PRINTABLE

internal fun asciiLowercase(text: String): String = buildString(text.length) {
    text.forEach { character -> append(asciiLowercase(character)) }
}

internal fun asciiUppercase(text: String): String = buildString(text.length) {
    text.forEach { character -> append(asciiUppercase(character)) }
}

internal fun asciiLowercase(character: Char): Char {
    if (character !in 'A'..'Z') return character
    return character + CASE_DISTANCE
}

internal fun asciiUppercase(character: Char): Char {
    if (character !in 'a'..'z') return character
    return character - CASE_DISTANCE
}
