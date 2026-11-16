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

package com.davils.kore.core.arguments.declaration

import com.davils.kore.core.arguments.conversion.isAsciiLetterOrDigit
import com.davils.kore.core.text.escape.SafeDisplay

internal const val LONG_PREFIX: String = "--"
internal const val SHORT_PREFIX: String = "-"

private const val NEGATION_INFIX: String = "no-"
private const val SHORT_NAME_LENGTH: Int = 2
private const val DASH: Char = '-'
private const val UNDERSCORE: Char = '_'

internal fun checkedNames(names: Array<out String>): List<String> = checkedNames(names.asList())

internal fun checkedNames(names: List<String>): List<String> {
    val declared = readOnlyCopy(names)
    require(declared.isNotEmpty()) { "A parameter needs at least one name" }
    declared.forEach(::checkName)
    return declared
}

internal fun negatedNamesOf(names: List<String>, isNegatable: Boolean): List<String> {
    if (!isNegatable) return emptyList()
    val longNames = names.filter { name -> name.startsWith(LONG_PREFIX) }
    return longNames.map { name -> LONG_PREFIX + NEGATION_INFIX + name.removePrefix(LONG_PREFIX) }
}

internal fun <T> readOnlyCopy(source: Collection<T>): List<T> = buildList(source.size) { addAll(source) }

private fun checkName(name: String) {
    require(name.startsWith(SHORT_PREFIX)) { "Parameter name ${SafeDisplay.quoted(name)} must start with a dash" }
    if (name.startsWith(LONG_PREFIX)) {
        checkLongName(name)
        return
    }
    require(name.length == SHORT_NAME_LENGTH) {
        "Short parameter name ${SafeDisplay.quoted(name)} must be exactly one character, or start with '--'"
    }
    require(isAsciiLetterOrDigit(name.last())) {
        "Short parameter name ${SafeDisplay.quoted(name)} must be an ASCII letter or digit after its dash"
    }
}

private fun checkLongName(name: String) {
    val body = name.removePrefix(LONG_PREFIX)
    require(body.isNotEmpty()) { "Parameter name ${SafeDisplay.quoted(name)} has no name after its dashes" }
    require(isAsciiLetterOrDigit(body.first()) && body.all(::isLongNameCharacter)) {
        "Long parameter name ${SafeDisplay.quoted(name)} must continue with an ASCII letter or digit after its " +
            "dashes, followed by ASCII letters, digits, '-' or '_'"
    }
}

private fun isLongNameCharacter(character: Char): Boolean =
    isAsciiLetterOrDigit(character) || character == DASH || character == UNDERSCORE
