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

package com.davils.kore.core.version.grammar

private const val SPACE: Char = ' '
private const val TAB: Char = '\t'

internal fun isRangeSpace(character: Char): Boolean = character == SPACE || character == TAB

internal fun trimmedRangeSpace(text: String): String = text.trim(::isRangeSpace)

internal fun skipRangeSpace(text: String, from: Int): Int {
    var index = from
    while (index < text.length && isRangeSpace(text[index])) {
        index++
    }
    return index
}

internal fun rangeTokens(text: String): List<String> {
    val pieces = text.split(SPACE, TAB)
    return pieces.filter(String::isNotEmpty)
}
